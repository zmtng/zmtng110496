package com.example.prototyp.externalWishlist

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExternalWishlistDao {

    @Insert
    suspend fun insertWishlist(wishlist: ExternalWishlist): Long

    @Query("SELECT * FROM external_wishlists ORDER BY name ASC")
    fun observeAllWishlists(): Flow<List<ExternalWishlist>>

    @Delete
    suspend fun deleteWishlist(wishlist: ExternalWishlist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<ExternalWishlistCard>)

    data class CardDetail(
        val setCode: String,
        val cardNumber: Int,
        val quantity: Int,
        val cardName: String,
        val setName: String,
        val color: String,
        val inOwnCollection: Boolean,
        val onOwnWishlist: Boolean
    )

    @Query("""
    SELECT
        ewc.setCode, ewc.cardNumber, ewc.quantity,
        m.cardName, m.setName, m.color,
        CASE WHEN own_c.quantity > 0 THEN 1 ELSE 0 END as inOwnCollection,
        CASE WHEN own_w.quantity > 0 THEN 1 ELSE 0 END as onOwnWishlist
    FROM external_wishlist_cards ewc
    JOIN master_cards m ON ewc.setCode = m.setCode AND ewc.cardNumber = m.cardNumber
    LEFT JOIN collection own_c ON ewc.setCode = own_c.setCode AND ewc.cardNumber = own_c.cardNumber
    LEFT JOIN wishlist own_w ON ewc.setCode = own_w.setCode AND ewc.cardNumber = own_w.cardNumber
    WHERE ewc.wishlistId = :wishlistId
      AND (:nameQuery = '' OR m.cardName LIKE '%' || :nameQuery || '%')
      AND (:colorFilter = '' OR m.color = :colorFilter)
      AND (:setFilter = '' OR m.setCode = :setFilter)
    ORDER BY m.cardName ASC
""")
    fun observeWishlistContents(
        wishlistId: Int,
        nameQuery: String,
        colorFilter: String,
        setFilter: String
    ): Flow<List<CardDetail>>



    @Transaction
    suspend fun createWishlistWithCards(wishlist: ExternalWishlist, cards: List<ExternalWishlistCard>) {
        val wishlistId = insertWishlist(wishlist)
        val cardsWithId = cards.map { it.copy(wishlistId = wishlistId.toInt()) }
        insertCards(cardsWithId)
    }
}