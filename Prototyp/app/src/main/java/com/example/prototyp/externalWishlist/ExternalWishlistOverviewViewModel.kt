package com.example.prototyp.externalWishlist

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExternalWishlistOverviewViewModel(private val dao: ExternalWishlistDao) : ViewModel() {

    val allWishlists = dao.observeAllWishlists()

    fun deleteWishlist(wishlist: ExternalWishlist) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteWishlist(wishlist)
        }
    }
}

class ExternalWishlistOverviewViewModelFactory(private val dao: ExternalWishlistDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExternalWishlistOverviewViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExternalWishlistOverviewViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}