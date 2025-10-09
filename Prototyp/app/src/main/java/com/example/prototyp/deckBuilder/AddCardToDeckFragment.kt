package com.example.prototyp.deckBuilder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prototyp.AppDatabase
import com.example.prototyp.databinding.FragmentAddCardWithFiltersBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import com.example.prototyp.camera.*
import com.example.prototyp.R

class AddCardToDeckFragment : Fragment() {

    private var _binding: FragmentAddCardWithFiltersBinding? = null
    private val binding get() = _binding!!

    private val cameraViewModel: CameraViewModel by activityViewModels()

    private val viewModel: DeckDetailViewModel by activityViewModels {
        val db = AppDatabase.getInstance(requireContext())
        DeckDetailViewModelFactory(db.deckDao(), db.masterCardDao(), db.wishlistDao())
    }

    private lateinit var searchAdapter: MasterCardSearchAdapter
    private val colorMap = mapOf("R" to "Rot", "B" to "Blau", "G" to "Grün", "Y" to "Gelb", "P" to "Lila", "O" to "Orange", "U" to "Grau", "M" to "Mehrfarbig")

    // StateFlows für die Filter
    private val searchQuery = MutableStateFlow("")
    private val colorFilter = MutableStateFlow("")
    private val setFilter = MutableStateFlow("")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddCardWithFiltersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = "Karte zum Deck hinzufügen"

        binding.btnCameraScan.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CameraFragment())
                .addToBackStack(null)
                .commit()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            cameraViewModel.scannedText.collect { text ->
                text?.let {
                    binding.searchView.setQuery(it, true) // Füllt die Suche aus
                    cameraViewModel.consumeScannedText() // Setzt den Wert zurück
                }
            }
        }

        setupAdapter()
        setupFilters()
        observeFilteredResults()
    }

    private fun setupAdapter() {
        searchAdapter = MasterCardSearchAdapter(emptyList()) { card ->
            viewModel.addCardToDeck(card)
            Toast.makeText(requireContext(), "'${card.cardName}' zum Deck hinzugefügt", Toast.LENGTH_SHORT).show()
        }
        binding.rvMasterCards.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMasterCards.adapter = searchAdapter
    }

    private fun setupFilters() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery.value = newText.orEmpty()
                return true
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            val colorsFromDb = viewModel.getFilterColors()
            val colorItems = colorsFromDb.map { colorMap[it] ?: it }.toMutableList().apply { add(0, "Alle Farben") }
            binding.spinnerColor.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, colorItems)
            binding.spinnerColor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    colorFilter.value = if (position == 0) "" else colorsFromDb[position - 1]
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            val setsFromDb = viewModel.getFilterSets()
            val setItems = setsFromDb.toMutableList().apply { add(0, "Alle Sets") }
            binding.spinnerSet.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, setItems)
            binding.spinnerSet.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    setFilter.value = if (position == 0) "" else setsFromDb[position - 1]
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun observeFilteredResults() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(searchQuery.debounce(300), colorFilter, setFilter) { query, color, set ->
                if (query.isBlank() && color.isBlank() && set.isBlank()) {
                    emptyList() // Leere Liste, wenn keine Filter aktiv sind
                } else {
                    viewModel.searchMasterCards(query, color, set)
                }
            }.collectLatest { results ->
                searchAdapter.updateData(results)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}