package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    videoUriOrPath: String,
    modifier: Modifier = Modifier,
    useControls: Boolean = true,
    autoPlay: Boolean = true,
    repeatMode: Int = Player.REPEAT_MODE_OFF,
    startPositionMs: Long = 0L,
    isMuted: Boolean = false,
    onPlayerReady: ((ExoPlayer) -> Unit)? = null
) {
    val context = LocalContext.current

    val exoPlayer = remember(videoUriOrPath) {
        ExoPlayer.Builder(context).build().apply {
            val uri = if (videoUriOrPath.startsWith("content://") || videoUriOrPath.startsWith("file://")) {
                Uri.parse(videoUriOrPath)
            } else {
                Uri.fromFile(File(videoUriOrPath))
            }
            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            prepare()
            if (startPositionMs > 0) {
                seekTo(startPositionMs)
            }
            this.repeatMode = repeatMode
            playWhenReady = autoPlay
            volume = if (isMuted) 0f else 1f
            onPlayerReady?.invoke(this)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = useControls
                    player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
