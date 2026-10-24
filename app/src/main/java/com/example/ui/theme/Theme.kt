package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AccentTheme(val displayName: String, val color: Color) {
    SAGE("Nordic Sage", YasAccentSage),
    TITANIUM("Titanium Grayscale", YasAccentTitanium),
    ICE("Ice Slate", YasAccentIce),
    AMBER("Warm Amber", YasAccentAmber)
}

@Composable
fun MyApplicationTheme(
    accentTheme: AccentTheme = AccentTheme.SAGE,
    content: @Composable () -> Unit
) {
    val activeAccent = accentTheme.color

    val colorScheme = darkColorScheme(
        primary = activeAccent,
        onPrimary = Color(0xFF101012),
        primaryContainer = YasSurfaceVariant,
        onPrimaryContainer = YasTextPrimary,
        secondary = YasTextSecondary,
        onSecondary = Color(0xFF101012),
        secondaryContainer = YasSurfaceElevated,
        onSecondaryContainer = YasTextPrimary,
        tertiary = activeAccent,
        onTertiary = Color(0xFF101012),
        background = YasBackground,
        onBackground = YasTextPrimary,
        surface = YasSurface,
        onSurface = YasTextPrimary,
        surfaceVariant = YasSurfaceVariant,
        onSurfaceVariant = YasTextSecondary,
        outline = YasBorder
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
