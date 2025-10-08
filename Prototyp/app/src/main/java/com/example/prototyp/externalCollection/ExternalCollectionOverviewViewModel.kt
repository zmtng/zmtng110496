package com.example.prototyp.externalCollection

import android.net.Uri
import android.content.Context
import androidx.lifecycle.*
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExternalCollectionOverviewViewModel(private val dao: ExternalCollectionDao) : ViewModel() {

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
                val newCards = mutableListOf<ExternalCollectionCard>()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    csvReader().open(inputStream) {
                        readAllWithHeaderAsSequence().forEach { row ->
                            val card = ExternalCollectionCard(
                                collectionId = 0, // Platzhalter
                                setCode = row["setCode"]!!,
                                cardNumber = row["cardNumber"]!!.toInt(),
                                quantity = row["quantity"]!!.toInt(),
                                price = row["price"]?.toDoubleOrNull()
                            )
                            newCards.add(card)
                        }
                    }
                }

                if (newCards.isNotEmpty()) {
                    val newCollection = ExternalCollection(name = name)
                    dao.createCollectionWithCards(newCollection, newCards)
                    // HIER IST DIE KORREKTUR
                    _userMessage.value = "'$name' mit ${newCards.size} Karten importiert!"
                } else {
                    // HIER IST DIE KORREKTUR
                    _userMessage.value = "Import fehlgeschlagen: Datei ist leer oder ungültig."
                }
            } catch (e: Exception) {
                // HIER IST DIE KORREKTUR
                _userMessage.value = "Import fehlgeschlagen: ${e.message}"
            }
        }
    }
}

class ExternalCollectionOverviewViewModelFactory(private val dao: ExternalCollectionDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExternalCollectionOverviewViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExternalCollectionOverviewViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}