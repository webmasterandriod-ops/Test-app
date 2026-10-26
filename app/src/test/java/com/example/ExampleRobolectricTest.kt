package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.SkipFrameDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppNameString() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SkipFrame", appName)
    }

    @Test
    fun testRoomDatabaseInstance() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = SkipFrameDatabase.getInstance(context)
        assertNotNull(db.processedVideoDao())
        assertNotNull(db.lotteryExportDao())
        assertNotNull(db.erosionDao())
        assertNotNull(db.viewingRecipeDao())
    }

    @Test
    fun testViewingRecipeSerializationAndPersistence() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = SkipFrameDatabase.getInstance(context)
        val dao = db.viewingRecipeDao()

        val recipe = com.example.data.recipe.ViewingRecipe(
            id = "test-recipe-1",
            name = "Cinema Noir Test",
            sourceIdentifier = "test_video.mp4",
            sourceDurationMs = 60000L,
            isMirrored = true,
            zoomScale = 1.5f,
            moodPreset = com.example.data.recipe.MoodPreset.MONOCHROME,
            playbackSpeed = 1.25f
        )

        val json = recipe.toJsonString()
        val parsed = com.example.data.recipe.ViewingRecipe.fromJsonString(json)
        assertEquals(recipe.id, parsed.id)
        assertEquals(recipe.name, parsed.name)
        assertEquals(true, parsed.isMirrored)
        assertEquals(1.5f, parsed.zoomScale, 0.001f)
        assertEquals(com.example.data.recipe.MoodPreset.MONOCHROME, parsed.moodPreset)
        assertEquals(1.25f, parsed.playbackSpeed, 0.001f)

        val entity = com.example.data.database.ViewingRecipeEntity(
            id = recipe.id,
            name = recipe.name,
            sourceIdentifier = recipe.sourceIdentifier,
            sourceDurationMs = recipe.sourceDurationMs,
            recipeJson = json
        )
        dao.insertOrUpdateRecipe(entity)

        val loaded = dao.getRecipeById("test-recipe-1")
        assertNotNull(loaded)
        assertEquals("Cinema Noir Test", loaded?.name)
    }

    @Test
    fun testExperienceRecipeValidationAndJsonSerialization() {
        val recipe = com.example.data.recipe.ExperienceRecipe(
            id = "exp-1",
            name = "Test Experience",
            playbackSpeed = 1.25f,
            isMirrored = true,
            zoomScale = 1.4f,
            moodPreset = com.example.data.recipe.MoodPreset.WARM
        )
        val validation = recipe.validate()
        org.junit.Assert.assertTrue(validation.isValid)

        val json = recipe.toJsonString()
        val parsed = com.example.data.recipe.ExperienceRecipe.fromJsonString(json)
        assertEquals(recipe.id, parsed.id)
        assertEquals(recipe.name, parsed.name)
        assertEquals(1.25f, parsed.playbackSpeed, 0.001f)
        assertEquals(true, parsed.isMirrored)
        assertEquals(1.4f, parsed.zoomScale, 0.001f)
        assertEquals(com.example.data.recipe.MoodPreset.WARM, parsed.moodPreset)
    }

    @Test
    fun testCapabilityRegistryIntegrity() {
        val allCaps = com.example.data.capabilities.CapabilityRegistry.ALL_CAPABILITIES
        org.junit.Assert.assertTrue(allCaps.size >= 10)
        val mirrorCap = com.example.data.capabilities.CapabilityRegistry.findById("cap_framing_mirror")
        assertNotNull(mirrorCap)
        assertEquals(com.example.data.capabilities.ExecutionMode.BOTH, mirrorCap?.executionMode)
    }

    @Test
    fun testAiDirectorPlannerDeterministicOutput() {
        val planner = com.example.engine.AiDirectorPlanner()
        val request = com.example.engine.DirectorPlanRequest(
            goal = com.example.engine.DirectorGoal.TIGHT_PACED,
            seed = 9999L
        )
        val response1 = planner.planExperience(request)
        val response2 = planner.planExperience(request)

        org.junit.Assert.assertTrue(response1.validationPassed)
        assertEquals(response1.plannedRecipe.playbackSpeed, response2.plannedRecipe.playbackSpeed, 0.001f)
        assertEquals(response1.plannedRecipe.rhythmicCutCount, response2.plannedRecipe.rhythmicCutCount)
        assertEquals(response1.plannedRecipe.moodPreset, response2.plannedRecipe.moodPreset)
    }

    @Test
    fun testExperimentEngineVariations() {
        val engine = com.example.engine.ExperimentEngine()
        val base = com.example.data.recipe.ExperienceRecipe(name = "Base")
        val suite = engine.generateSuiteOfVariations(base, baseSeed = 12345L)
        assertEquals(4, suite.size)
        // Verify variations have distinct speeds and profiles
        org.junit.Assert.assertNotEquals(suite[0].recipe.playbackSpeed, suite[1].recipe.playbackSpeed)
    }

    @Test
    fun testPersonalizationPersistenceAndMemoryClear() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = SkipFrameDatabase.getInstance(context)
        val expDao = db.experienceRecipeDao()
        val prefDao = db.personalizationDao()

        val recipe = com.example.data.recipe.ExperienceRecipe(
            id = "user-exp-1",
            name = "Personal Favorite",
            isFavorite = true
        )
        expDao.insertOrUpdate(
            com.example.data.personalization.ExperienceRecipeEntity(
                id = recipe.id,
                name = recipe.name,
                description = recipe.description,
                recipeJson = recipe.toJsonString(),
                isFavorite = true
            )
        )

        val loaded = expDao.getExperienceById("user-exp-1")
        assertNotNull(loaded)
        assertEquals("Personal Favorite", loaded?.name)
        assertEquals(true, loaded?.isFavorite)

        prefDao.setPreference(com.example.data.personalization.PersonalPreferenceEntity("theme", "cinematic_dark"))
        val pref = prefDao.getPreference("theme")
        assertEquals("cinematic_dark", pref)

        prefDao.clearAllPreferences()
        val cleared = prefDao.getPreference("theme")
        org.junit.Assert.assertNull(cleared)
    }
}
