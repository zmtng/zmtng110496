package com.example.prototyp.externalWishlist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.R
import com.example.prototyp.databinding.ItemExternalCollectionCardBinding // Layout wiederverwenden

class ExternalWishlistCardAdapter(
    private val onAddToWishlistClick: (ExternalWishlistDao.CardDetail) -> Unit
) : ListAdapter<ExternalWishlistDao.CardDetail, ExternalWishlistCardAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(private val binding: ItemExternalCollectionCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            card: ExternalWishlistDao.CardDetail,
            onAddToWishlistClick: (ExternalWishlistDao.CardDetail) -> Unit
        ) {
            binding.tvCardName.text = card.cardName
            binding.tvCardSet.text = "${card.setName} - #${card.cardNumber.toString().padStart(3, '0')}"
            binding.tvQuantity.text = "Gesucht: x${card.quantity}"

            binding.badgeInCollection.isVisible = card.inOwnCollection
            binding.badgeOnWishlist.isVisible = card.onOwnWishlist
            binding.btnAddToWishlist.isVisible = !card.onOwnWishlist

            binding.btnAddToWishlist.setOnClickListener {
                onAddToWishlistClick(card)
                it.isVisible = false // Button ausblenden für sofortiges Feedback
                binding.badgeOnWishlist.isVisible = true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExternalCollectionCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val card = getItem(position)
        holder.bind(card, onAddToWishlistClick)
        applyCardBackground(holder.itemView, card.color)
    }

    private fun applyCardBackground(view: View, colorCode: String?) {
        val cardView = view as? com.google.android.material.card.MaterialCardView ?: return
        val context = view.context
        val colorRes = when (colorCode?.trim()?.uppercase()) {
            "M" -> R.drawable.rainbow_gradient
            "R" -> R.color.card_red
            "B" -> R.color.card_blue
            "G" -> R.color.card_green
            "Y" -> R.color.card_yellow
            "P" -> R.color.card_purple
            "O" -> R.color.card_orange
            else -> R.color.card_grey
        }
        if(colorCode == "M") cardView.setBackgroundResource(colorRes)
        else cardView.setCardBackgroundColor(ContextCompat.getColor(context, colorRes))
    }

    class DiffCallback : DiffUtil.ItemCallback<ExternalWishlistDao.CardDetail>() {
        override fun areItemsTheSame(old: ExternalWishlistDao.CardDetail, new: ExternalWishlistDao.CardDetail) =
            old.setCode == new.setCode && old.cardNumber == new.cardNumber
        override fun areContentsTheSame(old: ExternalWishlistDao.CardDetail, new: ExternalWishlistDao.CardDetail) = old == new
    }
}