package com.example.prototyp.deckBuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.prototyp.MasterCard
import com.example.prototyp.MasterCardDao
import com.example.prototyp.wishlist.WishlistDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeckDetailViewModelFactory(
    private val deckDao: DeckDao,
    private val masterDao: MasterCardDao,
    private val wishlistDao: WishlistDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeckDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeckDetailViewModel(deckDao, masterDao, wishlistDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class DeckDetailViewModel(
    private val deckDao: DeckDao,
    private val masterDao: MasterCardDao,
    private val wishlistDao: WishlistDao
) : ViewModel() {

    private val _deckIdFlow = MutableStateFlow<Int?>(null)
    val deckId = _deckIdFlow.asStateFlow()

    fun incrementCardInDeck(card: DeckDao.DeckCardDetail) {
        _deckIdFlow.value?.let { currentDeckId ->
            viewModelScope.launch(Dispatchers.IO) {
                deckDao.addQuantityInDeck(currentDeckId, card.setCode, card.cardNumber, 1)
            }
        }
    }

    fun decrementCardInDeck(card: DeckDao.DeckCardDetail) {
        _deckIdFlow.value?.let { currentDeckId ->
            viewModelScope.launch(Dispatchers.IO) {
                if (card.quantity <= 1) {
                    deckDao.deleteCardFromDeck(currentDeckId, card.setCode, card.cardNumber)
                } else {
                    deckDao.addQuantityInDeck(currentDeckId, card.setCode, card.cardNumber, -1)
                }
            }
        }
    }

    fun addCardToDeck(card: MasterCard) {
        _deckIdFlow.value?.let { currentDeckId ->
            viewModelScope.launch(Dispatchers.IO) {
                deckDao.upsertCardInDeck(currentDeckId, card.setCode, card.cardNumber)
            }
        }
    }

    fun addCardToWishlist(card: DeckDao.DeckCardDetail) {
        viewModelScope.launch(Dispatchers.IO) {
            wishlistDao.upsertCard(card.setCode, card.cardNumber)
        }
    }

    suspend fun searchMasterCards(query: String, color: String, set: String): List<MasterCard> {
        return withContext(Dispatchers.IO) {
            masterDao.search(query, color, set)
        }
    }

    val deckContents = _deckIdFlow.filterNotNull().flatMapLatest { deckId ->
        deckDao.observeDeckContents(deckId)
    }

    fun setDeckId(id: Int) {
        _deckIdFlow.value = id
    }
    suspend fun getFilterColors(): List<String> {
        return withContext(Dispatchers.IO) {
            masterDao.getDistinctColors()
        }
    }

    suspend fun getFilterSets(): List<String> {
        return withContext(Dispatchers.IO) {
            masterDao.getDistinctSetCodes()
        }
    }

    fun deleteCard(card: DeckDao.DeckCardDetail) {

        val currentDeckId = deckId.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            deckDao.deleteCardFromDeck(currentDeckId, card.setCode, card.cardNumber)
        }
    }

}
