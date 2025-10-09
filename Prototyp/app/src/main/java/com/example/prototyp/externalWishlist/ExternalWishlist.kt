package com.example.prototyp.externalWishlist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "external_wishlists")
data class ExternalWishlist(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var name: String
)