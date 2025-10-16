package com.example.prototyp.trade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TradeResultViewModel(
    tradeDao: TradeDao,
    tradeType: String,
    list1Id: Int,
    list2Id: Int
) : ViewModel() {

    val tradeResults: StateFlow<List<TradeDao.TradeResult>> = when (tradeType) {
        "WANT" -> tradeDao.findWhatTheyHaveThatYouWant(list1Id)
        "OFFER_COLLECTION" -> tradeDao.findWhatYouHaveThatTheyWant(list1Id)
        "OFFER_DECK" -> tradeDao.findWhatYouHaveInDeckThatTheyWant(list1Id, list2Id)
        else -> throw IllegalArgumentException("Invalid trade type")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

}

class TradeResultViewModelFactory(
    private val tradeDao: TradeDao,
    private val tradeType: String,
    private val list1Id: Int,
    private val list2Id: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TradeResultViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TradeResultViewModel(tradeDao, tradeType, list1Id, list2Id) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
