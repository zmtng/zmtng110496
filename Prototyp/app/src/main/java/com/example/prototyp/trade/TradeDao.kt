package com.example.prototyp.trade

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {

    // Datenklasse für ein einheitliches Ergebnis
    data class TradeCard(
        val setCode: String,
        val cardNumber: Int,
        val cardName: String,
        val setName: String,
        val color: String,
        val yourQuantity: Int,   // Deine Anzahl (aus Sammlung oder Wunschliste)
        val theirQuantity: Int // Ihre Anzahl (aus Sammlung oder Wunschliste)
    )

    /**
     * Findet Karten, die du in deiner Sammlung hast und die auf der
     * externen Wunschliste stehen ("Du hast, was sie wollen").
     */
    @Query("""
        SELECT
            m.setCode, m.cardNumber, m.cardName, m.setName, m.color,
            own_c.quantity AS yourQuantity,
            ext_w.quantity AS theirQuantity
        FROM collection own_c
        JOIN external_wishlist_cards ext_w ON own_c.setCode = ext_w.setCode AND own_c.cardNumber = ext_w.cardNumber
        JOIN master_cards m ON own_c.setCode = m.setCode AND own_c.cardNumber = m.cardNumber
        WHERE ext_w.wishlistId = :externalWishlistId
        ORDER BY m.cardName ASC
    """)
    fun findYouHaveWhatTheyWant(externalWishlistId: Int): Flow<List<TradeCard>>

    /**
     * Findet Karten, die auf deiner Wunschliste stehen und die in der
     * externen Sammlung vorhanden sind ("Sie haben, was du willst").
     */
    @Query("""
        SELECT
            m.setCode, m.cardNumber, m.cardName, m.setName, m.color,
            own_w.quantity AS yourQuantity,
            ext_c.quantity AS theirQuantity
        FROM wishlist own_w
        JOIN external_collection_cards ext_c ON own_w.setCode = ext_c.setCode AND own_w.cardNumber = ext_c.cardNumber
        JOIN master_cards m ON own_w.setCode = m.setCode AND own_w.cardNumber = m.cardNumber
        WHERE ext_c.collectionId = :externalCollectionId
        ORDER BY m.cardName ASC
    """)
    fun findTheyHaveWhatYouWant(externalCollectionId: Int): Flow<List<TradeCard>>
}