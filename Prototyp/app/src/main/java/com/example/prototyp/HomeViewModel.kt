package com.example.prototyp

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prototyp.data.db.CardDao
import com.example.prototyp.CollectionEntry
import com.example.prototyp.externalWishlist.ExternalWishlist
import com.example.prototyp.externalWishlist.ExternalWishlistCard
import com.example.prototyp.externalWishlist.ExternalWishlistDao
import com.example.prototyp.statistics.SetCompletionStat
import com.example.prototyp.wishlist.WishlistDao
import com.example.prototyp.wishlist.WishlistEntry
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HomeViewModel(
    private val cardDao: CardDao,
    private val masterDao: MasterCardDao,
    private val wishlistDao: WishlistDao,
    private val externalWishlistDao: ExternalWishlistDao
) : ViewModel() {

    private val _totalCollectionValue = MutableStateFlow<Double?>(null)
    val totalCollectionValue = _totalCollectionValue.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    fun onUserMessageShown() {
        _userMessage.value = null
    }

    fun updateTotalValue() {
        viewModelScope.launch(Dispatchers.IO) {
            val allItems = cardDao.getCollectionForExport()
            val totalValue = allItems.sumOf { entry ->
                (entry.price ?: 0.0) * entry.quantity
            }
            _totalCollectionValue.value = totalValue
        }
    }

    val setCompletionStats: StateFlow<List<SetCompletionStat>> = combine(
        masterDao.getSetCardCounts(), // Gesamtanzahl Karten pro Set
        cardDao.getOwnedUniqueCardCountsPerSet() // Anzahl deiner einzigartigen Karten pro Set
    ) { totalCounts, ownedCounts ->
        val ownedMap = ownedCounts.associate { it.setName to it.count }
        totalCounts.map { totalCount ->
            val owned = ownedMap[totalCount.setName] ?: 0
            SetCompletionStat(
                setName = totalCount.setName,
                ownedUniqueCards = owned,
                totalCardsInSet = totalCount.count
            )
        }.sortedBy { it.setName } // Alphabetisch nach Set-Namen sortieren
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    enum class WishlistImportTarget { OWN_WISHLIST, EXTERNAL_WISHLIST }

    fun importWishlistFromCsv(
        uri: Uri,
        context: Context,
        target: WishlistImportTarget,
        externalWishlistName: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val validatedCards = mutableListOf<MasterCard>()
                var notFoundCount = 0

                val rows = context.contentResolver.openInputStream(uri)?.use {
                    csvReader().open(it) { readAllWithHeaderAsSequence().toList() }
                } ?: emptyList()

                rows.forEach { row ->
                    val setCode = row["setCode"]
                    val cardNumber = row["cardNumber"]?.toIntOrNull()
                    if (setCode != null && cardNumber != null) {
                        val masterCard = masterDao.getBySetAndNumber(setCode, cardNumber)
                        if (masterCard != null) {
                            validatedCards.add(masterCard)
                        } else {
                            notFoundCount++
                        }
                    }
                }

                val groupedCards = validatedCards.groupingBy { it }.eachCount()

                if (groupedCards.isEmpty()) {
                    _userMessage.value = "Keine gültigen Karten in der Datei gefunden."
                    return@launch
                }

                var successMessage = ""

                when (target) {
                    WishlistImportTarget.OWN_WISHLIST -> {
                        val entries = groupedCards.map { (card, qty) ->
                            WishlistEntry(card.setCode, card.cardNumber, qty)
                        }
                        wishlistDao.overrideWishlist(entries)
                        successMessage = "${entries.size} Einträge in deine Wunschliste importiert."
                    }
                    WishlistImportTarget.EXTERNAL_WISHLIST -> {
                        if (externalWishlistName.isNullOrBlank()) {
                            _userMessage.value = "Name für externe Wunschliste fehlt."
                            return@launch
                        }
                        val externalWishlist = ExternalWishlist(name = externalWishlistName)
                        val cardEntries = groupedCards.map { (card, qty) ->
                            ExternalWishlistCard(0, card.setCode, card.cardNumber, qty)
                        }
                        externalWishlistDao.createWishlistWithCards(externalWishlist, cardEntries)
                        successMessage = "Externe Wunschliste '$externalWishlistName' mit ${cardEntries.size} Einträgen erstellt."
                    }
                }

                if (notFoundCount > 0) {
                    successMessage += " ($notFoundCount Karten übersprungen.)"
                }
                _userMessage.value = successMessage

            } catch (e: Exception) {
                _userMessage.value = "Import fehlgeschlagen: ${e.message}"
                Log.e("WishlistImport", "Fehler", e)
            }
        }
    }
    fun importCollection(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val validatedEntries = mutableListOf<CollectionEntry>()
                var notFoundCount = 0

                val rows = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    csvReader().open(inputStream) {
                        readAllWithHeaderAsSequence().toList()
                    }
                } ?: emptyList()

                rows.forEach { row ->
                    val setCode = row["setCode"]
                    val cardNumber = row["cardNumber"]?.toIntOrNull()

                    if (setCode != null && cardNumber != null) {

                        val masterCard = masterDao.getBySetAndNumber(setCode, cardNumber)

                        if (masterCard != null) {
                            val entry = CollectionEntry(
                                setCode = setCode,
                                cardNumber = cardNumber,
                                quantity = row["quantity"]?.toIntOrNull() ?: 1,
                                price = row["price"]?.toDoubleOrNull(),
                                personalNotes = row["personalNotes"],
                                generalNotes = row["generalNotes"]
                            )
                            validatedEntries.add(entry)
                        } else {
                            notFoundCount++
                            Log.w("Import", "Karte nicht in Master-DB gefunden: Set=$setCode, Nummer=$cardNumber")
                        }
                    }
                }

                if (validatedEntries.isNotEmpty()) {
                    cardDao.overrideCollection(validatedEntries)
                    var message = "${validatedEntries.size} Karten erfolgreich importiert!"
                    if (notFoundCount > 0) {
                        message += " ($notFoundCount Karten nicht in der DB gefunden und übersprungen.)"
                    }
                    _userMessage.value = message
                } else {
                    _userMessage.value = "Import fehlgeschlagen: Keine gültigen Karten in der CSV-Datei gefunden."
                }
            } catch (e: Exception) {
                _userMessage.value = "Import fehlgeschlagen: ${e.message}"
                Log.e("Import", "Fehler beim Import", e)
            }
        }
    }

    suspend fun createCollectionCsvForSharing(context: Context): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val collection = cardDao.getCollectionForExport()
                if (collection.isEmpty()) {
                    _userMessage.value = "Sammlung ist leer. Nichts zu exportieren."
                    return@withContext null
                }

                val exportDir = File(context.cacheDir, "exports")
                if (!exportDir.exists()) {
                    exportDir.mkdirs()
                }
                val file = File(exportDir, "sammlung_export.csv")

                file.outputStream().use { outputStream ->
                    csvWriter().open(outputStream) {
                        writeRow("setCode", "cardNumber", "quantity", "price", "color", "personalNotes", "generalNotes")
                        collection.forEach { entry ->
                            writeRow(
                                entry.setCode,
                                entry.cardNumber,
                                entry.quantity,
                                entry.price,
                                entry.personalNotes,
                                entry.generalNotes
                            )
                        }
                    }
                }

                return@withContext FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )

            } catch (e: Exception) {
                _userMessage.value = "Export fehlgeschlagen: ${e.message}"
                Log.e("Export", "Fehler beim Erstellen der CSV", e)
                return@withContext null
            }
        }
    }

    suspend fun createWishlistCsvForSharing(context: Context): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val wishlist = wishlistDao.getWishlistForExport()
                if (wishlist.isEmpty()) {
                    _userMessage.value = "Wunschliste ist leer. Nichts zu exportieren."
                    return@withContext null
                }

                val exportDir = File(context.cacheDir, "exports")
                if (!exportDir.exists()) { exportDir.mkdirs() }
                val file = File(exportDir, "wunschliste_export.csv")

                file.outputStream().use { outputStream ->
                    csvWriter().open(outputStream) {
                        writeRow("setCode", "cardNumber", "quantity", "color")
                        wishlist.forEach { entry ->
                            writeRow(
                                entry.setCode,
                                entry.cardNumber,
                                entry.quantity,
                            )
                        }
                    }
                }

                return@withContext FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )

            } catch (e: Exception) {
                _userMessage.value = "Export fehlgeschlagen: ${e.message}"
                Log.e("Export", "Fehler beim Erstellen der Wunschlisten-CSV", e)
                return@withContext null
            }
        }
    }
}

