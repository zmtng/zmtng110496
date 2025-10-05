package com.example.prototyp

import androidx.room.ColumnInfo
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
    @ColumnInfo(name = "color", defaultValue = "'R'")
    val color: String = "R"   // **nicht** nullable + Default
)