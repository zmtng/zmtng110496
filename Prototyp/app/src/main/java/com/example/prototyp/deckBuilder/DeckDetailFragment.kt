package com.example.prototyp.deckBuilder

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.MasterCardDao
import com.example.prototyp.R
import com.example.prototyp.deckBuilder.DeckDao
import com.example.prototyp.ui.AddCardToDeckFragment
import com.example.prototyp.AppDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DeckDetailFragment : Fragment(R.layout.fragment_deck_detail) {

    private val deckId: Int by lazy { requireArguments().getInt(ARG_DECK_ID) }

    private val viewModel: DeckDetailViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        DeckDetailViewModelFactory(deckId, db.deckDao(), db.masterCardDao(), db.wishlistDao())
    }
    private lateinit var cardAdapter: DeckCardAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setDeckId(deckId)

        cardAdapter = DeckCardAdapter(
            onIncrement = { card -> viewModel.incrementCardInDeck(card) },
            onDecrement = { card -> viewModel.decrementCardInDeck(card) },
            onAddToWishlist = { card ->
                // Rufe die korrekte ViewModel-Funktion auf
                viewModel.addCardToWishlist(card)
                // Zeige dem Nutzer eine kurze Bestätigung
                Toast.makeText(requireContext(), "'${card.cardName}' zur Wunschliste hinzugefügt", Toast.LENGTH_SHORT).show()
            }
        )
        val rv = view.findViewById<RecyclerView>(R.id.rvDeckCards)
        rv.adapter = cardAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deckContents.collectLatest { cards ->
                cardAdapter.submitList(cards)
            }
        }

        view.findViewById<FloatingActionButton>(R.id.fabAddCardToDeck).setOnClickListener {
            parentFragmentManager.beginTransaction() // <-- Zurück zu parentFragmentManager
                .add(R.id.fragmentContainer, AddCardToDeckFragment()) // Verwende die ID aus der MainActivity
                .addToBackStack(null)
                .commit()
        }
    }

    companion object {
        private const val ARG_DECK_ID = "deck_id"
        fun newInstance(deckId: Int) = DeckDetailFragment().apply {
            arguments = bundleOf(ARG_DECK_ID to deckId)
        }
    }
}

