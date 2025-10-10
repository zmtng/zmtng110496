package com.example.prototyp.externalCollection

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "external_collection_cards",
    primaryKeys = ["collectionId", "setCode", "cardNumber"],
    foreignKeys = [
        ForeignKey(
            entity = ExternalCollection::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExternalCollectionCard(
    val collectionId: Int,
    val setCode: String,
    val cardNumber: Int,
    var quantity: Int,
    var price: Double?,
    val color: String
)
