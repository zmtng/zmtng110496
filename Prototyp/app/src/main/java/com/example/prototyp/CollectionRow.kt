package com.example.prototyp

data class CollectionRow(
    val cardId: Long? = null,        // aus c.uid
    val name: String,        // c.Name
    val setCode: String,     // c.`Set`
    val setName: String?,    // m.setName (kann bei LEFT JOIN null sein)
    val number: Int,        // m.cardNumber (kann null sein)
    val quantity: Int,       // c.Anzahl
    val price: Double? = null, // c.Preis
    val color: String
)
