package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun DeleteSelectedFAB(onDelete: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onDelete,
        containerColor = Color(0xFFCC0000),
        contentColor = Color.White,
        icon = { Icon(Icons.Default.Delete, null) },
        text = { Text("DELETE SELECTED", fontFamily = FontFamily.Monospace) },
        shape = RoundedCornerShape(4.dp)
    )
}
