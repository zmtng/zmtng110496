package com.example.prototyp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prototyp.data.db.CardDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

enum class SortOrder { BY_NAME, BY_NUMBER, BY_COLOR }

class CollectionViewModel(
    private val cardDao: CardDao,
    private val masterDao: MasterCardDao
) : ViewModel() {

    // --- StateFlows für Filter und Sortierung ---
    private val _sortOrder = MutableStateFlow(SortOrder.BY_NAME)
    private val _searchQuery = MutableStateFlow("")
    private val _colorFilter = MutableStateFlow<String?>(null)
    private val _setFilter = MutableStateFlow<String?>(null)

    // --- Der neue, intelligente Datenfluss ---
    val collection: StateFlow<List<CardDao.CollectionRowData>> = combine(
        cardDao.observeCollectionWithDetails(), // 1. Die Rohdaten von der DB
        _sortOrder,                              // 2. Die aktuelle Sortierung
        _searchQuery,                            // 3. Der aktuelle Suchtext
        _colorFilter,                            // 4. Der gewählte Farbfilter
        _setFilter                               // 5. Der gewählte Setfilter
    ) { fullList, sort, query, color, set ->
        // Dieser Block wird JEDES MAL ausgeführt, wenn sich irgendetwas ändert

        // Schritt A: Filtern
        val filteredList = fullList.filter { item ->
            (query.isBlank() || item.cardName.contains(query, ignoreCase = true)) &&
                    (color == null || item.color == color) &&
                    (set == null || item.setName == set)
        }

        // Schritt B: Sortieren
        when (sort) {
            SortOrder.BY_NAME -> filteredList.sortedBy { it.cardName }
            SortOrder.BY_NUMBER -> filteredList.sortedWith(compareBy({ it.setCode }, { it.cardNumber }))
            SortOrder.BY_COLOR -> filteredList.sortedBy { it.color }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- Öffentliche Funktionen zum Ändern der Filter ---
    fun setSortOrder(sortBy: SortOrder) { _sortOrder.value = sortBy }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setColorFilter(color: String?) { _colorFilter.value = color }
    fun setSetFilter(set: String?) { _setFilter.value = set }

    suspend fun getFilterColors(): List<String> = withContext(Dispatchers.IO) { masterDao.getDistinctColors() }
    suspend fun getFilterSets(): List<String> = withContext(Dispatchers.IO) { masterDao.getDistinctSetNames() }
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()
    fun onUserMessageShown() { _userMessage.value = null }


    fun updateNotes(setCode: String, cardNumber: Int, personalNotes: String?, generalNotes: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            cardDao.updateNotes(setCode, cardNumber, personalNotes, generalNotes)
        }
    }

    fun incrementQuantity(row: CardDao.CollectionRowData) {
        viewModelScope.launch(Dispatchers.IO) {
            cardDao.addQuantity(row.setCode, row.cardNumber, +1)
        }
    }

    fun decrementQuantity(row: CardDao.CollectionRowData) {
        viewModelScope.launch(Dispatchers.IO) {
            // Menge um 1 reduzieren
            cardDao.addQuantity(row.setCode, row.cardNumber, -1)

            // Prüfen, ob die Menge jetzt 0 oder weniger ist, und dann löschen
            val updatedEntry = cardDao.getByKey(row.setCode, row.cardNumber)
            if (updatedEntry == null || updatedEntry.quantity <= 0) {
                cardDao.deleteByKey(row.setCode, row.cardNumber)
            }
        }
    }

    private fun formatForUrl(text: String): String {
        val cleanedText = text
            .replace("(Main Set)", "")
            .replace("Origins: ", "")
            .trim()
            .lowercase() // Alles klein schreiben für eine einheitliche Basis
            .replace("\\s+".toRegex(), "-")
            .replace(Regex("[^a-z0-9\\-]"), "")
        return cleanedText.split('-').joinToString("-") { part ->
            part.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
            }
        }
    }

    fun fetchAllPrices() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentCollection = collection.value
            if (currentCollection.isEmpty()) {
                _userMessage.value = "Sammlung ist leer."
                return@launch
            }

            _userMessage.value = "Starte Preis-Update für ${currentCollection.size} Karten..."

            for ((index, card) in currentCollection.withIndex()) {
                _userMessage.value = "Prüfe Karte ${index + 1}/${currentCollection.size}: ${card.cardName}"
                fetchPriceForCard(card, showSuccessMessage = false) {}
                delay(1500L)
            }

            _userMessage.value = "Preis-Update abgeschlossen!"
        }
    }

    fun fetchPriceForCard(
        row: CardDao.CollectionRowData,
        showSuccessMessage: Boolean = true,
        onSuccess: (updatedPrice: Double) -> Unit
    ) {
        // Startet die gesamte Operation auf einem Hintergrund-Thread (IO)
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
                    Log.e("PriceScraper", "Zugriff verweigert (403). URL: $url")
                    if (showSuccessMessage) {
                        _userMessage.value = "Zugriff von Cardmarket verweigert (403)."
                    }
                    return@launch
                }

                val doc = response.parse()
                val priceText = doc.select("dt:contains(Preis-Trend) + dd").first()?.text()

                if (priceText.isNullOrBlank()) {
                    if (showSuccessMessage) _userMessage.value = "Kein Trendpreis für '${row.cardName}' gefunden."
                    return@launch
                }

                val priceValue = priceText.replace("€", "").replace(",", ".").trim().toDoubleOrNull()

                if (priceValue != null) {
                    cardDao.updatePrice(row.setCode, row.cardNumber, priceValue)
                    if (showSuccessMessage) {
                        _userMessage.value = "Preis für '${row.cardName}' auf ${priceValue}€ aktualisiert."
                    }

                    // *** HIER IST DIE FINALE KORREKTUR ***
                    // Wechsle kurz zum Main-Thread, um die UI-Callback-Funktion sicher aufzurufen.
                    withContext(Dispatchers.Main) {
                        onSuccess(priceValue)
                    }
                }
            } catch (e: Exception) {
                Log.e("PriceScraper", "Fehler beim Abrufen des Preises für '${row.cardName}' von URL: $url", e)
                if (showSuccessMessage) {
                    _userMessage.value = "Preis für '${row.cardName}' konnte nicht abgerufen werden."
                }
            }
        }
    }
}