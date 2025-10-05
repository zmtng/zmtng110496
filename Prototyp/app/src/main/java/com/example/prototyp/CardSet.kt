package com.example.prototyp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "card_set")
data class CardSet(
    @PrimaryKey val code: String,   // z.B. "OGN", "OGS"
    val name: String                // "Origins (Main Set)", ...
)