package com.example.prototyp.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.data.db.CardDao
import com.example.prototyp.databinding.ItemValuableCardBinding

class ValuableCardAdapter : ListAdapter<CardDao.CollectionRowData, ValuableCardAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(private val binding: ItemValuableCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: CardDao.CollectionRowData, position: Int) {
            binding.tvRank.text = "#${position + 1}"
            binding.tvCardName.text = card.cardName
            binding.tvSetName.text = card.setName
            binding.tvCardValue.text = String.format("%.2f €", card.price)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemValuableCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class DiffCallback : DiffUtil.ItemCallback<CardDao.CollectionRowData>() {
        override fun areItemsTheSame(oldItem: CardDao.CollectionRowData, newItem: CardDao.CollectionRowData): Boolean {
            return oldItem.setCode == newItem.setCode && oldItem.cardNumber == newItem.cardNumber
        }

        override fun areContentsTheSame(oldItem: CardDao.CollectionRowData, newItem: CardDao.CollectionRowData): Boolean {
            return oldItem == newItem
        }
    }
}
