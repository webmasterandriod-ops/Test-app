package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.ExportHelper
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
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    viewModel: SkipFrameViewModel
) {
    val context = LocalContext.current
    val result by viewModel.lastResult.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Export Completed",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        modifier = Modifier.testTag("results_home_button")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Home")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        if (result == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No export results available.", color = TextMuted)
            }
            return@Scaffold
        }

        val res = result!!
        val outputFile = File(res.outputPath)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = CyberTeal,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Measured Export Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = res.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            // In-App Player Preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, BorderStroke, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                VideoPlayerView(
                    videoUriOrPath = res.outputPath,
                    useControls = true,
                    autoPlay = true,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Honest Measured Metrics Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberTeal.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ACTUAL ON-DISK METRICS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = CyberTeal,
                        letterSpacing = 1.sp
                    )

                    // Size Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("File Size", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        val origMb = res.originalSizeBytes / (1024.0 * 1024.0)
                        val finalMb = res.finalSizeBytes / (1024.0 * 1024.0)
                        Text(
                            text = String.format("%.2f MB → %.2f MB", origMb, finalMb),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // Percentage Saved
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Storage Reduced", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Text(
                            text = String.format("%.1f%% Saved", res.percentSaved),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = ElectricAmber
                        )
                    }

                    // Duration Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Duration", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        val origSec = res.originalDurationMs / 1000.0
                        val finalSec = res.finalDurationMs / 1000.0
                        Text(
                            text = String.format("%.1fs → %.1fs", origSec, finalSec),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    // Processing Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Processing Time", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Text(
                            text = "${res.processingTimeMs / 1000.0}s",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    // Encoding Speed Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Encoding Engine Used", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Text(
                            text = res.encodingSpeed,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CyberTeal
                        )
                    }
                }
            }

            // Primary Action Buttons: Save to Gallery & Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val savedUri = ExportHelper.saveToGallery(context, outputFile, res.title)
                        if (savedUri != null) {
                            Toast.makeText(context, "Saved to Movies/SkipFrame!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to save to Gallery", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("save_to_gallery_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberTeal,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to Gallery", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { ExportHelper.shareVideo(context, outputFile) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("share_video_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricAmber,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontWeight = FontWeight.Bold)
                }
            }

            // Secondary: Delete and Home
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        outputFile.delete()
                        viewModel.navigateTo(Screen.Home)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("delete_export_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralFlame),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoralFlame.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Export")
                }

                OutlinedButton(
                    onClick = { viewModel.navigateTo(Screen.Home) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("back_home_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Home Screen")
                }
            }
        }
    }
}
