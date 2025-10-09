package com.example.prototyp.externalWishlist

import androidx.lifecycle.*
import com.example.prototyp.MasterCardDao
import com.example.prototyp.wishlist.WishlistDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExternalWishlistDetailViewModel(
    private val externalWishlistDao: ExternalWishlistDao,
    private val masterDao: MasterCardDao,
    private val wishlistDao: WishlistDao // Um Karten zur eigenen Wunschliste hinzuzufügen
) : ViewModel() {

    private val wishlistId = MutableStateFlow<Int?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _colorFilter = MutableStateFlow("")
    private val _setFilter = MutableStateFlow("")

    // Dieser Flow kombiniert die ID und alle Filter und holt die passenden Daten aus der DB.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val wishlistContents = wishlistId.filterNotNull().flatMapLatest { id ->
        combine(_searchQuery, _colorFilter, _setFilter) { query, color, set ->
            externalWishlistDao.observeWishlistContents(id, query, color, set)
        }.flatMapLatest { it }
    }

    fun setWishlistId(id: Int) {
        wishlistId.value = id
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setColorFilter(color: String) { _colorFilter.value = color }
    fun setSetFilter(set: String) { _setFilter.value = set }

    // Funktion, um eine Karte zur *eigenen* Haupt-Wunschliste hinzuzufügen
    fun addCardToOwnWishlist(card: ExternalWishlistDao.CardDetail) {
        viewModelScope.launch(Dispatchers.IO) {
            wishlistDao.upsertCard(card.setCode, card.cardNumber)
        }
    }

    suspend fun getFilterColors(): List<String> = withContext(Dispatchers.IO) { masterDao.getDistinctColors() }
    suspend fun getFilterSets(): List<String> = withContext(Dispatchers.IO) { masterDao.getDistinctSetCodes() }
}

class ExternalWishlistDetailViewModelFactory(
    private val externalWishlistDao: ExternalWishlistDao,
    private val masterDao: MasterCardDao,
    private val wishlistDao: WishlistDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExternalWishlistDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExternalWishlistDetailViewModel(externalWishlistDao, masterDao, wishlistDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}