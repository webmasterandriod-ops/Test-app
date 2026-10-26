package com.example.engine

import android.content.Context
import android.util.Log
import com.example.data.recipe.AspectRatioMode
import com.example.data.recipe.MoodPreset
import com.example.data.recipe.PlaybackSegment
import com.example.data.recipe.ViewingExperience
import com.example.data.recipe.ViewingRecipe
import com.example.ui.viewmodel.ExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.Random
import java.util.UUID

/**
 * RevisionEngine provides deterministic cut generation, chaos generation,
 * reverse clip rendering, and FFmpeg recipe export.
 */
class RevisionEngine(private val context: Context) {
    private val TAG = "RevisionEngine"

    /**
     * Generates a rhythmic sequence of cuts for Director's Cut based on source duration and a seed.
     */
    fun generateAlternateCut(
        sourceDurationMs: Long,
        seed: Long = System.currentTimeMillis(),
        rhythmTargetSec: Int = 4
    ): List<PlaybackSegment> {
        if (sourceDurationMs <= 3000L) {
            return listOf(PlaybackSegment(startMs = 0L, endMs = sourceDurationMs))
        }

        val rng = Random(seed)
        val segments = mutableListOf<PlaybackSegment>()
        val totalSec = (sourceDurationMs / 1000.0)

        // Partition source into candidates of 2 to 8 seconds
        var currentCursor = 0.5 // Skip first half-second
        val candidates = mutableListOf<Pair<Long, Long>>()
        while (currentCursor + 2.0 < totalSec - 0.5) {
            val segLen = (rhythmTargetSec + (rng.nextDouble() * 3.0 - 1.5)).coerceIn(2.0, 10.0)
            val startMs = (currentCursor * 1000).toLong()
            val endMs = ((currentCursor + segLen) * 1000).toLong().coerceAtMost(sourceDurationMs)
            candidates.add(startMs to endMs)
            currentCursor += segLen + rng.nextDouble() * 2.0 // small skip to vary rhythm
        }

        if (candidates.isEmpty()) {
            return listOf(PlaybackSegment(startMs = 0L, endMs = sourceDurationMs))
        }

        // Shuffle candidates deterministically or pick a dynamic sequence
        val chosen = candidates.shuffled(rng).take(12.coerceAtMost(candidates.size))
        val sequenced = if (rng.nextBoolean()) chosen.sortedBy { it.first } else chosen

        for ((startMs, endMs) in sequenced) {
            segments.add(
                PlaybackSegment(
                    id = UUID.randomUUID().toString(),
                    startMs = startMs,
                    endMs = endMs,
                    speed = 1.0f,
                    isEnabled = true
                )
            )
        }

        return segments
    }

    /**
     * Generates a Chaos Director recipe combining cuts, pacing, framing, and mood.
     */
    fun generateChaosRecipe(
        sourceId: String,
        sourceDurationMs: Long,
        seed: Long = System.currentTimeMillis()
    ): ViewingRecipe {
        val rng = Random(seed)

        // 1. Cuts: 50% chance of montage cuts vs continuous
        val useCuts = rng.nextBoolean() && sourceDurationMs > 8000L
        val segments = if (useCuts) {
            generateAlternateCut(sourceDurationMs, seed, rhythmTargetSec = rng.nextInt(3) + 3)
        } else {
            emptyList()
        }

        // 2. Framing: Mirror, subtle zoom, rotation
        val isMirrored = rng.nextFloat() > 0.6f
        val rotationDeg = if (rng.nextFloat() > 0.85f) 180 else 0
        val zoom = if (rng.nextBoolean()) 1.0f + rng.nextFloat() * 0.5f else 1.0f
        val aspectRatios = listOf(
            AspectRatioMode.ORIGINAL,
            AspectRatioMode.RATIO_16_9,
            AspectRatioMode.RATIO_9_16,
            AspectRatioMode.RATIO_2_39_1,
            AspectRatioMode.RATIO_1_1
        )
        val aspect = aspectRatios[rng.nextInt(aspectRatios.size)]

        // 3. Speed: 0.75x, 1.0x, 1.25x, 1.5x
        val speeds = listOf(0.75f, 1.0f, 1.0f, 1.25f, 1.5f)
        val speed = speeds[rng.nextInt(speeds.size)]

        // 4. Mood
        val moods = listOf(
            MoodPreset.NONE,
            MoodPreset.MONOCHROME,
            MoodPreset.WARM,
            MoodPreset.COOL,
            MoodPreset.HIGH_CONTRAST,
            MoodPreset.VIGNETTE
        )
        val mood = moods[rng.nextInt(moods.size)]
        val intensity = 0.5f + rng.nextFloat() * 0.45f

        return ViewingRecipe(
            id = UUID.randomUUID().toString(),
            name = "Chaos #${(seed % 10000).let { if (it < 0) -it else it }}",
            sourceIdentifier = sourceId,
            sourceDurationMs = sourceDurationMs,
            activeExperience = ViewingExperience.CHAOS_DIRECTOR,
            segments = segments,
            isMirrored = isMirrored,
            rotationDegrees = rotationDeg,
            zoomScale = zoom,
            aspectRatio = aspect,
            playbackSpeed = speed,
            moodPreset = mood,
            moodIntensity = intensity,
            randomSeed = seed
        )
    }

    /**
     * Pre-renders an accurate reverse playback clip cache using FFmpeg.
     * Guaranteed audio/video reverse synchronization.
     */
    suspend fun renderReverseCache(
        sourceFile: File,
        startMs: Long = 0L,
        durationMs: Long = 10000L,
        onProgress: ((Float) -> Unit)? = null
    ): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "revision_reverse").apply { mkdirs() }
        val outputFile = File(cacheDir, "rev_${sourceFile.nameWithoutExtension}_${startMs}_${durationMs}.mp4")
        if (outputFile.exists() && outputFile.length() > 1024L) {
            return@withContext outputFile
        }

        val startSec = startMs / 1000.0
        val durSec = durationMs / 1000.0

        val commandArgs = listOf(
            "-ss", String.format(Locale.US, "%.3f", startSec),
            "-t", String.format(Locale.US, "%.3f", durSec),
            "-i", sourceFile.absolutePath,
            "-vf", "reverse",
            "-af", "areverse",
            "-c:v", "libx264",
            "-preset", "veryfast",
            "-crf", "24",
            "-c:a", "aac",
            "-b:a", "128k",
            "-y",
            outputFile.absolutePath
        )

        FFmpegEngine.executeWithFilterScript(
            context = context,
            commandArgs = commandArgs,
            filterScriptContent = null,
            filterScriptPrefix = "rev",
            onProgress = onProgress
        )

        outputFile
    }

    /**
     * Exports the edited video applying all active transformations from the ViewingRecipe:
     * - Sequence cuts with audio/video concat
     * - Speed changes (setpts & atempo)
     * - Framing (mirror, rotation, zoom, aspect ratio crop)
     * - Mood presets (monochrome, warm, cool, contrast, vignette)
     * - Audio muting / level preservation
     */
    suspend fun exportRecipeVideo(
        sourceFile: File,
        recipe: ViewingRecipe,
        outputFile: File,
        onProgress: (Float, String) -> Unit
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val startTimeMs = System.currentTimeMillis()
        val originalSize = sourceFile.length()
        val meta = FFmpegEngine.probeMetadata(context, sourceFile)
            ?: throw IllegalStateException("Failed to inspect source video metadata")

        onProgress(0.05f, "Configuring transformation filtergraph...")

        val activeSegments = recipe.segments.filter { it.isEnabled }
        val effectiveSegments = if (activeSegments.isNotEmpty()) {
            activeSegments
        } else {
            // Whole video as single continuous segment
            listOf(PlaybackSegment(startMs = 0L, endMs = meta.durationMs))
        }

        val filterScript = buildExportFilterScript(recipe, effectiveSegments, meta)

        onProgress(0.15f, "Rendering transformations...")

        val command = mutableListOf<String>()
        command.add("-y")
        command.add("-i")
        command.add(sourceFile.absolutePath)
        command.add("-map")
        command.add("[outv]")
        command.add("-map")
        command.add("[outa]")
        command.add("-c:v")
        command.add("libx264")
        command.add("-preset")
        command.add("veryfast")
        command.add("-crf")
        command.add("22")
        command.add("-c:a")
        command.add("aac")
        command.add("-b:a")
        command.add("160k")
        command.add("-pix_fmt")
        command.add("yuv420p")
        command.add("-movflags")
        command.add("+faststart")
        command.add(outputFile.absolutePath)

        FFmpegEngine.executeWithFilterScript(
            context = context,
            commandArgs = command,
            filterScriptContent = filterScript,
            filterScriptPrefix = "revision_export",
            onProgress = { p ->
                onProgress(0.15f + p * 0.80f, "Encoding video (${(p * 100).toInt()}%)...")
            }
        )

        onProgress(1.0f, "Export complete!")
        val outMeta = FFmpegEngine.probeMetadata(context, outputFile) ?: meta

        ExecutionResult(
            title = "RE:VISION - ${recipe.name}",
            sourcePath = sourceFile.absolutePath,
            outputPath = outputFile.absolutePath,
            originalSizeBytes = originalSize,
            finalSizeBytes = outputFile.length(),
            originalDurationMs = meta.durationMs,
            finalDurationMs = outMeta.durationMs,
            processingTimeMs = System.currentTimeMillis() - startTimeMs,
            encodingSpeed = "libx264 / mediacodec",
            mode = "RE:VISION"
        )
    }

    /**
     * Builds the FFmpeg filter script for concat, video effects, audio pitch/speed, and mood presets.
     */
    private fun buildExportFilterScript(
        recipe: ViewingRecipe,
        segments: List<PlaybackSegment>,
        meta: VideoMetadata
    ): String {
        val sb = StringBuilder()

        // 1. Generate video & audio filter chains for each segment
        for (i in segments.indices) {
            val seg = segments[i]
            val sSec = seg.startMs / 1000.0
            val eSec = seg.endMs / 1000.0

            // Trim
            sb.append("[0:v]trim=start=${String.format(Locale.US, "%.3f", sSec)}:end=${String.format(Locale.US, "%.3f", eSec)},setpts=PTS-STARTPTS")

            // Playback speed for segment
            val effectiveSpeed = (seg.speed * recipe.playbackSpeed).coerceIn(0.25f, 4.0f)
            if (effectiveSpeed != 1.0f) {
                sb.append(",setpts=${String.format(Locale.US, "%.4f", 1.0f / effectiveSpeed)}*PTS")
            }

            // Spatial Framing: Mirror & Rotation
            if (recipe.isMirrored) {
                sb.append(",hflip")
            }
            when (recipe.rotationDegrees) {
                90 -> sb.append(",transpose=1")
                180 -> sb.append(",hflip,vflip")
                270 -> sb.append(",transpose=2")
            }

            // Zoom / Crop
            if (recipe.zoomScale > 1.01f) {
                val z = recipe.zoomScale
                sb.append(",crop=w=iw/$z:h=ih/$z:x=(iw-iw/$z)/2:y=(ih-ih/$z)/2,scale=iw:ih")
            }

            // Aspect ratio framing if non-original
            when (recipe.aspectRatio) {
                AspectRatioMode.ORIGINAL -> { /* keep native */ }
                AspectRatioMode.RATIO_16_9 -> sb.append(",crop=w=min(iw\\,ih*16/9):h=min(ih\\,iw*9/16)")
                AspectRatioMode.RATIO_9_16 -> sb.append(",crop=w=min(iw\\,ih*9/16):h=min(ih\\,iw*16/9)")
                AspectRatioMode.RATIO_1_1 -> sb.append(",crop=w=min(iw\\,ih):h=min(iw\\,ih)")
                AspectRatioMode.RATIO_4_3 -> sb.append(",crop=w=min(iw\\,ih*4/3):h=min(ih\\,iw*3/4)")
                AspectRatioMode.RATIO_2_39_1 -> sb.append(",crop=w=iw:h=iw/2.39")
            }

            // Mood Preset
            val moodFilter = getMoodFfmpegFilter(recipe.moodPreset, recipe.moodIntensity)
            if (moodFilter.isNotEmpty()) {
                sb.append(",$moodFilter")
            }

            sb.append("[v$i];\n")

            // Audio trim & tempo
            sb.append("[0:a]atrim=start=${String.format(Locale.US, "%.3f", sSec)}:end=${String.format(Locale.US, "%.3f", eSec)},asetpts=PTS-STARTPTS")
            if (effectiveSpeed != 1.0f) {
                if (effectiveSpeed in 0.5f..2.0f) {
                    sb.append(",atempo=${String.format(Locale.US, "%.3f", effectiveSpeed)}")
                } else if (effectiveSpeed < 0.5f) {
                    sb.append(",atempo=0.5,atempo=${String.format(Locale.US, "%.3f", effectiveSpeed / 0.5f)}")
                } else {
                    sb.append(",atempo=2.0,atempo=${String.format(Locale.US, "%.3f", effectiveSpeed / 2.0f)}")
                }
            }
            if (recipe.muteAudioOnSpeedChange && effectiveSpeed != 1.0f) {
                sb.append(",volume=0")
            }
            sb.append("[a$i];\n")
        }

        // 2. Concat all segments with interleaved pads
        for (i in segments.indices) {
            sb.append("[v$i][a$i]")
        }
        sb.append("concat=n=${segments.size}:v=1:a=1[outv][outa]\n")

        return sb.toString()
    }

    private fun getMoodFfmpegFilter(mood: MoodPreset, intensity: Float): String {
        val clampedIntensity = intensity.coerceIn(0.1f, 1.0f)
        return when (mood) {
            MoodPreset.NONE -> ""
            MoodPreset.MONOCHROME -> {
                val sat = (1.0f - clampedIntensity).coerceIn(0f, 1f)
                val contrast = 1.0f + 0.35f * clampedIntensity
                "eq=saturation=${String.format(Locale.US, "%.2f", sat)}:contrast=${String.format(Locale.US, "%.2f", contrast)}"
            }
            MoodPreset.WARM -> {
                val rLift = 0.20f * clampedIntensity
                val bCut = -0.20f * clampedIntensity
                "colorbalance=rs=${String.format(Locale.US, "%.2f", rLift)}:gs=0.08:bs=${String.format(Locale.US, "%.2f", bCut)}"
            }
            MoodPreset.COOL -> {
                val rCut = -0.18f * clampedIntensity
                val bLift = 0.25f * clampedIntensity
                "colorbalance=rs=${String.format(Locale.US, "%.2f", rCut)}:gs=0.05:bs=${String.format(Locale.US, "%.2f", bLift)}"
            }
            MoodPreset.HIGH_CONTRAST -> {
                val contrast = 1.0f + 0.6f * clampedIntensity
                val brightness = -0.05f * clampedIntensity
                "eq=contrast=${String.format(Locale.US, "%.2f", contrast)}:brightness=${String.format(Locale.US, "%.2f", brightness)}"
            }
            MoodPreset.VIGNETTE -> {
                val angle = Math.PI / 4.0 * clampedIntensity
                "vignette=angle=${String.format(Locale.US, "%.3f", angle)}"
            }
        }
    }
}
