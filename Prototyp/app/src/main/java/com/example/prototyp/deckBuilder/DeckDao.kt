package com.example.prototyp.deckBuilder

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {

    @Insert
    suspend fun insertDeck(deck: Deck): Long

    @Query("SELECT * FROM decks ORDER BY name ASC")
    fun observeAllDecks(): Flow<List<Deck>>
    data class DeckCardDetail(
        // Basis-Infos
        val setCode: String,
        val cardNumber: Int,
        val quantity: Int,
        val cardName: String,
        val setName: String,
        val color: String,
        val inCollection: Boolean,
        val onWishlist: Boolean
    )

    @Query("""
        SELECT
            dc.setCode, dc.cardNumber, dc.quantity, dc.color,
            m.cardName, m.setName,
            CASE WHEN c.quantity > 0 THEN 1 ELSE 0 END as inCollection,
            CASE WHEN w.quantity > 0 THEN 1 ELSE 0 END as onWishlist
        FROM deck_cards dc
        JOIN master_cards m ON dc.setCode = m.setCode AND dc.cardNumber = m.cardNumber
        LEFT JOIN collection c ON dc.setCode = c.setCode AND dc.cardNumber = c.cardNumber
        LEFT JOIN wishlist w ON dc.setCode = w.setCode AND dc.cardNumber = w.cardNumber
        WHERE dc.deckId = :deckId
        ORDER BY m.cardName ASC
    """)
    fun observeDeckContents(deckId: Int): Flow<List<DeckCardDetail>>

    @Transaction
    suspend fun upsertCardInDeck(deckId: Int, setCode: String, cardNumber: Int, color: String) {
        val existing = getCardInDeck(deckId, setCode, cardNumber)
        if (existing == null) {
            // Karte ist neu im Deck -> mit Menge 1 einfügen
            insertCardInDeck(DeckCard(deckId, setCode, cardNumber, 1, color))
        } else {
            // Karte ist schon im Deck -> Menge um 1 erhöhen
            updateCardQuantity(deckId, setCode, cardNumber, existing.quantity + 1)
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeckCards(cards: List<DeckCard>)

    @Transaction
    suspend fun createDeckWithCards(deck: Deck, cards: List<DeckCard>) {
        val deckId = insertDeck(deck)

        val cardsWithDeckId = cards.map { it.copy(deckId = deckId.toInt()) }

        insertDeckCards(cardsWithDeckId)
    }



    @Delete
    suspend fun deleteDeck(deck: Deck)
}
