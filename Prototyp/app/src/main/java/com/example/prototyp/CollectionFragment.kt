package com.example.prototyp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prototyp.data.db.CardDao
import com.example.prototyp.databinding.FragmentCollectionBinding
import com.example.prototyp.statistics.PriceHistoryDao
import com.example.prototyp.statistics.TotalValueHistoryDao
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class CollectionFragment : Fragment(R.layout.fragment_collection) {

    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CollectionViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        CollectionViewModelFactory(db.cardDao(), db.masterCardDao(), db.priceHistoryDao(), db.totalValueHistoryDao() )
    }
    private lateinit var adapter: CardAdapter
    private val colorMap = mapOf("R" to "Rot", "B" to "Blau", "G" to "Grün", "Y" to "Gelb", "P" to "Lila", "O" to "Orange", "U" to "Grau", "M" to "Mehrfarbig")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupListeners()
        setupObservers()
        setupFilters()


    }

    private fun setupAdapter() {
        adapter = CardAdapter(
            onIncrement = { card -> viewModel.incrementQuantity(card) },
            onDecrement = { card -> viewModel.decrementQuantity(card) },
            onItemClick = { card -> showEditNotesDialog(card) },
            onLongClick = { showDeleteConfirmationDialog(it) }
        )
        binding.rvCards.adapter = adapter
        binding.rvCards.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        binding.btnAddCard.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddCardToCollectionFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.fabUpdatePrices.setOnClickListener {
            viewModel.fetchAllPrices()
            Toast.makeText(requireContext(), "Starte Preis-Update...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.collection.collectLatest { collectionList ->
                        adapter.submitList(collectionList)
                    }
                }
                launch {
                    viewModel.userMessage.collectLatest { message ->
                        message?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            viewModel.onUserMessageShown()
                        }
                    }
                }
            }
        }
    }

    private fun setupFilters() {
        binding.searchViewCollection.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText.orEmpty())
                return true
            }
        })

        val sortOptions = listOf("Nach Name", "Nach Nummer", "Nach Farbe")
        binding.spinnerSort.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sortOptions)
        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val sortBy = when (position) {
                    0 -> SortOrder.BY_NAME
                    1 -> SortOrder.BY_NUMBER
                    2 -> SortOrder.BY_COLOR
                    else -> SortOrder.BY_NAME
                }
                viewModel.setSortOrder(sortBy)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Farb-Spinner
            val colorsFromDb = viewModel.getFilterColors()
            val colorItemsForSpinner = colorsFromDb.map { colorMap[it] ?: it }.toMutableList().apply { add(0, "Alle Farben") }
            binding.spinnerColor.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, colorItemsForSpinner)
            binding.spinnerColor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                    val selection = if (position == 0) null else colorsFromDb[position - 1]
                    viewModel.setColorFilter(selection)
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

            val setsFromDb = viewModel.getFilterSets()
            val setItemsForSpinner = setsFromDb.toMutableList().apply { add(0, "Alle Sets") }
            binding.spinnerSet.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, setItemsForSpinner)
            binding.spinnerSet.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                    val selection = if (position == 0) null else setsFromDb[position - 1]
                    viewModel.setSetFilter(selection)
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
        }
    }
    private fun showEditNotesDialog(row: CardDao.CollectionRowData) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_notes, null)

        val cardNameTextView = dialogView.findViewById<TextView>(R.id.dialogCardName)
        val cardDetailsTextView = dialogView.findViewById<TextView>(R.id.dialogCardDetails)
        val cardValueCountTextView = dialogView.findViewById<TextView>(R.id.dialogCardValueCount)
        val personalNotesEditText = dialogView.findViewById<EditText>(R.id.editTextPersonalNotes)
        val generalNotesEditText = dialogView.findViewById<EditText>(R.id.editTextGeneralNotes)
        val colorIndicator = dialogView.findViewById<View>(R.id.colorIndicator)
        val fetchPriceButton = dialogView.findViewById<Button>(R.id.btnFetchPrice)


        fetchPriceButton.setOnClickListener {
            it.isEnabled = false
            viewModel.fetchPriceForCard(row, showSuccessMessage = true) { newPrice ->
                cardValueCountTextView.text = "Wert: ${String.format("%.2f", newPrice)}€ | Anzahl: ${row.quantity}"
                it.isEnabled = true
            }
        }

        cardNameTextView.text = "Kartenname: ${row.cardName}"
        cardDetailsTextView.text = "Set: ${row.setName} | Nummer: ${row.cardNumber}"
        cardValueCountTextView.text = "Wert: ${row.price ?: 0.00}€ | Anzahl: ${row.quantity}"
        personalNotesEditText.setText(row.personalNotes)
        generalNotesEditText.setText(row.generalNotes)

        val colorCode = row.color.trim().uppercase()
        if (colorCode == "M") {
            colorIndicator.setBackgroundResource(R.drawable.rainbow_gradient)
        } else {
            val colorRes = when (colorCode) {
                "R" -> R.color.card_red
                "B" -> R.color.card_blue
                "G" -> R.color.card_green
                "Y" -> R.color.card_yellow
                "P" -> R.color.card_purple
                "O" -> R.color.card_orange
                "U" -> R.color.card_grey
                else -> R.color.card_grey
            }
            colorIndicator.background.setTint(ContextCompat.getColor(requireContext(), colorRes))
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Speichern") { _, _ ->
                val newPersonalNotes = personalNotesEditText.text.toString()
                val newGeneralNotes = generalNotesEditText.text.toString()
                viewModel.updateNotes(
                    setCode = row.setCode,
                    cardNumber = row.cardNumber,
                    personalNotes = newPersonalNotes,
                    generalNotes = newGeneralNotes
                )
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showDeleteConfirmationDialog(card: CardDao.CollectionRowData) {
        AlertDialog.Builder(requireContext())
            .setTitle("Karte löschen")
            .setMessage("Möchtest du '${card.cardName}' wirklich aus der Sammlung entfernen?")
            .setPositiveButton("Löschen") { _, _ ->
                viewModel.deleteCard(card)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }
}

class CollectionViewModelFactory(
    private val cardDao: CardDao,
    private val masterDao: MasterCardDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val totalValueHistoryDao: TotalValueHistoryDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CollectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CollectionViewModel(cardDao, masterDao, priceHistoryDao, totalValueHistoryDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}