package com.example.prototyp.statistics

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "total_value_history")
data class TotalValueHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val totalValue: Double
)