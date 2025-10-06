package com.example.prototyp.deckBuilder

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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

        // Adapter initialisieren und an den RecyclerView hängen
        deckAdapter = DeckAdapter { deck ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, DeckDetailFragment.newInstance(deck.id))
                .addToBackStack(null)
                .commit()
        }
        rvDecks.adapter = deckAdapter

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
        val etDeckColor = dialogView.findViewById<EditText>(R.id.etDeckColor)

        AlertDialog.Builder(requireContext())
            .setTitle("Neues Deck erstellen")
            .setView(dialogView)
            .setPositiveButton("Speichern") { _, _ ->
                val name = etDeckName.text.toString()
                val color = etDeckColor.text.toString()
                if (name.isNotBlank()) {
                    viewModel.createDeck(name, color)
                }
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