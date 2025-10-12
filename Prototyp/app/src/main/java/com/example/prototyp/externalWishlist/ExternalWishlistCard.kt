package com.example.prototyp.externalWishlist

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "external_wishlist_cards",
    primaryKeys = ["wishlistId", "setCode", "cardNumber"],
    foreignKeys = [
        ForeignKey(
            entity = ExternalWishlist::class,
            parentColumns = ["id"],
            childColumns = ["wishlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExternalWishlistCard(
    val wishlistId: Int,
    val setCode: String,
    val cardNumber: Int,
    var quantity: Int
)
