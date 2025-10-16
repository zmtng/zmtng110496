package com.example.prototyp.trade

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {

    data class TradeResult(
        val setCode: String,
        val cardNumber: Int,
        val cardName: String,
        val setName: String,
        val color: String,
        val theirQuantity: Int, // How many they have / want
        val yourQuantity: Int    // How many you have / want
    )

    @Query("""
        SELECT
            m.setCode,
            m.cardNumber,
            m.cardName,
            m.setName,
            m.color,
            ec.quantity AS theirQuantity,
            w.quantity AS yourQuantity
        FROM external_collection_cards ec
        JOIN wishlist w ON ec.setCode = w.setCode AND ec.cardNumber = w.cardNumber
        JOIN master_cards m ON ec.setCode = m.setCode AND ec.cardNumber = m.cardNumber
        WHERE ec.collectionId = :externalCollectionId
    """)
    fun findWhatTheyHaveThatYouWant(externalCollectionId: Int): Flow<List<TradeResult>>

    @Query("""
        SELECT
            m.setCode,
            m.cardNumber,
            m.cardName,
            m.setName,
            m.color,
            ewc.quantity AS theirQuantity,
            c.quantity AS yourQuantity
        FROM external_wishlist_cards ewc
        JOIN collection c ON ewc.setCode = c.setCode AND ewc.cardNumber = c.cardNumber
        JOIN master_cards m ON ewc.setCode = m.setCode AND ewc.cardNumber = m.cardNumber
        WHERE ewc.wishlistId = :externalWishlistId
    """)
    fun findWhatYouHaveThatTheyWant(externalWishlistId: Int): Flow<List<TradeResult>>

    @Query("""
        SELECT
            m.setCode,
            m.cardNumber,
            m.cardName,
            m.setName,
            m.color,
            ewc.quantity AS theirQuantity,
            dc.quantity AS yourQuantity
        FROM external_wishlist_cards ewc
        JOIN deck_cards dc ON ewc.setCode = dc.setCode AND ewc.cardNumber = dc.cardNumber
        JOIN master_cards m ON ewc.setCode = m.setCode AND ewc.cardNumber = m.cardNumber
        WHERE ewc.wishlistId = :externalWishlistId AND dc.deckId = :deckId
    """)
    fun findWhatYouHaveInDeckThatTheyWant(externalWishlistId: Int, deckId: Int): Flow<List<TradeResult>>
}
