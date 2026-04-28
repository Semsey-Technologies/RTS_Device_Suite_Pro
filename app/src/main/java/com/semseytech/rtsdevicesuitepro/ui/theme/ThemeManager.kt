package com.semseytech.rtsdevicesuitepro.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

enum class AppFont(val family: FontFamily, val displayName: String) {
    Default(FontFamily.Default, "System Default"),
    Monospace(FontFamily.Monospace, "Monospace"),
    Serif(FontFamily.Serif, "Serif"),
    SansSerif(FontFamily.SansSerif, "Sans Serif")
}

object ThemeManager {
    var currentTheme by mutableStateOf(ThemePresets[0].presets[0])
        private set

    // UI Scaling
    var uiScale by mutableFloatStateOf(1.2f) // Defaulting to 1.2 for "larger" as requested
    
    // Typography Settings
    var titleSizeScale by mutableFloatStateOf(1.2f)
    var subtitleSizeScale by mutableFloatStateOf(1.2f)
    var bodySizeScale by mutableFloatStateOf(1.2f)
    
    var isBold by mutableStateOf(false)
    var isItalic by mutableStateOf(false)
    var selectedFont by mutableStateOf(AppFont.Default)

    fun applyTheme(theme: ThemePreset) {
        currentTheme = theme
    }
    
    fun updateUiScale(scale: Float) { uiScale = scale }
    fun updateTitleScale(scale: Float) { titleSizeScale = scale }
    fun updateSubtitleScale(scale: Float) { subtitleSizeScale = scale }
    fun updateBodyScale(scale: Float) { bodySizeScale = scale }
}

val LocalTheme = staticCompositionLocalOf { ThemePresets[0].presets[0] }

