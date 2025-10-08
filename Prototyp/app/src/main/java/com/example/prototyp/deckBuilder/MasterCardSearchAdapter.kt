package com.example.prototyp.deckBuilder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.MasterCard
import com.example.prototyp.R

class MasterCardSearchAdapter(
    private var cards: List<MasterCard>,
    private val onAddClick: (MasterCard) -> Unit
) : RecyclerView.Adapter<MasterCardSearchAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.tvCardName)
        val setText: TextView = itemView.findViewById(R.id.tvCardSet)
        val addButton: Button = itemView.findViewById(R.id.btnAdd)

        val numberText: TextView = itemView.findViewById(R.id.tvCardNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_master_card_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val card = cards[position]
        holder.nameText.text = card.cardName
        holder.setText.text = card.setName
        holder.addButton.setOnClickListener { onAddClick(card) }

        holder.numberText.text = "#${card.cardNumber.toString().padStart(3, '0')}"
        applyCardBackground(holder.itemView, card.color)
    }

    override fun getItemCount() = cards.size

    fun updateData(newCards: List<MasterCard>) {
        cards = newCards
        notifyDataSetChanged()
    }

    // ##### HINZUGEFÜGT: Die Helfer-Logik für die Farben #####
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