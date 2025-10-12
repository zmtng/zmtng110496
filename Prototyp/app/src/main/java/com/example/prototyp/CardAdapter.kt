package com.example.prototyp

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
import com.example.prototyp.data.db.CardDao
import com.google.android.material.card.MaterialCardView

class CardAdapter(
    private val onIncrement: (CardDao.CollectionRowData) -> Unit,
    private val onDecrement: (CardDao.CollectionRowData) -> Unit,
    private val onItemClick: (CardDao.CollectionRowData) -> Unit,
    private val onLongClick: (CardDao.CollectionRowData) -> Unit
) : ListAdapter<CardDao.CollectionRowData, CardAdapter.VH>(CardDiffCallback()) {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.cardName)
        val tvNumber: TextView = itemView.findViewById(R.id.cardNumber)
        val tvSet: TextView = itemView.findViewById(R.id.cardSet)
        val tvQuantity: TextView = itemView.findViewById(R.id.cardAnzahl)
        val tvPrice: TextView = itemView.findViewById(R.id.cardPreis)
        val btnIncrement: ImageButton = itemView.findViewById(R.id.btnIncrement)
        val btnDecrement: ImageButton = itemView.findViewById(R.id.btnDecrement)
        // Reference to the new ImageView
        val gradientBackground: ImageView = itemView.findViewById(R.id.gradient_background)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val row = getItem(position)

        h.tvName.text = row.cardName
        h.tvNumber.text = "Nr. ${row.cardNumber}"
        h.tvSet.text = row.setName
        h.tvQuantity.text = "x${row.quantity}"
        h.tvPrice.text = row.price?.let { String.format("%.2f €", it) } ?: "–"

        h.btnIncrement.setOnClickListener { onIncrement(row) }
        h.btnDecrement.setOnClickListener { onDecrement(row) }
        h.itemView.setOnClickListener { onItemClick(row) }
        h.itemView.setOnLongClickListener {
            onLongClick(row)
            true
        }

        applyCardBackground(h.itemView as MaterialCardView, h.gradientBackground, row.color)
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


class CardDiffCallback : DiffUtil.ItemCallback<CardDao.CollectionRowData>() {
    override fun areItemsTheSame(oldItem: CardDao.CollectionRowData, newItem: CardDao.CollectionRowData): Boolean {
        return oldItem.setCode == newItem.setCode && oldItem.cardNumber == newItem.cardNumber
    }

    override fun areContentsTheSame(oldItem: CardDao.CollectionRowData, newItem: CardDao.CollectionRowData): Boolean {
        return oldItem == newItem
    }
}

