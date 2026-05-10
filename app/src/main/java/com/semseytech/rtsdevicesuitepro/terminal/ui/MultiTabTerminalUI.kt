package com.semseytech.rtsdevicesuitepro.terminal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MultiTabTerminalUI(viewModel: TerminalViewModel) {
    val sessions by viewModel.sessions.collectAsState()
    val activeIndex by viewModel.activeSessionIndex.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Tab Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121212))
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(modifier = Modifier.weight(1f)) {
                itemsIndexed(sessions) { index, session ->
                    TerminalTab(
                        title = "Session ${session.id}",
                        isActive = index == activeIndex,
                        onClick = { viewModel.setActiveSession(index) },
                        onClose = { viewModel.removeSession(index) }
                    )
                }
            }
            
            IconButton(onClick = { viewModel.addNewSession() }) {
                Icon(Icons.Default.Add, contentDescription = "New Tab", tint = Color.Green)
            }
        }

        // Active Session UI
        sessions.getOrNull(activeIndex)?.let { activeSession ->
            TerminalUI(activeSession)
        }
    }
}

@Composable
fun TerminalTab(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .background(if (isActive) Color(0xFF1E1E1E) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = if (isActive) Color.White else Color.Gray,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
        if (isActive) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.Gray,
                modifier = Modifier.size(14.dp).clickable { onClose() }
            )
        }
    }
}
