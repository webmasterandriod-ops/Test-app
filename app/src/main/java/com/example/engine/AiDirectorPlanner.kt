package com.example.engine

import com.example.data.capabilities.CapabilityRegistry
import com.example.data.recipe.AspectRatioMode
import com.example.data.recipe.ExperienceRecipe
import com.example.data.recipe.MoodPreset
import java.util.UUID

/**
 * Standard goals an agent or user can request from the AI Director.
 */
enum class DirectorGoal(val label: String, val promptTemplate: String) {
    TIGHT_PACED("Tightly Paced Edit", "Accelerate rhythm with brisk pacing and punchy dynamic contrast."),
    ATMOSPHERIC_DREAM("Atmospheric Dream", "Slow down time into a warm, contemplative amber slow-motion sequence."),
    SOCIAL_RETOUCH("Vertical Social Reframing", "Reframe into 9:16 portrait ratio with subtle center zoom for mobile viewing."),
    CINEMATIC_NOIR("Cinematic 2.39:1 Noir", "Apply ultra-wide anamorphic letterbox and monochrome black-and-white grade."),
    RANDOM_CHAOS("Stochastic Algorithmic Edit", "Synthesize unexpected combinations of framing, tempo, and atmospheric grading.")
}

/**
 * Structured request to the AI Director Planner.
 */
data class DirectorPlanRequest(
    val goal: DirectorGoal,
    val userPrompt: String = "",
    val currentRecipe: ExperienceRecipe? = null,
    val seed: Long = System.currentTimeMillis()
)

/**
 * Structured response containing the planned recipe, transparent explanation, and capabilities used.
 */
data class DirectorPlanResponse(
    val plannedRecipe: ExperienceRecipe,
    val explanationTitle: String,
    val explanationDetails: List<String>,
    val capabilitiesUsed: List<String>,
    val validationPassed: Boolean
)

/**
 * Model-independent AI Director Planner.
 * Today: High-fidelity rule-based engine mapping structured goals to validated capability operations.
 * Future: Pluggable with local on-device Gemini Nano / LLM inference.
 */
class AiDirectorPlanner {

    fun planExperience(request: DirectorPlanRequest): DirectorPlanResponse {
        val base = request.currentRecipe ?: ExperienceRecipe()
        val seed = request.seed

        val (plannedRecipe, explanationTitle, explanationDetails, caps) = when (request.goal) {
            DirectorGoal.TIGHT_PACED -> {
                val r = base.copy(
                    id = UUID.randomUUID().toString(),
                    name = "Director: Tight Pulse",
                    description = "Rhythmically accelerated 1.25x tempo with punchy contrast.",
                    playbackSpeed = 1.25f,
                    rhythmicCutCount = 8,
                    rhythmTargetSec = 3,
                    moodPreset = MoodPreset.HIGH_CONTRAST,
                    moodIntensity = 0.85f,
                    randomSeed = seed,
                    modifiedAt = System.currentTimeMillis()
                )
                val explanations = listOf(
                    "Speed set to 1.25x: increases tempo while preserving clarity.",
                    "Cutting rhythm set to 3-second segments: maintains brisk momentum.",
                    "High contrast preset applied: boosts shadow depth and highlights."
                )
                val used = listOf(
                    CapabilityRegistry.CAP_SPEED.id,
                    CapabilityRegistry.CAP_CUTS_SEQUENCE.id,
                    CapabilityRegistry.CAP_VISUAL_GRADE.id
                )
                Tuple4(r, "Tight Paced Montage Plan", explanations, used)
            }

            DirectorGoal.ATMOSPHERIC_DREAM -> {
                val r = base.copy(
                    id = UUID.randomUUID().toString(),
                    name = "Director: Amber Reverie",
                    description = "0.75x dreamy slow-mo with warm golden glow and audio protection.",
                    playbackSpeed = 0.75f,
                    zoomScale = 1.15f,
                    moodPreset = MoodPreset.WARM,
                    moodIntensity = 0.85f,
                    muteAudioOnSpeedChange = true,
                    randomSeed = seed,
                    modifiedAt = System.currentTimeMillis()
                )
                val explanations = listOf(
                    "Speed lowered to 0.75x: creates a languid, unhurried temporal flow.",
                    "Audio muted on speed change: eliminates pitch distortion artifacts.",
                    "Subtle 1.15x zoom: focuses attention inward toward subject center.",
                    "Amber Warmth grade at 85%: casts a golden-hour sunset glow over every frame."
                )
                val used = listOf(
                    CapabilityRegistry.CAP_SPEED.id,
                    CapabilityRegistry.CAP_AUDIO_MUTE.id,
                    CapabilityRegistry.CAP_ZOOM.id,
                    CapabilityRegistry.CAP_VISUAL_GRADE.id
                )
                Tuple4(r, "Atmospheric Dream Plan", explanations, used)
            }

            DirectorGoal.SOCIAL_RETOUCH -> {
                val r = base.copy(
                    id = UUID.randomUUID().toString(),
                    name = "Director: 9:16 Portrait Focus",
                    description = "Vertical reframing optimized for handheld mobile displays.",
                    aspectRatio = AspectRatioMode.RATIO_9_16,
                    zoomScale = 1.30f,
                    moodPreset = MoodPreset.COOL,
                    moodIntensity = 0.5f,
                    randomSeed = seed,
                    modifiedAt = System.currentTimeMillis()
                )
                val explanations = listOf(
                    "Aspect ratio converted to 9:16: natively fills vertical smartphone screens.",
                    "1.30x center zoom: eliminates boundary letterboxing and highlights subject.",
                    "Subtle cyan dusk grade: provides modern contemporary color balance."
                )
                val used = listOf(
                    CapabilityRegistry.CAP_ASPECT_RATIO.id,
                    CapabilityRegistry.CAP_ZOOM.id,
                    CapabilityRegistry.CAP_VISUAL_GRADE.id
                )
                Tuple4(r, "Social Reframing Plan", explanations, used)
            }

            DirectorGoal.CINEMATIC_NOIR -> {
                val r = base.copy(
                    id = UUID.randomUUID().toString(),
                    name = "Director: 2.39:1 Cinema Noir",
                    description = "Anamorphic widescreen letterbox with stark high-depth monochrome styling.",
                    aspectRatio = AspectRatioMode.RATIO_2_39_1,
                    moodPreset = MoodPreset.MONOCHROME,
                    moodIntensity = 0.95f,
                    playbackSpeed = 1.0f,
                    randomSeed = seed,
                    modifiedAt = System.currentTimeMillis()
                )
                val explanations = listOf(
                    "2.39:1 Scope ratio: applies theatrical letterboxing for wide cinematic scale.",
                    "Noir Monochrome at 95%: creates deep photographic black and rich silver tones."
                )
                val used = listOf(
                    CapabilityRegistry.CAP_ASPECT_RATIO.id,
                    CapabilityRegistry.CAP_VISUAL_GRADE.id
                )
                Tuple4(r, "Anamorphic Noir Plan", explanations, used)
            }

            DirectorGoal.RANDOM_CHAOS -> {
                val engine = ExperimentEngine()
                val alt = engine.generateAlternative(base, seed)
                val explanations = listOf(
                    "Stochastically generated variation from Seed #$seed.",
                    alt.rationale,
                    "Verified against all local device capability constraints."
                )
                val used = listOf(
                    CapabilityRegistry.CAP_CHAOS_GENERATOR.id,
                    CapabilityRegistry.CAP_SPEED.id,
                    CapabilityRegistry.CAP_ASPECT_RATIO.id,
                    CapabilityRegistry.CAP_VISUAL_GRADE.id
                )
                Tuple4(alt.recipe, "Stochastic Algorithmic Plan", explanations, used)
            }
        }

        // Validate planned recipe before approving execution
        val validation = plannedRecipe.validate()

        return DirectorPlanResponse(
            plannedRecipe = plannedRecipe,
            explanationTitle = explanationTitle,
            explanationDetails = explanationDetails,
            capabilitiesUsed = caps,
            validationPassed = validation.isValid
        )
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
