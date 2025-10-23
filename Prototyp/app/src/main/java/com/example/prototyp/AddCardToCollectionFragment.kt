package com.example.prototyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prototyp.deckBuilder.MasterCardSearchAdapter
import com.example.prototyp.databinding.FragmentAddCardWithFiltersBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import com.example.prototyp.deckBuilder.MasterCardWithQuantity // NEU
import com.example.prototyp.data.db.CardDao // NEU
import com.example.prototyp.wishlist.WishlistDao // NEU
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddCardToCollectionFragment : Fragment() {

    private var _binding: FragmentAddCardWithFiltersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CollectionViewModel by activityViewModels {
        CollectionViewModelFactory(
            AppDatabase.getInstance(requireContext()).cardDao(),
            AppDatabase.getInstance(requireContext()).masterCardDao(),
            AppDatabase.getInstance(requireContext()).priceHistoryDao(),
            AppDatabase.getInstance(requireContext()).totalValueHistoryDao()
        )
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

    // StateFlows für die reaktiven Filter
    private val searchQuery = MutableStateFlow("")
    private val colorFilter = MutableStateFlow("")
    private val setFilter = MutableStateFlow("")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddCardWithFiltersBinding.inflate(inflater, container, false)
        binding.toolbar.title = "Karte zur Sammlung hinzufügen"
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupFilterListeners()
        observeCombinedFilters()
    }

    private fun setupAdapter() {
        // NEU: Initialisiert den neuen Adapter
        searchAdapter = MasterCardSearchAdapter { card ->
            viewModel.addCardToCollection(card)
            Toast.makeText(requireContext(), "'${card.cardName}' zur Sammlung hinzugefügt", Toast.LENGTH_SHORT).show()

            // NEU: Trigger ein Re-Query, um die "Pille" sofort zu aktualisieren
            triggerSearchRefresh()
        }
        binding.rvMasterCards.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMasterCards.adapter = searchAdapter
    }


    private fun setupFilterListeners() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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

    private fun observeCombinedFilters() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(searchQuery.debounce(300), colorFilter, setFilter) { query, color, set ->
                Triple(query, color, set)
            }.collectLatest { (query, color, set) ->
                // Nur suchen, wenn mindestens ein Filter aktiv ist
                if (query.isNotBlank() || color.isNotBlank() || set.isNotBlank()) {

                    // NEU: Führe die DB-Abfragen im IO-Dispatcher aus
                    val resultsWithQuantity = withContext(Dispatchers.IO) {
                        // 1. Master-Karten suchen
                        val results = viewModel.searchMasterCards(query, color, set)

                        // 2. Ergebnisse mappen und mit Anzahlen anreichern
                        results.map { masterCard ->
                            val collectionQty = cardDao.getByKey(masterCard.setCode, masterCard.cardNumber)?.quantity ?: 0
                            val wishlistQty = wishlistDao.getByKey(masterCard.setCode, masterCard.cardNumber)?.quantity ?: 0 // Annahme: wishlistDao.getByKey existiert

                            MasterCardWithQuantity(
                                masterCard = masterCard,
                                collectionQuantity = collectionQty,
                                wishlistQuantity = wishlistQty
                            )
                        }
                    }
                    // 3. Aktualisiere den Adapter im Main-Thread
                    searchAdapter.submitList(resultsWithQuantity)
                } else {
                    searchAdapter.submitList(emptyList()) // Liste leeren
                }
            }
        }
    }

    // NEU: Hilfsfunktion, um die Suche neu anzustoßen
    private fun triggerSearchRefresh() {
        val currentQuery = searchQuery.value
        if (currentQuery.isBlank()) {
            // Falls die Suche leer war, müssen wir sie "flackern" lassen
            searchQuery.value = " "
            searchQuery.value = ""
        } else {
            // Ansonsten reicht es, den Wert neu zu setzen
            searchQuery.value = currentQuery
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

