package com.example.prototyp.externalCollection

import androidx.lifecycle.*
import com.example.prototyp.MasterCardDao
import com.example.prototyp.wishlist.WishlistDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExternalCollectionDetailViewModel(
    private val dao: ExternalCollectionDao,
    private val masterDao: MasterCardDao,
    private val wishlistDao: WishlistDao
) : ViewModel() {

    private val collectionId = MutableStateFlow<Int?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _colorFilter = MutableStateFlow("")
    private val _setFilter = MutableStateFlow("")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val collectionContents = combine(
        collectionId.filterNotNull(),
        _searchQuery,
        _colorFilter,
        _setFilter
    ) { id, query, color, set ->
        // Dieser Flow wird bei JEDER Änderung (ID, Suche, Filter) neu ausgelöst
        dao.observeCollectionContents(id, query, color, set)
    }.flatMapLatest { it }

    fun setCollectionId(id: Int) {
        collectionId.value = id
    }

    // Neue Funktionen, um die Filter aus dem Fragment zu setzen
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setColorFilter(color: String) { _colorFilter.value = color }
    fun setSetFilter(set: String) { _setFilter.value = set }

    fun addCardToWishlist(card: ExternalCollectionDao.CardDetail) {
        viewModelScope.launch(Dispatchers.IO) {
            wishlistDao.upsertCard(card.setCode, card.cardNumber)
        }
    }

    // Neue Funktionen, um die Daten für die Spinner zu laden
    suspend fun getFilterColors(): List<String> = withContext(Dispatchers.IO) { masterDao.getDistinctColors() }
    suspend fun getFilterSets(): List<String> = withContext(Dispatchers.IO) { masterDao.getDistinctSetCodes() }
}

class ExternalCollectionDetailViewModelFactory(
    private val dao: ExternalCollectionDao,
    private val masterDao: MasterCardDao,
    private val wishlistDao: WishlistDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExternalCollectionDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExternalCollectionDetailViewModel(dao, masterDao, wishlistDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}