package com.example.prototyp.deckBuilder

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.R
import com.example.prototyp.deckBuilder.DeckDao
import com.example.yourapp.data.db.AppDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DeckOverviewFragment : Fragment(R.layout.fragment_deck_overview) {

    private val viewModel: DeckViewModel by viewModels {
        DeckViewModelFactory(AppDatabase.getInstance(requireContext()).deckDao())
    }
    private lateinit var deckAdapter: DeckAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvDecks = view.findViewById<RecyclerView>(R.id.rvDecks)
        val fabAddDeck = view.findViewById<FloatingActionButton>(R.id.fabAddDeck)
        val fabImportDeck = view.findViewById<FloatingActionButton>(R.id.fabImportDeck)

        fabImportDeck.setOnClickListener {
            showImportDeckDialog()
        }

        // Adapter initialisieren und an den RecyclerView hängen
        deckAdapter = DeckAdapter(
            onDeckClick = { deck ->
                // Navigation zur Detailansicht (bleibt gleich)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, DeckDetailFragment.newInstance(deck.id))
                    .addToBackStack(null)
                    .commit()
            },
            onDeckLongClick = { deck ->
                // Ruft den neuen Bestätigungs-Dialog auf
                showDeleteConfirmationDialog(deck)
            }
        )
        rvDecks.adapter = deckAdapter

        rvDecks.layoutManager = GridLayoutManager(requireContext(), 2)

        // ViewModel beobachten und die Liste im Adapter aktualisieren
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allDecks.collectLatest { decks ->
                deckAdapter.submitList(decks)
            }
        }

        // Listener für den "Hinzufügen"-Button
        fabAddDeck.setOnClickListener {
            showCreateDeckDialog()
        }
    }

    private fun showCreateDeckDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_deck, null)
        val etDeckName = dialogView.findViewById<EditText>(R.id.etDeckName)
        val colorPaletteContainer = dialogView.findViewById<LinearLayout>(R.id.colorPaletteContainer)

        val colors = listOf(
            "#F44336", // Rot
            "#E91E63", // Pink
            "#9C27B0", // Lila
            "#2196F3", // Blau
            "#009688", // Türkis
            "#4CAF50", // Grün
            "#FFC107", // Gelb
            "#FF9800", // Orange
            "#607D8B"  // Grau
        )

        var selectedColorHex = colors.first()
        var selectedChip: View? = null

        // Erstelle die Farb-Punkte dynamisch
        colors.forEach { colorHex ->
            val chip = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (40 * resources.displayMetrics.density).toInt(), // 40dp in Pixel
                    (40 * resources.displayMetrics.density).toInt()
                ).apply {
                    marginEnd = (8 * resources.displayMetrics.density).toInt() // 8dp Margin
                }

                // Setze das Selector-Drawable als Hintergrund
                background = ContextCompat.getDrawable(requireContext(), R.drawable.color_chip_selector)
                // Färbe den Hintergrund mit der echten Farbe
                backgroundTintList = ColorStateList.valueOf(Color.parseColor(colorHex))

                // Klick-Listener für die Auswahl
                setOnClickListener {
                    selectedChip?.isSelected = false // Alte Auswahl deselektieren
                    it.isSelected = true // Neue Auswahl markieren
                    selectedChip = it // Neue Auswahl merken
                    selectedColorHex = colorHex // Farbe merken
                }
            }

            // Markiere den ersten Farb-Punkt als vorausgewählt
            if (colorHex == selectedColorHex) {
                chip.isSelected = true
                selectedChip = chip
            }

            colorPaletteContainer.addView(chip)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Neues Deck erstellen")
            .setView(dialogView)
            .setPositiveButton("Speichern") { _, _ ->
                val name = etDeckName.text.toString()
                if (name.isNotBlank()) {
                    viewModel.createDeck(name, selectedColorHex)
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun showImportDeckDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_import_deck, null)
        val etDeckName = dialogView.findViewById<EditText>(R.id.etDeckName)
        val etImportText = dialogView.findViewById<EditText>(R.id.etImportText)
        val colorPaletteContainer = dialogView.findViewById<LinearLayout>(R.id.colorPaletteContainer)

        // ##### HINZUGEFÜGT: Die komplette Logik für die Farb-Chips #####
        val colors = listOf(
            "#F44336", "#E91E63", "#9C27B0", "#2196F3", "#009688",
            "#4CAF50", "#FFC107", "#FF9800", "#607D8B"
        )

        var selectedColorHex = colors.first()
        var selectedChip: View? = null

        colors.forEach { colorHex ->
            val chip = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (40 * resources.displayMetrics.density).toInt(),
                    (40 * resources.displayMetrics.density).toInt()
                ).apply {
                    marginEnd = (8 * resources.displayMetrics.density).toInt()
                }
                background = ContextCompat.getDrawable(requireContext(), R.drawable.color_chip_selector)
                backgroundTintList = ColorStateList.valueOf(Color.parseColor(colorHex))

                setOnClickListener {
                    selectedChip?.isSelected = false
                    it.isSelected = true
                    selectedChip = it
                    selectedColorHex = colorHex
                }
            }
            if (colorHex == selectedColorHex) {
                chip.isSelected = true
                selectedChip = chip
            }
            colorPaletteContainer.addView(chip)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Deck aus Text importieren")
            .setView(dialogView)
            .setPositiveButton("Importieren") { _, _ ->
                val name = etDeckName.text.toString()
                val text = etImportText.text.toString()

                if (name.isNotBlank() && text.isNotBlank()) {
                    // ##### GEÄNDERT: Verwende die ausgewählte Farbe #####
                    viewModel.importDeckFromText(name, selectedColorHex, text)
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(deck: Deck) {
        AlertDialog.Builder(requireContext())
            .setTitle("Deck löschen")
            .setMessage("Möchtest du das Deck '${deck.name}' wirklich endgültig löschen?")
            .setPositiveButton("Löschen") { _, _ ->
                // Ruft die Funktion im ViewModel auf, um das Deck zu löschen
                viewModel.deleteDeck(deck)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }
}

// Die Factory-Klasse, die das DAO an das ViewModel übergibt
class DeckViewModelFactory(private val deckDao: DeckDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeckViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeckViewModel(deckDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}