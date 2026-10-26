package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.LoudnessProfile
import com.example.engine.LotteryMoment
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.CoralFlame
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.max

@Composable
fun TerrainView(
    profile: LoudnessProfile,
    currentPicks: List<LotteryMoment>,
    earlierExportRanges: List<Pair<Double, Double>>,
    onTapPreview: (sec: Double) -> Unit,
    onLongPressForcePick: (sec: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = max(0.1, profile.durationSec)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("terrain_view_container")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOUDNESS TERRAIN m(t)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = CyberTeal,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Tap: 5s Preview • Hold: Force Pick",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(DarkBackground, RoundedCornerShape(8.dp))
                .pointerInput(profile, duration) {
                    detectTapGestures(
                        onTap = { offset ->
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val sec = fraction * duration
                            onTapPreview(sec)
                        },
                        onLongPress = { offset ->
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val sec = fraction * duration
                            onLongPressForcePick(sec)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val blocks = profile.smoothedM.size
                if (blocks < 2) return@Canvas

                // 1. Shaded regions used by earlier exports (Fresh only exclusion)
                for ((eStart, eEnd) in earlierExportRanges) {
                    val x0 = ((eStart / duration) * w).toFloat().coerceIn(0f, w)
                    val x1 = ((eEnd / duration) * w).toFloat().coerceIn(0f, w)
                    drawRect(
                        color = Color(0x44FF5252),
                        topLeft = Offset(x0, 0f),
                        size = Size(max(2f, x1 - x0), h)
                    )
                }

                // 2. Waveform m(t) path
                val path = Path()
                path.moveTo(0f, h)

                for (i in 0 until blocks) {
                    val x = (i.toFloat() / (blocks - 1)) * w
                    val mVal = profile.smoothedM[i].coerceIn(0f, 1f)
                    val y = h - (mVal * (h - 8f)) - 4f
                    if (i == 0) path.lineTo(x, y) else path.lineTo(x, y)
                }
                path.lineTo(w, h)
                path.close()

                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CyberTeal.copy(alpha = 0.8f),
                            CyberTeal.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )

                // 3. Mark current picks (vertical glowing spans)
                for (pick in currentPicks) {
                    val px0 = ((pick.startSec / duration) * w).toFloat().coerceIn(0f, w)
                    val px1 = ((pick.endSec / duration) * w).toFloat().coerceIn(0f, w)
                    val pickWidth = max(4f, px1 - px0)

                    // Pick span highlight
                    drawRect(
                        color = ElectricAmber.copy(alpha = 0.35f),
                        topLeft = Offset(px0, 0f),
                        size = Size(pickWidth, h)
                    )

                    // Left & right cut boundaries
                    drawLine(
                        color = ElectricAmber,
                        start = Offset(px0, 0f),
                        end = Offset(px0, h),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = ElectricAmber,
                        start = Offset(px1, 0f),
                        end = Offset(px1, h),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Peak point marker
                    val peakX = ((pick.peakSec / duration) * w).toFloat().coerceIn(0f, w)
                    drawCircle(
                        color = CoralFlame,
                        radius = 3.dp.toPx(),
                        center = Offset(peakX, 8f)
                    )
                }

                // Bottom baseline
                drawLine(
                    color = BorderStroke,
                    start = Offset(0f, h - 1f),
                    end = Offset(w, h - 1f),
                    strokeWidth = 1f
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(10.dp)
                    .background(CyberTeal, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("m(t) energy", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 10.sp)

            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(10.dp)
                    .background(ElectricAmber, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Active picks", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 10.sp)

            if (earlierExportRanges.isNotEmpty()) {
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(10.dp)
                        .background(CoralFlame.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Prior exports", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 10.sp)
            }
        }
    }
}
