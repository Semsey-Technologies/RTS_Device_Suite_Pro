package com.semseytech.rtsdevicesuitepro.storage.analyzer

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SummaryCard(stats: StorageStats) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonBlue.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color.Black)))
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = "DRIVE VOL 01", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                Text(
                    text = "[${Formatter.formatFileSize(context, stats.totalBytes)}]", 
                    color = NeonBlue, fontFamily = FontFamily.Monospace, fontSize = 14.sp,
                    style = TextStyle(shadow = Shadow(color = NeonBlue.copy(alpha = 0.4f), blurRadius = 4f))
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { if (stats.totalBytes > 0) stats.usedBytes.toFloat() / stats.totalBytes else 0f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = NeonGreen,
                trackColor = Color.White.copy(alpha = 0.05f),
                strokeCap = StrokeCap.Square
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(text = "ALLOCATED", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text(text = Formatter.formatFileSize(context, stats.usedBytes), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "FREE BLOCKS", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text(text = Formatter.formatFileSize(context, stats.freeBytes), color = NeonGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
            }
        }
    }
}
