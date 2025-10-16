package com.example.prototyp.trade

import androidx.lifecycle.*
import com.example.prototyp.externalCollection.ExternalCollection
import com.example.prototyp.externalCollection.ExternalCollectionDao
import com.example.prototyp.externalWishlist.ExternalWishlist
import com.example.prototyp.externalWishlist.ExternalWishlistDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class TradeFinderViewModel(
    private val tradeDao: TradeDao,
    private val externalCollectionDao: ExternalCollectionDao,
    private val externalWishlistDao: ExternalWishlistDao
) : ViewModel() {


    val allExternalCollections: Flow<List<ExternalCollection>> = externalCollectionDao.observeAllCollections()
    val allExternalWishlists: Flow<List<ExternalWishlist>> = externalWishlistDao.observeAllWishlists()

    fun getYouHave(wishlistId: Int): Flow<List<TradeDao.TradeResult>> {
        return tradeDao.findWhatYouHaveThatTheyWant(wishlistId)
    }

    fun getTheyHave(collectionId: Int): Flow<List<TradeDao.TradeResult>> {
        return tradeDao.findWhatTheyHaveThatYouWant(collectionId)
    }
}

// Factory für den ViewModel
class TradeFinderViewModelFactory(
    private val tradeDao: TradeDao,
    private val externalCollectionDao: ExternalCollectionDao,
    private val externalWishlistDao: ExternalWishlistDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TradeFinderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TradeFinderViewModel(tradeDao, externalCollectionDao, externalWishlistDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}