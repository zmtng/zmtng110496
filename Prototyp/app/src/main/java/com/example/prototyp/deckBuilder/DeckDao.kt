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
    suspend fun insertDeck(deck: Deck): Long // Gibt die ID des neuen Decks zurück

    @Query("SELECT * FROM decks ORDER BY name ASC")
    fun observeAllDecks(): Flow<List<Deck>>

    // ##### HINZUGEFÜGT: DTO für die Detailansicht #####
    // Dieses Objekt hält das Ergebnis unserer komplexen Abfrage.
    data class DeckCardDetail(
        // Basis-Infos
        val setCode: String,
        val cardNumber: Int,
        val quantity: Int, // Anzahl im Deck
        val cardName: String,
        val setName: String,
        val color: String,
        // Der "in Sammlung"-Status
        val inCollection: Boolean
    )

    // ##### HINZUGEFÜGT: Die komplexe JOIN-Abfrage #####
    @Query("""
        SELECT
            dc.setCode, dc.cardNumber, dc.quantity,
            m.cardName, m.setName, m.color,
            CASE WHEN c.quantity > 0 THEN 1 ELSE 0 END as inCollection
        FROM deck_cards dc
        JOIN master_cards m ON dc.setCode = m.setCode AND dc.cardNumber = m.cardNumber
        LEFT JOIN collection c ON dc.setCode = c.setCode AND dc.cardNumber = c.cardNumber
        WHERE dc.deckId = :deckId
        ORDER BY m.cardName ASC
    """)
    fun observeDeckContents(deckId: Int): Flow<List<DeckCardDetail>>

    @Transaction
    suspend fun upsertCardInDeck(deckId: Int, setCode: String, cardNumber: Int) {
        val existing = getCardInDeck(deckId, setCode, cardNumber)
        if (existing == null) {
            // Karte ist neu im Deck -> mit Menge 1 einfügen
            insertCardInDeck(DeckCard(deckId, setCode, cardNumber, 1))
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

    // ##### HINZUGEFÜGT: Funktion zum Einfügen einer Liste von Karten #####
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeckCards(cards: List<DeckCard>)

    // ##### HINZUGEFÜGT: Transaktion für den kompletten Import #####
    @Transaction
    suspend fun createDeckWithCards(deck: Deck, cards: List<DeckCard>) {
        // 1. Erstelle das neue Deck und hole dir seine generierte ID
        val deckId = insertDeck(deck)

        // 2. Weise allen Karten diese neue Deck-ID zu
        val cardsWithDeckId = cards.map { it.copy(deckId = deckId.toInt()) }

        // 3. Füge alle Karten auf einmal in die Datenbank ein
        insertDeckCards(cardsWithDeckId)
    }

    @Delete
    suspend fun deleteDeck(deck: Deck)
}