package com.example.prototyp.wishlist

import androidx.room.Entity

@Entity(tableName = "wishlist", primaryKeys = ["setCode", "cardNumber"])
data class WishlistEntry(
    val setCode: String,
    val cardNumber: Int,
    var quantity: Int
)