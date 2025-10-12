package com.example.prototyp.externalCollection

import android.net.Uri
import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.example.prototyp.MasterCardDao
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExternalCollectionOverviewViewModel(
    private val dao: ExternalCollectionDao,
    private val masterDao: MasterCardDao
) : ViewModel() {

    val allCollections = dao.observeAllCollections()
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    fun onUserMessageShown() { _userMessage.value = null }

    fun deleteCollection(collection: ExternalCollection) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteCollection(collection)
        }
    }

    fun importCollection(uri: Uri, context: Context, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val validatedCards = mutableListOf<ExternalCollectionCard>()
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
                            val card = ExternalCollectionCard(
                                collectionId = 0,
                                setCode = setCode,
                                cardNumber = cardNumber,
                                quantity = row["quantity"]?.toIntOrNull() ?: 1,
                                price = row["price"]?.toDoubleOrNull(),

                            )
                            validatedCards.add(card)
                        } else {
                            notFoundCount++
                            Log.w("ExternalImport", "Karte nicht in Master-DB: Set=$setCode, Nr=$cardNumber")
                        }
                    }
                }

                if (validatedCards.isNotEmpty()) {
                    val newCollection = ExternalCollection(name = name)
                    dao.createCollectionWithCards(newCollection, validatedCards)
                    var message = "'$name' mit ${validatedCards.size} Karten importiert!"
                    if (notFoundCount > 0) {
                        message += " ($notFoundCount ungültige übersprungen.)"
                    }
                    _userMessage.value = message
                } else {
                    _userMessage.value = "Import fehlgeschlagen: Keine gültigen Karten gefunden."
                }
            } catch (e: Exception) {
                _userMessage.value = "Import fehlgeschlagen: ${e.message}"
                Log.e("ExternalImport", "Fehler beim Import", e)
            }
        }
    }
}

class ExternalCollectionOverviewViewModelFactory(
    private val dao: ExternalCollectionDao,
    private val masterDao: MasterCardDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExternalCollectionOverviewViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")

            return ExternalCollectionOverviewViewModel(dao, masterDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
