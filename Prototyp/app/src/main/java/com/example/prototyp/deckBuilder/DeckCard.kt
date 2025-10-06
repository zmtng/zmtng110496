package com.example.prototyp.deckBuilder

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "deck_cards",
    primaryKeys = ["deckId", "setCode", "cardNumber"], // Jede Karte pro Deck ist einzigartig
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE // WICHTIG: Löscht Karteneinträge, wenn das Deck gelöscht wird
        )
    ]
)
data class DeckCard(
    val deckId: Int,
    val setCode: String,
    val cardNumber: Int,
    var quantity: Int
)