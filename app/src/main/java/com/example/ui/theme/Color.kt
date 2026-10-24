package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// YASWAVE Restrained Dark Charcoal Palette
val YasBackground = Color(0xFF101012)        // Near-black background
val YasSurface = Color(0xFF18181B)           // Charcoal surface
val YasSurfaceVariant = Color(0xFF222226)    // Elevated charcoal container
val YasSurfaceElevated = Color(0xFF292930)   // Dialogs, sheets, modals
val YasBorder = Color(0xFF2D2D33)            // Subtle hairline separator
val YasBorderSubtle = Color(0xFF202024)

// Neutral Grayscale Typography
val YasTextPrimary = Color(0xFFF4F4F6)       // Clean neutral off-white
val YasTextSecondary = Color(0xFFA1A1AA)     // Zinc gray for artist/subtitle
val YasTextMuted = Color(0xFF71717A)         // Muted gray for timestamps/captions

// Single Restrained Accent Colors (User Selectable in Settings)
val YasAccentSage = Color(0xFF34D399)        // Restrained Nordic emerald/sage (Default)
val YasAccentTitanium = Color(0xFFE4E4E7)    // Minimalist pure grayscale/platinum
val YasAccentIce = Color(0xFF38BDF8)         // Muted studio slate/ice
val YasAccentAmber = Color(0xFFF59E0B)       // Warm acoustic amber

// Backwards-compatibility aliases
val YasWaveDarkBackground = YasBackground
val YasWaveDarkSurface = YasSurface
val YasWaveDarkSurfaceVariant = YasSurfaceVariant
val YasWaveTextPrimary = YasTextPrimary
val YasWaveTextSecondary = YasTextSecondary
val YasWaveTextMuted = YasTextMuted
val YasWaveCyan = YasAccentSage
val YasWaveViolet = YasAccentSage
val YasWavePink = YasAccentAmber
