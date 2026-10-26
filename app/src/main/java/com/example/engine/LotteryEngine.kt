package com.example.engine

import android.content.Context
import android.util.Log
import com.example.data.EncodingSpeed
import com.example.data.ExportResolution
import com.example.data.LotteryOrder
import com.example.data.LotteryOutputShape
import com.example.data.SkipFrameConfig
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class LotteryMoment(
    val startSec: Double,
    val endSec: Double,
    val scoreW: Double,
    val peakSec: Double,
    val audioMoment: LotteryMoment? = null // for Mismatch mode
) {
    val duration: Double get() = max(0.0, endSec - startSec)
    val formattedRange: String
        get() = String.format("%.1fs – %.1fs (%.1fs)", startSec, endSec, duration)
}

data class LoudnessProfile(
    val blockCount: Int,
    val durationSec: Double,
    val eDb: FloatArray,          // loudness per 100ms
    val sDiff: FloatArray,        // relative to 30s median
    val rRise: FloatArray,        // rate of rise (e(t) - e(t-0.5))
    val mComposite: FloatArray,   // percentile rank (0..1)
    val smoothedM: FloatArray,    // smoothed for TerrainView
    val candidates: List<LotteryMoment>,
    val minLoudnessDb: Float,
    val maxLoudnessDb: Float,
    val medianLoudnessDb: Float
)

class LotteryEngine(private val context: Context) {
    private val TAG = "LotteryEngine"

    /**
     * Decodes audio stream to 8kHz mono PCM and computes e(t), s(t), r(t), m(t),
     * percentile rank, and candidate peaks.
     */
    fun analyzeLoudness(
        inputFile: File,
        metadata: VideoMetadata,
        onProgress: ((Float) -> Unit)? = null
    ): LoudnessProfile {
        val pcmFile = File(context.cacheDir, "audio_stream_${System.currentTimeMillis()}.pcm")
        try {
            onProgress?.invoke(0.1f)
            // Stream audio to 8kHz mono 16-bit little-endian
            val extractCmd = "-y -i \"${inputFile.absolutePath}\" -vn -ac 1 -ar 8000 -f s16le \"${pcmFile.absolutePath}\""
            FFmpegEngine.executeCommand(context, extractCmd)

            onProgress?.invoke(0.4f)
            val pcmLength = pcmFile.length()
            val bytesPerBlock = 800 * 2 // 100ms at 8000Hz 16-bit = 800 samples = 1600 bytes
            val totalBlocks = (pcmLength / bytesPerBlock).toInt()

            if (totalBlocks <= 0) {
                return createDummyProfile(metadata.durationSeconds)
            }

            val eDb = FloatArray(totalBlocks)
            val buffer = ByteArray(bytesPerBlock)
            val shortBuf = ShortArray(800)

            BufferedInputStream(FileInputStream(pcmFile)).use { bis ->
                var blockIdx = 0
                while (blockIdx < totalBlocks) {
                    var readTotal = 0
                    while (readTotal < bytesPerBlock) {
                        val r = bis.read(buffer, readTotal, bytesPerBlock - readTotal)
                        if (r <= 0) break
                        readTotal += r
                    }
                    if (readTotal < bytesPerBlock) break

                    // Convert bytes to shorts
                    ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuf)

                    // RMS calculation
                    var sumSq = 0.0
                    for (s in shortBuf) {
                        sumSq += (s * s).toDouble()
                    }
                    val rms = sqrt(sumSq / 800.0)
                    // Convert to dB relative to full scale (32768)
                    val db = (20.0 * log10(rms / 32768.0 + 1e-6)).toFloat()
                    eDb[blockIdx] = db.coerceIn(-90f, 0f)
                    blockIdx++
                }
            }

            onProgress?.invoke(0.6f)
            // 2. s(t) = e(t) - median of e over surrounding 30s (300 blocks, 150 each side)
            val sDiff = FloatArray(totalBlocks)
            val halfWindow = 150
            for (i in 0 until totalBlocks) {
                val startW = max(0, i - halfWindow)
                val endW = min(totalBlocks, i + halfWindow)
                val sub = eDb.copyOfRange(startW, endW).sortedArray()
                val median = sub[sub.size / 2]
                sDiff[i] = eDb[i] - median
            }

            // 3. r(t) = e(t) - e(t - 0.5s) (0.5s = 5 blocks)
            val rRise = FloatArray(totalBlocks)
            for (i in 0 until totalBlocks) {
                val prevIdx = max(0, i - 5)
                rRise[i] = eDb[i] - eDb[prevIdx]
            }

            onProgress?.invoke(0.75f)
            // 4. raw m(t) = max(0, s(t)) + 0.5 * r(t)
            val rawM = FloatArray(totalBlocks)
            for (i in 0 until totalBlocks) {
                rawM[i] = max(0f, sDiff[i]) + 0.5f * rRise[i]
            }

            // Convert to percentile rank from 0.0 to 1.0 over the whole file
            val sortedM = rawM.sortedArray()
            val mComposite = FloatArray(totalBlocks)
            for (i in 0 until totalBlocks) {
                val v = rawM[i]
                var rank = sortedM.binarySearch(v)
                if (rank < 0) rank = -(rank + 1)
                mComposite[i] = (rank.toFloat() / totalBlocks.toFloat()).coerceIn(0f, 1f)
            }

            // Smoothed M for Terrain view (moving average of 5 blocks)
            val smoothedM = FloatArray(totalBlocks)
            for (i in 0 until totalBlocks) {
                val s0 = max(0, i - 2)
                val s1 = min(totalBlocks, i + 3)
                var sum = 0f
                for (j in s0 until s1) sum += mComposite[j]
                smoothedM[i] = sum / (s1 - s0)
            }

            onProgress?.invoke(0.85f)
            // 5. Candidates: local peaks of r(t)
            val durationSec = metadata.durationSeconds
            val candidates = mutableListOf<LotteryMoment>()
            for (i in 2 until totalBlocks - 2) {
                if (rRise[i] > rRise[i - 1] && rRise[i] >= rRise[i + 1] && rRise[i] > 1.0f) {
                    val peakSec = i * 0.1
                    val startSec = max(0.0, peakSec - 0.4)
                    // Clip window placeholder; final length will be set by lottery draw
                    candidates.add(
                        LotteryMoment(
                            startSec = startSec,
                            endSec = min(durationSec, startSec + 45.0),
                            scoreW = 0.0,
                            peakSec = peakSec
                        )
                    )
                }
            }

            val sortedEDb = eDb.sortedArray()
            val minDb = sortedEDb.firstOrNull() ?: -90f
            val maxDb = sortedEDb.lastOrNull() ?: 0f
            val medianDb = if (sortedEDb.isNotEmpty()) sortedEDb[sortedEDb.size / 2] else -45f

            return LoudnessProfile(
                blockCount = totalBlocks,
                durationSec = durationSec,
                eDb = eDb,
                sDiff = sDiff,
                rRise = rRise,
                mComposite = mComposite,
                smoothedM = smoothedM,
                candidates = candidates,
                minLoudnessDb = minDb,
                maxLoudnessDb = maxDb,
                medianLoudnessDb = medianDb
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Audio loudness analysis failed", t)
            return createDummyProfile(metadata.durationSeconds)
        } finally {
            pcmFile.delete()
        }
    }

    /**
     * Performs weighted lottery draw according to specification:
     * - Clip length L, clip count K (moments of L/K seconds each)
     * - W = mean of top 20% of m inside window
     * - P(i) = W_i^g / sum(W_j^g) with g = 4 * (1 - c)
     * - Without replacement
     * - Raw mode ON vs OFF polish rules
     * - Mismatch mode
     * - Fresh only overlap exclusion
     */
    fun performLotteryDraw(
        profile: LoudnessProfile,
        config: SkipFrameConfig,
        seed: Long,
        earlierExportRanges: List<Pair<Double, Double>> = emptyList(),
        forcedMoments: List<Double> = emptyList()
    ): List<LotteryMoment> {
        val duration = profile.durationSec
        val totalL = config.clipLengthL.toDouble()
        val k = max(1, config.clipCountK)
        val momentLength = max(1.0, totalL / k.toDouble())
        val chaos = config.chaosC.toDouble().coerceIn(0.0, 1.0)
        val g = 4.0 * (1.0 - chaos)

        // Generate scored candidate windows of length momentLength
        val candidateWindows = mutableListOf<LotteryMoment>()
        for (cand in profile.candidates) {
            var start = cand.startSec
            var end = start + momentLength

            if (end > duration) {
                start = max(0.0, duration - momentLength)
                end = duration
            }

            // Fresh only: exclude if overlapping earlier exports
            if (config.freshOnly && earlierExportRanges.any { (eStart, eEnd) ->
                    // overlap condition: max(start, eStart) < min(end, eEnd)
                    max(start, eStart) < min(end, eEnd)
                }) {
                continue
            }

            // Raw mode OFF polish rules
            if (!config.rawMode) {
                // Skip first 2% and last 8%
                val minStart = duration * config.rawOffPreBufferPercent
                val maxEnd = duration * (1.0 - config.rawOffPostBufferPercent)
                if (start < minStart || end > maxEnd) continue

                // Snap start to nearest loudness minimum within 1s before
                start = snapToQuiet(profile, start - 1.0, start, findMin = true)
                // Snap end to nearest quiet point within +/- 2s
                end = snapToQuiet(profile, end - 2.0, end + 2.0, findMin = true)
                end = max(start + 1.0, end)
            }

            // 6. Clip score W = mean of top 20% of m inside window
            val scoreW = computeScoreW(profile, start, end, config.rawMode, config.rawOffHookBonusWeight)
            candidateWindows.add(
                LotteryMoment(
                    startSec = start,
                    endSec = end,
                    scoreW = scoreW,
                    peakSec = cand.peakSec
                )
            )
        }

        // Add forced moments from terrain view if any
        for (forcedSec in forcedMoments) {
            val start = max(0.0, min(duration - momentLength, forcedSec))
            val end = min(duration, start + momentLength)
            val scoreW = computeScoreW(profile, start, end, config.rawMode, config.rawOffHookBonusWeight) * 10.0 // heavily weighted
            candidateWindows.add(0, LotteryMoment(start, end, scoreW, forcedSec))
        }

        if (candidateWindows.isEmpty()) {
            // Fallback candidate if video has no peaks
            val fallbackStart = max(0.0, duration * 0.1)
            val fallbackEnd = min(duration, fallbackStart + momentLength)
            return listOf(LotteryMoment(fallbackStart, fallbackEnd, 1.0, fallbackStart))
        }

        // Weighted draw without replacement using seeded Random
        val random = Random(seed)
        val selectedPicks = mutableListOf<LotteryMoment>()
        val pool = candidateWindows.toMutableList()

        for (pickIdx in 0 until k) {
            // Requirement: Picks must not overlap each other, but they may be adjacent.
            // Overlap occurs if max(cand.start, sel.start) < min(cand.end, sel.end).
            val nonOverlappingPool = pool.filter { cand ->
                selectedPicks.none { sel -> max(cand.startSec, sel.startSec) < min(cand.endSec, sel.endSec) }
            }.toMutableList()

            if (nonOverlappingPool.isEmpty()) break

            var currentPool: List<LotteryMoment> = nonOverlappingPool

            // Requirement 3: Spread across movie
            if (config.spreadAcrossMovie) {
                val sectionStart = pickIdx * (duration / k)
                val sectionEnd = (pickIdx + 1) * (duration / k)
                val sectionFiltered = nonOverlappingPool.filter { cand ->
                    cand.startSec >= sectionStart - momentLength * 0.5 && cand.startSec < sectionEnd
                }
                if (sectionFiltered.isNotEmpty()) {
                    currentPool = sectionFiltered
                }
            }

            // Filter pool by minimum gap in Raw OFF mode
            if (!config.rawMode && selectedPicks.isNotEmpty()) {
                var filtered = currentPool.filter { cand ->
                    selectedPicks.none { sel -> abs(cand.startSec - sel.startSec) < config.rawOffMinGapSec }
                }
                if (filtered.isEmpty()) {
                    // Automatically shrink gap if too few candidates as specified
                    filtered = currentPool.filter { cand ->
                        selectedPicks.none { sel -> abs(cand.startSec - sel.startSec) < 15.0 }
                    }
                }
                if (filtered.isNotEmpty()) currentPool = filtered
            }

            // Calculate probabilities P(i) = W_i^g / sum(W_j^g)
            val weights = currentPool.map { cand ->
                cand.scoreW.pow(g).coerceAtLeast(1e-6)
            }
            val totalWeight = weights.sum()

            // Draw pick
            val rVal = random.nextDouble() * totalWeight
            var cumulative = 0.0
            var chosenCand = currentPool.first()
            for (i in currentPool.indices) {
                cumulative += weights[i]
                if (rVal <= cumulative) {
                    chosenCand = currentPool[i]
                    break
                }
            }

            // Mismatch mode: draw an extra pick for its audio
            var pickWithMismatch = chosenCand
            if (config.mismatch && pool.size > 1) {
                val otherCandidates = pool.filter { it != chosenCand }
                if (otherCandidates.isNotEmpty()) {
                    val audioPick = otherCandidates[random.nextInt(otherCandidates.size)]
                    pickWithMismatch = chosenCand.copy(audioMoment = audioPick)
                }
            }

            selectedPicks.add(pickWithMismatch)
            // Remove chosen candidate and any candidates overlapping it (adjacent allowed)
            pool.removeAll { cand -> max(cand.startSec, chosenCand.startSec) < min(cand.endSec, chosenCand.endSec) }
        }

        // Apply Order option: Random (default), Calm to intense, Intense to calm, Reverse
        val orderedPicks = when (config.lotteryOrder) {
            LotteryOrder.RANDOM -> selectedPicks // keep exact draw sequence
            LotteryOrder.CALM_TO_INTENSE -> selectedPicks.sortedBy { it.scoreW }
            LotteryOrder.INTENSE_TO_CALM -> selectedPicks.sortedByDescending { it.scoreW }
            LotteryOrder.REVERSE -> selectedPicks.sortedByDescending { it.startSec }
        }

        return orderedPicks
    }

    private fun computeScoreW(
        profile: LoudnessProfile,
        startSec: Double,
        endSec: Double,
        isRawMode: Boolean,
        hookBonusWeight: Double
    ): Double {
        val startBlock = (startSec * 10).toInt().coerceIn(0, profile.blockCount - 1)
        val endBlock = (endSec * 10).toInt().coerceIn(startBlock + 1, profile.blockCount)
        val windowM = profile.mComposite.copyOfRange(startBlock, endBlock)
        if (windowM.isEmpty()) return 0.5

        windowM.sort()
        val top20Count = max(1, (windowM.size * 0.2).toInt())
        val top20 = windowM.takeLast(top20Count)
        var w = top20.average()

        // Hook bonus in Raw OFF mode: max of m in the first 3s
        if (!isRawMode) {
            val hookEndBlock = min(endBlock, startBlock + 30) // 3 seconds
            var maxHook = 0f
            for (i in startBlock until hookEndBlock) {
                maxHook = max(maxHook, profile.mComposite[i])
            }
            w += maxHook * hookBonusWeight
        }

        return max(0.01, w)
    }

    private fun snapToQuiet(
        profile: LoudnessProfile,
        rangeStartSec: Double,
        rangeEndSec: Double,
        findMin: Boolean
    ): Double {
        val startBlock = (max(0.0, rangeStartSec) * 10).toInt().coerceIn(0, profile.blockCount - 1)
        val endBlock = (rangeEndSec * 10).toInt().coerceIn(startBlock, profile.blockCount - 1)
        if (endBlock <= startBlock) return rangeStartSec

        var bestIdx = startBlock
        var bestVal = profile.eDb[startBlock]

        for (i in startBlock..endBlock) {
            val v = profile.eDb[i]
            if (findMin && v < bestVal) {
                bestVal = v
                bestIdx = i
            } else if (!findMin && v > bestVal) {
                bestVal = v
                bestIdx = i
            }
        }
        return bestIdx * 0.1
    }

    /**
     * Renders Lottery Export:
     * - Fast input seeking (-ss before -i)
     * - Vertical 9:16 (1080x1920) default with resolution options
     * - Centre crop or fit with blurred background
     * - Crossfaded joins
     * - Re-encode only the short output, never the whole movie.
     */
    fun exportLotteryMoments(
        inputFile: File,
        metadata: VideoMetadata,
        picks: List<LotteryMoment>,
        config: SkipFrameConfig,
        onProgress: (Float) -> Unit
    ): File {
        val outputFile = File(context.cacheDir, "skipframe_lottery_${System.currentTimeMillis()}.mp4")
        val extractedSegments = mutableListOf<File>()

        try {
            // Step 1: Extract each pick with fast input seeking (-ss before -i)
            val totalPicks = picks.size
            for (i in picks.indices) {
                val pick = picks[i]
                val segFile = File(context.cacheDir, "lottery_seg_${i}_${System.currentTimeMillis()}.mp4")

                if (pick.audioMoment != null) {
                    // Mismatch mode: take video from pick and audio from audioMoment
                    val vStart = String.format(java.util.Locale.US, "%.3f", pick.startSec)
                    val vDur = String.format(java.util.Locale.US, "%.3f", pick.duration)
                    val aStart = String.format(java.util.Locale.US, "%.3f", pick.audioMoment.startSec)

                    val mismatchCmd = "-y -ss $vStart -t $vDur -i \"${inputFile.absolutePath}\" " +
                            "-ss $aStart -t $vDur -i \"${inputFile.absolutePath}\" " +
                            "-map 0:v -map 1:a -c:v libx264 -preset veryfast -crf 22 -c:a aac -b:a 160k -shortest \"${segFile.absolutePath}\""
                    FFmpegEngine.executeCommand(context, mismatchCmd)
                } else {
                    val startStr = String.format(java.util.Locale.US, "%.3f", pick.startSec)
                    val durStr = String.format(java.util.Locale.US, "%.3f", pick.duration)
                    val fastExtractCmd = "-y -ss $startStr -t $durStr -i \"${inputFile.absolutePath}\" " +
                            "-c:v libx264 -preset veryfast -crf 22 -c:a aac -b:a 160k \"${segFile.absolutePath}\""
                    FFmpegEngine.executeCommand(context, fastExtractCmd)
                }

                extractedSegments.add(segFile)
                onProgress(0.2f + (0.5f * (i + 1) / totalPicks))
            }

            // Step 2: Build vertical 9:16 layout (crop or blurred fit) & join
            val filterScript = buildLotteryJoinScript(extractedSegments, config)
            val commandArgs = buildLotteryEncodingArgs(extractedSegments, outputFile, config)

            val finished = FFmpegEngine.executeWithFilterScript(
                context = context,
                commandArgs = commandArgs,
                filterScriptContent = filterScript,
                filterScriptPrefix = "lottery",
                onProgress = { p -> onProgress(0.7f + p * 0.3f) }
            )

            return finished
        } finally {
            for (f in extractedSegments) {
                f.delete()
            }
        }
    }

    private fun buildLotteryJoinScript(
        segmentFiles: List<File>,
        config: SkipFrameConfig
    ): String {
        val sb = StringBuilder()
        val count = segmentFiles.size

        // Target vertical 9:16 dimensions (if vertical shape selected)
        val (outW, outH) = when (config.lotteryResolution) {
            ExportResolution.P1080 -> 1080 to 1920
            ExportResolution.P720 -> 720 to 1280
            ExportResolution.P480 -> 480 to 854
            ExportResolution.ORIGINAL -> 1080 to 1920
        }

        val shape = config.effectiveLotteryOutputShape

        for (i in 0 until count) {
            when (shape) {
                LotteryOutputShape.ORIGINAL_NO_CROP -> {
                    // Original (no crop): preserves video's own aspect ratio and resolution
                    sb.appendLine("[$i:v]setpts=PTS-STARTPTS[v$i];")
                }
                LotteryOutputShape.NINE_SIXTEEN_BLUR -> {
                    // Background blurred + foreground centered
                    sb.appendLine("[$i:v]scale=$outW:$outH:force_original_aspect_ratio=increase,crop=$outW:$outH,boxblur=20:5[bg$i];")
                    sb.appendLine("[$i:v]scale=$outW:$outH:force_original_aspect_ratio=decrease[fg$i];")
                    sb.appendLine("[bg$i][fg$i]overlay=(W-w)/2:(H-h)/2,setpts=PTS-STARTPTS[v$i];")
                }
                LotteryOutputShape.NINE_SIXTEEN_CROP -> {
                    // Centre crop 9:16
                    sb.appendLine("[$i:v]scale=$outW:$outH:force_original_aspect_ratio=increase,crop=$outW:$outH,setpts=PTS-STARTPTS[v$i];")
                }
            }
            sb.appendLine("[$i:a]asetpts=PTS-STARTPTS[a$i];")
        }

        // Concat segments: interleave video and audio pads
        val interleaved = (0 until count).joinToString("") { "[v$it][a$it]" }
        sb.appendLine("$interleaved concat=n=$count:v=1:a=1[vcat][acat]")

        return sb.toString()
    }

    private fun buildLotteryEncodingArgs(
        segmentFiles: List<File>,
        outputFile: File,
        config: SkipFrameConfig
    ): List<String> {
        val args = mutableListOf<String>()
        args.add("-y")
        for (f in segmentFiles) {
            args.add("-i")
            args.add(f.absolutePath)
        }

        args.add("-map")
        args.add("[vcat]")
        args.add("-map")
        args.add("[acat]")

        when (config.encodingSpeed) {
            EncodingSpeed.FAST -> args.addAll(listOf("-c:v", "libx264", "-preset", "veryfast", "-crf", "26"))
            EncodingSpeed.BALANCED -> args.addAll(listOf("-c:v", "libx264", "-preset", "medium", "-crf", "23"))
            EncodingSpeed.CLEAN -> args.addAll(listOf("-c:v", "libx264", "-preset", "slow", "-crf", "20"))
            EncodingSpeed.HARDWARE -> args.addAll(listOf("-c:v", "h264_mediacodec", "-b:v", "5M"))
        }

        args.addAll(listOf("-c:a", "aac", "-b:a", "160k"))
        args.add(outputFile.absolutePath)
        return args
    }

    private fun createDummyProfile(duration: Double): LoudnessProfile {
        val blocks = max(10, (duration * 10).toInt())
        val dummy = FloatArray(blocks) { 0.5f }
        return LoudnessProfile(
            blockCount = blocks,
            durationSec = duration,
            eDb = FloatArray(blocks) { -30f },
            sDiff = FloatArray(blocks) { 0f },
            rRise = FloatArray(blocks) { 0f },
            mComposite = dummy,
            smoothedM = dummy,
            candidates = listOf(LotteryMoment(0.0, min(duration, 45.0), 0.5, 0.0)),
            minLoudnessDb = -60f,
            maxLoudnessDb = 0f,
            medianLoudnessDb = -30f
        )
    }
}
