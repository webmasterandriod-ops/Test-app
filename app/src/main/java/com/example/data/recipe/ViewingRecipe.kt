package com.example.data.recipe

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * PlaybackSegment represents a sliced range in source time (milliseconds).
 */
data class PlaybackSegment(
    val id: String = UUID.randomUUID().toString(),
    val startMs: Long,
    val endMs: Long,
    val speed: Float = 1.0f,
    val isHold: Boolean = false,
    val holdDurationMs: Long = 0L,
    val isReversed: Boolean = false,
    val isEnabled: Boolean = true
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

enum class AspectRatioMode(val displayName: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    RATIO_16_9("16:9 Cinema", 16f / 9f),
    RATIO_9_16("9:16 Vertical", 9f / 16f),
    RATIO_1_1("1:1 Square", 1f),
    RATIO_4_3("4:3 Classic", 4f / 3f),
    RATIO_2_39_1("2.39:1 Scope", 2.39f)
}

enum class MoodPreset(val displayName: String, val description: String) {
    NONE("Natural", "Original unprocessed color"),
    MONOCHROME("Noir Monochrome", "High-depth black and white"),
    WARM("Amber Warmth", "Golden hour sunset glow"),
    COOL("Cyan Dusk", "Neon nocturnal atmosphere"),
    HIGH_CONTRAST("High Contrast", "Punchy shadow and highlight dynamics"),
    VIGNETTE("Vignette Focus", "Soft edge darkening to highlight center")
}

enum class ViewingExperience(val title: String, val iconName: String, val summary: String) {
    DIRECTORS_CUT("Director's Cut", "Movie", "Segment sequences, pacing & rhythm"),
    FOCUS_LENS("Focus Lens", "Crop", "Framing, mirroring, zoom & aspect ratio"),
    DREAMCUT("Dreamcut", "Speed", "Speed modulation, loops & frame hold"),
    MOOD_ENGINE("Mood Engine", "Palette", "Live visual grade & atmospheric tones"),
    CHAOS_DIRECTOR("Chaos Director", "Casino", "Deterministic generative re-envisioning")
}

/**
 * ViewingRecipe encapsulates all transformation parameters applied during RE:VISION playback and export.
 */
data class ViewingRecipe(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Recipe",
    val sourceIdentifier: String = "",
    val sourceDurationMs: Long = 0L,

    // Active experience tab
    val activeExperience: ViewingExperience = ViewingExperience.DIRECTORS_CUT,

    // Experience A: Director's Cut (Segment Timeline)
    val segments: List<PlaybackSegment> = emptyList(),

    // Experience B: Focus Lens (Spatial reframing)
    val isMirrored: Boolean = false,
    val rotationDegrees: Int = 0, // 0, 90, 180, 270
    val zoomScale: Float = 1.0f,  // 1.0 to 3.0
    val panOffsetX: Float = 0f,   // -1.0 to 1.0
    val panOffsetY: Float = 0f,   // -1.0 to 1.0
    val aspectRatio: AspectRatioMode = AspectRatioMode.ORIGINAL,

    // Experience C: Dreamcut (Temporal & Motion)
    val playbackSpeed: Float = 1.0f, // 0.25 to 2.0
    val isLoopEnabled: Boolean = false,
    val loopStartMs: Long = 0L,
    val loopEndMs: Long = 0L,
    val holdFrameAtMs: Long? = null,
    val holdDurationMs: Long = 1500L,
    val isReversePlayback: Boolean = false,
    val muteAudioOnSpeedChange: Boolean = false,

    // Experience D: Mood Engine (Atmosphere)
    val moodPreset: MoodPreset = MoodPreset.NONE,
    val moodIntensity: Float = 0.8f, // 0.0 to 1.0

    // Experience E: Chaos Director (Deterministic randomness)
    val randomSeed: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,

    // Metadata
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
) {
    val isTransformed: Boolean
        get() = segments.isNotEmpty() ||
                isMirrored ||
                rotationDegrees != 0 ||
                zoomScale != 1.0f ||
                panOffsetX != 0f ||
                panOffsetY != 0f ||
                aspectRatio != AspectRatioMode.ORIGINAL ||
                playbackSpeed != 1.0f ||
                isLoopEnabled ||
                holdFrameAtMs != null ||
                isReversePlayback ||
                moodPreset != MoodPreset.NONE

    val activeTransformationSummary: String
        get() {
            val parts = mutableListOf<String>()
            if (segments.isNotEmpty()) parts.add("${segments.count { it.isEnabled }} cuts")
            if (isMirrored) parts.add("Mirrored")
            if (rotationDegrees != 0) parts.add("${rotationDegrees}°")
            if (zoomScale > 1.05f) parts.add(String.format("%.1fx Zoom", zoomScale))
            if (aspectRatio != AspectRatioMode.ORIGINAL) parts.add(aspectRatio.displayName)
            if (playbackSpeed != 1.0f) parts.add("${playbackSpeed}x")
            if (isLoopEnabled) parts.add("Loop")
            if (holdFrameAtMs != null) parts.add("Hold")
            if (isReversePlayback) parts.add("Reverse")
            if (moodPreset != MoodPreset.NONE) parts.add(moodPreset.displayName)
            return if (parts.isEmpty()) "Original (No Effects)" else parts.joinToString(" • ")
        }

    /**
     * Converts recipe to JSON for robust storage across database migrations.
     */
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("id", id)
        root.put("name", name)
        root.put("sourceIdentifier", sourceIdentifier)
        root.put("sourceDurationMs", sourceDurationMs)
        root.put("activeExperience", activeExperience.name)

        val segArray = JSONArray()
        for (seg in segments) {
            val obj = JSONObject()
            obj.put("id", seg.id)
            obj.put("startMs", seg.startMs)
            obj.put("endMs", seg.endMs)
            obj.put("speed", seg.speed.toDouble())
            obj.put("isHold", seg.isHold)
            obj.put("holdDurationMs", seg.holdDurationMs)
            obj.put("isReversed", seg.isReversed)
            obj.put("isEnabled", seg.isEnabled)
            segArray.put(obj)
        }
        root.put("segments", segArray)

        root.put("isMirrored", isMirrored)
        root.put("rotationDegrees", rotationDegrees)
        root.put("zoomScale", zoomScale.toDouble())
        root.put("panOffsetX", panOffsetX.toDouble())
        root.put("panOffsetY", panOffsetY.toDouble())
        root.put("aspectRatio", aspectRatio.name)

        root.put("playbackSpeed", playbackSpeed.toDouble())
        root.put("isLoopEnabled", isLoopEnabled)
        root.put("loopStartMs", loopStartMs)
        root.put("loopEndMs", loopEndMs)
        if (holdFrameAtMs != null) root.put("holdFrameAtMs", holdFrameAtMs)
        root.put("holdDurationMs", holdDurationMs)
        root.put("isReversePlayback", isReversePlayback)
        root.put("muteAudioOnSpeedChange", muteAudioOnSpeedChange)

        root.put("moodPreset", moodPreset.name)
        root.put("moodIntensity", moodIntensity.toDouble())

        root.put("randomSeed", randomSeed)
        root.put("isFavorite", isFavorite)
        root.put("version", version)
        root.put("createdAt", createdAt)
        root.put("modifiedAt", modifiedAt)

        return root.toString()
    }

    companion object {
        fun defaultForSource(sourceId: String, durationMs: Long): ViewingRecipe {
            return ViewingRecipe(
                name = "Original Vision",
                sourceIdentifier = sourceId,
                sourceDurationMs = durationMs,
                loopEndMs = durationMs
            )
        }

        fun fromJsonString(jsonStr: String): ViewingRecipe {
            val root = JSONObject(jsonStr)
            val segmentsList = mutableListOf<PlaybackSegment>()
            if (root.has("segments")) {
                val segArray = root.getJSONArray("segments")
                for (i in 0 until segArray.length()) {
                    val obj = segArray.getJSONObject(i)
                    segmentsList.add(
                        PlaybackSegment(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            startMs = obj.optLong("startMs", 0L),
                            endMs = obj.optLong("endMs", 0L),
                            speed = obj.optDouble("speed", 1.0).toFloat(),
                            isHold = obj.optBoolean("isHold", false),
                            holdDurationMs = obj.optLong("holdDurationMs", 0L),
                            isReversed = obj.optBoolean("isReversed", false),
                            isEnabled = obj.optBoolean("isEnabled", true)
                        )
                    )
                }
            }

            return ViewingRecipe(
                id = root.optString("id", UUID.randomUUID().toString()),
                name = root.optString("name", "Untitled Recipe"),
                sourceIdentifier = root.optString("sourceIdentifier", ""),
                sourceDurationMs = root.optLong("sourceDurationMs", 0L),
                activeExperience = try {
                    ViewingExperience.valueOf(root.optString("activeExperience", ViewingExperience.DIRECTORS_CUT.name))
                } catch (e: Exception) {
                    ViewingExperience.DIRECTORS_CUT
                },
                segments = segmentsList,
                isMirrored = root.optBoolean("isMirrored", false),
                rotationDegrees = root.optInt("rotationDegrees", 0),
                zoomScale = root.optDouble("zoomScale", 1.0).toFloat(),
                panOffsetX = root.optDouble("panOffsetX", 0.0).toFloat(),
                panOffsetY = root.optDouble("panOffsetY", 0.0).toFloat(),
                aspectRatio = try {
                    AspectRatioMode.valueOf(root.optString("aspectRatio", AspectRatioMode.ORIGINAL.name))
                } catch (e: Exception) {
                    AspectRatioMode.ORIGINAL
                },
                playbackSpeed = root.optDouble("playbackSpeed", 1.0).toFloat(),
                isLoopEnabled = root.optBoolean("isLoopEnabled", false),
                loopStartMs = root.optLong("loopStartMs", 0L),
                loopEndMs = root.optLong("loopEndMs", 0L),
                holdFrameAtMs = if (root.has("holdFrameAtMs")) root.optLong("holdFrameAtMs") else null,
                holdDurationMs = root.optLong("holdDurationMs", 1500L),
                isReversePlayback = root.optBoolean("isReversePlayback", false),
                muteAudioOnSpeedChange = root.optBoolean("muteAudioOnSpeedChange", false),
                moodPreset = try {
                    MoodPreset.valueOf(root.optString("moodPreset", MoodPreset.NONE.name))
                } catch (e: Exception) {
                    MoodPreset.NONE
                },
                moodIntensity = root.optDouble("moodIntensity", 0.8).toFloat(),
                randomSeed = root.optLong("randomSeed", System.currentTimeMillis()),
                isFavorite = root.optBoolean("isFavorite", false),
                version = root.optInt("version", 1),
                createdAt = root.optLong("createdAt", System.currentTimeMillis()),
                modifiedAt = root.optLong("modifiedAt", System.currentTimeMillis())
            )
        }
    }
}
