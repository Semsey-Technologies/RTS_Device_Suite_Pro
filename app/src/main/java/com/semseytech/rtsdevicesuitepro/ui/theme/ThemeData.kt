package com.semseytech.rtsdevicesuitepro.ui.theme

import androidx.compose.ui.graphics.Color

data class ThemePreset(
    val name: String,
    val startColor: Color,
    val endColor: Color,
    val accentColor: Color,
    val textColor: Color = Color.White,
    val subtitleColor: Color = Color.White.copy(alpha = 0.7f),
    val hasGlassTexture: Boolean = false,
    val hasGridOverlay: Boolean = false,
    val isAnimated: Boolean = false,
    val animatedColors: List<Color> = emptyList()
)

data class ThemeCategory(
    val title: String,
    val subtitle: String,
    val presets: List<ThemePreset>
)

val ThemePresets = listOf(
    ThemeCategory(
        title = "Cyber / Tech Core",
        subtitle = "High-performance digital aesthetics",
        presets = listOf(
            ThemePreset(
                name = "Neon Dark Cyber",
                startColor = Color(0xFF050712),
                endColor = Color(0xFF091C3E),
                accentColor = Color(0xFF00C8FF)
            ),
            ThemePreset(
                name = "Deep Blue Systems",
                startColor = Color(0xFF0A1B3A),
                endColor = Color(0xFF112B5A),
                accentColor = Color(0xFF00E0FF),
                hasGlassTexture = true
            ),
            ThemePreset(
                name = "Terminal Green",
                startColor = Color(0xFF000000),
                endColor = Color(0xFF0A0A0A),
                accentColor = Color(0xFF00FF66),
                textColor = Color(0xFF00FF66),
                hasGridOverlay = true
            ),
            ThemePreset(
                name = "Solarized Tech",
                startColor = Color(0xFF002B36),
                endColor = Color(0xFF073642),
                accentColor = Color(0xFFB58900)
            )
        )
    ),
    ThemeCategory(
        title = "Minimal / Professional",
        subtitle = "Clean and focused interfaces",
        presets = listOf(
            ThemePreset(
                name = "Midnight Minimal",
                startColor = Color(0xFF0E0E0E),
                endColor = Color(0xFF1A1A1A),
                accentColor = Color(0xFFC0C0C0),
                textColor = Color(0xFFC0C0C0)
            ),
            ThemePreset(
                name = "Light Minimal",
                startColor = Color(0xFFF4F6F8),
                endColor = Color(0xFFF4F6F8),
                accentColor = Color(0xFF2A7FFF),
                textColor = Color(0xFF1A1A1A),
                subtitleColor = Color(0xFF4A4F57)
            ),
            ThemePreset(
                name = "Graphite Edge",
                startColor = Color(0xFF1C1C1C),
                endColor = Color(0xFF2A2A2A),
                accentColor = Color(0xFF4A90E2)
            ),
            ThemePreset(
                name = "Silver-Blue Premium",
                startColor = Color(0xFF101820),
                endColor = Color(0xFF1E2A38),
                accentColor = Color(0xFFA8CFFF)
            )
        )
    ),
    ThemeCategory(
        title = "Creative / Experimental",
        subtitle = "Bold and dynamic visual styles",
        presets = listOf(
            ThemePreset(
                name = "Aurora Pulse",
                startColor = Color(0xFF0A0F2C),
                endColor = Color(0xFF1A2E5A),
                accentColor = Color(0xFFA020F0),
                isAnimated = true,
                animatedColors = listOf(Color(0xFF0A0F2C), Color(0xFF1A2E5A), Color(0xFFA020F0))
            ),
            ThemePreset(
                name = "Neon Grid",
                startColor = Color(0xFF050712),
                endColor = Color(0xFF091C3E),
                accentColor = Color(0xFFFF00FF),
                hasGridOverlay = true
            ),
            ThemePreset(
                name = "Crimson Core",
                startColor = Color(0xFF120000),
                endColor = Color(0xFF2A0000),
                accentColor = Color(0xFFFF0033)
            ),
            ThemePreset(
                name = "Obsidian Inferno",
                startColor = Color(0xFF000000),
                endColor = Color(0xFF1A0000),
                accentColor = Color(0xFFFF1A1A),
                textColor = Color(0xFFD0D0D0),
                subtitleColor = Color(0xFFB85A5A)
            ),
            ThemePreset(
                name = "Void Spectrum",
                startColor = Color(0xFF000000),
                endColor = Color(0xFF050712),
                accentColor = Color(0xFFFF00FF)
            )
        )
    ),
    ThemeCategory(
        title = "System / Diagnostic",
        subtitle = "Utility-first monitoring layouts",
        presets = listOf(
            ThemePreset(
                name = "Analyzer Mode",
                startColor = Color(0xFF0E1628),
                endColor = Color(0xFF1C2A3E),
                accentColor = Color(0xFF00FFFF)
            ),
            ThemePreset(
                name = "Recovery Mode",
                startColor = Color(0xFF1A0A0A),
                endColor = Color(0xFF3A1C1C),
                accentColor = Color(0xFFFF6600)
            ),
            ThemePreset(
                name = "Maintenance Mode",
                startColor = Color(0xFF1E1E1E),
                endColor = Color(0xFF2E2E2E),
                accentColor = Color(0xFFFFD700)
            )
        )
    )
)
