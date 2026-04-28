package com.semseytech.rtsdevicesuitepro.storage.analyzer

import android.text.format.Formatter
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Size as AndroidSize

data class GridItemSizes(
    val boxSize: Dp,
    val iconSize: Dp,
    val fontSizeTitle: TextUnit,
    val fontSizeSub: TextUnit
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    file: FileInfo,
    viewMode: ViewMode = ViewMode.GRID_MEDIUM,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    val sizes = remember(viewMode) {
        when (viewMode) {
            ViewMode.GRID_SMALL -> GridItemSizes(
                boxSize = 44.dp,
                iconSize = 32.dp,
                fontSizeTitle = 9.sp,
                fontSizeSub = 8.sp
            )
            ViewMode.GRID_LARGE -> GridItemSizes(
                boxSize = 100.dp,
                iconSize = 80.dp,
                fontSizeTitle = 14.sp,
                fontSizeSub = 12.sp
            )
            else -> GridItemSizes(
                boxSize = 60.dp,
                iconSize = 48.dp,
                fontSizeTitle = 11.sp,
                fontSizeSub = 10.sp
            )
        }
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
        if (appIcon != null) null
        else if (file.category == FileCategory.IMAGES || file.category == FileCategory.VIDEOS) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(
                        file.uri ?: android.net.Uri.fromFile(java.io.File(file.path)),
                        AndroidSize(128, 128),
                        null
                    )
                } else null
            } catch (e: Exception) { null }
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .background(if (isSelected) NeonBlue.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .border(0.5.dp, if (isSelected) NeonGreen else NeonBlue.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(sizes.boxSize)) {
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
                    modifier = Modifier.size(sizes.iconSize)
                )
            } else if (thumbnail != null) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().background(Color.Black, RoundedCornerShape(2.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
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
                    modifier = Modifier.size(sizes.iconSize)
                )
            }
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    colors = CheckboxDefaults.colors(checkedColor = NeonGreen, uncheckedColor = Color.Gray),
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = file.name, 
            color = Color.White, 
            maxLines = 1, 
            fontSize = sizes.fontSizeTitle, 
            fontFamily = FontFamily.Monospace, 
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = Formatter.formatFileSize(context, file.size), 
            color = NeonGreen, 
            fontSize = sizes.fontSizeSub, 
            fontFamily = FontFamily.Monospace
        )
    }
}
