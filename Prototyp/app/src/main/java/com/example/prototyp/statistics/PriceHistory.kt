package com.example.prototyp.statistics

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_history")
data class PriceHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val setCode: String,
    val cardNumber: Int,
    val price: Double,
    val timestamp: Long
)
