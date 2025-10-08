package com.example.prototyp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.data.db.CardDao

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

        applyCardBackground(h.itemView, row.color)
    }

    private fun applyCardBackground(view: View, colorCode: String?) {
        val context = view.context
        when (colorCode?.trim()?.uppercase()) {
            "M" -> {
                view.setBackgroundResource(R.drawable.rainbow_gradient)
            }

            "R" -> view.setBackgroundColor(ContextCompat.getColor(context, R.color.card_red))
            "B" -> view.setBackgroundColor(ContextCompat.getColor(context, R.color.card_blue))
            "G" -> view.setBackgroundColor(ContextCompat.getColor(context, R.color.card_green))
            "Y" -> view.setBackgroundColor(ContextCompat.getColor(context, R.color.card_yellow))
            "P" -> view.setBackgroundColor(ContextCompat.getColor(context, R.color.card_purple))
            "O" -> view.setBackgroundColor(ContextCompat.getColor(context, R.color.card_orange))
            "U" -> view.setBackgroundColor(ContextCompat.getColor(context, R.color.card_grey))
            else -> view.setBackgroundColor(ContextCompat.getColor(context, R.color.card_grey)) // Fallback
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