package com.example.prototyp.wishlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Transaction
import com.example.prototyp.MasterCard
import com.example.prototyp.MasterCardDao
import com.example.prototyp.data.db.CardDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*

private const val TAG = "WishlistViewModel"
class WishlistViewModel(
    private val wishlistDao: WishlistDao,
    private val masterDao: MasterCardDao,
    private val cardDao: CardDao
) : ViewModel() {

    // 1. StateFlows für jeden einzelnen Filter
    private val _searchQuery = MutableStateFlow("")
    private val _colorFilter = MutableStateFlow("")
    private val _setFilter = MutableStateFlow("")

    // 2. Kombiniere alle Filter zu einem einzigen "Trigger"-Flow
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val wishlistCards: Flow<List<WishlistDao.WishlistCard>> = combine(
        _searchQuery,
        _colorFilter,
        _setFilter
    ) { query, color, set ->
        Triple(query, color, set) // Packe alle Filterwerte zusammen
    }.flatMapLatest { (query, color, set) ->
        // 3. Jedes Mal, wenn sich ein Filter ändert, rufe die neue DAO-Funktion auf
        wishlistDao.observeFilteredWishlist(query, color, set)
    }

    // --- Funktionen, die das Fragment aufruft, um die Filter zu ändern ---
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setColorFilter(color: String) { _colorFilter.value = color }
    fun setSetFilter(set: String) { _setFilter.value = set }

    // --- Funktionen zum Holen der Filter-Daten für die Spinner ---
    suspend fun getFilterColors(): List<String> = withContext(Dispatchers.IO) { masterDao.getDistinctColors() }
    suspend fun getFilterSets(): List<String> = withContext(Dispatchers.IO) { masterDao.getDistinctSetCodes() }

    fun addCardToWishlist(card: MasterCard) {
        Log.d(TAG, "addCardToWishlist aufgerufen für: ${card.cardName}")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                wishlistDao.upsertCard(card.setCode, card.cardNumber)
            } catch (e: Exception) {
                // Logge den genauen Fehler, der vom DAO geworfen wird
                Log.e(TAG, "Fehler beim Ausführen von upsertCard", e)
            }
        }
    }
    fun incrementQuantity(card: WishlistDao.WishlistCard) {
        viewModelScope.launch(Dispatchers.IO) {
            wishlistDao.updateQuantity(card.setCode, card.cardNumber, card.quantity + 1)
        }
    }
    fun decrementQuantity(card: WishlistDao.WishlistCard) {
        viewModelScope.launch(Dispatchers.IO) {
            if (card.quantity <= 1) {
                // Bei 1 oder weniger wird die Karte von der Wunschliste entfernt
                wishlistDao.deleteEntry(card.setCode, card.cardNumber)
            } else {
                // Ansonsten wird die Menge nur reduziert
                wishlistDao.updateQuantity(card.setCode, card.cardNumber, card.quantity - 1)
            }
        }
    }



    @Transaction
    fun transferToCollection(card: WishlistDao.WishlistCard, quantity: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Reduziere die Menge auf der Wunschliste oder lösche den Eintrag
            wishlistDao.decrementOrDelete(card.setCode, card.cardNumber, quantity)

            // 2. Erhöhe die Menge in der Sammlung (Collection)
            cardDao.upsertBySetAndNumber(card.setCode, card.cardNumber, quantity, card.color)
        }
    }


    suspend fun searchMasterCards(query: String): List<MasterCard> {
        return withContext(Dispatchers.IO) {
            masterDao.search(query)
        }


    }
}