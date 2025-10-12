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
        val cardName: String, // from JOIN
        val setName: String   // from JOIN
    )

    @Query("UPDATE collection SET price = :price WHERE setCode = :setCode AND cardNumber = :cardNumber")
    suspend fun updatePrice(setCode: String, cardNumber: Int, price: Double?)

    @Query("""
        SELECT
            c.setCode, c.cardNumber, c.quantity, c.price, m.color, c.personalNotes, c.generalNotes,
            m.cardName, m.setName
        FROM collection c
        JOIN master_cards m ON c.setCode = m.setCode AND c.cardNumber = m.cardNumber
        WHERE 
            (:nameQuery = '' OR m.cardName LIKE '%' || :nameQuery || '%') AND
            (:colorFilter = '' OR m.color = :colorFilter) AND
            (:setFilter = '' OR c.setCode = :setFilter)
        ORDER BY
            CASE :sortBy WHEN 'name' THEN m.cardName END ASC,
            CASE :sortBy WHEN 'number' THEN c.cardNumber END ASC,
            CASE :sortBy WHEN 'color' THEN m.color END ASC
    """)
    fun observeFilteredCollection(
        nameQuery: String,
        colorFilter: String,
        setFilter: String,
        sortBy: String
    ): Flow<List<CollectionRowData>>

    @Query("""
        SELECT
            c.setCode, c.cardNumber, c.quantity, c.price, m.color, c.personalNotes, c.generalNotes,
            m.cardName, m.setName
        FROM collection c
        JOIN master_cards m ON c.setCode = m.setCode AND c.cardNumber = m.cardNumber
    """)
    fun observeCollectionWithDetails(): Flow<List<CollectionRowData>>

    @Query("SELECT * FROM collection")
    suspend fun getCollectionForExport(): List<CollectionEntry>

    @Transaction
    suspend fun overrideCollection(entries: List<CollectionEntry>) {
        deleteAll()
        insertAll(entries)
    }

    @Query("DELETE FROM collection")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<CollectionEntry>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun tryInsert(entry: CollectionEntry): Long

    @Query("UPDATE collection SET quantity = quantity + :delta WHERE setCode = :setCode AND cardNumber = :cardNumber")
    suspend fun addQuantity(setCode: String, cardNumber: Int, delta: Int)

    @Transaction
    suspend fun upsertBySetAndNumber(setCode: String, cardNumber: Int, delta: Int) {
        val inserted = tryInsert(
            CollectionEntry(setCode, cardNumber, delta, null, null, null)
        )
        if (inserted == -1L) {
            addQuantity(setCode, cardNumber, delta)
        }
    }

    @Query("DELETE FROM collection WHERE setCode = :setCode AND cardNumber = :cardNumber")
    suspend fun deleteByKey(setCode: String, cardNumber: Int)

    @Query("UPDATE collection SET quantity = CASE WHEN :quantity < 0 THEN 0 ELSE :quantity END WHERE setCode = :setCode AND cardNumber = :cardNumber")
    suspend fun setQuantity(setCode: String, cardNumber: Int, quantity: Int)

    @Query("UPDATE collection SET personalNotes = :personalNotes, generalNotes = :generalNotes WHERE setCode = :setCode AND cardNumber = :cardNumber")
    suspend fun updateNotes(setCode: String, cardNumber: Int, personalNotes: String?, generalNotes: String?)

    @Query("SELECT * FROM collection WHERE setCode = :setCode AND cardNumber = :cardNumber LIMIT 1")
    suspend fun getByKey(setCode: String, cardNumber: Int): CollectionEntry?

}

