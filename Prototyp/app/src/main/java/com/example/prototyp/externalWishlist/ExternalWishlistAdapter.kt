package com.example.prototyp.externalWishlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.R

// Kopie von ExternalCollectionAdapter, angepasst für ExternalWishlist
class ExternalWishlistAdapter(
    private val onClick: (ExternalWishlist) -> Unit,
    private val onLongClick: (ExternalWishlist) -> Unit
) : ListAdapter<ExternalWishlist, ExternalWishlistAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.collectionName)

        fun bind(
            wishlist: ExternalWishlist,
            onClick: (ExternalWishlist) -> Unit,
            onLongClick: (ExternalWishlist) -> Unit
        ) {
            nameTextView.text = wishlist.name
            itemView.setOnClickListener { onClick(wishlist) }
            itemView.setOnLongClickListener {
                onLongClick(wishlist)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_external_collection, parent, false) // Layout wiederverwenden
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick, onLongClick)
    }

    class DiffCallback : DiffUtil.ItemCallback<ExternalWishlist>() {
        override fun areItemsTheSame(oldItem: ExternalWishlist, newItem: ExternalWishlist): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: ExternalWishlist, newItem: ExternalWishlist): Boolean {
            return oldItem == newItem
        }
    }
}