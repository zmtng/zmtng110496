package com.example.prototyp.deckBuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prototyp.MasterCard
import com.example.prototyp.MasterCardDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeckViewModel(
    private val deckDao: DeckDao,
    private val masterCardDao: MasterCardDao // Erforderlich für den Farb-Lookup
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
            val cardCodeRegex = "([A-Z]{3})-([0-9]{3})-[0-9]+".toRegex()

            // 1. Alle Karten-Codes aus dem Text extrahieren
            val parsedCardKeys = cardCodeRegex.findAll(importText)
                .mapNotNull { match ->
                    val setCode = match.groupValues[1]
                    val cardNumber = match.groupValues[2].toIntOrNull()
                    if (cardNumber != null) setCode to cardNumber else null
                }.toList()

            // 2. Einmalig alle benötigten MasterCards aus der DB holen
            val masterCardMap = mutableMapOf<Pair<String, Int>, MasterCard>()
            for (key in parsedCardKeys.distinct()) {
                val masterCard = masterCardDao.getBySetAndNumber(key.first, key.second)
                if (masterCard != null) {
                    masterCardMap[key] = masterCard
                }
            }

            // 3. Die geparsten Karten gruppieren und mit der korrekten Farbe aus der Map erstellen
            val deckCards = parsedCardKeys
                .groupingBy { it }
                .eachCount()
                .mapNotNull { (cardKey, quantity) ->
                    val masterCard = masterCardMap[cardKey] // Nachschlagen in der lokalen Map
                    if (masterCard != null) {
                        DeckCard(
                            deckId = 0,
                            setCode = cardKey.first,
                            cardNumber = cardKey.second,
                            quantity = quantity,
                            color = masterCard.color, // Korrekte Farbe wird hier zugewiesen
                            price = null
                        )
                    } else {
                        null // Karte wurde nicht in der Master-DB gefunden
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

