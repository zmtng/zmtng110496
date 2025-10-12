package com.example.prototyp.deckBuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prototyp.AppDatabase
import com.example.prototyp.MasterCard
import com.example.prototyp.MasterCardDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DeckViewModel(
    private val deckDao: DeckDao,
    private val masterCardDao: MasterCardDao
) : ViewModel() {

    val allDecks = deckDao.observeAllDecks()

    fun createDeck(name: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newDeck = Deck(name = name, colorHex = colorHex)
            deckDao.insertDeck(newDeck)
        }
    }

    fun importDeckFromText(deckName: String, colorHex: String, importText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // --- NEU: Warten, bis die Datenbank garantiert bereit ist ---
            AppDatabase.isReady.first { it }
            // ---------------------------------------------------------

            val cardCodeRegex = "([A-Z]{3})-([0-9]{3})-[0-9]+".toRegex()

            val parsedCardKeys = cardCodeRegex.findAll(importText)
                .mapNotNull { match ->
                    val setCode = match.groupValues[1]
                    val cardNumber = match.groupValues[2].toIntOrNull()
                    if (cardNumber != null) setCode to cardNumber else null
                }.toList()

            val masterCardMap = mutableMapOf<Pair<String, Int>, MasterCard>()
            for (key in parsedCardKeys.distinct()) {
                val masterCard = masterCardDao.getBySetAndNumber(key.first, key.second)
                if (masterCard != null) {
                    masterCardMap[key] = masterCard
                }
            }

            val deckCards = parsedCardKeys
                .groupingBy { it }
                .eachCount()
                .mapNotNull { (cardKey, quantity) ->
                    val masterCard = masterCardMap[cardKey]
                    if (masterCard != null) {
                        // Da du die 'color' Spalte entfernt hast, wird sie hier auch nicht mehr benötigt.
                        DeckCard(
                            deckId = 0,
                            setCode = cardKey.first,
                            cardNumber = cardKey.second,
                            quantity = quantity,
                            price = null
                        )
                    } else {
                        null
                    }
                }

            if (deckCards.isNotEmpty()) {
                val newDeck = Deck(name = deckName, colorHex = colorHex)
                deckDao.createDeckWithCards(newDeck, deckCards)
            }
        }
    }

    fun deleteDeck(deck: Deck) {
        viewModelScope.launch(Dispatchers.IO) {
            deckDao.deleteDeck(deck)
        }
    }
}

