package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.personalization.ExperienceRecipeDao
import com.example.data.personalization.ExperienceRecipeEntity
import com.example.data.personalization.ExperimentHistoryEntity
import com.example.data.personalization.PersonalPreferenceEntity
import com.example.data.personalization.PersonalizationDao

@Database(
    entities = [
        ProcessedVideoEntity::class,
        LotteryExportEntity::class,
        ErosionEntity::class,
        ViewingRecipeEntity::class,
        ExperienceRecipeEntity::class,
        PersonalPreferenceEntity::class,
        ExperimentHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SkipFrameDatabase : RoomDatabase() {
    abstract fun processedVideoDao(): ProcessedVideoDao
    abstract fun lotteryExportDao(): LotteryExportDao
    abstract fun erosionDao(): ErosionDao
    abstract fun viewingRecipeDao(): ViewingRecipeDao
    abstract fun experienceRecipeDao(): ExperienceRecipeDao
    abstract fun personalizationDao(): PersonalizationDao

    companion object {
        @Volatile
        private var INSTANCE: SkipFrameDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS viewing_recipes (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        sourceIdentifier TEXT NOT NULL,
                        sourceDurationMs INTEGER NOT NULL,
                        recipeJson TEXT NOT NULL,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        version INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL,
                        modifiedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS experience_recipes (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        recipeJson TEXT NOT NULL,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        tags TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        modifiedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS personal_preferences (
                        key TEXT PRIMARY KEY NOT NULL,
                        value TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS experiment_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sourceIdentifier TEXT NOT NULL,
                        recipeName TEXT NOT NULL,
                        seed INTEGER NOT NULL,
                        summary TEXT NOT NULL,
                        rating TEXT NOT NULL,
                        recipeJson TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): SkipFrameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SkipFrameDatabase::class.java,
                    "skipframe_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
