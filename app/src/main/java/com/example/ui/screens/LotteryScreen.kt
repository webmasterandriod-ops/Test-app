package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.LotteryOrder
import com.example.data.LotteryOutputShape
import java.util.Locale
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.LotteryMoment
import com.example.ui.components.TerrainView
import com.example.ui.components.VideoPlayerView
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotteryScreen(
    viewModel: SkipFrameViewModel
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzingLottery.collectAsStateWithLifecycle()
    val profile by viewModel.loudnessProfile.collectAsStateWithLifecycle()
    val currentSeed by viewModel.currentSeed.collectAsStateWithLifecycle()
    val previousSeed by viewModel.previousSeed.collectAsStateWithLifecycle()
    val picks by viewModel.lotteryPicks.collectAsStateWithLifecycle()
    val earlierRanges by viewModel.earlierExportRanges.collectAsStateWithLifecycle()
    val terrainPreviewSec by viewModel.terrainPreviewSec.collectAsStateWithLifecycle()
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Surprise Lottery",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        modifier = Modifier.testTag("lottery_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.LotteryDebug) },
                        modifier = Modifier.testTag("open_debug_view_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Debug View",
                            tint = CyberTeal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CyberTeal)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Decoding 8kHz stream & calculating loudness...",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Interactive Terrain View
            item {
                if (profile != null) {
                    TerrainView(
                        profile = profile!!,
                        currentPicks = picks,
                        earlierExportRanges = earlierRanges,
                        onTapPreview = { sec -> viewModel.setTerrainPreview(sec) },
                        onLongPressForcePick = { sec ->
                            viewModel.forceMomentFromTerrain(sec)
                            Toast.makeText(context, "Forced moment at ${String.format("%.1f", sec)}s", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Seed & Re-roll Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderStroke, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "ACTIVE SEED (PRNG)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$currentSeed",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricAmber
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Lottery Seed", currentSeed.toString()))
                                        Toast.makeText(context, "Seed copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Seed", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (previousSeed != null) {
                                OutlinedButton(
                                    onClick = { viewModel.restorePreviousLotterySeed() },
                                    modifier = Modifier.testTag("previous_result_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Prev", fontSize = 12.sp)
                                }
                            }

                            Button(
                                onClick = { viewModel.rollLotteryDraw() },
                                modifier = Modifier.testTag("reroll_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberTeal, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Re-roll", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Lottery Parameters Card
            item {
                val maxK = (config.clipLengthL / 5).coerceIn(1, 200)
                var lengthInput by remember(config.clipLengthL) { mutableStateOf(config.clipLengthL.toString()) }
                var countInput by remember(config.clipCountK) { mutableStateOf(config.clipCountK.toString()) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderStroke, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Target Length (L) - up to 60 min (3600s)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Target length: ${String.format(Locale.US, "%.1f min", config.clipLengthL / 60.0)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberTeal
                                    )
                                    Text(
                                        text = "Average clip length: ${String.format(Locale.US, "%.1f s", config.clipLengthL.toDouble() / config.clipCountK.coerceAtLeast(1).toDouble())} (min 5s per clip)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }

                                // Quick numeric text field for L
                                OutlinedTextField(
                                    value = lengthInput,
                                    onValueChange = { str ->
                                        lengthInput = str
                                        val num = str.toIntOrNull()
                                        if (num != null && num in 10..3600) {
                                            viewModel.updateConfig { c -> c.copy(clipLengthL = num) }
                                        }
                                    },
                                    label = { Text("Sec", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .width(90.dp)
                                        .height(52.dp)
                                        .testTag("clip_length_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberTeal,
                                        unfocusedBorderColor = BorderStroke
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Slider(
                                value = config.clipLengthL.toFloat(),
                                onValueChange = {
                                    val newL = it.roundToInt()
                                    viewModel.updateConfig { c -> c.copy(clipLengthL = newL) }
                                },
                                valueRange = 10f..3600f,
                                colors = SliderDefaults.colors(thumbColor = CyberTeal, activeTrackColor = CyberTeal),
                                modifier = Modifier.testTag("clip_length_slider")
                            )
                        }

                        // 2. Clip Count (K) - up to 200, each clip at least 5s
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Clip Count (K): ${config.clipCountK} clip(s)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricAmber
                                    )
                                    Text(
                                        text = "Max K for ${config.clipLengthL}s is $maxK clips (at least 5s each)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }

                                OutlinedTextField(
                                    value = countInput,
                                    onValueChange = { str ->
                                        countInput = str
                                        val num = str.toIntOrNull()
                                        if (num != null && num in 1..maxK) {
                                            viewModel.updateConfig { c -> c.copy(clipCountK = num) }
                                        }
                                    },
                                    label = { Text("K", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .width(90.dp)
                                        .height(52.dp)
                                        .testTag("clip_count_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricAmber,
                                        unfocusedBorderColor = BorderStroke
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Slider(
                                value = config.clipCountK.coerceIn(1, maxK).toFloat(),
                                onValueChange = {
                                    val newK = it.roundToInt().coerceIn(1, maxK)
                                    viewModel.updateConfig { c -> c.copy(clipCountK = newK) }
                                },
                                valueRange = 1f..maxK.toFloat(),
                                colors = SliderDefaults.colors(thumbColor = ElectricAmber, activeTrackColor = ElectricAmber),
                                modifier = Modifier.testTag("clip_count_slider")
                            )
                        }

                        // 3. Output Shape: "Original (no crop)" vs 9:16
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Output Shape", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                val isAutoOriginal = config.clipLengthL > 180
                                Text(
                                    text = if (isAutoOriginal) "Auto: Original (> 3 min)" else "Auto: 9:16 (≤ 3 min)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyberTeal,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val currentShape = config.effectiveLotteryOutputShape
                                LotteryOutputShape.values().forEach { shape ->
                                    val isSelected = currentShape == shape
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.updateConfig { c -> c.copy(customLotteryOutputShape = shape) }
                                        },
                                        label = { Text(shape.displayName, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CyberTeal,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }
                        }

                        // 4. Spread across movie Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Spread across movie", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Divides movie into K equal sections and draws 1 moment from each", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                            }
                            Switch(
                                checked = config.spreadAcrossMovie,
                                onCheckedChange = { viewModel.updateConfig { c -> c.copy(spreadAcrossMovie = it) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyberTeal, checkedTrackColor = CyberTeal.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("spread_across_movie_switch")
                            )
                        }

                        // 5. Order Option: Random, Calm to intense, Intense to calm, Reverse
                        Column {
                            Text("Clip Order", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LotteryOrder.values().forEach { order ->
                                    val isSelected = config.lotteryOrder == order
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.updateConfig { c -> c.copy(lotteryOrder = order) }
                                        },
                                        label = { Text(order.displayName, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricAmber,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }
                            Text(config.lotteryOrder.description, style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                        }

                        // Slider Chaos (c)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Chaos (c)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                Text(String.format(Locale.US, "%.2f (g=%.1f)", config.chaosC, 4.0 * (1.0 - config.chaosC)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = CoralFlame)
                            }
                            Slider(
                                value = config.chaosC,
                                onValueChange = { viewModel.updateConfig { c -> c.copy(chaosC = it) } },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = CoralFlame, activeTrackColor = CoralFlame),
                                modifier = Modifier.testTag("chaos_slider")
                            )
                        }

                        // Switches: Raw mode, Mismatch, Fresh only
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Raw Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(if (config.rawMode) "ON: Uncut pure math peaks" else "OFF: Snaps cuts to silence & buffers", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                            }
                            Switch(
                                checked = config.rawMode,
                                onCheckedChange = { viewModel.updateConfig { c -> c.copy(rawMode = it) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyberTeal, checkedTrackColor = CyberTeal.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("raw_mode_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Mismatch Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Plays audio of a different draw under the video picture", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                            }
                            Switch(
                                checked = config.mismatch,
                                onCheckedChange = { viewModel.updateConfig { c -> c.copy(mismatch = it) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = ElectricAmber, checkedTrackColor = ElectricAmber.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("mismatch_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Fresh Only", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Excludes moments already exported in prior runs", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                            }
                            Switch(
                                checked = config.freshOnly,
                                onCheckedChange = { viewModel.updateConfig { c -> c.copy(freshOnly = it) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyberTeal, checkedTrackColor = CyberTeal.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("fresh_only_switch")
                            )
                        }
                    }
                }
            }

            // Current Picks List Header & Items
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "DRAW PICKS (${picks.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Output: ${config.effectiveLotteryOutputShape.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTeal
                    )
                }
            }

            itemsIndexed(picks) { index, pick ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderStroke, RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(ElectricAmber, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${index + 1}", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = pick.formattedRange,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Score W: ${String.format("%.3f", pick.scoreW)} • Peak at ${String.format("%.1f", pick.peakSec)}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            if (pick.audioMoment != null) {
                                Text(
                                    text = "Mismatch Audio: ${pick.audioMoment.formattedRange}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ElectricAmber,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.setTerrainPreview(pick.startSec) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Preview Pick", tint = CyberTeal)
                        }
                    }
                }
            }

            // Export Vertical Clip Action Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.exportLotteryClip() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("export_lottery_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberTeal,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Export ${config.effectiveLotteryOutputShape.displayName} Clip",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Tap-to-preview 5-second dialog
        if (terrainPreviewSec != null && selectedFile != null) {
            Dialog(onDismissRequest = { viewModel.setTerrainPreview(null) }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Instant Preview: ${String.format("%.1f", terrainPreviewSec!!)}s",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            IconButton(
                                onClick = { viewModel.setTerrainPreview(null) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            VideoPlayerView(
                                videoUriOrPath = selectedFile!!.absolutePath,
                                startPositionMs = (terrainPreviewSec!! * 1000).toLong(),
                                useControls = false,
                                autoPlay = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Playing live stream without encoding. Tap outside to close.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
