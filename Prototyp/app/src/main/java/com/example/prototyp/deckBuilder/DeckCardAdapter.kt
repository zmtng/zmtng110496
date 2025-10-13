package com.example.prototyp.deckBuilder

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

typealias OnQuantityChange = (DeckDao.DeckCardDetail) -> Unit
typealias OnWishlistClick = (DeckDao.DeckCardDetail) -> Unit

class DeckCardAdapter(
    private val onIncrement: (DeckDao.DeckCardDetail) -> Unit,
    private val onDecrement: (DeckDao.DeckCardDetail) -> Unit,
    private val onAddToWishlist: (DeckDao.DeckCardDetail) -> Unit,
    private val onLongClick: (DeckDao.DeckCardDetail) -> Unit
) : ListAdapter<DeckDao.DeckCardDetail, DeckCardAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val collectionQuantityText: TextView = itemView.findViewById(R.id.tvCollectionQuantity) //val statusIcon: ImageView = itemView.findViewById(R.id.ivStatus)
        val nameText: TextView = itemView.findViewById(R.id.tvCardName)
        val setText: TextView = itemView.findViewById(R.id.tvCardSet)
        val quantityText: TextView = itemView.findViewById(R.id.tvQuantity)
        val priceText: TextView = itemView.findViewById(R.id.tvCardPrice)
        val addToWishlistButton: ImageButton = itemView.findViewById(R.id.btnAddToWishlist)
        val decrementButton: ImageButton = itemView.findViewById(R.id.btnDecrement)
        val incrementButton: ImageButton = itemView.findViewById(R.id.btnIncrement)
        // Reference to the new ImageView
        val gradientBackground: ImageView = itemView.findViewById(R.id.gradient_background)

        val wishlistQuantity: TextView = itemView.findViewById(R.id.tvWishlistQuantity)

        fun bind(
            card: DeckDao.DeckCardDetail,
            onIncrement: (DeckDao.DeckCardDetail) -> Unit,
            onDecrement: (DeckDao.DeckCardDetail) -> Unit,
            onAddToWishlist: (DeckDao.DeckCardDetail) -> Unit,
            onLongClick: (DeckDao.DeckCardDetail) -> Unit
        ) {
            nameText.text = card.cardName
            setText.text = card.setName
            quantityText.text = "x${card.quantityInDeck}"
            priceText.text = card.price?.let { String.format("Preis: %.2f €", it) } ?: "Preis: –"

            wishlistQuantity.text = "Wunschliste: ${card.wishlistQuantity}"
            wishlistQuantity.isVisible = card.wishlistQuantity > 0

            collectionQuantityText.text = "Im Besitz: ${card.collectionQuantity}"
            val hasCard = card.collectionQuantity > 0
            val backgroundRes = if (hasCard) R.drawable.badge_background_green else R.drawable.badge_background_red
            collectionQuantityText.setBackgroundResource(backgroundRes)

            addToWishlistButton.isActivated = card.wishlistQuantity > 0
            addToWishlistButton.setOnClickListener {
                onAddToWishlist(card)

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
        holder.bind(card, onIncrement, onDecrement, onAddToWishlist, onLongClick)
        applyCardBackground(holder.itemView as MaterialCardView, holder.gradientBackground, card.color)
    }

    class DiffCallback : DiffUtil.ItemCallback<DeckDao.DeckCardDetail>() {
        override fun areItemsTheSame(old: DeckDao.DeckCardDetail, new: DeckDao.DeckCardDetail) =
            old.setCode == new.setCode && old.cardNumber == new.cardNumber
        override fun areContentsTheSame(old: DeckDao.DeckCardDetail, new: DeckDao.DeckCardDetail) =
            old == new
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
}

