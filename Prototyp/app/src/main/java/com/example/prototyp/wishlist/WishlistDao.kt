package com.example.prototyp.wishlist

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {

    // Ein DTO für das Ergebnis der Abfrage
    data class WishlistCard(
        val setCode: String,
        val cardNumber: Int,
        val quantity: Int, // gewünschte Anzahl
        val cardName: String,
        val setName: String,
        val color: String
    )

    // Abfrage, die die Wunschliste mit der Master-Tabelle verknüpft
    @Query("""
        SELECT
            w.setCode, w.cardNumber, w.quantity,
            m.cardName, m.setName, m.color
        FROM wishlist w
        JOIN master_cards m ON w.setCode = m.setCode AND w.cardNumber = m.cardNumber
        ORDER BY m.cardName ASC
    """)
    fun observeWishlist(): Flow<List<WishlistCard>>

    // Weitere Funktionen (hinzufügen, löschen, etc.) kommen in den nächsten Schritten
}