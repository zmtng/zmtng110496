package com.example.prototyp.trade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.prototyp.deckBuilder.DeckDao
import com.example.prototyp.externalCollection.ExternalCollectionDao
import com.example.prototyp.externalWishlist.ExternalWishlistDao

class TradeSelectionViewModel(
    externalCollectionDao: ExternalCollectionDao,
    externalWishlistDao: ExternalWishlistDao,
    deckDao: DeckDao
) : ViewModel() {

    val allExternalCollections = externalCollectionDao.observeAllCollections()
    val allExternalWishlists = externalWishlistDao.observeAllWishlists()
    val allDecks = deckDao.observeAllDecksWithCardCount()

}

class TradeSelectionViewModelFactory(
    private val externalCollectionDao: ExternalCollectionDao,
    private val externalWishlistDao: ExternalWishlistDao,
    private val deckDao: DeckDao,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TradeSelectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TradeSelectionViewModel(externalCollectionDao, externalWishlistDao, deckDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
