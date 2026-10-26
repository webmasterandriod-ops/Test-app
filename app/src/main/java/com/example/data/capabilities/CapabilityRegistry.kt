package com.example.data.capabilities

/**
 * ExecutionMode declares how a capability can run on the target device.
 */
enum class ExecutionMode(val displayName: String) {
    LIVE_ONLY("Live Preview Only"),
    OFFLINE_ONLY("Offline Export Only"),
    BOTH("Live Preview & Offline Export")
}

/**
 * ResourceCost provides an honest, explainable estimate of CPU/GPU/RAM requirements.
 */
enum class ResourceCost(val label: String, val description: String) {
    NEGLIGIBLE("Negligible", "Zero detectable latency, handled by GPU compositing"),
    LOW("Low", "Hardware accelerated player parameters"),
    MEDIUM("Medium", "Real-time timeline seeks or multi-pass matrix filters"),
    HIGH("High", "Full video/audio re-encoding via FFmpeg")
}

/**
 * ParameterType indicates the data type of an operation parameter.
 */
enum class ParameterType {
    BOOLEAN,
    FLOAT,
    INT,
    STRING,
    ENUM
}

data class CapabilityParameter(
    val key: String,
    val displayName: String,
    val description: String,
    val type: ParameterType,
    val defaultValue: Any,
    val minVal: Float? = null,
    val maxVal: Float? = null,
    val allowedEnumValues: List<String>? = null
)

/**
 * Capability represents a verified operation available in SkipFrame.
 */
data class Capability(
    val id: String,
    val displayName: String,
    val category: String,
    val description: String,
    val executionMode: ExecutionMode,
    val resourceCost: ResourceCost,
    val parameters: List<CapabilityParameter>,
    val failureBehavior: String,
    val hardwareRequirement: String = "Standard Android GPU/MediaCodec"
)

/**
 * Shared CapabilityRegistry exposing the real capabilities of the app.
 * Built-in modes and custom experiences share this registry.
 */
object CapabilityRegistry {

    val CAP_MIRROR = Capability(
        id = "cap_framing_mirror",
        displayName = "Horizontal Mirror",
        category = "Framing",
        description = "Flips the video frame horizontally across the vertical axis.",
        executionMode = ExecutionMode.BOTH,
        resourceCost = ResourceCost.NEGLIGIBLE,
        parameters = listOf(
            CapabilityParameter("enabled", "Enabled", "Whether horizontal mirroring is active", ParameterType.BOOLEAN, false)
        ),
        failureBehavior = "Defaults to unmirrored original if graphics surface fails."
    )

    val CAP_ROTATION = Capability(
        id = "cap_framing_rotation",
        displayName = "Frame Rotation",
        category = "Framing",
        description = "Rotates the video orientation by 0, 90, 180, or 270 degrees.",
        executionMode = ExecutionMode.BOTH,
        resourceCost = ResourceCost.NEGLIGIBLE,
        parameters = listOf(
            CapabilityParameter("degrees", "Rotation Degrees", "Angle in degrees", ParameterType.INT, 0, minVal = 0f, maxVal = 270f)
        ),
        failureBehavior = "Falls back to 0 degrees rotation."
    )

    val CAP_ZOOM = Capability(
        id = "cap_framing_zoom",
        displayName = "Spatial Zoom & Crop",
        category = "Framing",
        description = "Scales video canvas from 1.0x to 3.0x while clipping to bounds.",
        executionMode = ExecutionMode.BOTH,
        resourceCost = ResourceCost.NEGLIGIBLE,
        parameters = listOf(
            CapabilityParameter("scale", "Zoom Scale", "Magnification factor", ParameterType.FLOAT, 1.0f, minVal = 1.0f, maxVal = 3.0f)
        ),
        failureBehavior = "Clamps zoom within 1.0x..3.0x boundary."
    )

    val CAP_ASPECT_RATIO = Capability(
        id = "cap_framing_aspect",
        displayName = "Cinematic Aspect Ratio",
        category = "Framing",
        description = "Reframes playback into 16:9, 9:16, 1:1, 4:3, 2.39:1, or native original.",
        executionMode = ExecutionMode.BOTH,
        resourceCost = ResourceCost.LOW,
        parameters = listOf(
            CapabilityParameter(
                "mode",
                "Aspect Ratio Mode",
                "Selected framing aspect ratio",
                ParameterType.STRING,
                "ORIGINAL",
                allowedEnumValues = listOf("ORIGINAL", "RATIO_16_9", "RATIO_9_16", "RATIO_1_1", "RATIO_4_3", "RATIO_2_39_1")
            )
        ),
        failureBehavior = "Defaults to ORIGINAL native source ratio."
    )

    val CAP_SPEED = Capability(
        id = "cap_timing_speed",
        displayName = "Playback Speed Modulation",
        category = "Timing",
        description = "Adjusts playback speed from 0.25x to 2.0x in real time.",
        executionMode = ExecutionMode.BOTH,
        resourceCost = ResourceCost.LOW,
        parameters = listOf(
            CapabilityParameter("speed", "Speed Multiplier", "Playback rate", ParameterType.FLOAT, 1.0f, minVal = 0.25f, maxVal = 2.0f)
        ),
        failureBehavior = "Clamps to 1.0f normal playback if player rejects rate."
    )

    val CAP_LOOP = Capability(
        id = "cap_timing_loop",
        displayName = "Time Range Loop",
        category = "Timing",
        description = "Loops playback seamlessly between specified start and end timestamps.",
        executionMode = ExecutionMode.BOTH,
        resourceCost = ResourceCost.LOW,
        parameters = listOf(
            CapabilityParameter("enabled", "Loop Active", "Whether looping is enabled", ParameterType.BOOLEAN, false),
            CapabilityParameter("startMs", "Start Time", "Loop start in milliseconds", ParameterType.INT, 0, minVal = 0f),
            CapabilityParameter("endMs", "End Time", "Loop end in milliseconds", ParameterType.INT, 0, minVal = 0f)
        ),
        failureBehavior = "Disables loop and continues continuous playback."
    )

    val CAP_FRAME_HOLD = Capability(
        id = "cap_timing_hold",
        displayName = "Moment Frame Hold",
        category = "Timing",
        description = "Pauses at a specific timestamp for a hold duration before automatically resuming.",
        executionMode = ExecutionMode.BOTH,
        resourceCost = ResourceCost.LOW,
        parameters = listOf(
            CapabilityParameter("targetMs", "Hold Timestamp", "Timestamp to pause at", ParameterType.INT, 0, minVal = 0f),
            CapabilityParameter("durationMs", "Hold Duration", "Freeze length in milliseconds", ParameterType.INT, 1500, minVal = 500f, maxVal = 5000f)
        ),
        failureBehavior = "Skips hold and continues playback without pausing."
    )

    val CAP_REVERSE = Capability(
        id = "cap_timing_reverse",
        displayName = "Synchronized Video/Audio Reversal",
        category = "Timing",
        description = "Generates verified reverse video and audio stream. Not advertised as live-only to avoid seek stutter.",
        executionMode = ExecutionMode.OFFLINE_ONLY,
        resourceCost = ResourceCost.HIGH,
        parameters = listOf(
            CapabilityParameter("durationMs", "Clip Duration", "Length of reversed segment", ParameterType.INT, 10000, minVal = 1000f, maxVal = 60000f)
        ),
        failureBehavior = "Reports progress honestly; falls back to forward playback if rendering fails."
    )

    val CAP_VISUAL_GRADE = Capability(
        id = "cap_visual_grade",
        displayName = "Atmospheric Color Grade",
        category = "Visuals",
        description = "Hardware GPU color matrix filter: Noir Monochrome, Amber Warmth, Cyan Dusk, High Contrast, Vignette.",
        executionMode = ExecutionMode.BOTH,
        resourceCost = ResourceCost.NEGLIGIBLE,
        parameters = listOf(
            CapabilityParameter(
                "preset",
                "Color Preset",
                "Atmospheric color tone",
                ParameterType.STRING,
                "NONE",
                allowedEnumValues = listOf("NONE", "MONOCHROME", "WARM", "COOL", "HIGH_CONTRAST", "VIGNETTE")
            ),
            CapabilityParameter("intensity", "Grade Intensity", "Filter weight", ParameterType.FLOAT, 0.8f, minVal = 0.0f, maxVal = 1.0f)
        ),
        failureBehavior = "Displays natural unfiltered colors if shader is unsupported."
    )

    val CAP_AUDIO_MUTE = Capability(
        id = "cap_audio_mute",
        displayName = "Audio Speed Mute",
        category = "Audio",
        description = "Automatically mutes audio on speed changes to eliminate pitch distortion.",
        executionMode = ExecutionMode.BOTH,
        resourceCost = ResourceCost.NEGLIGIBLE,
        parameters = listOf(
            CapabilityParameter("muteOnSpeedChange", "Mute on Shift", "Mute when speed is not 1.0x", ParameterType.BOOLEAN, false)
        ),
        failureBehavior = "Leaves volume unchanged."
    )

    val CAP_CUTS_SEQUENCE = Capability(
        id = "cap_cuts_sequence",
        displayName = "Rhythmic Segment Sequencing",
        category = "Editing",
        description = "Plays an ordered sequence of source cuts with seamless audio/video timeline transition.",
        executionMode = ExecutionMode.BOTH,
        resourceCost = ResourceCost.MEDIUM,
        parameters = listOf(
            CapabilityParameter("cutCount", "Cut Count", "Number of active segments", ParameterType.INT, 4, minVal = 1f, maxVal = 50f),
            CapabilityParameter("rhythmSec", "Rhythm Target", "Target duration per cut", ParameterType.INT, 4, minVal = 2f, maxVal = 12f)
        ),
        failureBehavior = "Plays source continuously if segment seek fails."
    )

    val CAP_CHAOS_GENERATOR = Capability(
        id = "cap_chaos_generator",
        displayName = "Deterministic Chaos Synthesis",
        category = "Generative",
        description = "Algorithmic synthesis combining framing, cuts, pacing, and mood using reproducible random seeds.",
        executionMode = ExecutionMode.BOTH,
        resourceCost = ResourceCost.LOW,
        parameters = listOf(
            CapabilityParameter("seed", "Random Seed", "Reproducible seed number", ParameterType.INT, 42)
        ),
        failureBehavior = "Restores baseline original vision."
    )

    val ALL_CAPABILITIES: List<Capability> = listOf(
        CAP_MIRROR,
        CAP_ROTATION,
        CAP_ZOOM,
        CAP_ASPECT_RATIO,
        CAP_SPEED,
        CAP_LOOP,
        CAP_FRAME_HOLD,
        CAP_REVERSE,
        CAP_VISUAL_GRADE,
        CAP_AUDIO_MUTE,
        CAP_CUTS_SEQUENCE,
        CAP_CHAOS_GENERATOR
    )

    fun findById(id: String): Capability? = ALL_CAPABILITIES.firstOrNull { it.id == id }

    fun getByCategory(): Map<String, List<Capability>> = ALL_CAPABILITIES.groupBy { it.category }
}
