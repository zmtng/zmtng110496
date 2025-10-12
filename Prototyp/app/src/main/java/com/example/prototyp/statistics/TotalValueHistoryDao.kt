package com.example.prototyp.statistics

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TotalValueHistoryDao {
    @Insert
    suspend fun insert(totalValueHistory: TotalValueHistory)

    @Query("SELECT * FROM total_value_history ORDER BY timestamp ASC")
    fun getHistory(): Flow<List<TotalValueHistory>>
}