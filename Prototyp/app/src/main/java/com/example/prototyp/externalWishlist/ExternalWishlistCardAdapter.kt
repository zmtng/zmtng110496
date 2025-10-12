package com.example.prototyp.externalWishlist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.R
import com.example.prototyp.databinding.ItemExternalCollectionCardBinding
import com.google.android.material.card.MaterialCardView

class ExternalWishlistCardAdapter(
    private val onAddToWishlistClick: (ExternalWishlistDao.CardDetail) -> Unit
) : ListAdapter<ExternalWishlistDao.CardDetail, ExternalWishlistCardAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(private val binding: ItemExternalCollectionCardBinding) : RecyclerView.ViewHolder(binding.root) {
        val gradientBackground: ImageView = binding.gradientBackground

        fun bind(
            card: ExternalWishlistDao.CardDetail,
            onAddToWishlistClick: (ExternalWishlistDao.CardDetail) -> Unit
        ) {
            binding.tvCardName.text = card.cardName
            binding.tvCardSet.text = "${card.setName} - #${card.cardNumber.toString().padStart(3, '0')}"
            binding.tvQuantity.text = "Gesucht: x${card.quantity}"

            // ## KORREKTUR HIER ##
            // The logic now uses the integer counts.
            binding.tvCollectionQuantity.text = "In Sammlung: ${card.collectionQuantity}"
            binding.tvWishlistQuantity.text = "Auf Wunschliste: ${card.wishlistQuantity}"

            binding.tvCollectionQuantity.isVisible = card.collectionQuantity > 0
            binding.tvWishlistQuantity.isVisible = card.wishlistQuantity > 0

            val isOnOwnWishlist = card.wishlistQuantity > 0
            binding.btnAddToWishlist.isVisible = !isOnOwnWishlist
            binding.btnAddToWishlist.setOnClickListener {
                onAddToWishlistClick(card)
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
        applyCardBackground(holder.itemView as MaterialCardView, holder.gradientBackground, card.color)
    }

    class DiffCallback : DiffUtil.ItemCallback<ExternalWishlistDao.CardDetail>() {
        override fun areItemsTheSame(old: ExternalWishlistDao.CardDetail, new: ExternalWishlistDao.CardDetail) =
            old.setCode == new.setCode && old.cardNumber == new.cardNumber
        override fun areContentsTheSame(old: ExternalWishlistDao.CardDetail, new: ExternalWishlistDao.CardDetail) = old == new
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

