package com.semseytech.rtsdevicesuitepro.storage.analyzer

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CategoryRow(category: FileCategory, info: CategoryInfo, onClick: () -> Unit) {
    val context = LocalContext.current
    val icon = when(category) {
        FileCategory.IMAGES -> Icons.Default.Image
        FileCategory.VIDEOS -> Icons.Default.VideoFile
        FileCategory.AUDIO -> Icons.Default.Audiotrack
        FileCategory.DOCUMENTS -> Icons.Default.Description
        FileCategory.ARCHIVES -> Icons.Default.FolderZip
        FileCategory.APKS -> Icons.Default.Android
        FileCategory.OTHERS -> Icons.Default.QuestionMark
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .border(0.5.dp, NeonBlue.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(NeonBlue.copy(alpha = 0.05f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = NeonBlue, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = category.name, color = Color.White, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            Text(text = "COUNT: ${info.count}", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Text(text = Formatter.formatFileSize(context, info.totalSize), color = NeonGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
    }
}
