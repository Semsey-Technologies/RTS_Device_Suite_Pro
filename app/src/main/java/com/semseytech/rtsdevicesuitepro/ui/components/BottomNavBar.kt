package com.semseytech.rtsdevicesuitepro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
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
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme

@Composable
fun BottomNavBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    val currentTheme = LocalTheme.current
    Surface(
        color = currentTheme.endColor.copy(alpha = 0.98f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomButton(Icons.Default.Home, "Home", currentRoute == Screen.Dashboard.route, currentTheme.accentColor) { onNavigate(Screen.Dashboard.route) }
            BottomButton(Icons.Default.History, "Recovery", currentRoute == Screen.Recovery.route, currentTheme.accentColor) { onNavigate(Screen.Recovery.route) }
            BottomButton(Icons.Default.CleaningServices, "Cleaner", currentRoute == Screen.Cleaner.route, currentTheme.accentColor) { onNavigate(Screen.Cleaner.route) }
            BottomButton(Icons.Default.Build, "Tools", currentRoute == Screen.Tools.route, currentTheme.accentColor) { onNavigate(Screen.Tools.route) }
        }
    }
}

@Composable
fun BottomButton(icon: ImageVector, label: String, isActive: Boolean, accentColor: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 28.dp)
                .background(if (isActive) accentColor.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) accentColor else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label, 
            color = if (isActive) accentColor else Color.White,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}
