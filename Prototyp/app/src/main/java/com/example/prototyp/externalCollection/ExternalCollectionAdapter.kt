package com.example.prototyp.externalCollection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.R

// This is adapted from DeckAdapter.kt
class ExternalCollectionAdapter(
    private val onClick: (ExternalCollection) -> Unit,
    private val onLongClick: (ExternalCollection) -> Unit
) : ListAdapter<ExternalCollection, ExternalCollectionAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.collectionName)

        fun bind(
            collection: ExternalCollection,
            onClick: (ExternalCollection) -> Unit,
            onLongClick: (ExternalCollection) -> Unit
        ) {
            nameTextView.text = collection.name
            itemView.setOnClickListener { onClick(collection) }
            itemView.setOnLongClickListener {
                onLongClick(collection)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_external_collection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick, onLongClick)
    }

    class DiffCallback : DiffUtil.ItemCallback<ExternalCollection>() {
        override fun areItemsTheSame(oldItem: ExternalCollection, newItem: ExternalCollection): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExternalCollection, newItem: ExternalCollection): Boolean {
            return oldItem == newItem
        }
    }
}