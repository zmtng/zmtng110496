package com.example.prototyp.deckBuilder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.R
import com.example.prototyp.deckBuilder.DeckDao

class DeckCardAdapter(
    private val onIncrement: (DeckDao.DeckCardDetail) -> Unit,
    private val onDecrement: (DeckDao.DeckCardDetail) -> Unit
) : ListAdapter<DeckDao.DeckCardDetail, DeckCardAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val statusIcon: ImageView = itemView.findViewById(R.id.ivStatus)
        private val nameText: TextView = itemView.findViewById(R.id.tvCardName)
        private val setText: TextView = itemView.findViewById(R.id.tvCardSet)
        private val quantityText: TextView = itemView.findViewById(R.id.tvQuantity)
        // HINZUGEFÜGT: Die neuen Buttons finden
        val decrementButton: ImageButton = itemView.findViewById(R.id.btnDecrement)
        val incrementButton: ImageButton = itemView.findViewById(R.id.btnIncrement)

        // Die bind-Funktion wird erweitert, um die Listener zu empfangen
        fun bind(
            card: DeckDao.DeckCardDetail,
            onIncrement: (DeckDao.DeckCardDetail) -> Unit,
            onDecrement: (DeckDao.DeckCardDetail) -> Unit
        ) {
            nameText.text = card.cardName
            setText.text = card.setName
            quantityText.text = "x${card.quantity}"

            if (card.inCollection) {
                statusIcon.setImageResource(R.drawable.ic_check_circle)
            } else {
                statusIcon.setImageResource(R.drawable.ic_close_circle)
            }

            // Die Listener an die Buttons binden
            decrementButton.setOnClickListener { onDecrement(card) }
            incrementButton.setOnClickListener { onIncrement(card) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_deck_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onIncrement, onDecrement)
    }

    class DiffCallback : DiffUtil.ItemCallback<DeckDao.DeckCardDetail>() {
        override fun areItemsTheSame(old: DeckDao.DeckCardDetail, new: DeckDao.DeckCardDetail) =
            old.setCode == new.setCode && old.cardNumber == new.cardNumber
        override fun areContentsTheSame(old: DeckDao.DeckCardDetail, new: DeckDao.DeckCardDetail) =
            old == new
    }
}