package com.example.prototyp.wishlist

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("""
        SELECT
            w.setCode, w.cardNumber, w.quantity,
            m.cardName, m.setName, m.color
        FROM wishlist w
        JOIN master_cards m ON w.setCode = m.setCode AND w.cardNumber = m.cardNumber
        WHERE 
            (:nameQuery = '' OR m.cardName LIKE '%' || :nameQuery || '%') AND
            (:colorFilter = '' OR m.color = :colorFilter) AND
            (:setFilter = '' OR m.setCode = :setFilter)
        ORDER BY m.cardName ASC
    """)
    fun observeFilteredWishlist(nameQuery: String, colorFilter: String, setFilter: String): Flow<List<WishlistCard>>

    @Transaction
    suspend fun upsertCard(setCode: String, cardNumber: Int) {

        Log.d("WishlistDao", "Upsert wird versucht für: $setCode-$cardNumber")
        val existing = getEntry(setCode, cardNumber)
        if (existing == null) {
            insertEntry(WishlistEntry(setCode, cardNumber, 1))
        } else {
            updateQuantity(setCode, cardNumber, existing.quantity + 1)
        }
    }

    // --- Private Helfer-Funktionen für die Upsert-Logik ---
    @Query("SELECT * FROM wishlist WHERE setCode = :setCode AND cardNumber = :cardNumber")
    suspend fun getEntry(setCode: String, cardNumber: Int): WishlistEntry?

    @Insert
    suspend fun insertEntry(entry: WishlistEntry)

    @Query("UPDATE wishlist SET quantity = :quantity WHERE setCode = :setCode AND cardNumber = :cardNumber")
    suspend fun updateQuantity(setCode: String, cardNumber: Int, quantity: Int)


    @Query("DELETE FROM wishlist WHERE setCode = :setCode AND cardNumber = :cardNumber")
    suspend fun deleteEntry(setCode: String, cardNumber: Int)

    @Transaction
    suspend fun decrementOrDelete(setCode: String, cardNumber: Int, removeQuantity: Int) {
        // 1. Hole den aktuellen Eintrag
        val existing = getEntry(setCode, cardNumber) ?: return

        // 2. Prüfe, ob die Menge nach dem Abzug 0 oder weniger wäre
        if (existing.quantity <= removeQuantity) {
            // Wenn ja, lösche den kompletten Eintrag
            deleteEntry(existing.setCode, existing.cardNumber)
        } else {
            // Wenn nein, aktualisiere nur die Menge
            updateQuantity(setCode, cardNumber, existing.quantity - removeQuantity)
        }
    }
}