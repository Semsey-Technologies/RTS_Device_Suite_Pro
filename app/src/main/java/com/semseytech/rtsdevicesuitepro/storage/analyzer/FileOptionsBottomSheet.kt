package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileOptionsBottomSheet(
    file: FileInfo,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DeepDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = NeonBlue) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
            Text(
                text = file.name,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = file.path,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(16.dp))
            FileOptionItem(Icons.Default.OpenInNew, "OPEN", NeonBlue) { onAction("open") }
            FileOptionItem(Icons.Default.Share, "SHARE", NeonBlue) { onAction("share") }
            FileOptionItem(Icons.Default.Edit, "RENAME", NeonBlue) { onAction("rename") }
            FileOptionItem(Icons.Default.DriveFileMove, "MOVE", NeonBlue) { onAction("move") }
            FileOptionItem(Icons.Default.ContentCopy, "COPY", NeonBlue) { onAction("copy") }
            FileOptionItem(Icons.Default.Info, "DETAILS", NeonBlue) { onAction("details") }
            Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
            FileOptionItem(Icons.Default.Delete, "DELETE", Color(0xFFCC0000)) { onAction("delete") }
        }
    }
}

@Composable
private fun FileOptionItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(text = label, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        }
    }
}
