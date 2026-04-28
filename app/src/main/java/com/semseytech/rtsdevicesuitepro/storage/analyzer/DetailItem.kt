package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DetailItem(label: String, value: String) {
    Column {
        Text(text = "[$label]", color = NeonGreen.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(start = 4.dp))
    }
}
