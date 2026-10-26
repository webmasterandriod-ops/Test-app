package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.recipe.AspectRatioMode
import com.example.data.recipe.MoodPreset
import com.example.data.recipe.ViewingRecipe
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.ElectricAmber
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

/**
 * Builds the ColorMatrix corresponding to the selected MoodPreset and intensity.
 */
fun buildMoodColorMatrix(preset: MoodPreset, intensity: Float): ColorMatrix {
    val cm = ColorMatrix()
    val clamped = intensity.coerceIn(0f, 1f)
    when (preset) {
        MoodPreset.NONE -> {
            // Identity
        }
        MoodPreset.MONOCHROME -> {
            // Desaturate: sat = 1 - clamped
            val sat = 1.0f - clamped
            cm.setToSaturation(sat)
            // Slightly lift contrast
            val c = 1.0f + 0.3f * clamped
            val t = (1.0f - c) * 128f
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.timesAssign(contrastMatrix)
        }
        MoodPreset.WARM -> {
            // Boost red/yellow, tone down blue
            val rBoost = 1.0f + 0.25f * clamped
            val gBoost = 1.0f + 0.10f * clamped
            val bCut = 1.0f - 0.20f * clamped
            val warmMatrix = ColorMatrix(
                floatArrayOf(
                    rBoost, 0f, 0f, 0f, 15f * clamped,
                    0f, gBoost, 0f, 0f, 8f * clamped,
                    0f, 0f, bCut, 0f, -10f * clamped,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.timesAssign(warmMatrix)
        }
        MoodPreset.COOL -> {
            // Cyan dusk: boost blue & green, reduce red
            val rCut = 1.0f - 0.20f * clamped
            val gBoost = 1.0f + 0.08f * clamped
            val bBoost = 1.0f + 0.30f * clamped
            val coolMatrix = ColorMatrix(
                floatArrayOf(
                    rCut, 0f, 0f, 0f, -10f * clamped,
                    0f, gBoost, 0f, 0f, 5f * clamped,
                    0f, 0f, bBoost, 0f, 20f * clamped,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.timesAssign(coolMatrix)
        }
        MoodPreset.HIGH_CONTRAST -> {
            val c = 1.0f + 0.6f * clamped
            val t = (1.0f - c) * 128f
            val hcMatrix = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.timesAssign(hcMatrix)
        }
        MoodPreset.VIGNETTE -> {
            // Color matrix remains subtle, vignette is drawn via radial gradient overlay
        }
    }
    return cm
}

@OptIn(UnstableApi::class)
@Composable
fun RevisionPlayerSurface(
    videoPath: String,
    recipe: ViewingRecipe,
    modifier: Modifier = Modifier,
    onPlayerCreated: ((ExoPlayer) -> Unit)? = null,
    onPositionUpdate: ((Long, Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isHoldingFrame by remember { mutableStateOf(false) }
    var holdCooldownUntilMs by remember { mutableLongStateOf(0L) }
    var activeSegmentIndex by remember { mutableStateOf(0) }

    val exoPlayer = remember(videoPath) {
        ExoPlayer.Builder(context).build().apply {
            val uri = if (videoPath.startsWith("content://") || videoPath.startsWith("file://")) {
                Uri.parse(videoPath)
            } else {
                Uri.fromFile(File(videoPath))
            }
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            onPlayerCreated?.invoke(this)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Apply speed changes and mute settings live
    LaunchedEffect(recipe.playbackSpeed, recipe.muteAudioOnSpeedChange) {
        exoPlayer.playbackParameters = PlaybackParameters(recipe.playbackSpeed)
        exoPlayer.volume = if (recipe.muteAudioOnSpeedChange && recipe.playbackSpeed != 1.0f) 0f else 1f
    }

    // Position listener & timeline loop/hold/segment controller loop
    LaunchedEffect(exoPlayer, recipe) {
        while (isActive) {
            val currentPos = exoPlayer.currentPosition
            val totalDur = exoPlayer.duration.coerceAtLeast(1L)
            onPositionUpdate?.invoke(currentPos, totalDur)

            val now = System.currentTimeMillis()

            // 1. Dreamcut: Loop range handler
            if (recipe.isLoopEnabled && recipe.loopEndMs > recipe.loopStartMs) {
                if (currentPos >= recipe.loopEndMs || currentPos < recipe.loopStartMs) {
                    exoPlayer.seekTo(recipe.loopStartMs)
                }
            }

            // 2. Dreamcut: Frame Hold handler
            if (recipe.holdFrameAtMs != null && !isHoldingFrame && now > holdCooldownUntilMs) {
                val holdTarget = recipe.holdFrameAtMs
                if (kotlin.math.abs(currentPos - holdTarget) <= 250L) {
                    isHoldingFrame = true
                    val wasPlaying = exoPlayer.isPlaying
                    exoPlayer.pause()
                    delay(recipe.holdDurationMs.coerceIn(500L, 5000L))
                    if (wasPlaying) {
                        exoPlayer.play()
                    }
                    isHoldingFrame = false
                    holdCooldownUntilMs = System.currentTimeMillis() + 4000L // 4s cooldown before retriggering hold
                }
            }

            // 3. Director's Cut: Segment sequencing
            val activeSegments = recipe.segments.filter { it.isEnabled }
            if (activeSegments.isNotEmpty()) {
                val currentSeg = activeSegments.getOrNull(activeSegmentIndex)
                if (currentSeg != null) {
                    if (currentPos >= currentSeg.endMs || currentPos < currentSeg.startMs) {
                        // Advance to next segment
                        val nextIndex = (activeSegmentIndex + 1) % activeSegments.size
                        activeSegmentIndex = nextIndex
                        val nextSeg = activeSegments[nextIndex]
                        exoPlayer.seekTo(nextSeg.startMs)
                    }
                } else {
                    activeSegmentIndex = 0
                }
            }

            delay(100L)
        }
    }

    // Framing calculations (Focus Lens)
    val zoom = recipe.zoomScale.coerceIn(1.0f, 3.0f)
    val scaleX = if (recipe.isMirrored) -zoom else zoom
    val scaleY = zoom
    val rotation = recipe.rotationDegrees.toFloat()

    // Color grading (Mood Engine)
    val colorMatrix = remember(recipe.moodPreset, recipe.moodIntensity) {
        buildMoodColorMatrix(recipe.moodPreset, recipe.moodIntensity)
    }

    BoxWithConstraints(
        modifier = modifier
            .background(Color.Black)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Frame according to requested AspectRatioMode
        val containerModifier = when (recipe.aspectRatio) {
            AspectRatioMode.ORIGINAL -> Modifier.fillMaxSize()
            AspectRatioMode.RATIO_16_9 -> Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            AspectRatioMode.RATIO_9_16 -> Modifier.fillMaxWidth().aspectRatio(9f / 16f)
            AspectRatioMode.RATIO_1_1 -> Modifier.fillMaxWidth().aspectRatio(1f)
            AspectRatioMode.RATIO_4_3 -> Modifier.fillMaxWidth().aspectRatio(4f / 3f)
            AspectRatioMode.RATIO_2_39_1 -> Modifier.fillMaxWidth().aspectRatio(2.39f)
        }

        Box(
            modifier = containerModifier
                .clip(RoundedCornerShape(8.dp))
                .graphicsLayer {
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                    this.rotationZ = rotation
                    this.clip = true
                }
                .drawWithContent {
                    drawContent()

                    // Live Color Matrix filter (Monochrome, Warm, Cool, High Contrast)
                    if (recipe.moodPreset != MoodPreset.NONE && recipe.moodPreset != MoodPreset.VIGNETTE) {
                        drawIntoCanvas { canvas ->
                            val paint = Paint().apply {
                                colorFilter = ColorFilter.colorMatrix(colorMatrix)
                            }
                            canvas.saveLayer(
                                androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
                                paint
                            )
                            canvas.restore()
                        }
                    }

                    // Live Vignette overlay
                    if (recipe.moodPreset == MoodPreset.VIGNETTE && recipe.moodIntensity > 0.05f) {
                        val maxRadius = kotlin.math.hypot(size.width, size.height) / 1.7f
                        val vignetteBrush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = (recipe.moodIntensity * 0.85f).coerceIn(0f, 0.95f))
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = maxRadius
                        )
                        drawRect(vignetteBrush)
                    }
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = false // Custom controls managed via Compose
                        player = exoPlayer
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Live Hold Indicator Badge
        AnimatedVisibility(
            visible = isHoldingFrame,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ElectricAmber.copy(alpha = 0.9f)
            ) {
                Text(
                    text = "FRAME HOLD (${recipe.holdDurationMs / 1000.0}s)",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}
