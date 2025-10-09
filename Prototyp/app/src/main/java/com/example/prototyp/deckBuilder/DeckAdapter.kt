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
) : ListAdapter<Deck, DeckAdapter.DeckViewHolder>(DeckDiffCallback()) {

    class DeckViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.deckName)

        fun bind(
            deck: Deck,
            onDeckClick: (Deck) -> Unit,
            onDeckLongClick: (Deck) -> Unit
        ) {
            nameTextView.text = deck.name
            itemView.setOnClickListener { onDeckClick(deck) }

            itemView.setOnLongClickListener {
                onDeckLongClick(deck)
                true
            }

            try {
                val color = Color.parseColor(deck.colorHex)
                (itemView as? com.google.android.material.card.MaterialCardView)?.setCardBackgroundColor(color)
            } catch (e: IllegalArgumentException) {
                // Fallback, falls der Hex-Code ungültig ist
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

class DeckDiffCallback : DiffUtil.ItemCallback<Deck>() {
    override fun areItemsTheSame(oldItem: Deck, newItem: Deck): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Deck, newItem: Deck): Boolean {
        return oldItem == newItem
    }
}