package com.example.prototyp

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "master_cards",
    primaryKeys = ["setCode", "cardNumber"],
    foreignKeys = [ForeignKey(
        entity = CardSet::class,
        parentColumns = ["code"],
        childColumns = ["setCode"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["cardName"])]
)
data class CardMaster(
    val setCode: String,
    val setName: String,
    val cardNumber: Int,
    val cardName: String,
    val color: String
)