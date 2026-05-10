package com.semseytech.rtsdevicesuitepro.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.semseytech.rtsdevicesuitepro.R
import com.semseytech.rtsdevicesuitepro.navigation.Screen
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme

import kotlinx.coroutines.launch

@Composable
fun DashboardHeader(onNavigate: (String) -> Unit) {
    val theme = LocalTheme.current
    val context = LocalContext.current
    
    // Theme-aware banner loading logic
    val bannerPath = remember(theme.name) {
        val extensions = listOf("png", "jpeg", "jpg", "webp")
        var path: String? = null
        for (ext in extensions) {
            val assetPath = "banner/${theme.name}.$ext"
            try {
                context.assets.open(assetPath).use { it.close() }
                path = "file:///android_asset/$assetPath"
                break
            } catch (e: Exception) {}
        }
        path
    }

    // Custom icon positioning based on theme
    val iconAlignment = when (theme.name) {
        "Neon Dark Cyber" -> Alignment.Center
        "Deep Blue Systems", "Terminal Green", "Solarized Tech" -> Alignment.BottomCenter
        else -> Alignment.BottomStart
    }

    val iconArrangement = when (theme.name) {
        "Neon Dark Cyber" -> Arrangement.End
        "Deep Blue Systems" -> Arrangement.Center
        "Terminal Green", "Solarized Tech" -> Arrangement.SpaceBetween
        else -> Arrangement.Start
    }

    val iconPadding = when (theme.name) {
        "Deep Blue Systems" -> PaddingValues(bottom = 0.dp) // Absolute bottom of the banner
        "Terminal Green" -> PaddingValues(bottom = 8.dp, start = 16.dp, end = 16.dp)
        "Solarized Tech" -> PaddingValues(bottom = 8.dp, start = 24.dp, end = 24.dp) // Moved down and increased horizontal distance
        else -> PaddingValues(8.dp)
    }

    Surface(
        color = theme.startColor.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(140.dp)
        ) {
            // The Banner Image from assets (theme-specific) or fallback drawable
            AsyncImage(
                model = bannerPath ?: R.drawable.banner,
                contentDescription = "RTS Banner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Persistent Interactive Icons Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(iconAlignment)
                    .padding(iconPadding),
                horizontalArrangement = iconArrangement,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpinningHelpIcon(onClick = { onNavigate(Screen.HelpAndPermissions.route) })
                if (iconArrangement != Arrangement.SpaceBetween) {
                    Spacer(modifier = Modifier.width(12.dp))
                }
                RotatingCogIcon(onClick = { onNavigate(Screen.Tools.route) })
            }
        }
    }
}

@Composable
fun RotatingCogIcon(onClick: () -> Unit) {
    val theme = LocalTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "cogRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Tools",
            tint = theme.accentColor,
            modifier = Modifier
                .size(24.dp)
                .rotate(rotation)
        )
    }
}

@Composable
fun SpinningHelpIcon(onClick: () -> Unit) {
    val theme = LocalTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(isPressed, isHovered) {
        if (isPressed || isHovered) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
    }

    IconButton(
        onClick = {
            scope.launch {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                )
                onClick()
            }
        },
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Help,
            contentDescription = "Help",
            tint = theme.accentColor,
            modifier = Modifier
                .size(24.dp)
                .rotate(rotation.value)
        )
    }
}
