package com.example.prototyp.wishlist

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "wishlist",
    primaryKeys = ["setCode", "cardNumber"]
)
data class WishlistEntry(
    val setCode: String,
    val cardNumber: Int,
    val quantity: Int,
    val price: Double?,
    @ColumnInfo(name = "color", defaultValue = "'R'")
    val color: String = "R"   // **nicht** nullable + Default
)