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
}