package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScanningOverlay(progress: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NeonGreen, strokeWidth = 2.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "SCANNING STORAGE: $progress", 
                color = NeonGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                style = TextStyle(shadow = Shadow(color = NeonGreen.copy(alpha = 0.3f), blurRadius = 4f))
            )
        }
    }
}
