package com.example.prototyp.deckBuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prototyp.MasterCard
import com.example.prototyp.MasterCardDao

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeckViewModel(private val deckDao: DeckDao, private val masterCardDao: MasterCardDao) : ViewModel() {

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

            // Zuerst alle MasterCards sammeln, die den Einträgen entsprechen
            val foundMasterCards = mutableListOf<MasterCard>()
            cardCodeRegex.findAll(importText).forEach { match ->
                val setCode = match.groupValues[1]
                val cardNumber = match.groupValues[2].toIntOrNull()
                if (cardNumber != null) {
                    // Der suspend-Aufruf erfolgt jetzt korrekt innerhalb der Coroutine
                    masterCardDao.getBySetAndNumber(setCode, cardNumber)?.let { masterCard ->
                        foundMasterCards.add(masterCard)
                    }
                }
            }

            // Jetzt die gesammelten Karten gruppieren und in DeckCards umwandeln
            val deckCards = foundMasterCards
                .groupingBy { it } // Gruppiert nach dem gesamten MasterCard-Objekt
                .eachCount()
                .map { (masterCard, quantity) ->
                    DeckCard(
                        deckId = 0, // Wird in createDeckWithCards gesetzt
                        setCode = masterCard.setCode,
                        cardNumber = masterCard.cardNumber,
                        quantity = quantity,
                        color = masterCard.color // Die korrekte Farbe wird hier verwendet
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

