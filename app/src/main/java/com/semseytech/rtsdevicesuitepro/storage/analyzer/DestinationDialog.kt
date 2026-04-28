package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DestinationDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val folders = listOf("/storage/emulated/0/Download", "/storage/emulated/0/Documents", "/storage/emulated/0/Pictures")
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepDark,
        title = { Text(title, color = NeonBlue, fontFamily = FontFamily.Monospace) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(folders) { folder ->
                    Surface(
                        onClick = { onConfirm(folder) },
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = folder,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace)
            }
        }
    )
}
