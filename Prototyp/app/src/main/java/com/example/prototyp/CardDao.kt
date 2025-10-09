package com.example.prototyp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.prototyp.CollectionEntry
import kotlinx.coroutines.flow.Flow
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

    @Query("UPDATE collection SET price = :price WHERE setCode = :setCode AND cardNumber = :cardNumber")
    suspend fun updatePrice(setCode: String, cardNumber: Int, price: Double?)

    @Query("""
        SELECT
            c.setCode, c.cardNumber, c.quantity, c.price, c.color, c.personalNotes, c.generalNotes,
            m.cardName, m.setName
        FROM collection c
        JOIN master_cards m ON c.setCode = m.setCode AND c.cardNumber = m.cardNumber
        WHERE 
            (:nameQuery = '' OR m.cardName LIKE '%' || :nameQuery || '%') AND
            (:colorFilter = '' OR c.color = :colorFilter) AND
            (:setFilter = '' OR c.setCode = :setFilter)
        ORDER BY
            CASE :sortBy WHEN 'name' THEN m.cardName END ASC,
            CASE :sortBy WHEN 'number' THEN c.cardNumber END ASC,
            CASE :sortBy WHEN 'color' THEN c.color END ASC
    """)
    fun observeFilteredCollection(
        nameQuery: String,
        colorFilter: String,
        setFilter: String,
        sortBy: String
    ): Flow<List<CollectionRowData>>

    @Query("""
        SELECT
            c.setCode, c.cardNumber, c.quantity, c.price, c.color, c.personalNotes, c.generalNotes,
            m.cardName, m.setName
        FROM collection c
        JOIN master_cards m ON c.setCode = m.setCode AND c.cardNumber = m.cardNumber
    """)
    fun observeCollectionWithDetails(): Flow<List<CollectionRowData>>

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

    @Query("DELETE FROM collection WHERE setCode = :setCode AND cardNumber = :cardNumber")
    suspend fun deleteBySetAndNumber(setCode: String, cardNumber: Int)


}

data class CollectionItem(
    val setCode: String,
    val cardNumber: Int,
    val quantity: Int,
    val price: Double?,
    val color: String,
    val personalNotes: String?,
    val generalNotes: String?,
)

