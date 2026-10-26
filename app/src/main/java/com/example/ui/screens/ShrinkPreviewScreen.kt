package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.EncodingSpeed
import com.example.data.ExportResolution
import com.example.ui.navigation.Screen
import com.example.ui.theme.BorderStroke
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
fun ShrinkPreviewScreen(
    viewModel: SkipFrameViewModel
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val metadata by viewModel.selectedMetadata.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzingShrink.collectAsStateWithLifecycle()
    val estimate by viewModel.shrinkEstimate.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Shrink Preview",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        modifier = Modifier.testTag("back_to_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video Information Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderStroke, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = metadata?.fileName ?: "Selected Video",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Original: ${metadata?.formattedDuration ?: "0:00"} • ${metadata?.formattedSize ?: "0 MB"} • ${metadata?.width}x${metadata?.height} @ ${metadata?.fps?.roundToInt()}fps",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            // Honest Estimate Card (Prompt Requirement: Labeled strictly as an estimate, split into silence and frame skipping)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberTeal.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .testTag("shrink_estimate_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PREVIEW ESTIMATE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = CyberTeal
                        )
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = CyberTeal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (estimate != null) {
                        val est = estimate!!
                        Text(
                            text = "These settings will remove about ${String.format("%.1f", est.silenceRemovedSec + est.frameSkipRemovedSec)}s (${String.format("%.1f", est.estimatedPercentReduction)}%)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Split breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(DarkBackground, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("Silence Removal", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "-${String.format("%.1f", est.silenceRemovedSec)}s",
                                        fontWeight = FontWeight.Bold,
                                        color = CyberTeal,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text("${est.silenceCutsCount} cuts detected", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 10.sp)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(DarkBackground, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("Frame Skip (1/$config.frameSkipN)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "-${String.format("%.1f", est.frameSkipRemovedSec)}s",
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricAmber,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text("Drops every ${config.frameSkipN}th frame", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Estimated final duration: ~${String.format("%.1f", est.estimatedFinalDurationSec)}s (exact file size measured post-encode)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Text(
                            text = "Analyzing audio profile and frame rate structure...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }

            // Tuning Sliders Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderStroke, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = ElectricAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Shrink Tuning Controls",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // 1. Frame Skip N
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Frame Skip Step (N)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("Every ${config.frameSkipN}th frame", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ElectricAmber)
                        }
                        Slider(
                            value = config.frameSkipN.toFloat(),
                            onValueChange = { viewModel.updateConfig { c -> c.copy(frameSkipN = it.roundToInt()) } },
                            valueRange = 2f..24f,
                            steps = 21,
                            colors = SliderDefaults.colors(thumbColor = ElectricAmber, activeTrackColor = ElectricAmber),
                            modifier = Modifier.testTag("frame_skip_slider")
                        )
                        Text("Drops 1 frame every ${config.frameSkipN} frames, removing corresponding audio slice to stay in sync.", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                    }

                    // 2. Relative Silence Threshold
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Relative Silence Threshold", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("${config.relativeSilenceThresholdDb.roundToInt()} dB", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberTeal)
                        }
                        Slider(
                            value = config.relativeSilenceThresholdDb,
                            onValueChange = { viewModel.updateConfig { c -> c.copy(relativeSilenceThresholdDb = it) } },
                            valueRange = -45f..-15f,
                            colors = SliderDefaults.colors(thumbColor = CyberTeal, activeTrackColor = CyberTeal),
                            modifier = Modifier.testTag("silence_threshold_slider")
                        )
                    }

                    // 3. Quiet Parts Sensitivity
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Quiet Parts Sensitivity", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("${(config.quietPartsSensitivity * 100).roundToInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberTeal)
                        }
                        Slider(
                            value = config.quietPartsSensitivity,
                            onValueChange = { viewModel.updateConfig { c -> c.copy(quietPartsSensitivity = it) } },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = CyberTeal, activeTrackColor = CyberTeal),
                            modifier = Modifier.testTag("quiet_sensitivity_slider")
                        )
                        Text("Higher values also cut low background ambience as silence.", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                    }

                    // 4. Edge Cushion
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Edge Cushion", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("${config.edgeCushionMs} ms", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Slider(
                            value = config.edgeCushionMs.toFloat(),
                            onValueChange = { viewModel.updateConfig { c -> c.copy(edgeCushionMs = it.toLong()) } },
                            valueRange = 0f..300f,
                            colors = SliderDefaults.colors(thumbColor = ElectricAmber, activeTrackColor = ElectricAmber),
                            modifier = Modifier.testTag("edge_cushion_slider")
                        )
                        Text("Quiet padding kept around speech before/after silence cuts.", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                    }

                    // 5. Audio Crossfade
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Audio Crossfade", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("${config.audioCrossfadeMs} ms", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Slider(
                            value = config.audioCrossfadeMs.toFloat(),
                            onValueChange = { viewModel.updateConfig { c -> c.copy(audioCrossfadeMs = it.roundToInt()) } },
                            valueRange = 0f..10f,
                            steps = 9,
                            colors = SliderDefaults.colors(thumbColor = ElectricAmber, activeTrackColor = ElectricAmber),
                            modifier = Modifier.testTag("crossfade_slider")
                        )
                    }

                    // 6. Denoise Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Light Denoise", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Gentle 3D denoise filter before encoding", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = config.denoiseEnabled,
                            onCheckedChange = { viewModel.updateConfig { c -> c.copy(denoiseEnabled = it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricAmber, checkedTrackColor = ElectricAmber.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("denoise_switch")
                        )
                    }

                    // 7. Encoding Speed Chips
                    Column {
                        Text("Encoding Speed", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EncodingSpeed.values().forEach { speed ->
                                FilterChip(
                                    selected = config.encodingSpeed == speed,
                                    onClick = { viewModel.updateConfig { c -> c.copy(encodingSpeed = speed) } },
                                    label = { Text(speed.displayName, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricAmber,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                        Text(config.encodingSpeed.description, style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                    }

                    // 8. Resolution Chips
                    Column {
                        Text("Target Resolution", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExportResolution.values().forEach { res ->
                                FilterChip(
                                    selected = config.exportResolution == res,
                                    onClick = { viewModel.updateConfig { c -> c.copy(exportResolution = res) } },
                                    label = { Text(res.displayName, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberTeal,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Commit & Process Action Button
            Button(
                onClick = { viewModel.startShrinkProcessing() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("commit_shrink_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricAmber,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Commit & Shrink Video",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
