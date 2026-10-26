package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.Screen
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.SkipFrameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotteryDebugScreen(
    viewModel: SkipFrameViewModel
) {
    val context = LocalContext.current
    val profile by viewModel.loudnessProfile.collectAsStateWithLifecycle()
    val picks by viewModel.lotteryPicks.collectAsStateWithLifecycle()
    val seed by viewModel.currentSeed.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()

    val debugReport = buildString {
        appendLine("=== SKIPFRAME LOTTERY ENGINE DEBUG ===")
        appendLine("Seed: $seed")
        appendLine("Chaos (c): ${config.chaosC} (g = ${String.format("%.2f", 4.0 * (1.0 - config.chaosC))})")
        appendLine("Clip length L: ${config.clipLengthL}s, K: ${config.clipCountK}")
        appendLine("Raw mode: ${config.rawMode}, Mismatch: ${config.mismatch}, Fresh only: ${config.freshOnly}")
        appendLine("Target resolution: ${config.lotteryResolution.displayName}")
        appendLine()

        if (profile != null) {
            val p = profile!!
            appendLine("=== AUDIO STREAM ANALYSIS ===")
            appendLine("Audio duration: ${String.format("%.2f", p.durationSec)}s")
            appendLine("Total 100ms blocks: ${p.blockCount}")
            appendLine("Loudness range: [min: ${String.format("%.1f", p.minLoudnessDb)} dB, median: ${String.format("%.1f", p.medianLoudnessDb)} dB, max: ${String.format("%.1f", p.maxLoudnessDb)} dB]")
            appendLine("Candidate peaks detected: ${p.candidates.size}")
            appendLine()

            appendLine("=== CANDIDATE PEAKS SAMPLE (FIRST 15) ===")
            p.candidates.take(15).forEachIndexed { i, c ->
                appendLine("[$i] Peak: ${String.format("%.2f", c.peakSec)}s -> Start: ${String.format("%.2f", c.startSec)}s, End: ${String.format("%.2f", c.endSec)}s")
            }
            if (p.candidates.size > 15) {
                appendLine("... and ${p.candidates.size - 15} more candidates")
            }
            appendLine()
        } else {
            appendLine("No loudness profile computed yet.")
        }

        appendLine("=== ACTIVE PICKS DRAWN (${picks.size}) ===")
        picks.forEachIndexed { i, pick ->
            appendLine("Pick #${i + 1}: ${String.format("%.2f", pick.startSec)}s to ${String.format("%.2f", pick.endSec)}s (duration: ${String.format("%.2f", pick.duration)}s)")
            appendLine("  Score W: ${String.format("%.4f", pick.scoreW)}, Peak: ${String.format("%.2f", pick.peakSec)}s")
            if (pick.audioMoment != null) {
                appendLine("  [Mismatch Audio]: ${String.format("%.2f", pick.audioMoment.startSec)}s to ${String.format("%.2f", pick.audioMoment.endSec)}s")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = CyberTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lottery Engine Debug", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Lottery) },
                        modifier = Modifier.testTag("debug_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Lottery Debug Report", debugReport))
                            Toast.makeText(context, "Debug report copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("copy_debug_report_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Report", tint = ElectricAmber)
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
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                    .padding(12.dp)
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = debugReport,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = CyberTeal
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Lottery Debug Report", debugReport))
                    Toast.makeText(context, "Debug report copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("copy_debug_report_bottom_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberTeal, contentColor = DarkBackground)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Full Debug Output", fontWeight = FontWeight.Bold)
            }
        }
    }
}
