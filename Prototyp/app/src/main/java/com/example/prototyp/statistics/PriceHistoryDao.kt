package com.example.prototyp.statistics

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceHistoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(priceHistory: PriceHistory)

    data class DailyTotalValue(
        val date: Long,
        val totalValue: Double
    )

    @Query("""
        SELECT
            ph.timestamp / (1000 * 60 * 60 * 24) * (1000 * 60 * 60 * 24) as date,
            SUM(ph.price * c.quantity) as totalValue
        FROM price_history ph
        JOIN collection c ON ph.setCode = c.setCode AND ph.cardNumber = c.cardNumber
        GROUP BY date
        ORDER BY date ASC
    """)
    fun getDailyTotalValues(): Flow<List<DailyTotalValue>>
}