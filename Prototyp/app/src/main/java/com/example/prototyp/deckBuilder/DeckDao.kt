package com.example.prototyp.deckBuilder

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {

    @Insert
    suspend fun insertDeck(deck: Deck): Long

    data class DeckWithCardCount(
        @Embedded val deck: Deck,
        val cardCount: Int
    )

    @Query("""
        SELECT d.*, (SELECT SUM(dc.quantity) FROM deck_cards dc WHERE dc.deckId = d.id) as cardCount
        FROM decks d
        ORDER BY d.name ASC
    """)
    fun observeAllDecksWithCardCount(): Flow<List<DeckWithCardCount>>

    data class DeckCardDetail(
        val setCode: String,
        val cardNumber: Int,
        val cardName: String,
        val setName: String,
        val color: String,
        val price: Double?,
        val collectionQuantity: Int,
        val quantityInDeck: Int,
        val wishlistQuantity: Int
    )

    @Query("""
    SELECT
        dc.setCode,
        dc.cardNumber,
        m.cardName,
        m.setName,
        m.color,
        dc.quantity as quantityInDeck,
        dc.price, 
        COALESCE(c.quantity, 0) as collectionQuantity,
        COALESCE(w.quantity, 0) as wishlistQuantity
    FROM deck_cards dc
    JOIN master_cards m ON dc.setCode = m.setCode AND dc.cardNumber = m.cardNumber
    LEFT JOIN collection c ON dc.setCode = c.setCode AND dc.cardNumber = c.cardNumber
    LEFT JOIN wishlist w ON dc.setCode = w.setCode AND dc.cardNumber = w.cardNumber
    WHERE dc.deckId = :deckId
    ORDER BY m.cardName ASC
""")
    fun observeDeckContentsWithDetails(deckId: Int): Flow<List<DeckCardDetail>>


    @Delete
    suspend fun deleteDeck(deck: Deck)

    @Transaction
    suspend fun upsertCardInDeck(deckId: Int, setCode: String, cardNumber: Int) {
        val existing = getCardInDeck(deckId, setCode, cardNumber)
        if (existing == null) {
            insertCardInDeck(DeckCard(deckId, setCode, cardNumber, 1, null))
        } else {
            addQuantityInDeck(deckId, setCode, cardNumber, 1)
        }
    }

    @Query("UPDATE deck_cards SET quantity = quantity + :delta WHERE deckId = :deckId AND setCode = :setCode AND cardNumber = :cardNumber")
    suspend fun addQuantityInDeck(deckId: Int, setCode: String, cardNumber: Int, delta: Int)

    @Query("DELETE FROM deck_cards WHERE deckId = :deckId AND setCode = :setCode AND cardNumber = :cardNumber")
    suspend fun deleteCardFromDeck(deckId: Int, setCode: String, cardNumber: Int)

    @Query("SELECT * FROM deck_cards WHERE deckId = :deckId AND setCode = :setCode AND cardNumber = :cardNumber")
    suspend fun getCardInDeck(deckId: Int, setCode: String, cardNumber: Int): DeckCard?

    @Insert
    suspend fun insertCardInDeck(deckCard: DeckCard)

    @Query("UPDATE deck_cards SET quantity = :quantity WHERE deckId = :deckId AND setCode = :setCode AND cardNumber = :cardNumber")
    suspend fun updateCardQuantity(deckId: Int, setCode: String, cardNumber: Int, quantity: Int)


    @Query("UPDATE deck_cards SET price = :price WHERE deckId = :deckId AND setCode = :setCode AND cardNumber = :cardNumber")
    suspend fun updateDeckCardPrice(deckId: Int, setCode: String, cardNumber: Int, price: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeckCards(cards: List<DeckCard>)

    @Transaction
    suspend fun createDeckWithCards(deck: Deck, cards: List<DeckCard>) {
        val deckId = insertDeck(deck)
        val cardsWithId = cards.map { it.copy(deckId = deckId.toInt()) }
        insertDeckCards(cardsWithId)
    }
}

