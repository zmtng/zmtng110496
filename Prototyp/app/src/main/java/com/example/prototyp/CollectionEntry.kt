package com.example.prototyp

import androidx.room.Entity

@Entity(
    tableName = "collection",
    primaryKeys = ["setCode", "cardNumber"]
)
data class CollectionEntry(
    val setCode: String,
    val cardNumber: Int,
    val quantity: Int,
    val price: Double?,
    var personalNotes: String? = null,
    var generalNotes: String? = null
)