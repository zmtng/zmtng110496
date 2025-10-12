package com.example.prototyp.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.databinding.ItemSetCompletionBinding
import com.google.android.material.progressindicator.LinearProgressIndicator

class SetCompletionAdapter : ListAdapter<SetCompletionStat, SetCompletionAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(private val binding: ItemSetCompletionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stat: SetCompletionStat) {
            binding.tvSetName.text = stat.setName
            binding.tvSetProgressText.text = "${stat.ownedUniqueCards} / ${stat.totalCardsInSet} (${String.format("%.1f", stat.percentage)}%)"
            // The method is now defined below and called correctly
            setProgressAnimate(binding.progressBar, stat.percentage.toInt())
        }

        // This helper function was missing
        private fun setProgressAnimate(pb: LinearProgressIndicator, progressTo: Int) {
            // Use setProgressCompat for a smooth animation
            pb.setProgressCompat(progressTo, true)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSetCompletionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<SetCompletionStat>() {
        override fun areItemsTheSame(oldItem: SetCompletionStat, newItem: SetCompletionStat): Boolean {
            return oldItem.setName == newItem.setName
        }

        override fun areContentsTheSame(oldItem: SetCompletionStat, newItem: SetCompletionStat): Boolean {
            return oldItem == newItem
        }
    }
}