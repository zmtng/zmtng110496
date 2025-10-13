package com.example.prototyp.deckBuilder

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.prototyp.MasterCard
import com.example.prototyp.MasterCardDao
import com.example.prototyp.data.db.CardDao
import com.example.prototyp.wishlist.WishlistDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

data class DeckStats(
    val totalValue: Double = 0.0,
    val totalCards: Int = 0,
    val colorDistribution: Map<String, Float> = emptyMap()
)
class DeckDetailViewModelFactory(
    private val deckDao: DeckDao,
    private val masterDao: MasterCardDao,
    private val wishlistDao: WishlistDao,
    private val cardDao: CardDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // THE FIX IS HERE: Use ::class.java
        if (modelClass.isAssignableFrom(DeckDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeckDetailViewModel(deckDao, masterDao, wishlistDao, cardDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class DeckDetailViewModel(
    private val deckDao: DeckDao,
    private val masterDao: MasterCardDao,
    private val wishlistDao: WishlistDao,
    private val cardDao: CardDao
) : ViewModel() {

    private val _deckIdFlow = MutableStateFlow<Int?>(null)

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()
    fun onUserMessageShown() { _userMessage.value = null }


    val deckContents: StateFlow<List<DeckDao.DeckCardDetail>> = _deckIdFlow.filterNotNull().flatMapLatest { deckId ->
        // KORREKTUR 1: Funktionsname angepasst
        deckDao.observeDeckContentsWithDetails(deckId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deckStats: StateFlow<DeckStats> = deckContents.map { cards ->
        // KORREKTUR 2: Feldname angepasst
        val totalCards = cards.sumOf { it.quantityInDeck }
        val totalValue = cards.sumOf { (it.price ?: 0.0) * it.quantityInDeck }

        val colorCounts = cards.groupingBy { it.color }.fold(0) { acc, card -> acc + card.quantityInDeck }
        val colorDistribution = if (totalCards > 0) {
            colorCounts.mapValues { it.value.toFloat() / totalCards }
        } else {
            emptyMap()
        }
        DeckStats(totalValue, totalCards, colorDistribution)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DeckStats())


    fun setDeckId(id: Int) {
        _deckIdFlow.value = id
    }

    fun incrementCardInDeck(card: DeckDao.DeckCardDetail) {
        viewModelScope.launch(Dispatchers.IO) {
            _deckIdFlow.value?.let { deckId ->
                deckDao.addQuantityInDeck(deckId, card.setCode, card.cardNumber, 1)
            }
        }
    }

    fun decrementCardInDeck(card: DeckDao.DeckCardDetail) {
        viewModelScope.launch(Dispatchers.IO) {
            // KORREKTUR 3: Feldname angepasst
            if (card.quantityInDeck <= 1) {
                deleteCard(card)
            } else {
                _deckIdFlow.value?.let { deckId ->
                    deckDao.addQuantityInDeck(deckId, card.setCode, card.cardNumber, -1)
                }
            }
        }
    }

    fun deleteCard(card: DeckDao.DeckCardDetail) {
        viewModelScope.launch(Dispatchers.IO) {
            _deckIdFlow.value?.let { deckId ->
                deckDao.deleteCardFromDeck(deckId, card.setCode, card.cardNumber)
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

    suspend fun getFilterColors(): List<String> = withContext(Dispatchers.IO) { masterDao.getDistinctColors() }
    suspend fun getFilterSets(): List<String> = withContext(Dispatchers.IO) { masterDao.getDistinctSetCodes() }

    // ... (Restlicher Code für Preis-Scraping bleibt unverändert) ...
    fun fetchAllDeckPrices() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentDeck = deckContents.value
            val currentDeckId = _deckIdFlow.value
            if (currentDeck.isEmpty() || currentDeckId == null) {
                _userMessage.value = "Deck ist leer oder ungültig."
                return@launch
            }

            _userMessage.value = "Starte Preis-Update für ${currentDeck.size} Karten..."

            for ((index, card) in currentDeck.withIndex()) {
                _userMessage.value = "Prüfe Karte ${index + 1}/${currentDeck.size}: ${card.cardName}"
                fetchPriceForCard(currentDeckId, card)
                delay(500L) // Keep the delay to be nice to the server
            }
            _userMessage.value = "Preis-Update abgeschlossen!"

            _deckIdFlow.value = _deckIdFlow.value
        }
    }

    private fun fetchPriceForCard(deckId: Int, row: DeckDao.DeckCardDetail) {
        viewModelScope.launch(Dispatchers.IO) {
            var url = ""
            try {
                val cardNameUrl = formatForUrl(row.cardName)
                val setNameUrl = formatForUrl(row.setName)
                url = "https://www.cardmarket.com/de/Riftbound/Products/Singles/$setNameUrl/$cardNameUrl"

                val response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0")
                    .header("Accept-Language", "de-DE,de;q=0.5")
                    .referrer("https://www.google.com")
                    .ignoreHttpErrors(true)
                    .execute()

                if (response.statusCode() == 403) {
                    Log.e("PriceScraper", "Access denied (403). URL: $url")
                    _userMessage.value = "Access denied by Cardmarket (403)."
                    return@launch
                }

                val doc = response.parse()
                val priceText = doc.select("dt:contains(Preis-Trend) + dd").first()?.text()

                if (priceText.isNullOrBlank()) {
                    _userMessage.value = "No trend price found for '${row.cardName}'."
                    return@launch
                }

                val priceValue = priceText.replace("€", "").replace(",", ".").trim().toDoubleOrNull()

                if (priceValue != null) {
                    deckDao.updateDeckCardPrice(deckId, row.setCode, row.cardNumber, priceValue)
                }
            } catch (e: Exception) {
                Log.e("PriceScraper", "Error fetching price for '${row.cardName}' from URL: $url", e)
                _userMessage.value = "Could not fetch price for '${row.cardName}'."
            }
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
                if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
            }
        }
    }
}