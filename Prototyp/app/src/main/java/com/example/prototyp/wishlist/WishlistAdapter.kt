package com.example.prototyp.wishlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.R
import com.example.prototyp.wishlist.WishlistDao
import com.google.android.material.card.MaterialCardView

class WishlistAdapter(
    private val onIncrement: (WishlistDao.WishlistCard) -> Unit,
    private val onDecrement: (WishlistDao.WishlistCard) -> Unit
) : ListAdapter<WishlistDao.WishlistCard, WishlistAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.tvCardName)
        val setText: TextView = itemView.findViewById(R.id.tvCardSet)
        val quantityText: TextView = itemView.findViewById(R.id.tvQuantity)
        val incrementButton: ImageButton = itemView.findViewById(R.id.btnIncrement)
        val decrementButton: ImageButton = itemView.findViewById(R.id.btnDecrement)

        fun bind(
            card: WishlistDao.WishlistCard,
            onIncrement: (WishlistDao.WishlistCard) -> Unit,
            onDecrement: (WishlistDao.WishlistCard) -> Unit
        ) {
            nameText.text = card.cardName
            setText.text = card.setName
            quantityText.text = "x${card.quantity}"
            incrementButton.setOnClickListener { onIncrement(card) }
            decrementButton.setOnClickListener { onDecrement(card) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wishlist_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val card = getItem(position)
        holder.bind(card, onIncrement, onDecrement)
        applyCardBackground(holder.itemView, card.color)
    }

    private fun applyCardBackground(view: View, colorCode: String?) {
        val cardView = view as? MaterialCardView ?: return
        val context = view.context

        when (colorCode?.trim()?.uppercase()) {
            "M" -> cardView.setBackgroundResource(R.drawable.rainbow_gradient)
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

    class DiffCallback : DiffUtil.ItemCallback<WishlistDao.WishlistCard>() {
        override fun areItemsTheSame(old: WishlistDao.WishlistCard, new: WishlistDao.WishlistCard) =
            old.setCode == new.setCode && old.cardNumber == new.cardNumber
        override fun areContentsTheSame(old: WishlistDao.WishlistCard, new: WishlistDao.WishlistCard) =
            old == new
    }
}