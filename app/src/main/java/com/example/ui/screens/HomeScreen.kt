package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.ProcessedVideoEntity
import com.example.engine.ExportHelper
import com.example.engine.FFmpegEngine
import com.example.ui.components.VideoPlayerView
import com.example.ui.navigation.LabSubMode
import com.example.ui.navigation.Screen
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.CoralFlame
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ExecutionResult
import com.example.ui.viewmodel.SkipFrameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SkipFrameViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val processedList by viewModel.processedHistory.collectAsStateWithLifecycle()
    val totalSavedBytes by viewModel.totalStorageSaved.collectAsStateWithLifecycle()
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()

    var activePlaybackPath by remember { mutableStateOf<String?>(null) }
    var isGeneratingSample by remember { mutableStateOf(false) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectVideoUri(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Brush.linearGradient(listOf(ElectricAmber, CyberTeal)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = "SkipFrame Logo",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SKIPFRAME",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Settings) },
                        modifier = Modifier.testTag("settings_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero card: Total Storage Saved
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
                        .testTag("storage_saved_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL STORAGE SAVED",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyberTeal,
                                letterSpacing = 1.2.sp
                            )
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CyberTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        val formattedSaved = formatBytes(totalSavedBytes ?: 0L)
                        Text(
                            text = formattedSaved,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "On-device cuts remove silence & frames together. No gaps, perfect sync.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Primary Actions: Select Video, RE:VISION & Lab
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { videoPickerLauncher.launch("video/*") },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(54.dp)
                            .testTag("select_video_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricAmber,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Select Video",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.startRevisionMode() },
                        modifier = Modifier
                            .weight(1.1f)
                            .height(54.dp)
                            .testTag("revision_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CoralFlame
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralFlame)
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "RE:VISION",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (selectedFile != null) {
                                viewModel.navigateTo(Screen.Lab(LabSubMode.DRIFT))
                            } else {
                                viewModel.openLabSubMode(LabSubMode.DRIFT)
                            }
                        },
                        modifier = Modifier
                            .weight(0.8f)
                            .height(54.dp)
                            .testTag("lab_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CyberTeal
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberTeal)
                    ) {
                        Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Lab",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Phase 5: Personal Viewing Intelligence Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(Screen.PersonalIntelligence(0)) }
                        .border(1.dp, CyberTeal.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(CyberTeal.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = CyberTeal, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Personal Viewing Intelligence",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Custom experiences, experiment lab & AI director",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Shortcut tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DarkSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.navigateTo(Screen.PersonalIntelligence(0)) }
                            ) {
                                Text(
                                    text = "Experiences",
                                    fontSize = 11.sp,
                                    color = CyberTeal,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                    maxLines = 1
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DarkSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.navigateTo(Screen.PersonalIntelligence(1)) }
                            ) {
                                Text(
                                    text = "Experiments",
                                    fontSize = 11.sp,
                                    color = ElectricAmber,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                    maxLines = 1
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DarkSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.navigateTo(Screen.PersonalIntelligence(2)) }
                            ) {
                                Text(
                                    text = "AI Director",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                    maxLines = 1
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DarkSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.navigateTo(Screen.PersonalIntelligence(3)) }
                            ) {
                                Text(
                                    text = "Capabilities",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Active / Loaded video shortcut card if a video is already loaded
            if (selectedFile != null) {
                item {
                    val meta = viewModel.selectedMetadata.collectAsStateWithLifecycle().value
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ElectricAmber.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = ElectricAmber)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Current Video: ${meta?.fileName ?: selectedFile!!.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Duration: ${meta?.formattedDuration ?: "N/A"} • Size: ${meta?.formattedSize ?: "N/A"} • ${meta?.fps?.toInt() ?: 30} FPS",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.navigateTo(Screen.ShrinkPreview) },
                                    modifier = Modifier.weight(1f).testTag("resume_shrink_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricAmber, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Shrink", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Button(
                                    onClick = { viewModel.startLotteryMode() },
                                    modifier = Modifier.weight(1f).testTag("resume_lottery_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberTeal, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Lottery", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Button(
                                    onClick = { viewModel.startRevisionMode() },
                                    modifier = Modifier.weight(1f).testTag("resume_revision_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = CoralFlame, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("RE:VISION", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Generate Sample Video Button (for emulator / direct testing)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderStroke, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Need a test video on device?",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Synthesizes an on-device sample with speech & silent pauses.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Button(
                            onClick = {
                                isGeneratingSample = true
                                scope.launch(Dispatchers.IO) {
                                    val sampleFile = generateSampleVideo(context)
                                    withContext(Dispatchers.Main) {
                                        isGeneratingSample = false
                                        if (sampleFile != null) {
                                            viewModel.selectPresetFile(sampleFile)
                                        }
                                    }
                                }
                            },
                            enabled = !isGeneratingSample,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = ElectricAmber),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricAmber.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("generate_sample_video_button")
                        ) {
                            Text(if (isGeneratingSample) "Building..." else "Create Sample")
                        }
                    }
                }
            }

            // In-app player preview overlay if active
            if (activePlaybackPath != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            VideoPlayerView(
                                videoUriOrPath = activePlaybackPath!!,
                                useControls = true,
                                autoPlay = true,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { activePlaybackPath = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Close Player", tint = Color.White)
                            }
                        }
                    }
                }
            }

            // Recent Videos section header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RECENT PROCESSED VIDEOS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${processedList.size} exports",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            // Processed Videos List
            if (processedList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No processed videos yet. Select a video to start shrinking or rolling lottery clips.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            } else {
                items(processedList, key = { it.id }) { item ->
                    ProcessedVideoItem(
                        item = item,
                        onPlay = { activePlaybackPath = item.outputPath },
                        onShare = { ExportHelper.shareVideo(context, File(item.outputPath)) },
                        onDelete = { scope.launch { viewModel.repository.deleteProcessedVideo(item.id) } }
                    )
                }
            }
        }
    }
}

@Composable
fun ProcessedVideoItem(
    item: ProcessedVideoEntity,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderStroke, RoundedCornerShape(12.dp))
            .clickable { onPlay() }
            .testTag("processed_video_item_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Mode badge
                val (badgeColor, badgeText) = when (item.mode) {
                    "SHRINK" -> ElectricAmber to "SHRUNK"
                    "LOTTERY" -> CyberTeal to "LOTTERY"
                    else -> CoralFlame to "ERODED"
                }
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }

                Text(
                    text = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(item.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))
            // Size & Duration savings
            val origMb = item.originalSizeBytes / (1024.0 * 1024.0)
            val finalMb = item.finalSizeBytes / (1024.0 * 1024.0)
            val percentSaved = if (item.originalSizeBytes > 0) {
                ((item.originalSizeBytes - item.finalSizeBytes).toDouble() / item.originalSizeBytes.toDouble()) * 100.0
            } else 0.0

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format("%.1f MB → %.1f MB", origMb, finalMb),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("(%.1f%% saved)", percentSaved),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = CyberTeal
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPlay, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = ElectricAmber)
                }
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = TextSecondary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CoralFlame)
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.0f KB", kb)
        else -> "$bytes B"
    }
}

/**
 * Creates a synthetic local sample video using FFmpeg with color bars, tone bursts and silent pauses.
 */
private fun generateSampleVideo(context: android.content.Context): File? {
    return try {
        val out = File(context.cacheDir, "sample_skipframe_demo.mp4")
        if (out.exists()) return out
        // Generates 15 seconds test video with 3 burst audio segments and 2 silent gaps
        val cmd = "-y -f lavfi -i testsrc=size=1280x720:rate=30:duration=15 " +
                "-f lavfi -i \"sine=frequency=1000:duration=3,volume=0.8[a1];aevalsrc=0:d=3[s1];sine=frequency=800:duration=4,volume=0.9[a2];aevalsrc=0:d=2[s2];sine=frequency=1200:duration=3,volume=0.7[a3];[a1][s1][a2][s2][a3]concat=n=5:v=0:a=1[aout]\" " +
                "-map 0:v -map \"[aout]\" -c:v libx264 -preset ultrafast -crf 26 -c:a aac -b:a 128k \"${out.absolutePath}\""
        FFmpegEngine.executeCommand(context, cmd)
        if (out.exists() && out.length() > 0) out else null
    } catch (t: Throwable) {
        null
    }
}
