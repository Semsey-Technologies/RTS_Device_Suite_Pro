package com.semseytech.rtsdevicesuitepro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.navigation.Screen

val NavBarBackground = Color(0xFF020817)
val NeonBlue = Color(0xFF00BFFF)

@Composable
fun BottomNavBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    Surface(
        color = NavBarBackground.copy(alpha = 0.95f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomButton(Icons.Default.Home, "Home", currentRoute == Screen.Dashboard.route) { onNavigate(Screen.Dashboard.route) }
            BottomButton(Icons.Default.Backup, "Backup", currentRoute == Screen.Backup.route) { onNavigate(Screen.Backup.route) }
            BottomButton(Icons.Default.SettingsBackupRestore, "Restore", currentRoute == Screen.Restore.route) { onNavigate(Screen.Restore.route) }
            BottomButton(Icons.Default.History, "Recover", currentRoute == Screen.Recovery.route) { onNavigate(Screen.Recovery.route) }
        }
    }
}

@Composable
fun BottomButton(icon: ImageVector, label: String, isActive: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) NeonBlue else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label, 
            color = if (isActive) NeonBlue else Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}
