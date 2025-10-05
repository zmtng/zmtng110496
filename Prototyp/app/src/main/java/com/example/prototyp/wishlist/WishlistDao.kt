package com.example.prototyp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.prototyp.wishlist.WishlistEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {

    // ---------------------------
    // Insert / Upsert / Delete
    // ---------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun tryInsert(entry: WishlistEntry): Long

    @Query("""
    UPDATE wishlist
    SET quantity = quantity + :delta
    WHERE setCode = :setCode AND cardNumber = :cardNumber
""")
    suspend fun addQuantity(setCode: String, cardNumber: Int, delta: Int)

    @Transaction
    suspend fun upsertBySetAndNumber(setCode: String, cardNumber: Int, delta: Int, color: String) {
        val inserted = tryInsert(
            WishlistEntry(setCode, cardNumber, delta, null, color)
        )
        if (inserted == -1L) {
            addQuantity(setCode, cardNumber, delta)
        }
    }

    /** Eintrag löschen (Composite Key). */
    @Query("""
    DELETE FROM wishlist
    WHERE setCode = :setCode AND cardNumber = :cardNumber
""")
    suspend fun deleteByKey(setCode: String, cardNumber: Int)

    // ---------------------------
    // Menge & Preis
    // ---------------------------

    @Query("""
        UPDATE wishlist
        SET quantity = CASE WHEN :quantity < 0 THEN 0 ELSE :quantity END
        WHERE setCode = :setCode AND cardNumber = :cardNumber
    """)
    suspend fun setQuantity(setCode: String, cardNumber: Int, quantity: Int)

    @Query("""
        UPDATE wishlist
        SET price = :price
        WHERE setCode = :setCode AND cardNumber = :cardNumber
    """)
    suspend fun updatePrice(setCode: String, cardNumber: Int, price: Double?)


    // Alle Collection-Einträge (roh, ohne Joins)
    @Query("""
    SELECT
      setCode    AS setCode,
      cardNumber AS cardNumber,
      quantity   AS quantity,
      price      AS price,
      color      AS color          -- NEU
    FROM wishlist
    ORDER BY setCode, cardNumber
""")
    fun observeCollectionRaw(): Flow<List<WishlistItem>>

    // Nach Set filtern
    @Query("""
    SELECT
      setCode    AS setCode,
      cardNumber AS cardNumber,
      quantity   AS quantity,
      price      AS price,
      color      AS color          -- NEU
    FROM wishlist
    WHERE setCode = :setCode
    ORDER BY cardNumber
""")
    suspend fun listBySetRaw(setCode: String): List<WishlistItem>

    // Einzelnen Eintrag holen
    @Query("""
    SELECT
      setCode    AS setCode,
      cardNumber AS cardNumber,
      quantity   AS quantity,
      price      AS price,
      color      AS color          -- NEU
    FROM wishlist
    WHERE setCode = :setCode AND cardNumber = :cardNumber
    LIMIT 1
""")
    suspend fun getOneRaw(setCode: String, cardNumber: Int): WishlistItem?

    @Query("""
    SELECT * FROM wishlist
    WHERE setCode = :setCode AND cardNumber = :cardNumber
    LIMIT 1
""")
    suspend fun getByKey(setCode: String, cardNumber: Int): WishlistEntry?


}

/** Kleines DTO nur für `collection`-Spalten (keine Joins). */
data class WishlistItem(
    val setCode: String,
    val cardNumber: Int,
    val quantity: Int,
    val price: Double?,
    val color:String
)
