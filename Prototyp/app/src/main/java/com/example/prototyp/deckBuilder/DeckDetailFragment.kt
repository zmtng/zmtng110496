package com.example.prototyp.deckBuilder

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.AppDatabase
import com.example.prototyp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DeckDetailFragment : Fragment(R.layout.fragment_deck_detail) {

    private val deckId: Int by lazy { requireArguments().getInt(ARG_DECK_ID) }

    private val viewModel: DeckDetailViewModel by activityViewModels {
        val db = AppDatabase.getInstance(requireContext())
        DeckDetailViewModelFactory(db.deckDao(), db.masterCardDao(), db.wishlistDao())
    }
    private lateinit var cardAdapter: DeckCardAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setDeckId(deckId)

        cardAdapter = DeckCardAdapter(
            onIncrement = { card -> viewModel.incrementCardInDeck(card) },
            onDecrement = { card -> viewModel.decrementCardInDeck(card) },
            onAddToWishlist = { card ->
                viewModel.addCardToWishlist(card)
                Toast.makeText(requireContext(), "'${card.cardName}' zur Wunschliste hinzugefügt", Toast.LENGTH_SHORT).show()
            },
            onLongClick = { showDeleteConfirmationDialog(it) }
        )
        val rv = view.findViewById<RecyclerView>(R.id.rvDeckCards)
        rv.adapter = cardAdapter
        rv.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deckContents.collectLatest { cards ->
                cardAdapter.submitList(cards)
            }
        }

        view.findViewById<FloatingActionButton>(R.id.fabAddCardToDeck).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddCardToDeckFragment()) // Ziel bleibt gleich
                .addToBackStack(null)
                .commit()
        }
    }
    private fun showDeleteConfirmationDialog(card: DeckDao.DeckCardDetail) {
        AlertDialog.Builder(requireContext())
            .setTitle("Karte löschen")
            .setMessage("Möchtest du '${card.cardName}' wirklich aus dem Deck entfernen?")
            .setPositiveButton("Löschen") { _, _ ->
                viewModel.deleteCard(card)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    companion object {
        private const val ARG_DECK_ID = "deck_id"
        fun newInstance(deckId: Int) = DeckDetailFragment().apply {
            arguments = bundleOf(ARG_DECK_ID to deckId)
        }
    }
}
