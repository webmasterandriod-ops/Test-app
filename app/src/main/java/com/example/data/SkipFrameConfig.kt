package com.example.data

/**
 * All tuning numbers in one centralized configuration object as mandated by specification.
 */
data class SkipFrameConfig(
    // ----- Mode 1: Shrink -----
    val relativeSilenceThresholdDb: Float = -28f,    // Quieter than this relative to file mean
    val quietPartsSensitivity: Float = 0.5f,         // 0.0 to 1.0 (higher treats more low background noise as silence)
    val minSilenceDurationMs: Long = 350L,           // Minimum silence duration to cut
    val edgeCushionMs: Long = 120L,                  // Quiet cushion kept on each side of silence cut
    val frameSkipN: Int = 8,                         // Remove every N-th frame in sequence
    val audioCrossfadeMs: Int = 3,                   // Audio crossfade duration at cuts (0 allowed)
    val denoiseEnabled: Boolean = true,              // Light denoise before encoding
    val encodingSpeed: EncodingSpeed = EncodingSpeed.BALANCED,
    val exportResolution: ExportResolution = ExportResolution.ORIGINAL,

    // ----- Mode 2: Surprise Lottery -----
    val clipLengthL: Int = 45,                       // Total length in seconds (up to 3600s / 60 min)
    val clipCountK: Int = 1,                        // Number of sub-moments (up to 200, each >= 5s)
    val chaosC: Float = 0.5f,                        // Chaos factor (0.0 to 1.0)
    val rawMode: Boolean = true,                     // Raw mode: exact peak math without polish snapping
    val mismatch: Boolean = false,                   // Mismatch: picture from pick, audio from extra draw
    val freshOnly: Boolean = true,                   // Exclude time ranges from earlier exports
    val spreadAcrossMovie: Boolean = false,          // Divide movie into K equal sections and draw 1 from each
    val lotteryOrder: LotteryOrder = LotteryOrder.RANDOM, // Ordering of picks: Random, Calm to intense, Intense to calm, Reverse
    val customLotteryOutputShape: LotteryOutputShape? = null, // null = auto (Original if L > 180s, else 9:16)
    val lotteryResolution: ExportResolution = ExportResolution.P1080,
    val lotteryAspectFitBlur: Boolean = false,       // Backward compatibility
    val rawOffPreBufferPercent: Double = 0.02,       // Skip first 2% in Raw OFF
    val rawOffPostBufferPercent: Double = 0.08,      // Skip last 8% in Raw OFF
    val rawOffMinGapSec: Double = 120.0,             // Minimum gap between picks in Raw OFF
    val rawOffHookBonusWeight: Double = 0.25,        // Hook bonus weight for peak loudness in first 3s

    // ----- Mode 3: Lab & Erode -----
    val driftMinDurationSec: Int = 5,
    val driftMaxDurationSec: Int = 20,
    val splitMinDurationSec: Int = 8,
    val splitMaxDurationSec: Int = 25,
    val erodeBucketSec: Int = 10,                    // 10-second analysis buckets
    val erodeFraction: Float = 0.125f                // 12.5% default erosion step
) {
    /**
     * Requirement: Output shape "Original (no crop)" default when target length > 3 minutes (180s),
     * and 9:16 default for shorter targets, unless explicitly overridden.
     */
    val effectiveLotteryOutputShape: LotteryOutputShape
        get() = customLotteryOutputShape ?: if (clipLengthL > 180) {
            LotteryOutputShape.ORIGINAL_NO_CROP
        } else {
            LotteryOutputShape.NINE_SIXTEEN_CROP
        }
}

enum class LotteryOutputShape(val displayName: String, val description: String) {
    ORIGINAL_NO_CROP("Original (no crop)", "Preserves native aspect ratio & resolution without cropping"),
    NINE_SIXTEEN_CROP("9:16 Vertical", "Center-cropped to 9:16 vertical"),
    NINE_SIXTEEN_BLUR("9:16 Blurred Fit", "Vertical 9:16 fit with blurred background")
}

enum class LotteryOrder(val displayName: String, val description: String) {
    RANDOM("Random", "Current default randomized draw order"),
    CALM_TO_INTENSE("Calm to intense", "Sorted by clip score, lowest first"),
    INTENSE_TO_CALM("Intense to calm", "Sorted by clip score, highest first"),
    REVERSE("Reverse", "Reverse chronological order of source position")
}

enum class EncodingSpeed(val displayName: String, val description: String) {
    FAST("Fast", "x264 veryfast, CRF 26 - High speed, good quality"),
    BALANCED("Balanced", "x264 medium, CRF 23 - Default standard"),
    CLEAN("Clean", "x264 slow, CRF 20 - Maximum visual fidelity"),
    HARDWARE("Hardware", "h264_mediacodec - Fast GPU accelerated")
}

enum class ExportResolution(val displayName: String, val width: Int?, val height: Int?) {
    ORIGINAL("Original", null, null),
    P1080("1080p", 1920, 1080),
    P720("720p", 1280, 720),
    P480("480p", 854, 480)
}
