package com.example.prototyp.deckBuilder

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var name: String,
    var colorHex: String // z.B. "#FFC107" für die Kachelfarbe
)