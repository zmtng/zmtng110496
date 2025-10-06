package com.example.prototyp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prototyp.data.db.CardDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

enum class SortOrder {
    BY_NAME, BY_NUMBER, BY_COLOR
}

class CollectionViewModel(private val cardDao: CardDao) : ViewModel() {

    val currentSortOrder = MutableStateFlow(SortOrder.BY_NAME)

    val collection = currentSortOrder.flatMapLatest { sortOrder ->
        when (sortOrder) {
            SortOrder.BY_NAME -> cardDao.observeCollectionSortedByName()
            SortOrder.BY_NUMBER -> cardDao.observeCollectionSortedByNumber()
            SortOrder.BY_COLOR -> cardDao.observeCollectionSortedByColor()
        }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        currentSortOrder.value = sortOrder
    }

    // ##### HINZUGEFÜGT: updateNotes #####
    fun updateNotes(setCode: String, cardNumber: Int, personalNotes: String?, generalNotes: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            cardDao.updateNotes(setCode, cardNumber, personalNotes, generalNotes)
        }
    }

    // ##### HINZUGEFÜGT: incrementQuantity #####
    fun incrementQuantity(row: CardDao.CollectionRowData) {
        viewModelScope.launch(Dispatchers.IO) {
            cardDao.addQuantity(row.setCode, row.cardNumber, +1)
        }
    }

    // ##### HINZUGEFÜGT: decrementQuantity (inklusive Lösch-Logik) #####
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
}