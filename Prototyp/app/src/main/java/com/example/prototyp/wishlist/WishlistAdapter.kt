package com.example.prototyp.wishlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.R
import com.example.prototyp.wishlist.WishlistDao

class WishlistAdapter : ListAdapter<WishlistDao.WishlistCard, WishlistAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.tvCardName)
        private val setText: TextView = itemView.findViewById(R.id.tvCardSet)
        private val quantityText: TextView = itemView.findViewById(R.id.tvQuantity)

        fun bind(card: WishlistDao.WishlistCard) {
            nameText.text = card.cardName
            setText.text = card.setName
            quantityText.text = "x${card.quantity}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wishlist_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<WishlistDao.WishlistCard>() {
        override fun areItemsTheSame(old: WishlistDao.WishlistCard, new: WishlistDao.WishlistCard) =
            old.setCode == new.setCode && old.cardNumber == new.cardNumber
        override fun areContentsTheSame(old: WishlistDao.WishlistCard, new: WishlistDao.WishlistCard) =
            old == new
    }
}