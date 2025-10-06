package com.example.prototyp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.data.db.CardDao
import com.example.prototyp.data.db.CollectionItem
import com.example.yourapp.data.db.AppDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CollectionFragment : Fragment(R.layout.fragment_collection) {

    private lateinit var cardDao: CardDao
    private lateinit var masterDao: MasterCardDao

    private var nameByKey: Map<Pair<String, Int>, String> = emptyMap()
    private var setNameByCode: Map<String, String> = emptyMap()

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CardAdapter
    private val rows = mutableListOf<CollectionRow>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getInstance(requireContext())
        cardDao = db.cardDao()
        masterDao = db.masterCardDao()

        // RecyclerView

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = CardAdapter(
            rows,
            onIncrement = { row ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    cardDao.addQuantity(row.setCode, row.number, +1)
                }
            },
            onDecrement = { row ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    // Menge um 1 reduzieren
                    cardDao.addQuantity(row.setCode, row.number, -1)

                    // prüfen, ob 0 -> löschen
                    val updated = cardDao.getByKey(row.setCode, row.number)
                    if (updated == null || updated.quantity <= 0) {
                        cardDao.deleteByKey(row.setCode, row.number)
                    }
                }
            },
            onItemClick = { row ->
                showEditNotesDialog(row)
            }
        )
        recyclerView.adapter = adapter

        // "+ Karte" → AddCardFragment öffnen
        view.findViewById< FloatingActionButton>(R.id.btnAddCard).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddCardFragment())
                .addToBackStack(null)
                .commit()
        }


        // 1) Master-Cache laden, 2) danach Sammlung beobachten
        viewLifecycleOwner.lifecycleScope.launch {
            // 1) Masterdaten im IO-Thread aufbauen
            withContext(Dispatchers.IO) {
                val all = masterDao.search("") // LIKE '%%' → alle Masterkarten
                nameByKey = all.associate { (it.setCode to it.cardNumber) to it.cardName }
                setNameByCode = all.asSequence()
                    .map { it.setCode to it.setName }
                    .distinct()
                    .toMap()
            }

            // 2) Beobachten (läuft auf Main; Mapping nutzt den Cache)
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                cardDao.observeCollectionRaw().collectLatest { list: List<CollectionItem> ->
                    val ui = list.map { ci ->
                        val key = ci.setCode to ci.cardNumber
                        CollectionRow(
                            setCode  = ci.setCode,
                            setName  = setNameByCode[ci.setCode],       // kann null sein
                            number   = ci.cardNumber,
                            name     = nameByKey[key] ?: "(unbekannt)", // Fallback
                            quantity = ci.quantity,
                            price    = ci.price,
                            color    = ci.color,
                            personalNotes = ci.personalNotes,
                            generalNotes = ci.generalNotes
                        )
                    }
                    rows.clear()
                    rows.addAll(ui)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun showEditNotesDialog(row: CollectionRow) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_notes, null)

        // Views aus dem Dialog-Layout initialisieren
        val cardNameTextView = dialogView.findViewById<TextView>(R.id.dialogCardName)
        val cardDetailsTextView = dialogView.findViewById<TextView>(R.id.dialogCardDetails)
        val cardValueCountTextView = dialogView.findViewById<TextView>(R.id.dialogCardValueCount)
        val personalNotesEditText = dialogView.findViewById<EditText>(R.id.editTextPersonalNotes)
        val generalNotesEditText = dialogView.findViewById<EditText>(R.id.editTextGeneralNotes)
        val colorIndicator = dialogView.findViewById<View>(R.id.colorIndicator)

        // Daten der angeklickten Karte in die Views eintragen
        cardNameTextView.text = "Kartenname: ${row.name}"
        cardDetailsTextView.text = "Set: ${row.setName} | Nummer: ${row.number}"
        cardValueCountTextView.text = "Wert: ${row.price ?: 0.00} | Anzahl: ${row.quantity}"
        personalNotesEditText.setText(row.personalNotes)
        generalNotesEditText.setText(row.generalNotes)

         val colorRes = when (row.color) {
             "R" -> R.color.card_red
             "B" -> R.color.card_blue
             "G" -> R.color.card_green
             "Y" -> R.color.card_yellow
             "P" -> R.color.card_purple
             "O" -> R.color.card_orange
            else -> R.color.card_red
         }
         colorIndicator.background.setTint(ContextCompat.getColor(requireContext(), colorRes))

        // Dialog erstellen und anzeigen
        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Speichern") { dialog, _ ->
                val newPersonalNotes = personalNotesEditText.text.toString()
                val newGeneralNotes = generalNotesEditText.text.toString()

                // Starte die Coroutine für das Datenbank-Update
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    // 1. Speichere die Daten in der Datenbank
                    cardDao.updateNotes(
                        setCode = row.setCode,
                        cardNumber = row.number,

                        personalNotes = newPersonalNotes,
                        generalNotes = newGeneralNotes
                    )

                    // 2. Wechsle zum Main-Thread, um den Dialog sicher zu schließen
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                    }
                }
            }
            .setNegativeButton("Abbrechen") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }
}
