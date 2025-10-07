package com.example.prototyp.deckBuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.prototyp.MasterCard
import com.example.prototyp.MasterCardDao
import com.example.prototyp.deckBuilder.DeckDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeckDetailViewModelFactory(
    private val deckId: Int,
    private val deckDao: DeckDao,
    private val masterDao: MasterCardDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeckDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // HINZUGEFÜGT: Übergib das masterDao an das ViewModel
            return DeckDetailViewModel(deckId, deckDao, masterDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class DeckDetailViewModel(
    private val deckId: Int,
    private val deckDao: DeckDao,
    private val masterDao: MasterCardDao
) : ViewModel() {
    private val deckIdFlow = MutableStateFlow<Int?>(null)

    fun incrementCardInDeck(card: DeckDao.DeckCardDetail) {
        viewModelScope.launch(Dispatchers.IO) {
            deckDao.addQuantityInDeck(deckId, card.setCode, card.cardNumber, 1)
        }
    }

    fun decrementCardInDeck(card: DeckDao.DeckCardDetail) {
        viewModelScope.launch(Dispatchers.IO) {
            if (card.quantity <= 1) {
                // Bei 1 oder weniger wird die Karte aus dem Deck entfernt
                deckDao.deleteCardFromDeck(deckId, card.setCode, card.cardNumber)
            } else {
                // Ansonsten wird die Menge reduziert
                deckDao.addQuantityInDeck(deckId, card.setCode, card.cardNumber, -1)
            }
        }
    }

    fun addCardToDeck(card: MasterCard) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentDeckId = deckIdFlow.value
            if (currentDeckId != null) {
                deckDao.upsertCardInDeck(currentDeckId, card.setCode, card.cardNumber)
            }
        }
    }

    suspend fun searchMasterCards(query: String): List<MasterCard> {
        return withContext(Dispatchers.IO) {
            masterDao.search(query)
        }
    }

    val deckContents = deckIdFlow.filterNotNull().flatMapLatest { deckId ->
        deckDao.observeDeckContents(deckId)
    }

    // Neue Funktion, um das anzuzeigende Deck zu setzen
    fun setDeckId(id: Int) {
        deckIdFlow.value = id
    }

}