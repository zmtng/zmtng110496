package com.example.prototyp

import androidx.room.ColumnInfo
import androidx.room.Entity

//Kommentar Test-Commit Collection-Entry
@Entity(
    tableName = "collection",
    primaryKeys = ["setCode", "cardNumber"]
)
data class CollectionEntry(
    val setCode: String,
    val cardNumber: Int,
    val quantity: Int,
    val price: Double?,
    @ColumnInfo(name = "color", defaultValue = "'R'")
    val color: String = "R",   // **nicht** nullable + Default
    var personalNotes: String? = null,
    var generalNotes: String? = null
)