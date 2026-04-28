package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

@Composable
fun RenameDialog(file: FileInfo, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var newName by remember { mutableStateOf(file.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepDark,
        title = { Text("RENAME FILE", color = NeonBlue, fontFamily = FontFamily.Monospace) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("NEW NAME", color = Color.Gray, fontFamily = FontFamily.Monospace) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = Color.Gray
                ),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(newName) }) {
                Text("RENAME", color = NeonGreen, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace)
            }
        }
    )
}
