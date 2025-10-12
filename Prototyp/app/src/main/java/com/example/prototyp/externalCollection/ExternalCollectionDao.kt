package com.example.prototyp.externalCollection

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExternalCollectionDao {

    @Insert
    suspend fun insertCollection(collection: ExternalCollection): Long

    @Query("SELECT * FROM external_collections ORDER BY name ASC")
    fun observeAllCollections(): Flow<List<ExternalCollection>>

    @Delete
    suspend fun deleteCollection(collection: ExternalCollection)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<ExternalCollectionCard>)

    data class CardDetail(
        val setCode: String,
        val cardNumber: Int,
        val quantity: Int,
        val price: Double?,
        val cardName: String,
        val setName: String,
        val color: String,
        val collectionQuantity: Int, // Formerly inOwnCollection
        val wishlistQuantity: Int   // Formerly onOwnWishlist
    )

    @Query("""
        SELECT
            ec.setCode,
            ec.cardNumber,
            ec.quantity,
            ec.price,
            m.cardName,
            m.setName,
            m.color,
            COALESCE(own_c.quantity, 0) as collectionQuantity,
            COALESCE(own_w.quantity, 0) as wishlistQuantity
        FROM external_collection_cards ec
        JOIN master_cards m ON ec.setCode = m.setCode AND ec.cardNumber = m.cardNumber
        LEFT JOIN collection own_c ON ec.setCode = own_c.setCode AND ec.cardNumber = own_c.cardNumber
        LEFT JOIN wishlist own_w ON ec.setCode = own_w.setCode AND ec.cardNumber = own_w.cardNumber
        WHERE ec.collectionId = :collectionId
        ORDER BY m.cardName ASC
    """)
    fun observeCollectionContents(collectionId: Int): Flow<List<CardDetail>>

    @Transaction
    suspend fun createCollectionWithCards(collection: ExternalCollection, cards: List<ExternalCollectionCard>) {
        val collectionId = insertCollection(collection)
        val cardsWithId = cards.map { it.copy(collectionId = collectionId.toInt()) }
        insertCards(cardsWithId)
    }

    @Query("""
        SELECT
            ec.setCode, 
            ec.cardNumber, 
            ec.quantity, 
            ec.price,
            m.cardName, 
            m.setName, 
            m.color,
            COALESCE(own_c.quantity, 0) as collectionQuantity,
            COALESCE(own_w.quantity, 0) as wishlistQuantity
        FROM external_collection_cards ec
        JOIN master_cards m ON ec.setCode = m.setCode AND ec.cardNumber = m.cardNumber
        LEFT JOIN collection own_c ON ec.setCode = own_c.setCode AND ec.cardNumber = own_c.cardNumber
        LEFT JOIN wishlist own_w ON ec.setCode = own_w.setCode AND ec.cardNumber = own_w.cardNumber
        WHERE ec.collectionId = :collectionId
          AND (:nameQuery = '' OR m.cardName LIKE '%' || :nameQuery || '%')
          AND (:colorFilter = '' OR m.color = :colorFilter)
          AND (:setFilter = '' OR ec.setCode = :setFilter)
        ORDER BY m.cardName ASC
    """)
    fun observeCollectionContents(
        collectionId: Int,
        nameQuery: String,
        colorFilter: String,
        setFilter: String
    ): Flow<List<CardDetail>>
}

