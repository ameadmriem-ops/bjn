package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val directAnswer: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourcesCount: Int,
    val status: String,
    val language: String,
    val sourcesJson: String = "",
    val confidencePercentage: Int = 0
)

@Entity(tableName = "search_cache")
data class SearchCacheEntity(
    @PrimaryKey
    val queryKey: String,
    val directAnswer: String,
    val language: String,
    val status: String,
    val sourcesCount: Int,
    val sourcesJson: String,
    val confidencePercentage: Int,
    val howWeFoundItJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
