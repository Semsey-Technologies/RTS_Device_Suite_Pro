package com.semseytech.rtsdevicesuitepro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun RTSDeviceSuiteProTheme(
    content: @Composable () -> Unit
) {
    val theme = ThemeManager.currentTheme
    
    val fontWeight = if (ThemeManager.isBold) FontWeight.Bold else FontWeight.Normal
    val fontStyle = if (ThemeManager.isItalic) FontStyle.Italic else FontStyle.Normal
    val fontFamily = ThemeManager.selectedFont.family

    val typography = Typography(
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSize = (22 * ThemeManager.titleSizeScale).sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSize = (18 * ThemeManager.titleSizeScale).sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSize = (14 * ThemeManager.titleSizeScale).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSize = (16 * ThemeManager.bodySizeScale).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSize = (14 * ThemeManager.bodySizeScale).sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSize = (14 * ThemeManager.subtitleSizeScale).sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSize = (11 * ThemeManager.subtitleSizeScale).sp
        )
    )
    
    val colorScheme = darkColorScheme(
        primary = theme.accentColor,
        secondary = theme.accentColor.copy(alpha = 0.7f),
        tertiary = theme.accentColor.copy(alpha = 0.5f),
        background = theme.startColor,
        surface = theme.endColor,
        onPrimary = theme.textColor,
        onSecondary = theme.textColor,
        onTertiary = theme.textColor,
        onBackground = theme.textColor,
        onSurface = theme.textColor
    )

    CompositionLocalProvider(LocalTheme provides theme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
