package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ExportHelper
import com.example.engine.FFmpegFailure
import com.example.ui.theme.CoralFlame
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun FFmpegErrorDialog(
    failure: FFmpegFailure,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = CoralFlame
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FFmpeg Execution Failed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CoralFlame
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Return Code: ${failure.returnCode ?: "N/A"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                if (!failure.filterScriptPath.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Filter Script: ${failure.filterScriptPath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                if (!failure.fullLogFilePath.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Saved Full Log: ${failure.fullLogFilePath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Last 40 lines of session logs:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .background(DarkBackground, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                        .horizontalScroll(rememberScrollState())
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = failure.last40Logs,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFFFB4A2),
                        lineHeight = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            if (!failure.fullLogFilePath.isNullOrBlank()) {
                Button(
                    onClick = {
                        ExportHelper.shareLogFile(context, failure.fullLogFilePath)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralFlame),
                    modifier = Modifier.testTag("share_error_log_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share Log", modifier = Modifier.padding(end = 4.dp))
                    Text("Share Log")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_error_dialog_button")
            ) {
                Text("Dismiss")
            }
        },
        containerColor = DarkSurfaceVariant
    )
}
