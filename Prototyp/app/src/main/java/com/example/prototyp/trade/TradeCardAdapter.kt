package com.example.prototyp.trade

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.databinding.ItemTradeCardBinding

class TradeCardAdapter : ListAdapter<TradeDao.TradeResult, TradeCardAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(private val binding: ItemTradeCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tradeResult: TradeDao.TradeResult, tradeType: String) {
            binding.tvCardName.text = tradeResult.cardName
            binding.tvCardSet.text = "${tradeResult.setName} - #${tradeResult.cardNumber.toString().padStart(3, '0')}"

            binding.tvTradeQuantities.text = when (tradeType) {
                "WANT" -> "Sie haben: ${tradeResult.theirQuantity} / Du willst: ${tradeResult.yourQuantity}"
                "OFFER_COLLECTION", "OFFER_DECK" -> "Sie wollen: ${tradeResult.theirQuantity} / Du hast: ${tradeResult.yourQuantity}"
                else -> ""
            }
        }
    }

    private var tradeType: String = "WANT"

    fun setTradeType(type: String) {
        tradeType = type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTradeCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), tradeType)
    }

    class DiffCallback : DiffUtil.ItemCallback<TradeDao.TradeResult>() {
        override fun areItemsTheSame(oldItem: TradeDao.TradeResult, newItem: TradeDao.TradeResult): Boolean {
            return oldItem.setCode == newItem.setCode && oldItem.cardNumber == newItem.cardNumber
        }

        override fun areContentsTheSame(oldItem: TradeDao.TradeResult, newItem: TradeDao.TradeResult): Boolean {
            return oldItem == newItem
        }
    }
}
