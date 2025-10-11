package com.example.prototyp.deckBuilder

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "deck_cards",
    primaryKeys = ["deckId", "setCode", "cardNumber"], // Each card per deck is unique
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE // IMPORTANT: Deletes card entries when the deck is deleted
        )
    ]
)
data class DeckCard(
    val deckId: Int,
    val setCode: String,
    val cardNumber: Int,
    var quantity: Int,
    var color: String,
    var price: Double? = null // Price is now stored directly here
)

