package com.example.prototyp.deckBuilder

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.R
import com.google.android.material.card.MaterialCardView

class DeckCardAdapter(
    private val onIncrement: (DeckDao.DeckCardDetail) -> Unit,
    private val onDecrement: (DeckDao.DeckCardDetail) -> Unit,
    private val onAddToWishlist: (DeckDao.DeckCardDetail) -> Unit,
    private val onLongClick: (DeckDao.DeckCardDetail) -> Unit
) : ListAdapter<DeckDao.DeckCardDetail, DeckCardAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val statusIcon: ImageView = itemView.findViewById(R.id.ivStatus)
        private val nameText: TextView = itemView.findViewById(R.id.tvCardName)
        private val setText: TextView = itemView.findViewById(R.id.tvCardSet)
        private val quantityText: TextView = itemView.findViewById(R.id.tvQuantity)
        private val priceText: TextView = itemView.findViewById(R.id.tvCardPrice) // Added this line

        val addToWishlistButton: ImageButton = itemView.findViewById(R.id.btnAddToWishlist)
        val decrementButton: ImageButton = itemView.findViewById(R.id.btnDecrement)
        val incrementButton: ImageButton = itemView.findViewById(R.id.btnIncrement)

        fun bind(
            card: DeckDao.DeckCardDetail,
            onIncrement: (DeckDao.DeckCardDetail) -> Unit,
            onDecrement: (DeckDao.DeckCardDetail) -> Unit,
            onAddToWishlist: (DeckDao.DeckCardDetail) -> Unit,
            onLongClick: (DeckDao.DeckCardDetail) -> Unit
        ) {
            nameText.text = card.cardName
            setText.text = card.setName
            quantityText.text = "x${card.quantity}"

            // Set the price text, formatted correctly
            priceText.text = card.price?.let { String.format("Preis: %.2f €", it) } ?: "Preis: –"

            if (card.inCollection) {
                statusIcon.setImageResource(R.drawable.ic_check_circle)
            } else {
                statusIcon.setImageResource(R.drawable.ic_close_circle)
            }

            addToWishlistButton.isActivated = card.onWishlist

            addToWishlistButton.setOnClickListener {
                onAddToWishlist(card)
                it.isActivated = true
            }

            itemView.setOnLongClickListener {
                onLongClick(card)
                true
            }

            decrementButton.setOnClickListener { onDecrement(card) }
            incrementButton.setOnClickListener { onIncrement(card) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_deck_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val card = getItem(position)
        holder.bind(getItem(position), onIncrement, onDecrement, onAddToWishlist, onLongClick)
        applyCardBackground(holder.itemView, card.color)
    }

    class DiffCallback : DiffUtil.ItemCallback<DeckDao.DeckCardDetail>() {
        override fun areItemsTheSame(old: DeckDao.DeckCardDetail, new: DeckDao.DeckCardDetail) =
            old.setCode == new.setCode && old.cardNumber == new.cardNumber
        override fun areContentsTheSame(old: DeckDao.DeckCardDetail, new: DeckDao.DeckCardDetail) =
            old == new
    }

    private fun applyCardBackground(view: View, colorCode: String?) {
        val cardView = view as? MaterialCardView ?: return
        val context = view.context

        when (colorCode?.trim()?.uppercase()) {
            "M" -> {
                cardView.setCardBackgroundColor(Color.TRANSPARENT)
                cardView.setBackgroundResource(R.drawable.rainbow_gradient)
            }
            "R" -> cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_red))
            "B" -> cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_blue))
            "G" -> cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_green))
            "Y" -> cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_yellow))
            "P" -> cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_purple))
            "O" -> cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_orange))
            "U" -> cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_grey))
            else -> cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_grey))
        }
    }
}
