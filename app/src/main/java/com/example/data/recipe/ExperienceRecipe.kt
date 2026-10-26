package com.example.data.recipe

import com.example.data.capabilities.CapabilityRegistry
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * ValidationResult reports whether an ExperienceRecipe is valid, safe, and compatible.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * RecipeOperation is a single unit of transformation tied to a registered capability.
 */
data class RecipeOperation(
    val capabilityId: String,
    val parameters: Map<String, Any> = emptyMap(),
    val isEnabled: Boolean = true
)

/**
 * Portable, versioned ExperienceRecipe format.
 * Completely decoupled from specific local media paths or private identifiers.
 * Can be stored locally, customized, exchanged, and validated.
 */
data class ExperienceRecipe(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Custom Experience",
    val description: String = "User-authored viewing mode",
    val version: Int = 1,
    val author: String = "Local Owner",
    val tags: List<String> = listOf("Custom"),
    val operations: List<RecipeOperation> = emptyList(),

    // Core playback transformation parameters
    val playbackSpeed: Float = 1.0f,
    val isMirrored: Boolean = false,
    val rotationDegrees: Int = 0,
    val zoomScale: Float = 1.0f,
    val aspectRatio: AspectRatioMode = AspectRatioMode.ORIGINAL,
    val moodPreset: MoodPreset = MoodPreset.NONE,
    val moodIntensity: Float = 0.8f,
    val isLoopEnabled: Boolean = false,
    val loopWindowSec: Float = 6.0f,
    val isHoldEnabled: Boolean = false,
    val holdDurationMs: Long = 1500L,
    val muteAudioOnSpeedChange: Boolean = false,
    val rhythmicCutCount: Int = 0, // 0 = continuous, >0 = rhythmically cut
    val rhythmTargetSec: Int = 4,
    val randomSeed: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
) {
    /**
     * Validates this recipe against the CapabilityRegistry.
     * Enforces parameter bounds and flags unknown or unsupported capabilities.
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (name.isBlank()) {
            errors.add("Recipe name cannot be blank.")
        }

        if (playbackSpeed !in 0.25f..2.0f) {
            errors.add("Playback speed $playbackSpeed is outside supported range (0.25x - 2.0x).")
        }

        if (zoomScale !in 1.0f..3.0f) {
            errors.add("Zoom scale $zoomScale is outside supported range (1.0x - 3.0x).")
        }

        if (rotationDegrees !in listOf(0, 90, 180, 270)) {
            errors.add("Rotation degrees $rotationDegrees must be 0, 90, 180, or 270.")
        }

        if (moodIntensity !in 0.0f..1.0f) {
            errors.add("Mood intensity $moodIntensity must be between 0.0 and 1.0.")
        }

        // Validate operations against registry
        for (op in operations) {
            val cap = CapabilityRegistry.findById(op.capabilityId)
            if (cap == null) {
                warnings.add("Unknown capability '${op.capabilityId}' will be ignored gracefully.")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Creates a deep duplicate of this recipe with a new ID and timestamp.
     */
    fun duplicate(newName: String? = null): ExperienceRecipe {
        return copy(
            id = UUID.randomUUID().toString(),
            name = newName ?: "$name (Copy)",
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            isFavorite = false
        )
    }

    /**
     * Converts to an active ViewingRecipe bound to a specific source video.
     */
    fun toViewingRecipe(
        sourceIdentifier: String,
        sourceDurationMs: Long,
        generatedSegments: List<PlaybackSegment> = emptyList()
    ): ViewingRecipe {
        return ViewingRecipe(
            id = id,
            name = name,
            sourceIdentifier = sourceIdentifier,
            sourceDurationMs = sourceDurationMs,
            segments = generatedSegments,
            isMirrored = isMirrored,
            rotationDegrees = rotationDegrees,
            zoomScale = zoomScale,
            aspectRatio = aspectRatio,
            playbackSpeed = playbackSpeed,
            isLoopEnabled = isLoopEnabled,
            loopStartMs = 0L,
            loopEndMs = (loopWindowSec * 1000).toLong().coerceAtMost(sourceDurationMs),
            holdFrameAtMs = if (isHoldEnabled) sourceDurationMs / 2 else null,
            holdDurationMs = holdDurationMs,
            muteAudioOnSpeedChange = muteAudioOnSpeedChange,
            moodPreset = moodPreset,
            moodIntensity = moodIntensity,
            randomSeed = randomSeed,
            isFavorite = isFavorite,
            version = version,
            createdAt = createdAt,
            modifiedAt = modifiedAt
        )
    }

    /**
     * Serializes to a clean portable JSON string for storage or exchange.
     */
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("id", id)
        root.put("name", name)
        root.put("description", description)
        root.put("version", version)
        root.put("author", author)

        val tagsArray = JSONArray()
        tags.forEach { tagsArray.put(it) }
        root.put("tags", tagsArray)

        root.put("playbackSpeed", playbackSpeed.toDouble())
        root.put("isMirrored", isMirrored)
        root.put("rotationDegrees", rotationDegrees)
        root.put("zoomScale", zoomScale.toDouble())
        root.put("aspectRatio", aspectRatio.name)

        root.put("moodPreset", moodPreset.name)
        root.put("moodIntensity", moodIntensity.toDouble())

        root.put("isLoopEnabled", isLoopEnabled)
        root.put("loopWindowSec", loopWindowSec.toDouble())
        root.put("isHoldEnabled", isHoldEnabled)
        root.put("holdDurationMs", holdDurationMs)
        root.put("muteAudioOnSpeedChange", muteAudioOnSpeedChange)

        root.put("rhythmicCutCount", rhythmicCutCount)
        root.put("rhythmTargetSec", rhythmTargetSec)
        root.put("randomSeed", randomSeed)
        root.put("isFavorite", isFavorite)

        root.put("createdAt", createdAt)
        root.put("modifiedAt", modifiedAt)

        val opsArray = JSONArray()
        for (op in operations) {
            val opObj = JSONObject()
            opObj.put("capabilityId", op.capabilityId)
            opObj.put("isEnabled", op.isEnabled)
            val paramsObj = JSONObject()
            op.parameters.forEach { (k, v) -> paramsObj.put(k, v) }
            opObj.put("parameters", paramsObj)
            opsArray.put(opObj)
        }
        root.put("operations", opsArray)

        return root.toString(2)
    }

    companion object {
        val PRESET_CINEMA_SCOPE = ExperienceRecipe(
            id = "preset_cinema_scope",
            name = "2.39:1 Anamorphic Noir",
            description = "Ultra-wide anamorphic letterbox with monochrome high-contrast grading.",
            tags = listOf("Cinematic", "Noir", "Wide"),
            aspectRatio = AspectRatioMode.RATIO_2_39_1,
            moodPreset = MoodPreset.MONOCHROME,
            moodIntensity = 0.9f,
            playbackSpeed = 1.0f
        )

        val PRESET_WARM_DREAM = ExperienceRecipe(
            id = "preset_warm_dream",
            name = "Warm Sunset Dream",
            description = "Golden hour ambient grade at 0.75x dreamy slow-motion.",
            tags = listOf("Dreamy", "Slow-Mo", "Warm"),
            playbackSpeed = 0.75f,
            moodPreset = MoodPreset.WARM,
            moodIntensity = 0.85f,
            muteAudioOnSpeedChange = true
        )

        val PRESET_VERTICAL_FOCUS = ExperienceRecipe(
            id = "preset_vertical_focus",
            name = "9:16 Vertical Highlight",
            description = "Cropped to vertical aspect ratio with 1.3x center punch-in.",
            tags = listOf("Vertical", "Crop", "Mobile"),
            aspectRatio = AspectRatioMode.RATIO_9_16,
            zoomScale = 1.3f
        )

        val PRESET_FAST_MONTAGE = ExperienceRecipe(
            id = "preset_fast_montage",
            name = "Brisk Rhythmic Montage",
            description = "1.25x accelerated rhythm with 3-second cutting intervals.",
            tags = listOf("Montage", "Fast", "Rhythmic"),
            playbackSpeed = 1.25f,
            rhythmicCutCount = 8,
            rhythmTargetSec = 3,
            moodPreset = MoodPreset.HIGH_CONTRAST,
            moodIntensity = 0.7f
        )

        val BUILT_IN_PRESETS = listOf(
            PRESET_CINEMA_SCOPE,
            PRESET_WARM_DREAM,
            PRESET_VERTICAL_FOCUS,
            PRESET_FAST_MONTAGE
        )

        /**
         * Parses and validates a JSON recipe string.
         * Throws IllegalArgumentException on invalid JSON or corrupt schemas.
         */
        fun fromJsonString(jsonStr: String): ExperienceRecipe {
            val root = JSONObject(jsonStr)

            val tagsList = mutableListOf<String>()
            if (root.has("tags")) {
                val tArr = root.getJSONArray("tags")
                for (i in 0 until tArr.length()) {
                    tagsList.add(tArr.getString(i))
                }
            }

            val opsList = mutableListOf<RecipeOperation>()
            if (root.has("operations")) {
                val oArr = root.getJSONArray("operations")
                for (i in 0 until oArr.length()) {
                    val oObj = oArr.getJSONObject(i)
                    val capId = oObj.getString("capabilityId")
                    val isEnabled = oObj.optBoolean("isEnabled", true)
                    val paramsMap = mutableMapOf<String, Any>()
                    if (oObj.has("parameters")) {
                        val pObj = oObj.getJSONObject("parameters")
                        val keys = pObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            paramsMap[k] = pObj.get(k)
                        }
                    }
                    opsList.add(RecipeOperation(capId, paramsMap, isEnabled))
                }
            }

            val recipe = ExperienceRecipe(
                id = root.optString("id", UUID.randomUUID().toString()),
                name = root.optString("name", "Imported Experience"),
                description = root.optString("description", "Imported viewing recipe"),
                version = root.optInt("version", 1),
                author = root.optString("author", "External"),
                tags = if (tagsList.isEmpty()) listOf("Custom") else tagsList,
                operations = opsList,
                playbackSpeed = root.optDouble("playbackSpeed", 1.0).toFloat().coerceIn(0.25f, 2.0f),
                isMirrored = root.optBoolean("isMirrored", false),
                rotationDegrees = root.optInt("rotationDegrees", 0),
                zoomScale = root.optDouble("zoomScale", 1.0).toFloat().coerceIn(1.0f, 3.0f),
                aspectRatio = try {
                    AspectRatioMode.valueOf(root.optString("aspectRatio", AspectRatioMode.ORIGINAL.name))
                } catch (e: Exception) {
                    AspectRatioMode.ORIGINAL
                },
                moodPreset = try {
                    MoodPreset.valueOf(root.optString("moodPreset", MoodPreset.NONE.name))
                } catch (e: Exception) {
                    MoodPreset.NONE
                },
                moodIntensity = root.optDouble("moodIntensity", 0.8).toFloat().coerceIn(0.0f, 1.0f),
                isLoopEnabled = root.optBoolean("isLoopEnabled", false),
                loopWindowSec = root.optDouble("loopWindowSec", 6.0).toFloat(),
                isHoldEnabled = root.optBoolean("isHoldEnabled", false),
                holdDurationMs = root.optLong("holdDurationMs", 1500L),
                muteAudioOnSpeedChange = root.optBoolean("muteAudioOnSpeedChange", false),
                rhythmicCutCount = root.optInt("rhythmicCutCount", 0),
                rhythmTargetSec = root.optInt("rhythmTargetSec", 4),
                randomSeed = root.optLong("randomSeed", System.currentTimeMillis()),
                isFavorite = root.optBoolean("isFavorite", false),
                createdAt = root.optLong("createdAt", System.currentTimeMillis()),
                modifiedAt = root.optLong("modifiedAt", System.currentTimeMillis())
            )

            val validation = recipe.validate()
            if (!validation.isValid) {
                throw IllegalArgumentException("Recipe schema validation failed: ${validation.errors.joinToString("; ")}")
            }
            return recipe
        }
    }
}
