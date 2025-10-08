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
import com.example.prototyp.AppDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DeckOverviewFragment : Fragment(R.layout.fragment_deck_overview) {

    private val viewModel: DeckViewModel by viewModels {
        DeckViewModelFactory(AppDatabase.getInstance(requireContext()).deckDao())
    }
    private lateinit var deckAdapter: DeckAdapter

    // Liste der Farben, die an beiden Stellen verwendet wird
    private val deckColors = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#2196F3", "#009688",
        "#4CAF50", "#FFC107", "#FF9800", "#607D8B"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvDecks = view.findViewById<RecyclerView>(R.id.rvDecks)
        val fabAddDeck = view.findViewById<FloatingActionButton>(R.id.fabAddDeck)
        val fabImportDeck = view.findViewById<FloatingActionButton>(R.id.fabImportDeck)

        fabImportDeck.setOnClickListener {
            showImportDeckDialog()
        }

        deckAdapter = DeckAdapter(
            onDeckClick = { deck ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, DeckDetailFragment.newInstance(deck.id))
                    .addToBackStack(null)
                    .commit()
            },
            onDeckLongClick = { deck ->
                showDeleteConfirmationDialog(deck)
            }
        )
        rvDecks.adapter = deckAdapter
        rvDecks.layoutManager = GridLayoutManager(requireContext(), 2)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allDecks.collectLatest { decks ->
                deckAdapter.submitList(decks)
            }
        }

        fabAddDeck.setOnClickListener {
            showCreateDeckDialog()
        }
    }

    private fun showCreateDeckDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_deck, null)
        val etDeckName = dialogView.findViewById<EditText>(R.id.etDeckName)
        val colorPaletteContainer = dialogView.findViewById<LinearLayout>(R.id.colorPaletteContainer)

        // Die neue, ausgelagerte Funktion wird hier aufgerufen
        val onColorSelected = setupColorPalette(colorPaletteContainer)

        AlertDialog.Builder(requireContext())
            .setTitle("Neues Deck erstellen")
            .setView(dialogView)
            .setPositiveButton("Speichern") { _, _ ->
                val name = etDeckName.text.toString()
                if (name.isNotBlank()) {
                    // Die ausgewählte Farbe wird von der neuen Funktion zurückgegeben
                    viewModel.createDeck(name, onColorSelected())
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

        // Die neue, ausgelagerte Funktion wird AUCH HIER aufgerufen
        val onColorSelected = setupColorPalette(colorPaletteContainer)

        AlertDialog.Builder(requireContext())
            .setTitle("Deck aus Text importieren")
            .setView(dialogView)
            .setPositiveButton("Importieren") { _, _ ->
                val name = etDeckName.text.toString()
                val text = etImportText.text.toString()

                if (name.isNotBlank() && text.isNotBlank()) {
                    // Die ausgewählte Farbe wird auch hier korrekt verwendet
                    viewModel.importDeckFromText(name, onColorSelected(), text)
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    /**
     * NEU: Eine zentrale Funktion, die die Farb-Chips erstellt und verwaltet.
     * Sie gibt eine Funktion zurück, die den aktuell ausgewählten Farb-Hex-Code liefert.
     */
    private fun setupColorPalette(container: LinearLayout): () -> String {
        var selectedColorHex = deckColors.first()
        var selectedChip: View? = null

        deckColors.forEach { colorHex ->
            val chip = ImageView(requireContext()).apply {
                val sizeInPx = (42 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(sizeInPx, sizeInPx).apply {
                    marginEnd = (8 * resources.displayMetrics.density).toInt()
                }
                isClickable = true

                // --- HIER IST DIE FINALE, ROBUSTE LÖSUNG ---

                // 1. Erstelle den farbigen Kreis als eigenes Drawable-Objekt.
                val colorCircle = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.parseColor(colorHex))
                }

                // 2. Lade unseren Auswahlring-Selektor als zweites Drawable.
                val selectionRing = ContextCompat.getDrawable(requireContext(), R.drawable.color_chip_selector)!!

                // 3. Kombiniere beide in einer LayerDrawable.
                //    Der farbige Kreis liegt unten, der Ring darüber.
                val layers = arrayOf(colorCircle, selectionRing)
                val layerDrawable = android.graphics.drawable.LayerDrawable(layers)

                // 4. Setze einen Abstand (Inset) NUR für den farbigen Kreis,
                //    damit der Ring außen Platz hat.
                val inset = (3 * resources.displayMetrics.density).toInt()
                layerDrawable.setLayerInset(0, inset, inset, inset, inset) // index 0 = colorCircle

                // 5. Setze die fertige LayerDrawable als Hintergrund.
                background = layerDrawable
                // ---

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
            container.addView(chip)
        }
        return { selectedColorHex }
    }

    private fun showDeleteConfirmationDialog(deck: Deck) {
        AlertDialog.Builder(requireContext())
            .setTitle("Deck löschen")
            .setMessage("Möchtest du das Deck '${deck.name}' wirklich endgültig löschen?")
            .setPositiveButton("Löschen") { _, _ ->
                viewModel.deleteDeck(deck)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }
}

// Die Factory-Klasse bleibt unverändert
class DeckViewModelFactory(private val deckDao: DeckDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeckViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeckViewModel(deckDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}