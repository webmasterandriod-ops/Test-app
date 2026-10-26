package com.example.engine

import com.example.data.recipe.AspectRatioMode
import com.example.data.recipe.ExperienceRecipe
import com.example.data.recipe.MoodPreset
import java.util.Random
import java.util.UUID

/**
 * ExperimentVariation wraps an alternative recipe with its generation rationale and seed.
 */
data class ExperimentVariation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val rationale: String,
    val recipe: ExperienceRecipe,
    val seed: Long
)

/**
 * ExperimentEngine generates reproducible, deterministic interpretations of a video
 * based on verified capabilities without requiring AI models or network connectivity.
 */
class ExperimentEngine {

    /**
     * Generates a single deterministic alternative from a seed.
     */
    fun generateAlternative(
        baseRecipe: ExperienceRecipe,
        seed: Long = System.currentTimeMillis()
    ): ExperimentVariation {
        val rng = Random(seed)

        val speedChoices = listOf(0.75f, 1.0f, 1.25f, 1.5f)
        val selectedSpeed = speedChoices[rng.nextInt(speedChoices.size)]

        val aspectChoices = listOf(
            AspectRatioMode.ORIGINAL,
            AspectRatioMode.RATIO_16_9,
            AspectRatioMode.RATIO_9_16,
            AspectRatioMode.RATIO_2_39_1,
            AspectRatioMode.RATIO_1_1
        )
        val selectedAspect = aspectChoices[rng.nextInt(aspectChoices.size)]

        val moodChoices = listOf(
            MoodPreset.NONE,
            MoodPreset.MONOCHROME,
            MoodPreset.WARM,
            MoodPreset.COOL,
            MoodPreset.HIGH_CONTRAST,
            MoodPreset.VIGNETTE
        )
        val selectedMood = moodChoices[rng.nextInt(moodChoices.size)]

        val mirror = rng.nextBoolean()
        val zoom = 1.0f + (rng.nextFloat() * 0.6f)

        val newRecipe = baseRecipe.copy(
            id = UUID.randomUUID().toString(),
            name = "Variation #${(seed % 1000).let { if (it < 0) -it else it }}",
            description = "Deterministic variation (Seed $seed)",
            playbackSpeed = selectedSpeed,
            aspectRatio = selectedAspect,
            moodPreset = selectedMood,
            isMirrored = mirror,
            zoomScale = zoom,
            randomSeed = seed,
            modifiedAt = System.currentTimeMillis()
        )

        return ExperimentVariation(
            title = newRecipe.name,
            rationale = "Paced at ${selectedSpeed}x with ${selectedAspect.displayName} framing and ${selectedMood.displayName} atmospheric grade.",
            recipe = newRecipe,
            seed = seed
        )
    }

    /**
     * Generates 4 distinct thematic variations for comparison:
     * 1. High-Energy Cut
     * 2. Dreamy Atmospheric Slow-Mo
     * 3. Anamorphic Cinematic Frame
     * 4. Mirrored Contemporary Reel
     */
    fun generateSuiteOfVariations(
        baseRecipe: ExperienceRecipe,
        baseSeed: Long = System.currentTimeMillis()
    ): List<ExperimentVariation> {
        val list = mutableListOf<ExperimentVariation>()

        // 1. High-Energy Cut
        val r1 = baseRecipe.copy(
            id = UUID.randomUUID().toString(),
            name = "Rhythmic Pulse",
            description = "1.25x accelerated speed, punchy contrast, 3s cutting rhythm.",
            playbackSpeed = 1.25f,
            moodPreset = MoodPreset.HIGH_CONTRAST,
            moodIntensity = 0.8f,
            rhythmicCutCount = 8,
            rhythmTargetSec = 3,
            randomSeed = baseSeed + 1,
            modifiedAt = System.currentTimeMillis()
        )
        list.add(
            ExperimentVariation(
                title = r1.name,
                rationale = "Tightly paced tempo designed to accelerate viewer engagement.",
                recipe = r1,
                seed = baseSeed + 1
            )
        )

        // 2. Dreamy Atmospheric Slow-Mo
        val r2 = baseRecipe.copy(
            id = UUID.randomUUID().toString(),
            name = "Atmospheric Drift",
            description = "0.75x slow dream with amber sunset grade and audio protection.",
            playbackSpeed = 0.75f,
            moodPreset = MoodPreset.WARM,
            moodIntensity = 0.85f,
            muteAudioOnSpeedChange = true,
            zoomScale = 1.15f,
            randomSeed = baseSeed + 2,
            modifiedAt = System.currentTimeMillis()
        )
        list.add(
            ExperimentVariation(
                title = r2.name,
                rationale = "Slow-motion reverie focusing attention on visual texture.",
                recipe = r2,
                seed = baseSeed + 2
            )
        )

        // 3. Anamorphic Cinematic Scope
        val r3 = baseRecipe.copy(
            id = UUID.randomUUID().toString(),
            name = "2.39:1 Cinema Noir",
            description = "Wide anamorphic framing with high-depth monochrome styling.",
            aspectRatio = AspectRatioMode.RATIO_2_39_1,
            moodPreset = MoodPreset.MONOCHROME,
            moodIntensity = 0.9f,
            playbackSpeed = 1.0f,
            randomSeed = baseSeed + 3,
            modifiedAt = System.currentTimeMillis()
        )
        list.add(
            ExperimentVariation(
                title = r3.name,
                rationale = "Classic wide-screen theatrical framing and stark black-and-white tones.",
                recipe = r3,
                seed = baseSeed + 3
            )
        )

        // 4. Mirrored Contemporary Reel
        val r4 = baseRecipe.copy(
            id = UUID.randomUUID().toString(),
            name = "Mirrored Vertical 9:16",
            description = "Reframed 9:16 vertical crop with horizontal mirror reflection.",
            aspectRatio = AspectRatioMode.RATIO_9_16,
            isMirrored = true,
            zoomScale = 1.25f,
            moodPreset = MoodPreset.COOL,
            moodIntensity = 0.75f,
            randomSeed = baseSeed + 4,
            modifiedAt = System.currentTimeMillis()
        )
        list.add(
            ExperimentVariation(
                title = r4.name,
                rationale = "Portrait reframing that flips orientation for novel spatial perspective.",
                recipe = r4,
                seed = baseSeed + 4
            )
        )

        return list
    }
}
