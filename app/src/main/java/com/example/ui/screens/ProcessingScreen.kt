package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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

@Composable
fun ProcessingScreen(
    viewModel: SkipFrameViewModel
) {
    val status by viewModel.processingStatus.collectAsStateWithLifecycle()
    val metadata by viewModel.selectedMetadata.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()

    val animatedProgress by animateFloatAsState(
        targetValue = status.progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "ProcessingProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Pulse Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(ElectricAmber.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, ElectricAmber, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(80.dp),
                    color = CyberTeal,
                    strokeWidth = 3.dp,
                    trackColor = DarkSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = ElectricAmber,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = status.stageName.ifBlank { "Processing..." },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${(animatedProgress * 100).toInt()}% Complete",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = CyberTeal
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .testTag("processing_linear_progress"),
                color = CyberTeal,
                trackColor = DarkSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Video details card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderStroke, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = metadata?.fileName ?: "Target Video",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Original Duration", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text(metadata?.formattedDuration ?: "0:00", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Original Size", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text(metadata?.formattedSize ?: "0 MB", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Encoding Engine", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text(config.encodingSpeed.displayName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ElectricAmber)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Cancel Button
            OutlinedButton(
                onClick = { viewModel.cancelActiveProcessing() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("cancel_processing_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralFlame),
                border = androidx.compose.foundation.BorderStroke(1.dp, CoralFlame.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cancel Processing")
            }
        }
    }
}
