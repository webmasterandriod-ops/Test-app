package com.example.data.repository

import com.example.data.database.ErosionDao
import com.example.data.database.ErosionEntity
import com.example.data.database.LotteryExportDao
import com.example.data.database.LotteryExportEntity
import com.example.data.database.ProcessedVideoDao
import com.example.data.database.ProcessedVideoEntity
import com.example.data.database.ViewingRecipeDao
import com.example.data.database.ViewingRecipeEntity
import com.example.data.personalization.ExperienceRecipeDao
import com.example.data.personalization.ExperienceRecipeEntity
import com.example.data.personalization.ExperimentHistoryEntity
import com.example.data.personalization.PersonalPreferenceEntity
import com.example.data.personalization.PersonalizationDao
import com.example.data.recipe.ExperienceRecipe
import com.example.data.recipe.ViewingRecipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VideoRepository(
    private val processedVideoDao: ProcessedVideoDao,
    private val lotteryExportDao: LotteryExportDao,
    private val erosionDao: ErosionDao,
    private val viewingRecipeDao: ViewingRecipeDao,
    private val experienceRecipeDao: ExperienceRecipeDao,
    private val personalizationDao: PersonalizationDao
) {
    val allProcessedVideos: Flow<List<ProcessedVideoEntity>> = processedVideoDao.getAllProcessedVideos()
    val totalBytesSaved: Flow<Long?> = processedVideoDao.getTotalBytesSaved()

    suspend fun saveProcessedVideo(entity: ProcessedVideoEntity): Long {
        return processedVideoDao.insertProcessedVideo(entity)
    }

    suspend fun deleteProcessedVideo(id: Long) {
        processedVideoDao.deleteProcessedVideo(id)
    }

    suspend fun getLotteryExports(sourceId: String): List<LotteryExportEntity> {
        return lotteryExportDao.getExportsForSource(sourceId)
    }

    fun observeLotteryExports(sourceId: String): Flow<List<LotteryExportEntity>> {
        return lotteryExportDao.observeExportsForSource(sourceId)
    }

    suspend fun recordLotteryExport(sourceId: String, startSec: Double, endSec: Double, seed: Long): Long {
        return lotteryExportDao.insertLotteryExport(
            LotteryExportEntity(
                sourceIdentifier = sourceId,
                startSec = startSec,
                endSec = endSec,
                seed = seed
            )
        )
    }

    suspend fun recordErosionTelemetry(sourceId: String, bucketIndex: Int, holdTimeDeltaMs: Long, wasSkipped: Boolean) {
        val existing = erosionDao.getBucket(sourceId, bucketIndex)
        val updated = if (existing != null) {
            existing.copy(
                holdTimeMs = existing.holdTimeMs + holdTimeDeltaMs,
                skipCount = existing.skipCount + (if (wasSkipped) 1 else 0),
                updatedTimestamp = System.currentTimeMillis()
            )
        } else {
            ErosionEntity(
                sourceIdentifier = sourceId,
                bucketIndex = bucketIndex,
                holdTimeMs = holdTimeDeltaMs,
                skipCount = if (wasSkipped) 1 else 0
            )
        }
        erosionDao.insertOrUpdate(updated)
    }

    suspend fun applyErosionFraction(sourceId: String, fraction: Float): List<Int> {
        val buckets = erosionDao.getBucketsForSource(sourceId).toMutableList()
        if (buckets.isEmpty()) return emptyList()

        val nonEroded = buckets.filter { !it.isEroded }
        if (nonEroded.isEmpty()) return emptyList()

        val sorted = nonEroded.sortedWith(
            compareByDescending<ErosionEntity> { it.skipCount }
                .thenBy { it.holdTimeMs }
        )

        val countToErode = (sorted.size * fraction).toInt().coerceAtLeast(1)
        val toErode = sorted.take(countToErode)

        for (bucket in toErode) {
            erosionDao.insertOrUpdate(bucket.copy(isEroded = true, updatedTimestamp = System.currentTimeMillis()))
        }

        return toErode.map { it.bucketIndex }
    }

    suspend fun restoreErosion(sourceId: String) {
        erosionDao.restoreAllBuckets(sourceId)
    }

    suspend fun getErodedBuckets(sourceId: String): List<Int> {
        return erosionDao.getBucketsForSource(sourceId)
            .filter { it.isEroded }
            .map { it.bucketIndex }
    }

    fun observeErosion(sourceId: String): Flow<List<ErosionEntity>> {
        return erosionDao.observeBucketsForSource(sourceId)
    }

    // --- RE:VISION Recipe Persistence ---

    fun observeRecipesForSource(sourceId: String): Flow<List<ViewingRecipeEntity>> {
        return viewingRecipeDao.getRecipesForSource(sourceId)
    }

    fun observeAllRecipes(): Flow<List<ViewingRecipeEntity>> {
        return viewingRecipeDao.getAllRecipes()
    }

    suspend fun saveRecipe(recipe: ViewingRecipe) {
        val entity = ViewingRecipeEntity(
            id = recipe.id,
            name = recipe.name,
            sourceIdentifier = recipe.sourceIdentifier,
            sourceDurationMs = recipe.sourceDurationMs,
            recipeJson = recipe.toJsonString(),
            isFavorite = recipe.isFavorite,
            version = recipe.version,
            createdAt = recipe.createdAt,
            modifiedAt = System.currentTimeMillis()
        )
        viewingRecipeDao.insertOrUpdateRecipe(entity)
    }

    suspend fun loadRecipeById(id: String): ViewingRecipe? {
        val entity = viewingRecipeDao.getRecipeById(id) ?: return null
        return try {
            ViewingRecipe.fromJsonString(entity.recipeJson)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteRecipe(id: String) {
        viewingRecipeDao.deleteRecipe(id)
    }

    // --- Phase 5: My Experiences (Portable Experience Recipes) ---

    fun observeAllExperiences(): Flow<List<ExperienceRecipe>> {
        return experienceRecipeDao.getAllExperiences().map { list ->
            list.mapNotNull { entity ->
                try {
                    ExperienceRecipe.fromJsonString(entity.recipeJson)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    suspend fun saveExperience(recipe: ExperienceRecipe) {
        val entity = ExperienceRecipeEntity(
            id = recipe.id,
            name = recipe.name,
            description = recipe.description,
            recipeJson = recipe.toJsonString(),
            isFavorite = recipe.isFavorite,
            tags = recipe.tags.joinToString(","),
            createdAt = recipe.createdAt,
            modifiedAt = System.currentTimeMillis()
        )
        experienceRecipeDao.insertOrUpdate(entity)
    }

    suspend fun deleteExperience(id: String) {
        experienceRecipeDao.deleteExperience(id)
    }

    suspend fun getExperienceById(id: String): ExperienceRecipe? {
        val entity = experienceRecipeDao.getExperienceById(id) ?: return null
        return try {
            ExperienceRecipe.fromJsonString(entity.recipeJson)
        } catch (e: Exception) {
            null
        }
    }

    // --- Phase 5: Personalization & Preferences Memory ---

    fun observePreferences(): Flow<List<PersonalPreferenceEntity>> {
        return personalizationDao.getAllPreferences()
    }

    suspend fun getPreference(key: String): String? {
        return personalizationDao.getPreference(key)
    }

    suspend fun setPreference(key: String, value: String) {
        personalizationDao.setPreference(PersonalPreferenceEntity(key, value, System.currentTimeMillis()))
    }

    suspend fun deletePreference(key: String) {
        personalizationDao.deletePreference(key)
    }

    suspend fun clearAllPreferences() {
        personalizationDao.clearAllPreferences()
    }

    // --- Phase 5: Experiment History ---

    fun observeExperimentHistory(): Flow<List<ExperimentHistoryEntity>> {
        return personalizationDao.getAllExperiments()
    }

    suspend fun recordExperiment(
        sourceId: String,
        recipeName: String,
        seed: Long,
        summary: String,
        rating: String,
        recipeJson: String
    ): Long {
        return personalizationDao.recordExperiment(
            ExperimentHistoryEntity(
                sourceIdentifier = sourceId,
                recipeName = recipeName,
                seed = seed,
                summary = summary,
                rating = rating,
                recipeJson = recipeJson,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateExperimentRating(id: Long, rating: String) {
        personalizationDao.updateExperimentRating(id, rating)
    }

    suspend fun clearExperimentHistory() {
        personalizationDao.clearExperimentHistory()
    }
}
