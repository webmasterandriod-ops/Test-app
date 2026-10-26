package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ExportHelper {
    private const val TAG = "ExportHelper"

    /**
     * Saves exported MP4 to the user's on-device gallery/Movies folder via MediaStore.
     */
    fun saveToGallery(context: Context, videoFile: File, title: String): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "${title.replace(".mp4", "")}_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/SkipFrame")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null

        try {
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(videoFile).use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            return uri
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to save video to MediaStore", t)
            resolver.delete(uri, null, null)
            return null
        }
    }

    /**
     * Shares a video file using Android system share sheet with FileProvider.
     */
    fun shareVideo(context: Context, videoFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, videoFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share SkipFrame Video").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to share video file", t)
        }
    }

    /**
     * Shares a failure log file using Android system share sheet.
     */
    fun shareLogFile(context: Context, logFilePath: String) {
        try {
            val file = File(logFilePath)
            if (!file.exists()) return

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "SkipFrame FFmpeg Failure Log")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Failure Log").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to share log file", t)
        }
    }
}
