package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.engine.LotteryMoment
import com.example.ui.components.VideoPlayerView
import com.example.ui.navigation.LabSubMode
import com.example.ui.navigation.Screen
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.CoralFlame
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SkipFrameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Random

@OptIn(UnstableApi::class)
@Composable
fun LabScreen(
    viewModel: SkipFrameViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeSubMode by viewModel.activeLabSubMode.collectAsStateWithLifecycle()
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val metadata by viewModel.selectedMetadata.collectAsStateWithLifecycle()
    val currentSeed by viewModel.currentSeed.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val lotteryPicks by viewModel.lotteryPicks.collectAsStateWithLifecycle()

    var showErodeSheet by remember { mutableStateOf(false) }

    // Intercept back button to show erosion/exit cleanly
    BackHandler {
        showErodeSheet = true
    }

    if (selectedFile == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select a video first to enter the Lab.", color = TextPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.navigateTo(Screen.Home) }) {
                    Text("Return Home")
                }
            }
        }
        return
    }

    val videoPath = selectedFile!!.absolutePath
    val durationSec = metadata?.durationSeconds ?: 60.0

    // Main Lab UI: Gestures active, NO progress bar, NO time display, NO pause button!
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                // Swipe down detector to leave Lab
                detectDragGestures { _, dragAmount ->
                    if (dragAmount.y > 40f) {
                        showErodeSheet = true
                    }
                }
            }
    ) {
        when (activeSubMode) {
            LabSubMode.DRIFT -> {
                DriftModeView(
                    videoPath = videoPath,
                    durationSec = durationSec,
                    seed = currentSeed,
                    picks = lotteryPicks,
                    onRecordTelemetry = { pos, hold, skip ->
                        viewModel.recordLabBucketTelemetry(pos, hold, skip)
                    },
                    onRequestLeave = { showErodeSheet = true }
                )
            }
            LabSubMode.SPLIT -> {
                SplitModeView(
                    videoPath = videoPath,
                    durationSec = durationSec,
                    seed = currentSeed,
                    onRecordTelemetry = { pos, hold, skip ->
                        viewModel.recordLabBucketTelemetry(pos, hold, skip)
                    },
                    onRequestLeave = { showErodeSheet = true }
                )
            }
            LabSubMode.MOSAIC -> {
                MosaicModeView(
                    videoPath = videoPath,
                    durationSec = durationSec,
                    seed = currentSeed,
                    onRecordTelemetry = { pos, hold, skip ->
                        viewModel.recordLabBucketTelemetry(pos, hold, skip)
                    },
                    onRequestLeave = { showErodeSheet = true }
                )
            }
            LabSubMode.ERODE -> {
                // Erode is accessible via gesture sheet
                DriftModeView(
                    videoPath = videoPath,
                    durationSec = durationSec,
                    seed = currentSeed,
                    picks = lotteryPicks,
                    onRecordTelemetry = { pos, hold, skip ->
                        viewModel.recordLabBucketTelemetry(pos, hold, skip)
                    },
                    onRequestLeave = { showErodeSheet = true }
                )
            }
        }

        // Top bar overlay: Submode tabs & seed (minimalist, unobtrusive)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 36.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Seed small in corner; tapping copies it as specified
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                        .border(1.dp, CyberTeal.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("Lab Seed", currentSeed.toString()))
                            Toast.makeText(context, "Seed $currentSeed copied", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("lab_seed_badge")
                ) {
                    Text(
                        text = "Seed: $currentSeed",
                        color = CyberTeal,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Submode Switcher Chips
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(LabSubMode.DRIFT, LabSubMode.SPLIT, LabSubMode.MOSAIC).forEach { mode ->
                        val isSel = activeSubMode == mode
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSel) ElectricAmber else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.openLabSubMode(mode) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = mode.title,
                                color = if (isSel) Color.Black else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Close / Leave button
                IconButton(
                    onClick = { showErodeSheet = true },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .testTag("lab_leave_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Leave Lab", tint = TextPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Gesture Hints Overlay (fades out after user starts)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Tap: Next Moment • Hold: Stay Longer • Swipe Down: Leave",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Erode & Session Wrap Drawer Dialog
        if (showErodeSheet) {
            Dialog(onDismissRequest = { showErodeSheet = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .testTag("erode_modal_surface")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = CoralFlame)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Lab Session & Erosion",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            IconButton(onClick = { showErodeSheet = false }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                            }
                        }

                        Text(
                            text = "Erode removes a seeded 12.5% fraction of the movie's most-skipped 10s buckets based on your live watching behavior.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        // Action 1: Erode Button
                        Button(
                            onClick = {
                                viewModel.applyErosion()
                                Toast.makeText(context, "Erosion applied! 12.5% most-skipped buckets cut.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("apply_erosion_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CoralFlame, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Erode (-12.5% Skipped Buckets)", fontWeight = FontWeight.Bold)
                        }

                        // Action 2: Bake Real File Button
                        Button(
                            onClick = {
                                showErodeSheet = false
                                viewModel.bakeErosion()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("bake_erosion_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricAmber, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bake Eroded Video (Export)", fontWeight = FontWeight.Bold)
                        }

                        // Action 3: Restore Erosion
                        OutlinedButton(
                            onClick = {
                                viewModel.restoreErosion()
                                Toast.makeText(context, "Erosion restored to original.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("restore_erosion_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTeal),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberTeal.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore All Buckets")
                        }

                        // Leave Lab Button
                        OutlinedButton(
                            onClick = {
                                showErodeSheet = false
                                viewModel.navigateTo(Screen.Home)
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("leave_lab_home_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Exit to Home")
                        }
                    }
                }
            }
        }
    }
}

/**
 * A. Drift: Movie as a radio station. Endless chain of 5 to 20s lottery moments with audio crossfades.
 * Respects configured lottery draw order: Random, Calm to intense, Intense to calm, Reverse.
 */
@Composable
fun DriftModeView(
    videoPath: String,
    durationSec: Double,
    seed: Long,
    picks: List<LotteryMoment>,
    onRecordTelemetry: (pos: Long, hold: Long, skip: Boolean) -> Unit,
    onRequestLeave: () -> Unit
) {
    val random = remember(seed) { Random(seed) }
    var currentPickIndex by remember(picks, seed) { mutableIntStateOf(0) }

    val currentMomentStartSec: Double
    val currentMomentDurationSec: Double

    if (picks.isNotEmpty()) {
        val pick = picks[currentPickIndex.coerceIn(0, picks.size - 1)]
        currentMomentStartSec = pick.startSec
        currentMomentDurationSec = pick.duration.coerceIn(5.0, 20.0)
    } else {
        currentMomentStartSec = random.nextDouble() * durationSec * 0.8
        currentMomentDurationSec = (5 + random.nextInt(16)).toDouble()
    }

    var momentStartTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isHolding by remember { mutableStateOf(false) }

    // Auto-advance loop when moment duration ends (unless held)
    LaunchedEffect(currentPickIndex, currentMomentStartSec, currentMomentDurationSec) {
        momentStartTimeMs = System.currentTimeMillis()
        val targetMs = (currentMomentDurationSec * 1000).toLong()
        delay(targetMs)
        while (isHolding) {
            delay(500)
        }
        if (picks.isNotEmpty()) {
            currentPickIndex = (currentPickIndex + 1) % picks.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(picks, currentPickIndex) {
                detectTapGestures(
                    onTap = {
                        val elapsed = System.currentTimeMillis() - momentStartTimeMs
                        val wasSkipped = elapsed < 3000L
                        onRecordTelemetry((currentMomentStartSec * 1000).toLong(), 0L, wasSkipped)

                        // Advance to next moment in configured lottery order
                        if (picks.isNotEmpty()) {
                            currentPickIndex = (currentPickIndex + 1) % picks.size
                        }
                    },
                    onPress = {
                        isHolding = true
                        val holdStart = System.currentTimeMillis()
                        tryAwaitRelease()
                        val holdDelta = System.currentTimeMillis() - holdStart
                        isHolding = false
                    }
                )
            }
    ) {
        VideoPlayerView(
            videoUriOrPath = videoPath,
            startPositionMs = (currentMomentStartSec * 1000).toLong(),
            useControls = false,
            autoPlay = true,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * B. Split: Sound and picture follow separate paths!
 * Muted video player and audio player, each on own timer (8 to 25s).
 * Tap = new picture draw. Double tap = new sound draw.
 */
@Composable
fun SplitModeView(
    videoPath: String,
    durationSec: Double,
    seed: Long,
    onRecordTelemetry: (pos: Long, hold: Long, skip: Boolean) -> Unit,
    onRequestLeave: () -> Unit
) {
    val randomV = remember(seed) { Random(seed) }
    val randomA = remember(seed) { Random(seed + 9999) }

    var vStartSec by remember { mutableDoubleStateOf(randomV.nextDouble() * durationSec * 0.7) }
    var aStartSec by remember { mutableDoubleStateOf(randomA.nextDouble() * durationSec * 0.7) }
    var vDurationSec by remember { mutableDoubleStateOf((8 + randomV.nextInt(18)).toDouble()) }
    var aDurationSec by remember { mutableDoubleStateOf((8 + randomA.nextInt(18)).toDouble()) }

    // Video timer loop
    LaunchedEffect(vStartSec, vDurationSec) {
        delay((vDurationSec * 1000).toLong())
        val nextDur = (8 + randomV.nextInt(18)).toDouble()
        vStartSec = (randomV.nextDouble() * (durationSec - nextDur)).coerceAtLeast(0.0)
        vDurationSec = nextDur
    }

    // Audio timer loop
    LaunchedEffect(aStartSec, aDurationSec) {
        delay((aDurationSec * 1000).toLong())
        val nextDur = (8 + randomA.nextInt(18)).toDouble()
        aStartSec = (randomA.nextDouble() * (durationSec - nextDur)).coerceAtLeast(0.0)
        aDurationSec = nextDur
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // Double tap = new sound draw as specified
                        val nextDur = (8 + randomA.nextInt(18)).toDouble()
                        aStartSec = (randomA.nextDouble() * (durationSec - nextDur)).coerceAtLeast(0.0)
                        aDurationSec = nextDur
                    },
                    onTap = {
                        // Single tap = new picture draw as specified
                        val nextDur = (8 + randomV.nextInt(18)).toDouble()
                        vStartSec = (randomV.nextDouble() * (durationSec - nextDur)).coerceAtLeast(0.0)
                        vDurationSec = nextDur
                    }
                )
            }
    ) {
        // Video layer (muted)
        VideoPlayerView(
            videoUriOrPath = videoPath,
            startPositionMs = (vStartSec * 1000).toLong(),
            useControls = false,
            autoPlay = true,
            isMuted = true,
            modifier = Modifier.fillMaxSize()
        )

        // Audio layer (playing sound from independent start position)
        Box(modifier = Modifier.size(1.dp)) {
            VideoPlayerView(
                videoUriOrPath = videoPath,
                startPositionMs = (aStartSec * 1000).toLong(),
                useControls = false,
                autoPlay = true,
                isMuted = false,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Split Mode subtle status indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Picture: ${String.format("%.1f", vStartSec)}s • Sound: ${String.format("%.1f", aStartSec)}s", color = CyberTeal, fontSize = 10.sp)
        }
    }
}

/**
 * C. Mosaic: Two stacked panels each playing a different lottery moment, muted.
 * Sound comes from the panel tapped; tapping the other moves the sound.
 */
@Composable
fun MosaicModeView(
    videoPath: String,
    durationSec: Double,
    seed: Long,
    onRecordTelemetry: (pos: Long, hold: Long, skip: Boolean) -> Unit,
    onRequestLeave: () -> Unit
) {
    val random1 = remember(seed) { Random(seed + 1) }
    val random2 = remember(seed) { Random(seed + 2) }

    var pos1Sec by remember { mutableDoubleStateOf(random1.nextDouble() * durationSec * 0.8) }
    var pos2Sec by remember { mutableDoubleStateOf(random2.nextDouble() * durationSec * 0.8) }
    var activeAudioPanel by remember { mutableIntStateOf(1) } // 1 or 2

    Column(modifier = Modifier.fillMaxSize()) {
        // Panel 1
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(2.dp, if (activeAudioPanel == 1) CyberTeal else Color.Transparent)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            activeAudioPanel = 1
                            // New moment on tap
                            pos1Sec = random1.nextDouble() * durationSec * 0.8
                        }
                    )
                }
        ) {
            VideoPlayerView(
                videoUriOrPath = videoPath,
                startPositionMs = (pos1Sec * 1000).toLong(),
                useControls = false,
                autoPlay = true,
                isMuted = activeAudioPanel != 1,
                modifier = Modifier.fillMaxSize()
            )
            Icon(
                imageVector = if (activeAudioPanel == 1) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                contentDescription = null,
                tint = if (activeAudioPanel == 1) CyberTeal else TextMuted,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            )
        }

        // Panel 2
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(2.dp, if (activeAudioPanel == 2) ElectricAmber else Color.Transparent)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            activeAudioPanel = 2
                            // New moment on tap
                            pos2Sec = random2.nextDouble() * durationSec * 0.8
                        }
                    )
                }
        ) {
            VideoPlayerView(
                videoUriOrPath = videoPath,
                startPositionMs = (pos2Sec * 1000).toLong(),
                useControls = false,
                autoPlay = true,
                isMuted = activeAudioPanel != 2,
                modifier = Modifier.fillMaxSize()
            )
            Icon(
                imageVector = if (activeAudioPanel == 2) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                contentDescription = null,
                tint = if (activeAudioPanel == 2) ElectricAmber else TextMuted,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            )
        }
    }
}
