package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 50")
    fun getAllHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SearchHistoryEntity): Long

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}

@Dao
interface SearchCacheDao {
    @Query("SELECT * FROM search_cache WHERE queryKey = :key LIMIT 1")
    suspend fun getCache(key: String): SearchCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: SearchCacheEntity)

    @Query("DELETE FROM search_cache WHERE timestamp < :cutoff")
    suspend fun deleteExpired(cutoff: Long)

    @Query("DELETE FROM search_cache")
    suspend fun clearAll()
}
