package com.example.prototyp.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.prototyp.MasterCardDao
import com.example.prototyp.data.db.CardDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

// Data class to hold the combined statistics for a single set
data class SetCompletionStat(
    val setName: String,
    val ownedUniqueCards: Int,
    val totalCardsInSet: Int
) {
    val percentage: Float
        get() = if (totalCardsInSet > 0) {
            (ownedUniqueCards.toFloat() / totalCardsInSet) * 100
        } else {
            0f
        }
}

class StatisticsViewModel(
    private val cardDao: CardDao,
    private val masterDao: MasterCardDao
) : ViewModel() {

    // Flow that combines data from two DAOs to calculate set completion statistics
    val setCompletionStats: StateFlow<List<SetCompletionStat>> = combine(
        masterDao.getSetCardCounts(), // Total cards per set
        cardDao.getOwnedUniqueCardCountsPerSet() // Owned unique cards per set
    ) { totalCounts, ownedCounts ->
        val ownedMap = ownedCounts.associate { it.setName to it.count }
        totalCounts.map { totalCount ->
            val owned = ownedMap[totalCount.setName] ?: 0
            SetCompletionStat(
                setName = totalCount.setName,
                ownedUniqueCards = owned,
                totalCardsInSet = totalCount.count
            )
        }.sortedBy { it.setName } // Sort alphabetically by set name
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow for the top 5 most valuable cards
    val topValuableCards: StateFlow<List<CardDao.CollectionRowData>> =
        cardDao.getTopValuableCards()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

}

// Factory for creating StatisticsViewModel
class StatisticsViewModelFactory(
    private val cardDao: CardDao,
    private val masterDao: MasterCardDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatisticsViewModel(cardDao, masterDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
