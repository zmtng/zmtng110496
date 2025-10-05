package com.example.prototyp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.prototyp.CollectionEntry

/**
 * CardDao: arbeitet NUR auf der Tabelle `collection`.
 * Keine Joins, keine Verweise auf `card_master` oder `card_set`.
 *
 * Erwartetes Entity-Schema:
 * @Entity(tableName = "collection", primaryKeys = ["setCode","cardNumber"])
 * data class CollectionEntry(
 *   val setCode: String,
 *   val cardNumber: Int,
 *   val quantity: Int,
 *   val price: Double? = null
 * )
 */
@Dao
interface CardDao {

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
            CollectionEntry(setCode, cardNumber, delta, null, color)
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
    // Menge & Preis
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

    // ---------------------------
    // Lesen aus collection (ohne Joins)
    // ---------------------------

    // Alle Collection-Einträge (roh, ohne Joins)
    @Query("""
    SELECT
      setCode    AS setCode,
      cardNumber AS cardNumber,
      quantity   AS quantity,
      price      AS price,
      color      AS color          -- NEU
    FROM collection
    ORDER BY setCode, cardNumber
""")
    fun observeCollectionRaw(): kotlinx.coroutines.flow.Flow<List<CollectionItem>>

    // Nach Set filtern
    @Query("""
    SELECT
      setCode    AS setCode,
      cardNumber AS cardNumber,
      quantity   AS quantity,
      price      AS price,
      color      AS color          -- NEU
    FROM collection
    WHERE setCode = :setCode
    ORDER BY cardNumber
""")
    suspend fun listBySetRaw(setCode: String): List<CollectionItem>

    // Einzelnen Eintrag holen
    @Query("""
    SELECT
      setCode    AS setCode,
      cardNumber AS cardNumber,
      quantity   AS quantity,
      price      AS price,
      color      AS color          -- NEU
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
data class CollectionItem(
    val setCode: String,
    val cardNumber: Int,
    val quantity: Int,
    val price: Double?,
    val color:String
)
