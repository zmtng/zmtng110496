package com.example.prototyp.externalCollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prototyp.AppDatabase
import com.example.prototyp.databinding.FragmentExternalCollectionDetailBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ExternalCollectionDetailFragment : Fragment() {

    private var _binding: FragmentExternalCollectionDetailBinding? = null
    private val binding get() = _binding!!

    private val collectionId: Int by lazy { requireArguments().getInt(ARG_COLLECTION_ID) }

    private val viewModel: ExternalCollectionDetailViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        ExternalCollectionDetailViewModelFactory(
            db.externalCollectionDao(),
            db.masterCardDao(),
            db.wishlistDao()
        )
    }

    private lateinit var cardAdapter: ExternalCollectionCardAdapter
    private val colorMap = mapOf("R" to "Rot", "B" to "Blau", "G" to "Grün", "Y" to "Gelb", "P" to "Lila", "O" to "Orange", "U" to "Grau", "M" to "Mehrfarbig")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExternalCollectionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = "Sammlungs-Details"
        viewModel.setCollectionId(collectionId)

        cardAdapter = ExternalCollectionCardAdapter { card ->
            viewModel.addCardToWishlist(card)
            Toast.makeText(requireContext(), "'${card.cardName}' zur Wunschliste hinzugefügt", Toast.LENGTH_SHORT).show()
        }
        binding.rvExternalCards.adapter = cardAdapter
        binding.rvExternalCards.layoutManager = LinearLayoutManager(requireContext())

        // Die Logik zum Beobachten der Daten bleibt gleich, sie ist jetzt automatisch reaktiv
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.collectionContents.collectLatest { cards ->
                cardAdapter.submitList(cards)
            }
        }

        // NEU: Rufe die Funktion zum Einrichten der Filter auf
        setupFilters()
    }

    private fun setupFilters() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText.orEmpty())
                return true
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            // Farb-Spinner
            val colorsFromDb = viewModel.getFilterColors()
            val colorItems = colorsFromDb.map { colorMap[it] ?: it }.toMutableList().apply { add(0, "Alle Farben") }
            binding.spinnerColor.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, colorItems)
            binding.spinnerColor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    viewModel.setColorFilter(if (position == 0) "" else colorsFromDb[position - 1])
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            // Set-Spinner
            val setsFromDb = viewModel.getFilterSets()
            val setItems = setsFromDb.toMutableList().apply { add(0, "Alle Sets") }
            binding.spinnerSet.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, setItems)
            binding.spinnerSet.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    viewModel.setSetFilter(if (position == 0) "" else setsFromDb[position - 1])
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_COLLECTION_ID = "collection_id"
        fun newInstance(collectionId: Int) = ExternalCollectionDetailFragment().apply {
            arguments = bundleOf(ARG_COLLECTION_ID to collectionId)
        }
    }
}