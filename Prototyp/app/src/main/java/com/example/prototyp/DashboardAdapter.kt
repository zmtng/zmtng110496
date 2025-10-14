package com.example.prototyp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.databinding.ItemDashboardCardBinding

class DashboardAdapter(
    private val onItemClick: (DashboardItem) -> Unit
) : ListAdapter<DashboardItem, DashboardAdapter.ViewHolder>(DashboardDiffCallback()) {

    class ViewHolder(private val binding: ItemDashboardCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DashboardItem, onItemClick: (DashboardItem) -> Unit) {
            binding.itemTitle.text = item.title
            binding.itemIcon.setImageResource(item.iconRes)
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDashboardCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }
}

class DashboardDiffCallback : DiffUtil.ItemCallback<DashboardItem>() {
    override fun areItemsTheSame(oldItem: DashboardItem, newItem: DashboardItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DashboardItem, newItem: DashboardItem): Boolean {
        return oldItem == newItem
    }
}