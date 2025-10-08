package com.example.prototyp.externalCollection

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "external_collections")
data class ExternalCollection(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var name: String
)