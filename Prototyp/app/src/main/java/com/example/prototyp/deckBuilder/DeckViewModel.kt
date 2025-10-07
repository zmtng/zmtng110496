package com.example.prototyp.deckBuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prototyp.deckBuilder.Deck
import com.example.prototyp.deckBuilder.DeckDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeckViewModel(private val deckDao: DeckDao) : ViewModel() {

    // Ein Flow, der die Liste aller Decks aus der Datenbank bereitstellt
    val allDecks = deckDao.observeAllDecks()

    // Funktion, um ein neues Deck in der Datenbank zu speichern
    fun createDeck(name: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newDeck = Deck(name = name, colorHex = colorHex)
            deckDao.insertDeck(newDeck)
        }
    }

    fun importDeckFromText(deckName: String, colorHex: String, importText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Regex, um "SET-NUMMER-EDITION" zu extrahieren
            val cardCodeRegex = "([A-Z]{3})-([0-9]{3})-[0-9]+".toRegex()

            val deckCards = cardCodeRegex.findAll(importText)
                .map { match ->
                    // Extrahiere Set-Code und Kartennummer
                    val setCode = match.groupValues[1]
                    val cardNumber = match.groupValues[2].toInt()
                    // Erstelle ein Paar zur einfachen Gruppierung
                    setCode to cardNumber
                }
                .groupingBy { it } // Gruppiere identische Paare
                .eachCount()       // Zähle, wie oft jedes Paar vorkommt
                .map { (cardKey, quantity) ->
                    // Wandle das Ergebnis in DeckCard-Objekte um
                    DeckCard(
                        deckId = 0, // Platzhalter, wird in der Transaktion gesetzt
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