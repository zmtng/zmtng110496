package com.example.prototyp.externalCollection

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.databinding.ItemExternalCollectionCardBinding
import com.example.prototyp.R

class ExternalCollectionCardAdapter(
    private val onAddToWishlistClick: (ExternalCollectionDao.CardDetail) -> Unit
) : ListAdapter<ExternalCollectionDao.CardDetail, ExternalCollectionCardAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(private val binding: ItemExternalCollectionCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            card: ExternalCollectionDao.CardDetail,
            onAddToWishlistClick: (ExternalCollectionDao.CardDetail) -> Unit
        ) {
            binding.tvCardName.text = card.cardName
            binding.tvCardSet.text = "${card.setName} - #${card.cardNumber.toString().padStart(3, '0')}"
            binding.tvQuantity.text = "Anzahl: x${card.quantity}"

            binding.badgeInCollection.isVisible = card.inOwnCollection
            binding.badgeOnWishlist.isVisible = card.onOwnWishlist

            binding.btnAddToWishlist.isVisible = !card.onOwnWishlist

            binding.btnAddToWishlist.setOnClickListener {
                onAddToWishlistClick(card)

                it.isVisible = false
                binding.badgeOnWishlist.isVisible = true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExternalCollectionCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val card = getItem(position)
        holder.bind(card, onAddToWishlistClick)
        applyCardBackground(holder.itemView, card.color)
    }

    class DiffCallback : DiffUtil.ItemCallback<ExternalCollectionDao.CardDetail>() {
        override fun areItemsTheSame(oldItem: ExternalCollectionDao.CardDetail, newItem: ExternalCollectionDao.CardDetail): Boolean {
            return oldItem.setCode == newItem.setCode && oldItem.cardNumber == newItem.cardNumber
        }

        override fun areContentsTheSame(oldItem: ExternalCollectionDao.CardDetail, newItem: ExternalCollectionDao.CardDetail): Boolean {
            return oldItem == newItem
        }
    }

    private fun applyCardBackground(view: View, colorCode: String?) {
        val cardView = view as? com.google.android.material.card.MaterialCardView ?: return
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