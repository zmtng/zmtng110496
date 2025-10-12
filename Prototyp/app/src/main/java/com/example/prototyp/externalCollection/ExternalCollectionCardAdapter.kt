package com.example.prototyp.externalCollection

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

class ExternalCollectionCardAdapter(
    private val onAddToWishlistClick: (ExternalCollectionDao.CardDetail) -> Unit
) : ListAdapter<ExternalCollectionDao.CardDetail, ExternalCollectionCardAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(private val binding: ItemExternalCollectionCardBinding) : RecyclerView.ViewHolder(binding.root) {
        // Reference to the new ImageView
        val gradientBackground: ImageView = binding.gradientBackground

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
        applyCardBackground(holder.itemView as MaterialCardView, holder.gradientBackground, card.color)
    }

    class DiffCallback : DiffUtil.ItemCallback<ExternalCollectionDao.CardDetail>() {
        override fun areItemsTheSame(oldItem: ExternalCollectionDao.CardDetail, newItem: ExternalCollectionDao.CardDetail): Boolean {
            return oldItem.setCode == newItem.setCode && oldItem.cardNumber == newItem.cardNumber
        }
        override fun areContentsTheSame(oldItem: ExternalCollectionDao.CardDetail, newItem: ExternalCollectionDao.CardDetail): Boolean {
            return oldItem == newItem
        }
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

