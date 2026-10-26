package com.example.engine

import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.Session
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FFmpegFailure(
    val returnCode: Int?,
    val last40Logs: String,
    val filterScriptPath: String?,
    val fullLogFilePath: String?,
    val command: String,
    val exceptionMessage: String? = null
)

class FFmpegExecutionException(
    val failure: FFmpegFailure
) : Exception(
    "FFmpeg execution failed with code ${failure.returnCode}: ${failure.exceptionMessage ?: "Check logs"}"
)

object FFmpegEngine {
    private const val TAG = "FFmpegEngine"

    /**
     * Probes media metadata safely using FFprobeKit, wrapped in try/catch(Throwable).
     */
    fun probeMetadata(context: Context, videoFile: File): VideoMetadata? {
        return try {
            val session = FFprobeKit.getMediaInformation(videoFile.absolutePath)
            val info = session.mediaInformation
            if (info == null) {
                Log.e(TAG, "FFprobe failed to extract media information: ${session.failStackTrace}")
                return null
            }

            val durationSeconds = info.duration?.toDoubleOrNull() ?: 0.0
            val durationMs = (durationSeconds * 1000).toLong()
            val sizeBytes = info.size?.toLongOrNull() ?: videoFile.length()

            var width = 0
            var height = 0
            var fps = 30.0
            var isVfr = false
            var hasAudio = false
            var audioSampleRate = 44100
            var audioChannels = 2

            val streams = info.streams ?: emptyList()
            for (stream in streams) {
                val type = stream.type
                if ("video".equals(type, ignoreCase = true)) {
                    width = stream.width?.toInt() ?: width
                    height = stream.height?.toInt() ?: height
                    val avgFrameRate = stream.averageFrameRate
                    val rFrameRate = stream.realFrameRate
                    if (!avgFrameRate.isNullOrBlank()) {
                        fps = parseFps(avgFrameRate) ?: fps
                    }
                    if (!avgFrameRate.isNullOrBlank() && !rFrameRate.isNullOrBlank() && avgFrameRate != rFrameRate) {
                        isVfr = true
                    }
                } else if ("audio".equals(type, ignoreCase = true)) {
                    hasAudio = true
                    audioSampleRate = stream.sampleRate?.toIntOrNull() ?: audioSampleRate
                    audioChannels = stream.getNumberProperty("channels")?.toInt()
                        ?: stream.getStringProperty("channels")?.toIntOrNull()
                        ?: 2
                }
            }

            VideoMetadata(
                filePath = videoFile.absolutePath,
                fileName = videoFile.name,
                sizeBytes = sizeBytes,
                durationMs = durationMs,
                width = if (width > 0) width else 1920,
                height = if (height > 0) height else 1080,
                fps = if (fps > 0.0) fps else 30.0,
                isVariableFps = isVfr,
                hasAudio = hasAudio,
                audioSampleRate = audioSampleRate,
                audioChannels = audioChannels
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Error probing video metadata", t)
            null
        }
    }

    /**
     * Executes an FFmpeg command with filter script and automatic fallback from libx264 to h264_mediacodec.
     */
    fun executeWithFilterScript(
        context: Context,
        commandArgs: List<String>,
        filterScriptContent: String?,
        filterScriptPrefix: String = "filter",
        onProgress: ((Float) -> Unit)? = null
    ): File {
        var filterFile: File? = null
        val fullArgs = commandArgs.toMutableList()

        if (!filterScriptContent.isNullOrBlank()) {
            filterFile = createFilterScriptFile(context, filterScriptContent, filterScriptPrefix)
            val scriptPath = filterFile.absolutePath
            // Find insertion point or replace placeholder
            val filterIndex = fullArgs.indexOf("-filter_complex_script")
            if (filterIndex != -1 && filterIndex + 1 < fullArgs.size) {
                fullArgs[filterIndex + 1] = scriptPath
            } else {
                // Insert before output argument (last item)
                val outArg = fullArgs.removeAt(fullArgs.size - 1)
                fullArgs.add("-filter_complex_script")
                fullArgs.add(scriptPath)
                fullArgs.add(outArg)
            }
        }

        val outputFile = File(fullArgs.last())
        val commandLine = fullArgs.joinToString(" ")

        try {
            val session = executeSession(commandLine, onProgress)
            val returnCode = session.returnCode

            if (ReturnCode.isSuccess(returnCode) && outputFile.exists() && outputFile.length() > 0) {
                return outputFile
            }

            // If libx264 failed or was rejected, attempt automatic fallback to h264_mediacodec
            if (commandLine.contains("libx264")) {
                Log.w(TAG, "libx264 failed. Attempting automatic fallback to h264_mediacodec...")
                val fallbackArgs = fullArgs.map { arg ->
                    when (arg) {
                        "libx264" -> "h264_mediacodec"
                        "-crf" -> "-b:v"
                        "26", "23", "20" -> "4M"
                        "-preset" -> "-b:v"
                        "veryfast", "medium", "slow" -> "4M"
                        else -> arg
                    }
                }
                val fallbackCmd = fallbackArgs.joinToString(" ")
                val fallbackSession = executeSession(fallbackCmd, onProgress)
                if (ReturnCode.isSuccess(fallbackSession.returnCode) && outputFile.exists() && outputFile.length() > 0) {
                    return outputFile
                }
                // Both failed, record fallback session failure
                handleSessionFailure(context, fallbackSession, fallbackCmd, filterFile?.absolutePath)
            }

            handleSessionFailure(context, session, commandLine, filterFile?.absolutePath)
        } catch (e: FFmpegExecutionException) {
            throw e
        } catch (t: Throwable) {
            val failure = FFmpegFailure(
                returnCode = null,
                last40Logs = "Uncaught Throwable: ${t.localizedMessage}",
                filterScriptPath = filterFile?.absolutePath,
                fullLogFilePath = writeFailureLogToFile(context, "Exception: ${t.stackTraceToString()}", filterFile?.absolutePath),
                command = commandLine,
                exceptionMessage = t.message
            )
            throw FFmpegExecutionException(failure)
        }
    }

    /**
     * Executes arbitrary FFmpeg command safely (e.g. for audio PCM extraction).
     */
    fun executeCommand(
        context: Context,
        command: String,
        onProgress: ((Float) -> Unit)? = null
    ): FFmpegSession {
        return try {
            val session = executeSession(command, onProgress)
            if (!ReturnCode.isSuccess(session.returnCode)) {
                handleSessionFailure(context, session, command, null)
            }
            session
        } catch (e: FFmpegExecutionException) {
            throw e
        } catch (t: Throwable) {
            val failure = FFmpegFailure(
                returnCode = null,
                last40Logs = "Uncaught Throwable: ${t.localizedMessage}",
                filterScriptPath = null,
                fullLogFilePath = writeFailureLogToFile(context, "Exception: ${t.stackTraceToString()}", null),
                command = command,
                exceptionMessage = t.message
            )
            throw FFmpegExecutionException(failure)
        }
    }

    private fun executeSession(
        command: String,
        onProgress: ((Float) -> Unit)? = null
    ): FFmpegSession {
        return FFmpegKit.execute(command)
    }

    private fun handleSessionFailure(
        context: Context,
        session: Session,
        command: String,
        filterScriptPath: String?
    ): Nothing {
        val returnCode = session.returnCode?.value
        val allLogs = session.allLogsAsString ?: ""
        val last40Lines = getLastLines(allLogs, 40)

        val logFilePath = writeFailureLogToFile(
            context = context,
            logContent = buildString {
                appendLine("=== FFmpeg Session Failure Report ===")
                appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                appendLine("Return Code: $returnCode")
                appendLine("Command:\n$command")
                if (filterScriptPath != null) {
                    appendLine("Filter Script Path: $filterScriptPath")
                    val scriptFile = File(filterScriptPath)
                    if (scriptFile.exists()) {
                        appendLine("Filter Script Content:\n${scriptFile.readText()}")
                    }
                }
                appendLine("\n=== Session Logs ===")
                appendLine(allLogs)
                appendLine("\n=== Fail Stack Trace ===")
                appendLine(session.failStackTrace)
            },
            filterScriptPath = filterScriptPath
        )

        val failure = FFmpegFailure(
            returnCode = returnCode,
            last40Logs = last40Lines,
            filterScriptPath = filterScriptPath,
            fullLogFilePath = logFilePath,
            command = command,
            exceptionMessage = "Process exited with code $returnCode"
        )
        throw FFmpegExecutionException(failure)
    }

    fun createFilterScriptFile(context: Context, content: String, prefix: String = "filter"): File {
        val dir = File(context.cacheDir, "filters").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.filter")
        FileWriter(file).use { it.write(content) }
        return file
    }

    private fun writeFailureLogToFile(context: Context, logContent: String, filterScriptPath: String?): String {
        return try {
            val logsDir = File(context.cacheDir, "ffmpeg_logs").apply { mkdirs() }
            val logFile = File(logsDir, "ffmpeg_failure_${System.currentTimeMillis()}.txt")
            FileWriter(logFile).use { it.write(logContent) }
            logFile.absolutePath
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to write error log to file", t)
            ""
        }
    }

    private fun getLastLines(text: String, lineCount: Int): String {
        if (text.isBlank()) return "(No logs recorded)"
        val lines = text.lines()
        val tail = lines.takeLast(lineCount)
        return tail.joinToString("\n")
    }

    private fun parseFps(fpsString: String): Double? {
        return try {
            if (fpsString.contains("/")) {
                val parts = fpsString.split("/")
                val num = parts[0].toDouble()
                val den = parts[1].toDouble()
                if (den != 0.0) num / den else null
            } else {
                fpsString.toDoubleOrNull()
            }
        } catch (t: Throwable) {
            null
        }
    }
}
