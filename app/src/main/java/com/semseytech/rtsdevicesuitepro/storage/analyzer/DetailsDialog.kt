package com.semseytech.rtsdevicesuitepro.storage.analyzer

import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DetailsDialog(file: FileInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepDark,
        title = { Text("FILE DETAILS", color = NeonBlue, fontFamily = FontFamily.Monospace) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailItem("NAME", file.name)
                DetailItem("PATH", file.path)
                DetailItem("SIZE", Formatter.formatFileSize(context, file.size))
                DetailItem("MODIFIED", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified)))
                DetailItem("TYPE", file.name.substringAfterLast('.', "").uppercase())
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = NeonBlue, fontFamily = FontFamily.Monospace)
            }
        }
    )
}
