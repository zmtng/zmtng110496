package com.example.prototyp.deckBuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeckViewModel(private val deckDao: DeckDao) : ViewModel() {

    val allDecks = deckDao.observeAllDecks()

    fun createDeck(name: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newDeck = Deck(name = name, colorHex = colorHex)
            deckDao.insertDeck(newDeck)
        }
    }

    fun importDeckFromText(deckName: String, colorHex: String, importText: String) {
        viewModelScope.launch(Dispatchers.IO) {

            val cardCodeRegex = "([A-Z]{3})-([0-9]{3})-[0-9]+".toRegex()

            val deckCards = cardCodeRegex.findAll(importText)
                .map { match ->

                    val setCode = match.groupValues[1]
                    val cardNumber = match.groupValues[2].toInt()
                    setCode to cardNumber
                }
                .groupingBy { it }
                .eachCount()
                .map { (cardKey, quantity) ->

                    DeckCard(
                        deckId = 0,
                        setCode = cardKey.first,
                        cardNumber = cardKey.second,
                        quantity = quantity
                    )
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