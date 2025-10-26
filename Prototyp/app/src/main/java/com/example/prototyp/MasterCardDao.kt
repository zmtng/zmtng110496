package com.example.prototyp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterCardDao {

    data class SetCardCount(
        val setName: String,
        val count: Int
    )

    @Query("SELECT COUNT(*) FROM master_cards")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MasterCard>)

    @Query("SELECT * FROM master_cards WHERE setCode = :setCode")
    suspend fun bySet(setCode: String): List<MasterCard>

    @Query("SELECT * FROM master_cards")
    fun allFlow(): kotlinx.coroutines.flow.Flow<List<MasterCard>>

    @Query("""
        SELECT setCode, cardNumber, cardName, setName, color
        FROM master_cards
        WHERE LOWER(cardName) LIKE '%' || LOWER(:q) || '%'
        ORDER BY setCode, cardNumber
    """)
    suspend fun search(q: String): List<MasterCard>

    @Query("SELECT DISTINCT cardName FROM master_cards ORDER BY cardName")
    fun allNamesFlow(): kotlinx.coroutines.flow.Flow<List<String>>

    @Query("UPDATE master_cards SET color = :color WHERE setCode = :setCode AND cardNumber = :number")
    suspend fun updateColor(setCode: String, number: Int, color: String)

    @Query("""
        SELECT cardNumber
        FROM master_cards
        WHERE setCode = :setCode AND LOWER(cardName) = LOWER(:cardName)
        LIMIT 1
    """)
    suspend fun getNumberForName(setCode: String, cardName: String): Int?

    @Query("""
        SELECT setCode, cardNumber, cardName, setName, color
        FROM master_cards
        WHERE setCode = :setCode AND cardNumber = :cardNumber
        LIMIT 1
    """)
    suspend fun getBySetAndNumber(setCode: String, cardNumber: Int): MasterCard?

    @Query("SELECT DISTINCT color FROM master_cards ORDER BY color ASC")
    suspend fun getDistinctColors(): List<String>

    @Query("SELECT DISTINCT setCode FROM master_cards ORDER BY setCode ASC")
    suspend fun getDistinctSetCodes(): List<String>

    @Query("SELECT DISTINCT cardName FROM master_cards ORDER BY cardName ASC")
    suspend fun getDistinctCardNames(): List<String>

    @Query("SELECT DISTINCT setName FROM master_cards ORDER BY setName ASC")
    suspend fun getDistinctSetNames(): List<String>

    @Query("""
    SELECT *
    FROM master_cards
    WHERE
        (:q = '' OR LOWER(cardName) LIKE '%' || LOWER(:q) || '%') AND
        (:color = '' OR color = :color) AND
        (:set = '' OR setCode = :set) 
    ORDER BY cardName, setCode, cardNumber
""")
    suspend fun search(q: String, color: String, set: String): List<MasterCard>

    @Query("""
        SELECT setName, COUNT(cardNumber) as count
        FROM master_cards
        GROUP BY setName
    """)
    fun getSetCardCounts(): Flow<List<SetCardCount>>


}

