package com.example.prototyp.deckBuilder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.MasterCard
import com.example.prototyp.R
import com.google.android.material.card.MaterialCardView

data class MasterCardWithQuantity(
    val masterCard: MasterCard,
    val collectionQuantity: Int,
    val wishlistQuantity: Int
)

class MasterCardSearchAdapter(
    private val onAddClick: (MasterCard) -> Unit
) : ListAdapter<MasterCardWithQuantity, MasterCardSearchAdapter.ViewHolder>(MasterCardDiffCallback()) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.tvCardName)
        val setText: TextView = itemView.findViewById(R.id.tvCardSet)
        val addButton: Button = itemView.findViewById(R.id.btnAdd)
        val numberText: TextView = itemView.findViewById(R.id.tvCardNumber)
        val tvCollectionQty: TextView = itemView.findViewById(R.id.tvCollectionQuantity)
        val tvWishlistQty: TextView = itemView.findViewById(R.id.tvWishlistQuantity)

        fun bind(item: MasterCardWithQuantity, onAddClick: (MasterCard) -> Unit) {
            val card = item.masterCard
            nameText.text = card.cardName
            setText.text = card.setName
            numberText.text = "#${card.cardNumber.toString().padStart(3, '0')}"
            addButton.setOnClickListener { onAddClick(card) }

            // Logik für "In Sammlung"-Pille
            if (item.collectionQuantity > 0) {
                tvCollectionQty.text = "In Sammlung: ${item.collectionQuantity}"
                tvCollectionQty.isVisible = true
            } else {
                tvCollectionQty.isVisible = false
            }

            // Logik für "Wunschliste"-Pille
            if (item.wishlistQuantity > 0) {
                tvWishlistQty.text = "Wunschliste: ${item.wishlistQuantity}"
                tvWishlistQty.isVisible = true
            } else {
                tvWishlistQty.isVisible = false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_master_card_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onAddClick)
        applyCardBackground(holder.itemView as MaterialCardView, item.masterCard.color)
    }

    private fun applyCardBackground(view: View, colorCode: String?) {
        val context = view.context
        when (colorCode?.trim()?.uppercase()) {
            "M" -> view.setBackgroundResource(R.drawable.rainbow_gradient)
            "R" -> view.setBackgroundColor(ContextCompat.getColor(context, R.color.card_red))
            "B" -> view.setBackgroundColor(ContextCompat.getColor(context, R.color.card_blue))
            "G" -> view.setBackgroundColor(ContextCompat.getColor(context, R.color.card_green))
            "Y" -> view.setBackgroundColor(ContextCompat.getColor(context, R.color.card_yellow))
            "P" -> view.setBackgroundColor(ContextCompat.getColor(context, R.color.card_purple))
            "O" -> view.setBackgroundColor(ContextCompat.getColor(context, R.color.card_orange))
            "U" -> view.setBackgroundColor(ContextCompat.getColor(context, R.color.card_grey))
            else -> view.setBackgroundColor(ContextCompat.getColor(context, R.color.card_grey))
        }
    }
}

class MasterCardDiffCallback : DiffUtil.ItemCallback<MasterCardWithQuantity>() {
    override fun areItemsTheSame(oldItem: MasterCardWithQuantity, newItem: MasterCardWithQuantity): Boolean {
        return oldItem.masterCard.setCode == newItem.masterCard.setCode &&
                oldItem.masterCard.cardNumber == newItem.masterCard.cardNumber
    }

    override fun areContentsTheSame(oldItem: MasterCardWithQuantity, newItem: MasterCardWithQuantity): Boolean {
        return oldItem == newItem
    }
}

