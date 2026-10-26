package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessedVideoDao {
    @Query("SELECT * FROM processed_videos ORDER BY timestamp DESC")
    fun getAllProcessedVideos(): Flow<List<ProcessedVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcessedVideo(video: ProcessedVideoEntity): Long

    @Query("DELETE FROM processed_videos WHERE id = :id")
    suspend fun deleteProcessedVideo(id: Long)

    @Query("SELECT SUM(originalSizeBytes - finalSizeBytes) FROM processed_videos")
    fun getTotalBytesSaved(): Flow<Long?>
}

@Dao
interface LotteryExportDao {
    @Query("SELECT * FROM lottery_exports WHERE sourceIdentifier = :sourceId ORDER BY timestamp ASC")
    suspend fun getExportsForSource(sourceId: String): List<LotteryExportEntity>

    @Query("SELECT * FROM lottery_exports WHERE sourceIdentifier = :sourceId ORDER BY timestamp ASC")
    fun observeExportsForSource(sourceId: String): Flow<List<LotteryExportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLotteryExport(export: LotteryExportEntity): Long

    @Query("DELETE FROM lottery_exports WHERE sourceIdentifier = :sourceId")
    suspend fun clearExportsForSource(sourceId: String)
}

@Dao
interface ErosionDao {
    @Query("SELECT * FROM erosion_records WHERE sourceIdentifier = :sourceId ORDER BY bucketIndex ASC")
    suspend fun getBucketsForSource(sourceId: String): List<ErosionEntity>

    @Query("SELECT * FROM erosion_records WHERE sourceIdentifier = :sourceId ORDER BY bucketIndex ASC")
    fun observeBucketsForSource(sourceId: String): Flow<List<ErosionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: ErosionEntity): Long

    @Query("SELECT * FROM erosion_records WHERE sourceIdentifier = :sourceId AND bucketIndex = :bucketIndex LIMIT 1")
    suspend fun getBucket(sourceId: String, bucketIndex: Int): ErosionEntity?

    @Query("UPDATE erosion_records SET isEroded = 0 WHERE sourceIdentifier = :sourceId")
    suspend fun restoreAllBuckets(sourceId: String)

    @Query("DELETE FROM erosion_records WHERE sourceIdentifier = :sourceId")
    suspend fun clearAllForSource(sourceId: String)
}

@Dao
interface ViewingRecipeDao {
    @Query("SELECT * FROM viewing_recipes ORDER BY modifiedAt DESC")
    fun getAllRecipes(): Flow<List<ViewingRecipeEntity>>

    @Query("SELECT * FROM viewing_recipes WHERE sourceIdentifier = :sourceId ORDER BY modifiedAt DESC")
    fun getRecipesForSource(sourceId: String): Flow<List<ViewingRecipeEntity>>

    @Query("SELECT * FROM viewing_recipes WHERE id = :id LIMIT 1")
    suspend fun getRecipeById(id: String): ViewingRecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecipe(recipe: ViewingRecipeEntity)

    @Query("DELETE FROM viewing_recipes WHERE id = :id")
    suspend fun deleteRecipe(id: String)
}
