package com.example.prototyp.wishlist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.R
import com.google.android.material.card.MaterialCardView

class WishlistAdapter(
    private val onIncrement: (WishlistDao.WishlistCard) -> Unit,
    private val onDecrement: (WishlistDao.WishlistCard) -> Unit,
    private val onMoveToCollection: (WishlistDao.WishlistCard) -> Unit,
    private val onLongClick: (WishlistDao.WishlistCard) -> Unit
) : ListAdapter<WishlistDao.WishlistCard, WishlistAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.tvCardName)
        val setText: TextView = itemView.findViewById(R.id.tvCardSet)
        val quantityText: TextView = itemView.findViewById(R.id.tvQuantity)
        val incrementButton: ImageButton = itemView.findViewById(R.id.btnIncrement)
        val decrementButton: ImageButton = itemView.findViewById(R.id.btnDecrement)
        val moveButton: Button = itemView.findViewById(R.id.btnMoveToCollection)
        val numberText: TextView = itemView.findViewById(R.id.tvCardNumber)
        // Reference to the new ImageView
        val gradientBackground: ImageView = itemView.findViewById(R.id.gradient_background)
        val collectionQuantityText: TextView = itemView.findViewById(R.id.tvCollectionQuantity)

        fun bind(
            card: WishlistDao.WishlistCard,
            onIncrement: (WishlistDao.WishlistCard) -> Unit,
            onDecrement: (WishlistDao.WishlistCard) -> Unit,
            onMoveToCollection: (WishlistDao.WishlistCard) -> Unit,
            onLongClick: (WishlistDao.WishlistCard) -> Unit
        ) {
            nameText.text = card.cardName
            setText.text = card.setName
            numberText.text = "#${card.cardNumber.toString().padStart(3, '0')}"
            quantityText.text = "x${card.quantity}"
            incrementButton.setOnClickListener { onIncrement(card) }
            decrementButton.setOnClickListener { onDecrement(card) }
            moveButton.setOnClickListener { onMoveToCollection(card) }
            itemView.setOnLongClickListener {
                onLongClick(card)
                true
            }

            collectionQuantityText.text = "Im Besitz: ${card.collectionQuantity}"
            collectionQuantityText.isVisible = card.collectionQuantity > 0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wishlist_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val card = getItem(position)
        holder.bind(card, onIncrement, onDecrement, onMoveToCollection, onLongClick)
        applyCardBackground(holder.itemView as MaterialCardView, holder.gradientBackground, card.color)
    }

    private fun applyCardBackground(cardView: MaterialCardView, gradientView: ImageView, colorCode: String?) {
        val context = cardView.context
        if (colorCode?.trim()?.uppercase() == "M") {
            gradientView.isVisible = true
            cardView.setCardBackgroundColor(Color.TRANSPARENT)
        } else {
            gradientView.isVisible = false
            val colorRes = when (colorCode?.trim()?.uppercase()) {
                "R" -> R.color.card_red
                "B" -> R.color.card_blue
                "G" -> R.color.card_green
                "Y" -> R.color.card_yellow
                "P" -> R.color.card_purple
                "O" -> R.color.card_orange
                "U" -> R.color.card_grey
                else -> R.color.card_grey
            }
            cardView.setCardBackgroundColor(ContextCompat.getColor(context, colorRes))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<WishlistDao.WishlistCard>() {
        override fun areItemsTheSame(old: WishlistDao.WishlistCard, new: WishlistDao.WishlistCard) =
            old.setCode == new.setCode && old.cardNumber == new.cardNumber
        override fun areContentsTheSame(old: WishlistDao.WishlistCard, new: WishlistDao.WishlistCard) =
            old == new
    }
}

