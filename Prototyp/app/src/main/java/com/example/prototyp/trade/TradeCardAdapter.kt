package com.example.prototyp.trade

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.databinding.ItemTradeCardBinding

// Der Konstruktor ist jetzt leer
class TradeCardAdapter : ListAdapter<TradeDao.TradeCard, TradeCardAdapter.ViewHolder>(DiffCallback()) {

    // Die Vorlage ist jetzt eine öffentliche Variable
    var quantityTextTemplate: String = ""

    class ViewHolder(private val binding: ItemTradeCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: TradeDao.TradeCard, template: String) {
            binding.tvCardName.text = card.cardName
            binding.tvCardSet.text = "${card.setName} - #${card.cardNumber.toString().padStart(3, '0')}"
            binding.tvTradeQuantities.text = String.format(template, card.yourQuantity, card.theirQuantity)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTradeCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Hier wird jetzt die öffentliche Variable der Klasse verwendet
        holder.bind(getItem(position), quantityTextTemplate)
    }

    class DiffCallback : DiffUtil.ItemCallback<TradeDao.TradeCard>() {
        override fun areItemsTheSame(old: TradeDao.TradeCard, new: TradeDao.TradeCard) =
            old.setCode == new.setCode && old.cardNumber == new.cardNumber
        override fun areContentsTheSame(old: TradeDao.TradeCard, new: TradeDao.TradeCard) = old == new
    }
}