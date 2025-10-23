package com.example.prototyp.wishlist

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
import com.example.prototyp.R
import com.example.prototyp.deckBuilder.MasterCardSearchAdapter
import com.example.prototyp.databinding.FragmentAddCardWithFiltersBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import com.example.prototyp.deckBuilder.MasterCardWithQuantity // NEU
import com.example.prototyp.data.db.CardDao // NEU
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class AddCardToWishlistFragment : Fragment() {

    private var _binding: FragmentAddCardWithFiltersBinding? = null
    private val binding get() = _binding!!


    private val viewModel: WishlistViewModel by activityViewModels {
        val db = AppDatabase.getInstance(requireContext())
        WishlistViewModelFactory(db.wishlistDao(), db.masterCardDao(),db.cardDao())
    }

    // NEU: Direkter DAO-Zugriff für Anzahlen
    private val cardDao: CardDao by lazy {
        AppDatabase.getInstance(requireContext()).cardDao()
    }
    private val wishlistDao: WishlistDao by lazy {
        AppDatabase.getInstance(requireContext()).wishlistDao()
    }

    private lateinit var searchAdapter: MasterCardSearchAdapter
    private val colorMap = mapOf("R" to "Rot", "B" to "Blau", "G" to "Grün", "Y" to "Gelb", "P" to "Lila", "O" to "Orange", "U" to "Grau", "M" to "Mehrfarbig")

    private val searchQuery = MutableStateFlow("")
    private val colorFilter = MutableStateFlow("")
    private val setFilter = MutableStateFlow("")


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddCardWithFiltersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = "Karte zur Wunschliste hinzufügen"

        setupAdapter()
        setupFilters()
        observeFilteredResults()
    }

    private fun setupAdapter() {
        // NEU: Initialisiert den neuen Adapter
        searchAdapter = MasterCardSearchAdapter { card ->
            viewModel.addCardToWishlist(card)
            Toast.makeText(requireContext(), "'${card.cardName}' zur Wunschliste hinzugefügt", Toast.LENGTH_SHORT).show()

            // NEU: Trigger ein Re-Query, um die "Pille" sofort zu aktualisieren
            triggerSearchRefresh()
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
            // Farb-Spinner füllen
            val colorsFromDb = viewModel.getFilterColors()
            val colorItems = colorsFromDb.map { colorMap[it] ?: it }.toMutableList().apply { add(0, "Alle Farben") }
            binding.spinnerColor.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, colorItems)
            binding.spinnerColor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    colorFilter.value = if (position == 0) "" else colorsFromDb[position - 1]
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            // Set-Spinner füllen
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
                Triple(query, color, set) // NEU: Triple erstellen
            }.collectLatest { (query, color, set) -> // NEU: Triple destrukturieren
                if (query.isBlank() && color.isBlank() && set.isBlank()) {
                    searchAdapter.submitList(emptyList()) // Leere Liste, wenn keine Filter aktiv sind
                } else {
                    // NEU: Logik zur Anreicherung der Daten
                    val resultsWithQuantity = withContext(Dispatchers.IO) {
                        val results = viewModel.searchMasterCards(query, color, set)
                        results.map { masterCard ->
                            val collectionQty = cardDao.getByKey(masterCard.setCode, masterCard.cardNumber)?.quantity ?: 0
                            val wishlistQty = wishlistDao.getByKey(masterCard.setCode, masterCard.cardNumber)?.quantity ?: 0

                            MasterCardWithQuantity(
                                masterCard = masterCard,
                                collectionQuantity = collectionQty,
                                wishlistQuantity = wishlistQty
                            )
                        }
                    }
                    searchAdapter.submitList(resultsWithQuantity)
                }
            }
        }
    }

    // NEU: Hilfsfunktion, um die Suche neu anzustoßen
    private fun triggerSearchRefresh() {
        val currentQuery = searchQuery.value
        if (currentQuery.isBlank()) {
            searchQuery.value = " "
            searchQuery.value = ""
        } else {
            searchQuery.value = currentQuery
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

