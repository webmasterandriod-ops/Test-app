package com.example.engine

data class VideoMetadata(
    val filePath: String,
    val fileName: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fps: Double,
    val isVariableFps: Boolean,
    val hasAudio: Boolean,
    val audioSampleRate: Int = 44100,
    val audioChannels: Int = 2
) {
    val durationSeconds: Double get() = durationMs / 1000.0

    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val millis = (durationMs % 1000) / 100
            return String.format("%02d:%02d.%d", minutes, seconds, millis)
        }

    val formattedSize: String
        get() {
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }
}
