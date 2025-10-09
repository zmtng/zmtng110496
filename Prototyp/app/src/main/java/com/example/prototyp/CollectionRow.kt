package com.example.prototyp

data class CollectionRow(
    val cardId: Long? = null,        // aus c.uid
    val name: String,        // c.Name
    val setCode: String,     // c.`Set`
    val setName: String?,    // m.setName
    val number: Int,        // m.cardNumber
    val quantity: Int,       // c.Anzahl
    val price: Double? = null, // c.Preis
    val color: String,
    val personalNotes: String? = null,
    val generalNotes: String? = null
)
