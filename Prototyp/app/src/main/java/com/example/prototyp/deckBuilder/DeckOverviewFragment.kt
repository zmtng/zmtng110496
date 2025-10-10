package com.example.prototyp.deckBuilder

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
import com.example.prototyp.MasterCardDao
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DeckOverviewFragment : Fragment(R.layout.fragment_deck_overview) {

    private val viewModel: DeckViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        DeckViewModelFactory(db.deckDao(), db.masterCardDao())
    }
    private lateinit var deckAdapter: DeckAdapter

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

        val onColorSelected = setupColorPalette(colorPaletteContainer)

        AlertDialog.Builder(requireContext())
            .setTitle("Neues Deck erstellen")
            .setView(dialogView)
            .setPositiveButton("Speichern") { _, _ ->
                val name = etDeckName.text.toString()
                if (name.isNotBlank()) {
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

        val onColorSelected = setupColorPalette(colorPaletteContainer)

        AlertDialog.Builder(requireContext())
            .setTitle("Deck aus Text importieren")
            .setView(dialogView)
            .setPositiveButton("Importieren") { _, _ ->
                val name = etDeckName.text.toString()
                val text = etImportText.text.toString()

                if (name.isNotBlank() && text.isNotBlank()) {
                    viewModel.importDeckFromText(name, onColorSelected(), text)
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

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

                val colorCircle = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.parseColor(colorHex))
                }

                val selectionRing = ContextCompat.getDrawable(requireContext(), R.drawable.color_chip_selector)!!

                val layers = arrayOf(colorCircle, selectionRing)
                val layerDrawable = android.graphics.drawable.LayerDrawable(layers)

                val inset = (3 * resources.displayMetrics.density).toInt()
                layerDrawable.setLayerInset(0, inset, inset, inset, inset) // index 0 = colorCircle

                background = layerDrawable
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

class DeckViewModelFactory(private val deckDao: DeckDao, private val masterCardDao: MasterCardDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeckViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeckViewModel(deckDao, masterCardDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
