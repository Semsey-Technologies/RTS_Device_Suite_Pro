package com.semseytech.rtsdevicesuitepro.storage.analyzer

import android.text.format.Formatter
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Size as AndroidSize
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(
    file: FileInfo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .background(if (isSelected) NeonBlue.copy(alpha = 0.05f) else Color.Transparent)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                colors = CheckboxDefaults.colors(checkedColor = NeonGreen, uncheckedColor = Color.Gray)
            )
            Spacer(Modifier.width(8.dp))
        }

        val appIcon = remember(file) {
            if (file.category == FileCategory.APKS) {
                try {
                    if (file.path.startsWith("/")) {
                        val packageInfo = context.packageManager.getPackageArchiveInfo(file.path, 0)
                        packageInfo?.applicationInfo?.let { appInfo ->
                            appInfo.sourceDir = file.path
                            appInfo.publicSourceDir = file.path
                            appInfo.loadIcon(context.packageManager)
                        }
                    } else {
                        context.packageManager.getApplicationIcon(file.path)
                    }
                } catch (e: Exception) { null }
            } else null
        }

        val thumbnail = remember(file.path) {
            if (appIcon == null && (file.category == FileCategory.IMAGES || file.category == FileCategory.VIDEOS)) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        context.contentResolver.loadThumbnail(
                            file.uri ?: android.net.Uri.fromFile(java.io.File(file.path)),
                            AndroidSize(64, 64),
                            null
                        )
                    } else null
                } catch (e: Exception) { null }
            } else null
        }

        if (appIcon != null) {
            val iconBitmap = remember(appIcon) {
                val bitmap = android.graphics.Bitmap.createBitmap(
                    appIcon.intrinsicWidth.coerceAtLeast(1),
                    appIcon.intrinsicHeight.coerceAtLeast(1),
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bitmap)
                appIcon.setBounds(0, 0, canvas.width, canvas.height)
                appIcon.draw(canvas)
                bitmap.asImageBitmap()
            }
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp))
            )
        } else if (thumbnail != null) {
            Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            val icon = when (file.category) {
                FileCategory.IMAGES -> Icons.Default.Image
                FileCategory.VIDEOS -> Icons.Default.Movie
                FileCategory.AUDIO -> Icons.Default.MusicNote
                FileCategory.APKS -> Icons.Default.Android
                else -> Icons.Default.Description
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) NeonGreen else NeonBlue.copy(alpha = 0.6f),
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = file.name, color = Color.White, maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
            Text(text = file.path.takeLast(40), color = Color.Gray, fontSize = 10.sp, maxLines = 1, fontFamily = FontFamily.Monospace)
        }
        Text(text = Formatter.formatFileSize(context, file.size), color = NeonGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}
