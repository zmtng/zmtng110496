package com.example.prototyp.wishlist

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.R
import com.example.prototyp.wishlist.WishlistDao
import com.example.yourapp.data.db.AppDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WishlistFragment : Fragment(R.layout.fragment_wishlist) {

    private val viewModel: WishlistViewModel by viewModels {
        WishlistViewModelFactory(AppDatabase.getInstance(requireContext()).wishlistDao())
    }
    private lateinit var wishlistAdapter: WishlistAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wishlistAdapter = WishlistAdapter()
        val rv = view.findViewById<RecyclerView>(R.id.rvWishlist)
        rv.adapter = wishlistAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.wishlistCards.collectLatest { cards ->
                wishlistAdapter.submitList(cards)
            }
        }
    }
}

// Die Factory-Klasse, die das DAO an das ViewModel übergibt
class WishlistViewModelFactory(private val wishlistDao: WishlistDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WishlistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WishlistViewModel(wishlistDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}