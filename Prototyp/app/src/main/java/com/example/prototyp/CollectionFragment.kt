package com.example.prototyp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.data.db.CardDao
import com.example.yourapp.data.db.AppDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CollectionFragment : Fragment(R.layout.fragment_collection) {

    // ##### ENTFERNT: Direkter DAO-Zugriff und manuelle Caches #####
    // private lateinit var cardDao: CardDao
    // ... und die ganzen Map-Variablen ...

    // ##### HINZUGEFÜGT: ViewModel für die gesamte Logik und Zustandsverwaltung #####
    private val viewModel: CollectionViewModel by viewModels {
        CollectionViewModelFactory(AppDatabase.getInstance(requireContext()).cardDao())
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CardAdapter // Dein Adapter muss an 'CollectionRowData' angepasst werden

    // ##### STARK GEÄNDERT: onViewCreated ist jetzt viel schlanker #####
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // UI-Elemente initialisieren
        recyclerView = view.findViewById(R.id.recyclerView)
        val spinner = view.findViewById<Spinner>(R.id.spinnerSort)

        // WICHTIGER HINWEIS: Dein CardAdapter und deine CollectionRow-Klasse müssen
        // so angepasst werden, dass sie mit `CardDao.CollectionRowData` arbeiten können.
        adapter = CardAdapter(
            onIncrement = { row: CardDao.CollectionRowData ->
                // Die Logik hier bleibt ähnlich, greift aber auf das ViewModel zu
                viewModel.incrementQuantity(row)
            },
            onDecrement = { row: CardDao.CollectionRowData ->
                viewModel.decrementQuantity(row)
            },
            onItemClick = { row: CardDao.CollectionRowData ->
                showEditNotesDialog(row)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // "+ Karte" → AddCardFragment öffnen
        view.findViewById<FloatingActionButton>(R.id.btnAddCard).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddCardFragment())
                .addToBackStack(null)
                .commit()
        }

        // ##### HINZUGEFÜGT: Logik zur Einrichtung des Sortier-Spinners #####
        setupSortSpinner(spinner)

        // ##### NEU: Beobachten der Daten aus dem ViewModel #####
        // Dieser Block ersetzt die alte, komplexe Lade-Logik.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.collection.collectLatest { collectionList ->
                    // Der Adapter wird jedes Mal aktualisiert, wenn die sortierte Liste sich ändert
                    adapter.submitList(collectionList) // <-- Richtiger Name
                }
            }
        }
    }

    // ##### HINZUGEFÜGT: Komplette Funktion zur Einrichtung des Spinners #####
    private fun setupSortSpinner(spinner: Spinner) {
        val sortOptions = listOf("Nach Name", "Nach Nummer", "Nach Farbe")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newSortOrder = when (position) {
                    0 -> SortOrder.BY_NAME
                    1 -> SortOrder.BY_NUMBER
                    2 -> SortOrder.BY_COLOR
                    else -> SortOrder.BY_NUMBER
                }
                viewModel.setSortOrder(newSortOrder)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // ##### ANGEPASST: Arbeitet jetzt mit dem neuen 'CollectionRowData'-Objekt #####
    private fun showEditNotesDialog(row: CardDao.CollectionRowData) { // <-- ÄNDERUNG 1: Parameter-Typ angepasst
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_notes, null)

        val cardNameTextView = dialogView.findViewById<TextView>(R.id.dialogCardName)
        val cardDetailsTextView = dialogView.findViewById<TextView>(R.id.dialogCardDetails)
        val cardValueCountTextView = dialogView.findViewById<TextView>(R.id.dialogCardValueCount)
        val personalNotesEditText = dialogView.findViewById<EditText>(R.id.editTextPersonalNotes)
        val generalNotesEditText = dialogView.findViewById<EditText>(R.id.editTextGeneralNotes)
        val colorIndicator = dialogView.findViewById<View>(R.id.colorIndicator)

        // ##### ÄNDERUNG 2: Feldnamen an CollectionRowData angepasst #####
        cardNameTextView.text = "Kartenname: ${row.cardName}" // .name -> .cardName
        cardDetailsTextView.text = "Set: ${row.setName} | Nummer: ${row.cardNumber}" // .number -> .cardNumber
        cardValueCountTextView.text = "Wert: ${row.price ?: 0.00}€ | Anzahl: ${row.quantity}"
        personalNotesEditText.setText(row.personalNotes)
        generalNotesEditText.setText(row.generalNotes)

        val colorCode = row.color?.trim()?.uppercase()
        if (colorCode == "M") {
            // Fall 1: Mehrfarbig -> Setze den Gradient als Hintergrund für den Indikator
            colorIndicator.setBackgroundResource(R.drawable.rainbow_gradient)
        } else {
            // Fall 2: Einfarbig -> Töte den Kreis mit der entsprechenden Farbe
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
            // Wichtig: 'setTint' wird nur für die soliden Farben verwendet.
            colorIndicator.background.setTint(ContextCompat.getColor(requireContext(), colorRes))
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Speichern") { dialog, _ ->
                val newPersonalNotes = personalNotesEditText.text.toString()
                val newGeneralNotes = generalNotesEditText.text.toString()

                // Greift jetzt auf die ViewModel-Funktion zu
                viewModel.updateNotes(
                    setCode = row.setCode,
                    cardNumber = row.cardNumber,
                    personalNotes = newPersonalNotes,
                    generalNotes = newGeneralNotes
                )
                // Das Schließen des Dialogs passiert jetzt am besten im ViewModel oder hier
                // nach einer Erfolgs-Rückmeldung, aber für den Moment ist das okay.
            }
            .setNegativeButton("Abbrechen") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }
}

// ##### HINZUGEFÜGT: ViewModelFactory für die Erstellung des ViewModels #####
// Diese Klasse kannst du entweder hier unten oder in eine eigene Datei packen.
class CollectionViewModelFactory(private val cardDao: CardDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CollectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CollectionViewModel(cardDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}