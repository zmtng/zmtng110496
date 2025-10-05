package com.example.prototyp

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "master_cards", primaryKeys = ["setCode","cardNumber"])
data class MasterCard(
    val setCode: String,
    val cardNumber: Int,
    val cardName: String,
    val setName: String,
    @ColumnInfo(name = "color", defaultValue = "'R'")
    val color: String = "R"
)