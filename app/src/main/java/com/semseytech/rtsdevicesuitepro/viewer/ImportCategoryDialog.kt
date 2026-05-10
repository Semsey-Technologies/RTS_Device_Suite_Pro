package com.semseytech.rtsdevicesuitepro.viewer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemePreset

@Composable
fun ImportCategoryDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
    theme: ThemePreset
) {
    var selectedCategories by remember { mutableStateOf(categories.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Categories to Replace", color = theme.accentColor) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Existing data in these categories will be replaced with data from the backup.", 
                    color = theme.textColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(categories) { category ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedCategories.contains(category),
                                onCheckedChange = { checked ->
                                    selectedCategories = if (checked) {
                                        selectedCategories + category
                                    } else {
                                        selectedCategories - category
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = theme.accentColor)
                            )
                            Text(category, color = theme.textColor, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedCategories) },
                enabled = selectedCategories.isNotEmpty()
            ) {
                Text("REPLACE SELECTED", color = theme.accentColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = theme.textColor.copy(alpha = 0.7f))
            }
        },
        containerColor = theme.startColor
    )
}
