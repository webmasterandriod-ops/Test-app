package com.example.data.personalization

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "experience_recipes")
data class ExperienceRecipeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val recipeJson: String,
    val isFavorite: Boolean = false,
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "personal_preferences")
data class PersonalPreferenceEntity(
    @PrimaryKey
    val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "experiment_history")
data class ExperimentHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceIdentifier: String,
    val recipeName: String,
    val seed: Long,
    val summary: String,
    val rating: String, // "LIKED", "DISLIKED", "SAVED", "UNRATED"
    val recipeJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ExperienceRecipeDao {
    @Query("SELECT * FROM experience_recipes ORDER BY isFavorite DESC, modifiedAt DESC")
    fun getAllExperiences(): Flow<List<ExperienceRecipeEntity>>

    @Query("SELECT * FROM experience_recipes WHERE id = :id LIMIT 1")
    suspend fun getExperienceById(id: String): ExperienceRecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(recipe: ExperienceRecipeEntity)

    @Query("DELETE FROM experience_recipes WHERE id = :id")
    suspend fun deleteExperience(id: String)
}

@Dao
interface PersonalizationDao {
    @Query("SELECT * FROM personal_preferences")
    fun getAllPreferences(): Flow<List<PersonalPreferenceEntity>>

    @Query("SELECT value FROM personal_preferences WHERE key = :key LIMIT 1")
    suspend fun getPreference(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(pref: PersonalPreferenceEntity)

    @Query("DELETE FROM personal_preferences WHERE key = :key")
    suspend fun deletePreference(key: String)

    @Query("DELETE FROM personal_preferences")
    suspend fun clearAllPreferences()

    // Experiment History
    @Query("SELECT * FROM experiment_history ORDER BY timestamp DESC LIMIT 100")
    fun getAllExperiments(): Flow<List<ExperimentHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordExperiment(experiment: ExperimentHistoryEntity): Long

    @Query("UPDATE experiment_history SET rating = :rating WHERE id = :id")
    suspend fun updateExperimentRating(id: Long, rating: String)

    @Query("DELETE FROM experiment_history WHERE id = :id")
    suspend fun deleteExperiment(id: Long)

    @Query("DELETE FROM experiment_history")
    suspend fun clearExperimentHistory()
}
