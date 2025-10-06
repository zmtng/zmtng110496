package com.example.prototyp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.prototyp.CollectionEntry
import kotlinx.coroutines.flow.Flow

/**
 * CardDao: arbeitet NUR auf der Tabelle `collection`.
 * Keine Joins, keine Verweise auf `card_master` oder `card_set`.
 *
 * Erwartetes Entity-Schema:
 * @Entity(tableName = "collection", primaryKeys = ["setCode","cardNumber"])
 * data class CollectionEntry(
 * val setCode: String,
 * val cardNumber: Int,
 * val quantity: Int,
 * val price: Double? = null,
 * val color: String,
 * // NEU: Notiz-Felder hinzufügen
 * val personal_notes: String? = null,
 * val general_notes: String? = null
 * )
 */
@Dao
interface CardDao {

    data class CollectionRowData(
        val setCode: String,
        val cardNumber: Int,
        val quantity: Int,
        val price: Double?,
        val color: String,
        val personalNotes: String?,
        val generalNotes: String?,
        val cardName: String, // vom JOIN
        val setName: String   // vom JOIN
    )

    //Sortierte Abfragen
    @Query("""
        SELECT
            c.setCode, c.cardNumber, c.quantity, c.price, c.color, c.personalNotes, c.generalNotes,
            m.cardName, m.setName
        FROM collection c
        JOIN master_cards m ON c.setCode = m.setCode AND c.cardNumber = m.cardNumber
        ORDER BY m.cardName ASC
    """)
    fun observeCollectionSortedByName(): Flow<List<CollectionRowData>>

    @Query("""
        SELECT
            c.setCode, c.cardNumber, c.quantity, c.price, c.color, c.personalNotes, c.generalNotes,
            m.cardName, m.setName
        FROM collection c
        JOIN master_cards m ON c.setCode = m.setCode AND c.cardNumber = m.cardNumber
        ORDER BY c.setCode ASC, c.cardNumber ASC
    """)
    fun observeCollectionSortedByNumber(): Flow<List<CollectionRowData>>

    @Query("""
        SELECT
            c.setCode, c.cardNumber, c.quantity, c.price, c.color, c.personalNotes, c.generalNotes,
            m.cardName, m.setName
        FROM collection c
        JOIN master_cards m ON c.setCode = m.setCode AND c.cardNumber = m.cardNumber
        ORDER BY c.color ASC, m.cardName ASC
    """)
    fun observeCollectionSortedByColor(): Flow<List<CollectionRowData>>

    // EXPORT
    /** Liest die komplette Sammlung als Liste für den Export. */
    @Query("SELECT * FROM collection")
    suspend fun getCollectionForExport(): List<CollectionEntry>


    // IMPORT
    /** Löscht die bestehende Sammlung und fügt eine neue Liste ein. */
    @Transaction
    suspend fun overrideCollection(entries: List<CollectionEntry>) {
        deleteAll() // Zuerst alles löschen
        insertAll(entries) // Dann die neuen Einträge einfügen
    }

    @Query("DELETE FROM collection")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<CollectionEntry>)

    // ---------------------------
    // Insert / Upsert / Delete
    // ---------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun tryInsert(entry: CollectionEntry): Long

    @Query("""
    UPDATE collection
    SET quantity = quantity + :delta
    WHERE setCode = :setCode AND cardNumber = :cardNumber
""")
    suspend fun addQuantity(setCode: String, cardNumber: Int, delta: Int)

    @Transaction
    suspend fun upsertBySetAndNumber(setCode: String, cardNumber: Int, delta: Int, color: String) {
        val inserted = tryInsert(
            // WICHTIG: CollectionEntry wurde um die Notiz-Felder erweitert
            CollectionEntry(setCode, cardNumber, delta, null, color, null, null)
        )
        if (inserted == -1L) {
            addQuantity(setCode, cardNumber, delta)
        }
    }

    /** Eintrag löschen (Composite Key). */
    @Query("""
    DELETE FROM collection
    WHERE setCode = :setCode AND cardNumber = :cardNumber
""")
    suspend fun deleteByKey(setCode: String, cardNumber: Int)

    // ---------------------------
    // Menge, Preis & Notizen
    // ---------------------------

    @Query("""
        UPDATE collection
        SET quantity = CASE WHEN :quantity < 0 THEN 0 ELSE :quantity END
        WHERE setCode = :setCode AND cardNumber = :cardNumber
    """)
    suspend fun setQuantity(setCode: String, cardNumber: Int, quantity: Int)

    @Query("""
        UPDATE collection
        SET price = :price
        WHERE setCode = :setCode AND cardNumber = :cardNumber
    """)
    suspend fun updatePrice(setCode: String, cardNumber: Int, price: Double?)

    // ##### HINZUGEFÜGT: updateNotes Methode #####
    @Query("""
        UPDATE collection
        SET personalNotes = :personalNotes, generalNotes = :generalNotes
        WHERE setCode = :setCode AND cardNumber = :cardNumber
    """)
    suspend fun updateNotes(setCode: String, cardNumber: Int, personalNotes: String?, generalNotes: String?)


    // ---------------------------
    // Lesen aus collection (ohne Joins)
    // ---------------------------

    // Alle Collection-Einträge (roh, ohne Joins)
    @Query("""
    SELECT
      setCode         AS setCode,
      cardNumber      AS cardNumber,
      quantity        AS quantity,
      price           AS price,
      color           AS color,
      personalNotes  AS personalNotes,
      generalNotes   AS generalNotes
    FROM collection
    ORDER BY setCode, cardNumber
""")
    fun observeCollectionRaw(): kotlinx.coroutines.flow.Flow<List<CollectionItem>>

    // Nach Set filtern
    @Query("""
    SELECT
      setCode         AS setCode,
      cardNumber      AS cardNumber,
      quantity        AS quantity,
      price           AS price,
      color           AS color,
      personalNotes  AS personalNotes,
      generalNotes   AS generalNotes
    FROM collection
    WHERE setCode = :setCode
    ORDER BY cardNumber
""")
    suspend fun listBySetRaw(setCode: String): List<CollectionItem>

    // Einzelnen Eintrag holen
    @Query("""
    SELECT
      setCode         AS setCode,
      cardNumber      AS cardNumber,
      quantity        AS quantity,
      price           AS price,
      color           AS color,
      personalNotes  AS personalNotes,
      generalNotes   AS generalNotes
    FROM collection
    WHERE setCode = :setCode AND cardNumber = :cardNumber
    LIMIT 1
""")
    suspend fun getOneRaw(setCode: String, cardNumber: Int): CollectionItem?

    @Query("""
    SELECT * FROM collection
    WHERE setCode = :setCode AND cardNumber = :cardNumber
    LIMIT 1
""")
    suspend fun getByKey(setCode: String, cardNumber: Int): CollectionEntry?


}

/** Kleines DTO nur für `collection`-Spalten (keine Joins). */
// ##### ANGEPASST: Notiz-Felder hinzugefügt #####
data class CollectionItem(
    val setCode: String,
    val cardNumber: Int,
    val quantity: Int,
    val price: Double?,
    val color: String,
    val personalNotes: String?,
    val generalNotes: String?,
)

