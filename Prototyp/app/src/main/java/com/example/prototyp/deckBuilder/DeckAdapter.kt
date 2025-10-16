package com.example.prototyp.deckBuilder

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.R

class DeckAdapter(
    private val onDeckClick: (Deck) -> Unit,
    private val onDeckLongClick: (Deck) -> Unit
) : ListAdapter<DeckDao.DeckWithCardCount, DeckAdapter.DeckViewHolder>(DeckDiffCallback()) { // MODIFIED: ListAdapter now uses DeckWithCardCount

    class DeckViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.deckName)
        private val cardCountTextView: TextView = itemView.findViewById(R.id.deckCardCount)

        fun bind(
            deckWithCount: DeckDao.DeckWithCardCount,
            onDeckClick: (Deck) -> Unit,
            onDeckLongClick: (Deck) -> Unit
        ) {
            val deck = deckWithCount.deck
            nameTextView.text = deck.name
            cardCountTextView.text = "${deckWithCount.cardCount} Karten"

            itemView.setOnClickListener { onDeckClick(deck) }
            itemView.setOnLongClickListener {
                onDeckLongClick(deck)
                true
            }

            try {
                val color = Color.parseColor(deck.colorHex)
                (itemView as? com.google.android.material.card.MaterialCardView)?.setCardBackgroundColor(color)
            } catch (e: IllegalArgumentException) {
                // Fallback for invalid hex code
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_deck, parent, false)
        return DeckViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeckViewHolder, position: Int) {
        holder.bind(getItem(position), onDeckClick, onDeckLongClick)
    }
}

class DeckDiffCallback : DiffUtil.ItemCallback<DeckDao.DeckWithCardCount>() {
    override fun areItemsTheSame(oldItem: DeckDao.DeckWithCardCount, newItem: DeckDao.DeckWithCardCount): Boolean {
        return oldItem.deck.id == newItem.deck.id
    }

    override fun areContentsTheSame(oldItem: DeckDao.DeckWithCardCount, newItem: DeckDao.DeckWithCardCount): Boolean {
        return oldItem == newItem
    }
}

