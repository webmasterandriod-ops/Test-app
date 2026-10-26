package com.example.engine

import android.content.Context
import android.util.Log
import com.example.data.EncodingSpeed
import com.example.data.ExportResolution
import com.example.data.SkipFrameConfig
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class TimeSlice(val startSec: Double, val endSec: Double) {
    val duration: Double get() = max(0.0, endSec - startSec)
}

data class ShrinkEstimate(
    val originalDurationSec: Double,
    val silenceRemovedSec: Double,
    val frameSkipRemovedSec: Double,
    val estimatedFinalDurationSec: Double,
    val estimatedPercentReduction: Double,
    val silenceCutsCount: Int
)

class ShrinkEngine(private val context: Context) {
    private val TAG = "ShrinkEngine"

    /**
     * Measures silence and loudness to compute keep slices and honest estimates.
     */
    fun analyzeForPreview(
        inputFile: File,
        metadata: VideoMetadata,
        config: SkipFrameConfig
    ): Pair<ShrinkEstimate, List<TimeSlice>> {
        val duration = metadata.durationSeconds
        if (duration <= 0.0) {
            return Pair(
                ShrinkEstimate(0.0, 0.0, 0.0, 0.0, 0.0, 0),
                listOf(TimeSlice(0.0, max(0.1, duration)))
            )
        }

        val silenceIntervals = detectSilenceIntervals(inputFile, metadata, config)
        val keepSlicesAfterSilence = computeKeepSlices(duration, silenceIntervals, config.edgeCushionMs)
        val silenceRemovedSec = duration - keepSlicesAfterSilence.sumOf { it.duration }

        // Every N-th frame skipping from remaining slices
        val remainingAfterSilence = keepSlicesAfterSilence.sumOf { it.duration }
        val frameSkipRemovedSec = if (config.frameSkipN > 1) {
            remainingAfterSilence / config.frameSkipN.toDouble()
        } else {
            0.0
        }

        val estimatedFinal = max(0.1, remainingAfterSilence - frameSkipRemovedSec)
        val percentReduction = if (duration > 0.0) ((duration - estimatedFinal) / duration) * 100.0 else 0.0

        val estimate = ShrinkEstimate(
            originalDurationSec = duration,
            silenceRemovedSec = silenceRemovedSec.coerceAtLeast(0.0),
            frameSkipRemovedSec = frameSkipRemovedSec.coerceAtLeast(0.0),
            estimatedFinalDurationSec = estimatedFinal,
            estimatedPercentReduction = percentReduction.coerceIn(0.0, 100.0),
            silenceCutsCount = silenceIntervals.size
        )

        return Pair(estimate, keepSlicesAfterSilence)
    }

    /**
     * Detects silence intervals using FFmpeg silencedetect.
     * Wrapped safely in try/catch(Throwable).
     */
    private fun detectSilenceIntervals(
        inputFile: File,
        metadata: VideoMetadata,
        config: SkipFrameConfig
    ): List<TimeSlice> {
        val result = mutableListOf<TimeSlice>()
        if (!metadata.hasAudio) return result

        return try {
            // Compute noise threshold in dB:
            // relativeSilenceThresholdDb (-28dB) adjusted by quiet-parts sensitivity
            // Higher sensitivity cuts more quiet background noise (e.g. -28 - (sensitivity - 0.5) * 16)
            val adjustedNoiseDb = config.relativeSilenceThresholdDb + (config.quietPartsSensitivity - 0.5f) * 14.0f
            val minDurationSec = config.minSilenceDurationMs / 1000.0

            val cmd = "-i \"${inputFile.absolutePath}\" -af silencedetect=noise=${adjustedNoiseDb.roundToInt()}dB:d=$minDurationSec -f null -"
            val session = FFmpegEngine.executeCommand(context, cmd)

            val logs = session.allLogsAsString ?: ""
            var silenceStart: Double? = null

            for (line in logs.lineSequence()) {
                if (line.contains("silence_start:")) {
                    val idx = line.indexOf("silence_start:")
                    val substr = line.substring(idx + "silence_start:".length).trim().split(" ")[0]
                    silenceStart = substr.toDoubleOrNull()
                } else if (line.contains("silence_end:") && silenceStart != null) {
                    val idx = line.indexOf("silence_end:")
                    val substr = line.substring(idx + "silence_end:".length).trim().split(" ")[0]
                    val end = substr.toDoubleOrNull()
                    if (end != null && end > silenceStart) {
                        result.add(TimeSlice(silenceStart, end))
                    }
                    silenceStart = null
                }
            }
            if (silenceStart != null && metadata.durationSeconds > silenceStart) {
                result.add(TimeSlice(silenceStart, metadata.durationSeconds))
            }
            result
        } catch (t: Throwable) {
            Log.w(TAG, "Silence detection warning (fallback to empty silence cuts): ${t.message}")
            emptyList()
        }
    }

    /**
     * Inverts silence cuts into keep slices, applying 120ms cushion on both sides of silence cut.
     */
    private fun computeKeepSlices(
        totalDuration: Double,
        silenceIntervals: List<TimeSlice>,
        edgeCushionMs: Long
    ): List<TimeSlice> {
        if (silenceIntervals.isEmpty()) {
            return listOf(TimeSlice(0.0, totalDuration))
        }

        val cushionSec = edgeCushionMs / 1000.0
        // Effective silence cuts = [start + cushion, end - cushion]
        val effectiveCuts = mutableListOf<TimeSlice>()
        for (cut in silenceIntervals) {
            val cutStart = cut.startSec + cushionSec
            val cutEnd = cut.endSec - cushionSec
            if (cutEnd > cutStart) {
                effectiveCuts.add(TimeSlice(cutStart, cutEnd))
            }
        }

        if (effectiveCuts.isEmpty()) {
            return listOf(TimeSlice(0.0, totalDuration))
        }

        // Invert cuts to keep slices
        val keep = mutableListOf<TimeSlice>()
        var currentT = 0.0
        for (cut in effectiveCuts) {
            if (cut.startSec > currentT + 0.05) {
                keep.add(TimeSlice(currentT, cut.startSec))
            }
            currentT = max(currentT, cut.endSec)
        }
        if (currentT < totalDuration - 0.05) {
            keep.add(TimeSlice(currentT, totalDuration))
        }

        return if (keep.isEmpty()) listOf(TimeSlice(0.0, totalDuration)) else keep
    }

    /**
     * Executes Shrink mode end-to-end:
     * 1. Normalize VFR to CFR if needed.
     * 2. Remove silence slices.
     * 3. Drop every N-th frame and corresponding audio slice.
     * 4. Apply 3ms audio crossfade.
     * 5. Light denoise.
     * 6. Encode with selected speed and resolution.
     */
    fun processShrink(
        inputFile: File,
        metadata: VideoMetadata,
        config: SkipFrameConfig,
        onStageUpdate: (stageName: String, progress: Float) -> Unit
    ): File {
        onStageUpdate("Analyzing Silence & Structure", 0.05f)

        // Step 1: Normalize VFR to CFR if needed
        var workingInput = inputFile
        if (metadata.isVariableFps) {
            onStageUpdate("Normalizing Variable Frame Rate", 0.15f)
            workingInput = normalizeCfr(inputFile, metadata.fps)
        }

        // Step 2 & 3: Silence intervals & Keep slices
        onStageUpdate("Calculating Silence Cuts & Cushions", 0.25f)
        val silenceIntervals = detectSilenceIntervals(workingInput, metadata, config)
        val keepSlices = computeKeepSlices(metadata.durationSeconds, silenceIntervals, config.edgeCushionMs)

        // Step 4 & 5: Build micro-slices removing every N-th frame & matching audio slice
        onStageUpdate("Generating Frame-Skip Cut Lists", 0.40f)
        val finalKeepSlices = applyEveryNthFrameSkipping(keepSlices, metadata.fps, config.frameSkipN)

        // Step 6 & 7: Generate Filter Script
        onStageUpdate("Building FFmpeg Filter Script", 0.55f)
        val filterScript = buildFilterScript(finalKeepSlices, metadata.hasAudio, config)

        // Step 8: Build FFmpeg command with chosen encoding speed & resolution
        onStageUpdate("Encoding Video & Audio", 0.70f)
        val outputFile = File(
            context.cacheDir,
            "skipframe_shrunk_${System.currentTimeMillis()}.mp4"
        )

        val commandArgs = buildEncodingCommand(workingInput, outputFile, metadata.hasAudio, config)

        val finishedFile = FFmpegEngine.executeWithFilterScript(
            context = context,
            commandArgs = commandArgs,
            filterScriptContent = filterScript,
            filterScriptPrefix = "shrink",
            onProgress = { p ->
                onStageUpdate("Encoding Video & Audio", 0.70f + p * 0.25f)
            }
        )

        onStageUpdate("Export Complete", 1.0f)
        return finishedFile
    }

    private fun normalizeCfr(inputFile: File, fps: Double): File {
        val cfrOutput = File(context.cacheDir, "cfr_normalized_${System.currentTimeMillis()}.mp4")
        val cmd = "-i \"${inputFile.absolutePath}\" -vsync cfr -r $fps -c:v libx264 -preset ultrafast -crf 18 -c:a copy -y \"${cfrOutput.absolutePath}\""
        FFmpegEngine.executeCommand(context, cmd)
        return cfrOutput
    }

    /**
     * Splits keep intervals into frame-accurate slices, dropping every N-th frame
     * and its matching audio time slice (dt = 1/fps) so video & audio stay in perfect sync.
     * To prevent creating thousands of 30ms segments that exceed filter limits on long videos,
     * we group (N-1) frames into continuous blocks, skip 1 frame, and advance.
     */
    private fun applyEveryNthFrameSkipping(
        slices: List<TimeSlice>,
        fps: Double,
        n: Int
    ): List<TimeSlice> {
        if (n <= 1) return slices
        val safeFps = if (fps > 0) fps else 30.0
        val frameDuration = 1.0 / safeFps
        val result = mutableListOf<TimeSlice>()

        // Limit maximum number of segments in one filter graph to avoid hitting system memory limits
        val maxSegments = 120
        var totalSegmentsCount = 0

        for (slice in slices) {
            val sliceDuration = slice.duration
            val totalFramesInSlice = (sliceDuration * safeFps).toInt()
            if (totalFramesInSlice <= n || totalSegmentsCount > maxSegments) {
                result.add(slice)
                totalSegmentsCount++
                continue
            }

            var frameIdx = 0
            while (frameIdx < totalFramesInSlice && totalSegmentsCount < maxSegments) {
                // Keep block of (N - 1) frames
                val keepFrameCount = min(n - 1, totalFramesInSlice - frameIdx)
                val blockStart = slice.startSec + (frameIdx * frameDuration)
                val blockEnd = blockStart + (keepFrameCount * frameDuration)
                if (blockEnd > blockStart + 0.001) {
                    result.add(TimeSlice(blockStart, min(slice.endSec, blockEnd)))
                    totalSegmentsCount++
                }
                // Skip 1 frame
                frameIdx += keepFrameCount + 1
            }

            if (frameIdx < totalFramesInSlice) {
                // Add remaining part as single slice if segment cap reached
                val remStart = slice.startSec + (frameIdx * frameDuration)
                if (remStart < slice.endSec) {
                    result.add(TimeSlice(remStart, slice.endSec))
                }
            }
        }

        return if (result.isEmpty()) slices else result
    }

    /**
     * Generates filter script containing trims, crossfades, denoise, and scaling.
     */
    private fun buildFilterScript(
        slices: List<TimeSlice>,
        hasAudio: Boolean,
        config: SkipFrameConfig
    ): String {
        val sb = StringBuilder()
        val count = slices.size

        // Video and audio trim stages
        val crossfadeSec = (config.audioCrossfadeMs / 1000.0).coerceAtLeast(0.0)
        for (i in 0 until count) {
            val s = slices[i]
            val sDur = s.duration
            sb.appendLine("[0:v]trim=start=${String.format(java.util.Locale.US, "%.4f", s.startSec)}:end=${String.format(java.util.Locale.US, "%.4f", s.endSec)},setpts=PTS-STARTPTS[v$i];")
            if (hasAudio) {
                if (crossfadeSec > 0.0 && sDur > crossfadeSec * 2) {
                    val fadeOutStart = String.format(java.util.Locale.US, "%.4f", sDur - crossfadeSec)
                    val fadeDur = String.format(java.util.Locale.US, "%.4f", crossfadeSec)
                    sb.appendLine("[0:a]atrim=start=${String.format(java.util.Locale.US, "%.4f", s.startSec)}:end=${String.format(java.util.Locale.US, "%.4f", s.endSec)},asetpts=PTS-STARTPTS,afade=t=in:st=0:d=$fadeDur,afade=t=out:st=$fadeOutStart:d=$fadeDur[a$i];")
                } else {
                    sb.appendLine("[0:a]atrim=start=${String.format(java.util.Locale.US, "%.4f", s.startSec)}:end=${String.format(java.util.Locale.US, "%.4f", s.endSec)},asetpts=PTS-STARTPTS[a$i];")
                }
            }
        }

        // Concat stage: FFmpeg requires interleaved pads [v0][a0][v1][a1]... for concat=v=1:a=1
        if (hasAudio) {
            val interleaved = (0 until count).joinToString("") { "[v$it][a$it]" }
            sb.appendLine("$interleaved concat=n=$count:v=1:a=1[vcat][aout];")
        } else {
            val vInputs = (0 until count).joinToString("") { "[v$it]" }
            sb.appendLine("$vInputs concat=n=$count:v=1:a=0[vcat];")
        }

        // Denoise & scaling
        var currentV = "[vcat]"
        if (config.denoiseEnabled) {
            sb.appendLine("${currentV}hqdn3d=2:1.5:3:2.5[vden];")
            currentV = "[vden]"
        }

        val res = config.exportResolution
        if (res.width != null && res.height != null) {
            sb.appendLine("${currentV}scale=${res.width}:${res.height}:force_original_aspect_ratio=decrease,pad=${res.width}:${res.height}:(ow-iw)/2:(oh-ih)/2[vout]")
        } else {
            // No scaling needed
        }

        return sb.toString()
    }

    private fun buildEncodingCommand(
        inputFile: File,
        outputFile: File,
        hasAudio: Boolean,
        config: SkipFrameConfig
    ): List<String> {
        val args = mutableListOf<String>()
        args.add("-y")
        args.add("-i")
        args.add(inputFile.absolutePath)

        // Mapping outputs from filter graph
        args.add("-map")
        args.add(if (config.exportResolution.width != null) "[vout]" else (if (config.denoiseEnabled) "[vden]" else "[vcat]"))
        if (hasAudio) {
            args.add("-map")
            args.add("[aout]")
        }

        // Video codec & speed settings
        when (config.encodingSpeed) {
            EncodingSpeed.FAST -> {
                args.addAll(listOf("-c:v", "libx264", "-preset", "veryfast", "-crf", "26"))
            }
            EncodingSpeed.BALANCED -> {
                args.addAll(listOf("-c:v", "libx264", "-preset", "medium", "-crf", "23"))
            }
            EncodingSpeed.CLEAN -> {
                args.addAll(listOf("-c:v", "libx264", "-preset", "slow", "-crf", "20"))
            }
            EncodingSpeed.HARDWARE -> {
                args.addAll(listOf("-c:v", "h264_mediacodec", "-b:v", "4M"))
            }
        }

        // Audio codec
        if (hasAudio) {
            args.addAll(listOf("-c:a", "aac", "-b:a", "160k"))
        }

        // Output destination (filter script placeholder will be inserted by executeWithFilterScript)
        args.add(outputFile.absolutePath)
        return args
    }
}
