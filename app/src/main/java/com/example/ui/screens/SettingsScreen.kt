package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.SkipFrameConfig
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
fun SettingsScreen(
    viewModel: SkipFrameViewModel
) {
    val config by viewModel.config.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Engine Settings",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        modifier = Modifier.testTag("settings_back_button")
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
                        onClick = { viewModel.updateConfig { SkipFrameConfig() } },
                        modifier = Modifier.testTag("reset_defaults_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Reset Defaults",
                            tint = TextSecondary
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
            // Mode 1: Shrink Settings Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderStroke, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = ElectricAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mode 1: Shrink Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // Relative Silence Threshold
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Relative Silence Threshold", color = TextPrimary, fontSize = 13.sp)
                            Text("${config.relativeSilenceThresholdDb.roundToInt()} dB", fontWeight = FontWeight.Bold, color = CyberTeal)
                        }
                        Slider(
                            value = config.relativeSilenceThresholdDb,
                            onValueChange = { viewModel.updateConfig { c -> c.copy(relativeSilenceThresholdDb = it) } },
                            valueRange = -45f..-15f,
                            colors = SliderDefaults.colors(thumbColor = CyberTeal, activeTrackColor = CyberTeal)
                        )
                    }

                    // Quiet parts sensitivity
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Quiet Parts Sensitivity", color = TextPrimary, fontSize = 13.sp)
                            Text("${(config.quietPartsSensitivity * 100).roundToInt()}%", fontWeight = FontWeight.Bold, color = CyberTeal)
                        }
                        Slider(
                            value = config.quietPartsSensitivity,
                            onValueChange = { viewModel.updateConfig { c -> c.copy(quietPartsSensitivity = it) } },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = CyberTeal, activeTrackColor = CyberTeal)
                        )
                    }

                    // Minimum silence duration
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Minimum Silence Duration", color = TextPrimary, fontSize = 13.sp)
                            Text("${config.minSilenceDurationMs} ms", fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Slider(
                            value = config.minSilenceDurationMs.toFloat(),
                            onValueChange = { viewModel.updateConfig { c -> c.copy(minSilenceDurationMs = it.toLong()) } },
                            valueRange = 100f..1000f,
                            colors = SliderDefaults.colors(thumbColor = ElectricAmber, activeTrackColor = ElectricAmber)
                        )
                    }

                    // Edge cushion
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Edge Cushion", color = TextPrimary, fontSize = 13.sp)
                            Text("${config.edgeCushionMs} ms", fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Slider(
                            value = config.edgeCushionMs.toFloat(),
                            onValueChange = { viewModel.updateConfig { c -> c.copy(edgeCushionMs = it.toLong()) } },
                            valueRange = 0f..300f,
                            colors = SliderDefaults.colors(thumbColor = ElectricAmber, activeTrackColor = ElectricAmber)
                        )
                    }

                    // Frame skip N
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Frame Skip N", color = TextPrimary, fontSize = 13.sp)
                            Text("N = ${config.frameSkipN}", fontWeight = FontWeight.Bold, color = ElectricAmber)
                        }
                        Slider(
                            value = config.frameSkipN.toFloat(),
                            onValueChange = { viewModel.updateConfig { c -> c.copy(frameSkipN = it.roundToInt()) } },
                            valueRange = 2f..24f,
                            steps = 21,
                            colors = SliderDefaults.colors(thumbColor = ElectricAmber, activeTrackColor = ElectricAmber)
                        )
                    }

                    // Audio crossfade
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Audio Crossfade", color = TextPrimary, fontSize = 13.sp)
                            Text("${config.audioCrossfadeMs} ms", fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Slider(
                            value = config.audioCrossfadeMs.toFloat(),
                            onValueChange = { viewModel.updateConfig { c -> c.copy(audioCrossfadeMs = it.roundToInt()) } },
                            valueRange = 0f..10f,
                            steps = 9,
                            colors = SliderDefaults.colors(thumbColor = ElectricAmber, activeTrackColor = ElectricAmber)
                        )
                    }

                    // Denoise
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Light Denoise", color = TextPrimary, fontSize = 13.sp)
                        Switch(
                            checked = config.denoiseEnabled,
                            onCheckedChange = { viewModel.updateConfig { c -> c.copy(denoiseEnabled = it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricAmber, checkedTrackColor = ElectricAmber.copy(alpha = 0.4f))
                        )
                    }
                }
            }

            // Mode 2: Surprise Lottery Settings Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderStroke, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Casino, contentDescription = null, tint = CyberTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mode 2: Surprise Lottery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // Aspect format
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Aspect Format", color = TextPrimary, fontSize = 13.sp)
                            Text(if (config.lotteryAspectFitBlur) "Fit with blurred background" else "Center crop 9:16", color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = config.lotteryAspectFitBlur,
                            onCheckedChange = { viewModel.updateConfig { c -> c.copy(lotteryAspectFitBlur = it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberTeal, checkedTrackColor = CyberTeal.copy(alpha = 0.4f))
                        )
                    }
                }
            }

            // Mode 3: Lab & Erode Settings
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderStroke, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = CoralFlame)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mode 3: Lab & Erode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // Erode fraction
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Erosion Fraction Step", color = TextPrimary, fontSize = 13.sp)
                            Text("${(config.erodeFraction * 100).roundToInt()}%", fontWeight = FontWeight.Bold, color = CoralFlame)
                        }
                        Slider(
                            value = config.erodeFraction,
                            onValueChange = { viewModel.updateConfig { c -> c.copy(erodeFraction = it) } },
                            valueRange = 0.05f..0.30f,
                            colors = SliderDefaults.colors(thumbColor = CoralFlame, activeTrackColor = CoralFlame)
                        )
                    }
                }
            }

            // Reset to defaults button
            OutlinedButton(
                onClick = { viewModel.updateConfig { SkipFrameConfig() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("reset_all_settings_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset All to Defaults")
            }
        }
    }
}
