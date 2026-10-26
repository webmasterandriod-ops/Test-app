package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_videos")
data class ProcessedVideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val sourcePathOrUri: String,
    val outputPath: String,
    val mode: String, // "SHRINK" or "LOTTERY" or "ERODE_BAKE"
    val originalSizeBytes: Long,
    val finalSizeBytes: Long,
    val originalDurationMs: Long,
    val finalDurationMs: Long,
    val processingTimeMs: Long,
    val encodingSpeed: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "lottery_exports")
data class LotteryExportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceIdentifier: String,
    val startSec: Double,
    val endSec: Double,
    val seed: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "erosion_records")
data class ErosionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceIdentifier: String,
    val bucketIndex: Int, // 10s bucket (0 = 0..10s, 1 = 10..20s, etc.)
    val holdTimeMs: Long = 0L,
    val skipCount: Int = 0, // tapped away within 3s
    val isEroded: Boolean = false,
    val updatedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "viewing_recipes")
data class ViewingRecipeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val sourceIdentifier: String,
    val sourceDurationMs: Long,
    val recipeJson: String,
    val isFavorite: Boolean = false,
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)
