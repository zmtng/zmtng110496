package com.example.prototyp.wishlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Transaction
import com.example.prototyp.MasterCard
import com.example.prototyp.MasterCardDao
import com.example.prototyp.data.db.CardDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import org.jsoup.Jsoup
import java.util.Locale

class WishlistViewModel(
    private val wishlistDao: WishlistDao,
    private val masterDao: MasterCardDao,
    private val cardDao: CardDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _colorFilter = MutableStateFlow("")
    private val _setFilter = MutableStateFlow("")
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()
    fun onUserMessageShown() { _userMessage.value = null }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val wishlistCards: Flow<List<WishlistDao.WishlistCard>> = combine(
        _searchQuery,
        _colorFilter,
        _setFilter
    ) { query, color, set ->
        Triple(query, color, set)
    }.flatMapLatest { (query, color, set) ->
        wishlistDao.observeFilteredWishlist(query, color, set)
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setColorFilter(color: String) { _colorFilter.value = color }
    fun setSetFilter(set: String) { _setFilter.value = set }

    suspend fun getFilterColors(): List<String> = withContext(Dispatchers.IO) { masterDao.getDistinctColors() }
    suspend fun getFilterSets(): List<String> = withContext(Dispatchers.IO) { masterDao.getDistinctSetCodes() }

    fun addCardToWishlist(card: MasterCard) {
        viewModelScope.launch(Dispatchers.IO) {
            wishlistDao.upsertCard(card.setCode, card.cardNumber)
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
                wishlistDao.deleteEntry(card.setCode, card.cardNumber)
            } else {
                wishlistDao.updateQuantity(card.setCode, card.cardNumber, card.quantity - 1)
            }
        }
    }

    @Transaction
    fun transferToCollection(card: WishlistDao.WishlistCard, quantity: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            wishlistDao.decrementOrDelete(card.setCode, card.cardNumber, quantity)
            cardDao.upsertBySetAndNumber(card.setCode, card.cardNumber, quantity)
        }
    }

    suspend fun searchMasterCards(query: String, color: String, set: String): List<MasterCard> {
        return withContext(Dispatchers.IO) {
            masterDao.search(query, color, set)
        }
    }

    fun deleteCard(card: WishlistDao.WishlistCard) {
        viewModelScope.launch(Dispatchers.IO) {
            wishlistDao.deleteEntry(card.setCode, card.cardNumber)
        }
    }

    private fun formatForUrl(text: String): String {
        val cleanedText = text
            .replace("(Main Set)", "")
            .replace("Origins: ", "")
            .trim()
            .lowercase()
            .replace("\\s+".toRegex(), "-")
            .replace(Regex("[^a-z0-9\\-]"), "")
        return cleanedText.split('-').joinToString("-") { part ->
            part.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
    }

    fun fetchPriceForCard(
        card: WishlistDao.WishlistCard,
        showSuccessMessage: Boolean = true,
        onSuccess: (updatedPrice: Double) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var url = ""
            try {
                val cardNameUrl = formatForUrl(card.cardName)
                val setNameUrl = formatForUrl(card.setName)
                url = "https://www.cardmarket.com/de/Riftbound/Products/Singles/$setNameUrl/$cardNameUrl"

                val response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0")
                    .header("Accept-Language", "de-DE,de;q=0.5")
                    .referrer("https://www.google.com")
                    .ignoreHttpErrors(true)
                    .execute()

                if (response.statusCode() == 403) {
                    Log.e("PriceScraper", "Zugriff verweigert (403). URL: $url")
                    if (showSuccessMessage) {
                        _userMessage.value = "Zugriff von Cardmarket verweigert (403)."
                    }
                    return@launch
                }
                val doc = response.parse()
                val priceText = doc.select("dt:contains(Preis-Trend) + dd").first()?.text()
                if (priceText.isNullOrBlank()) {
                    if (showSuccessMessage) _userMessage.value = "Kein Trendpreis für '${card.cardName}' gefunden."
                    return@launch
                }
                val priceValue = priceText.replace("€", "").replace(",", ".").trim().toDoubleOrNull()
                if (priceValue != null) {
                    // KORRIGIERT: Preis in der Wishlist-Tabelle speichern
                    wishlistDao.updatePrice(card.setCode, card.cardNumber, priceValue)

                    if (showSuccessMessage) {
                        _userMessage.value = "Preis für '${card.cardName}' auf ${priceValue}€ aktualisiert."
                    }
                    withContext(Dispatchers.Main) {
                        onSuccess(priceValue)
                    }
                }
            } catch (e: Exception) {
                Log.e("PriceScraper", "Fehler beim Abrufen des Preises für '${card.cardName}' von URL: $url", e)
                if (showSuccessMessage) {
                    _userMessage.value = "Preis für '${card.cardName}' konnte nicht abgerufen werden."
                }
            }
        }
    }

    fun fetchAllPrices() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentWishlist = wishlistCards.firstOrNull()
            if (currentWishlist.isNullOrEmpty()) {
                _userMessage.value = "Wunschliste ist leer."
                return@launch
            }
            _userMessage.value = "Starte Preis-Update für ${currentWishlist.size} Karten..."
            for ((index, card) in currentWishlist.withIndex()) {
                fetchPriceForCard(card, showSuccessMessage = false) {}
                delay(100L)
            }
            _userMessage.value = "Preis-Update abgeschlossen!"
        }
    }
}
