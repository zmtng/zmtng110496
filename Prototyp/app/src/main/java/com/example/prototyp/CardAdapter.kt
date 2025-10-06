package com.example.prototyp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CardAdapter(
    private val items: List<CollectionRow>,
    private val onIncrement: (CollectionRow) -> Unit,
    private val onDecrement: (CollectionRow) -> Unit,
    private val onItemClick: (CollectionRow) -> Unit

) : RecyclerView.Adapter<CardAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName = itemView.findViewById<TextView>(R.id.cardName)
        val tvNumber = itemView.findViewById<TextView>(R.id.cardNumber)
        val tvSet = itemView.findViewById<TextView>(R.id.cardSet)
        val tvQuantity = itemView.findViewById<TextView>(R.id.cardAnzahl)
        val tvPrice = itemView.findViewById<TextView>(R.id.cardPreis)
        val btnIncrement = itemView.findViewById<ImageButton>(R.id.btnIncrement)
        val btnDecrement = itemView.findViewById<ImageButton>(R.id.btnDecrement)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return VH(v)
    }

    private fun colorResFor(letter: String?): Int = when (letter?.trim()?.uppercase()) {
        "R" -> R.color.card_red
        "B" -> R.color.card_blue
        "G" -> R.color.card_green
        "Y" -> R.color.card_yellow
        "P" -> R.color.card_purple
        "O" -> R.color.card_orange
        else -> R.color.card_blue // Fallback
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val row = items[position]

        h.tvName.text = row.name
        h.tvNumber.text = "Nr. ${row.number}"
        h.tvSet.text = row.setCode
        h.tvQuantity.text = "x${row.quantity}"
        h.tvPrice.text = row.price?.let { String.format("%.2f €", it) } ?: "–"

        h.btnIncrement.setOnClickListener { onIncrement(row) }
        h.btnDecrement.setOnClickListener { onDecrement(row) }
        h.itemView.setOnClickListener { onItemClick(row) }

        val ctx = h.itemView.context
        val colorInt = ContextCompat.getColor(ctx, colorResFor(row.color))
        h.itemView.setBackgroundColor(colorInt)
    }

    override fun getItemCount() = items.size
}
