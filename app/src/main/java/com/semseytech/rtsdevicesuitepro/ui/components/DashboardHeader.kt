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

    Surface(
        color = theme.startColor.copy(alpha = 0.95f),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(), // Move down so it's not interrupted by camera cutout
        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            // The Banner Image from assets (theme-specific) or fallback drawable
            AsyncImage(
                model = bannerPath ?: R.drawable.banner,
                contentDescription = "RTS Banner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            
            // Persistent Interactive Icons Overlay
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                RotatingCogIcon(onClick = { onNavigate(Screen.Tools.route) })
                Spacer(modifier = Modifier.width(16.dp))
                SpinningHelpIcon(onClick = { onNavigate(Screen.HelpAndPermissions.route) })
            }
        }
    }
}

@Composable
fun RotatingCogIcon(onClick: () -> Unit) {
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
            tint = Color.White,
            modifier = Modifier
                .size(24.dp)
                .rotate(rotation)
        )
    }
}

@Composable
fun SpinningHelpIcon(onClick: () -> Unit) {
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
            tint = Color.White,
            modifier = Modifier
                .size(24.dp)
                .rotate(rotation.value)
        )
    }
}
