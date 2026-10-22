package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

enum class MedPalette(
    val id: String,
    val displayName: String,
    val primary: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val accentMint: Color
) {
    ROYAL(
        id = "ROYAL",
        displayName = "Royal Blue",
        primary = Color(0xFF1D5FD6),
        primaryContainer = Color(0xFFEFF6FF),
        onPrimary = Color.White,
        accentMint = Color(0xFF10B981)
    ),
    TEAL(
        id = "TEAL",
        displayName = "Clinical Teal",
        primary = Color(0xFF0D9488),
        primaryContainer = Color(0xFFF0FDFA),
        onPrimary = Color.White,
        accentMint = Color(0xFF14B8A6)
    ),
    NAVY(
        id = "NAVY",
        displayName = "Midnight Navy",
        primary = Color(0xFF0F172A),
        primaryContainer = Color(0xFFF1F5F9),
        onPrimary = Color.White,
        accentMint = Color(0xFF38BDF8)
    ),
    MINT(
        id = "MINT",
        displayName = "Botanical Mint",
        primary = Color(0xFF059669),
        primaryContainer = Color(0xFFECFDF5),
        onPrimary = Color.White,
        accentMint = Color(0xFF10B981)
    ),
    SKY(
        id = "SKY",
        displayName = "Sky Blue",
        primary = Color(0xFF0284C7),
        primaryContainer = Color(0xFFF0F9FF),
        onPrimary = Color.White,
        accentMint = Color(0xFF38BDF8)
    ),
    CORAL(
        id = "CORAL",
        displayName = "Sunset Rose",
        primary = Color(0xFFE11D48),
        primaryContainer = Color(0xFFFFF1F2),
        onPrimary = Color.White,
        accentMint = Color(0xFFF43F5E)
    );

    companion object {
        fun fromId(id: String): MedPalette {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ROYAL
        }
    }
}

data class MedicoCustomColors(
    val brandMint: Color = Color(0xFF10B981),
    val warningText: Color = MedLowStockTextLight,
    val warningBg: Color = MedLowStockSurfaceLight,
    val alertText: Color = Color(0xFFDC2626),
    val alertBg: Color = Color(0xFFFEF2F2),
    val successText: Color = Color(0xFF059669),
    val successBg: Color = Color(0xFFECFDF5),
    val accentTint: Color = Color(0xFFEFF6FF),
    val lines: Color = Color(0xFFE2E8F0),
    val lowStockSurface: Color = MedLowStockSurfaceLight,
    val lowStockBorder: Color = MedLowStockBorderLight,
    val lowStockText: Color = MedLowStockTextLight,
    val lowStockSecondaryText: Color = MedLowStockSecondaryTextLight,
    val lowStockIcon: Color = MedLowStockIconLight,
    val lowStockIconBg: Color = MedLowStockIconBgLight,
    val lowStockBadgeBg: Color = MedLowStockBadgeBgLight,
    val lowStockBadgeText: Color = MedLowStockBadgeTextLight,
    val lowStockAccent: Color = MedLowStockAccentLight,
    val lowStockTrack: Color = MedLowStockTrackLight,
    val lowStockItemSurface: Color = MedLowStockItemSurfaceLight
)

val LocalMedicoCustomColors = staticCompositionLocalOf { MedicoCustomColors() }

@Composable
fun MedTrackTheme(
    paletteName: String = "ROYAL",
    appearance: String = "LIGHT",
    textSizeScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val palette = MedPalette.fromId(paletteName)
    val isDark = when (appearance.uppercase()) {
        "DARK" -> true
        "SYSTEM" -> androidx.compose.foundation.isSystemInDarkTheme()
        else -> false
    }

    val colorScheme = lightColorScheme(
        primary = palette.primary,
        onPrimary = palette.onPrimary,
        primaryContainer = palette.primaryContainer,
        onPrimaryContainer = palette.primary,
        secondary = MedLightTextSecondary,
        onSecondary = Color.White,
        secondaryContainer = MedLightSurface,
        onSecondaryContainer = MedLightTextPrimary,
        background = MedLightBg,
        onBackground = MedLightTextPrimary,
        surface = MedLightSurface,
        onSurface = MedLightTextPrimary,
        surfaceVariant = MedLightSurface,
        onSurfaceVariant = MedLightTextSecondary,
        outline = MedLightLines,
        outlineVariant = MedLightLines,
        error = MedLightStatusOverdueText,
        errorContainer = MedLightStatusOverdueBg,
        onError = Color.White,
        onErrorContainer = MedLightStatusOverdueText
    )

    val customColors = MedicoCustomColors(
        brandMint = palette.accentMint,
        warningText = if (isDark) MedLowStockTextDark else MedLowStockTextLight,
        warningBg = if (isDark) MedLowStockSurfaceDark else MedLowStockSurfaceLight,
        alertText = MedLightStatusOverdueText,
        alertBg = MedLightStatusOverdueBg,
        successText = MedLightStatusOkText,
        successBg = MedLightStatusOkBg,
        accentTint = palette.primaryContainer,
        lines = MedLightLines,
        lowStockSurface = if (isDark) MedLowStockSurfaceDark else MedLowStockSurfaceLight,
        lowStockBorder = if (isDark) MedLowStockBorderDark else MedLowStockBorderLight,
        lowStockText = if (isDark) MedLowStockTextDark else MedLowStockTextLight,
        lowStockSecondaryText = if (isDark) MedLowStockSecondaryTextDark else MedLowStockSecondaryTextLight,
        lowStockIcon = if (isDark) MedLowStockIconDark else MedLowStockIconLight,
        lowStockIconBg = if (isDark) MedLowStockIconBgDark else MedLowStockIconBgLight,
        lowStockBadgeBg = if (isDark) MedLowStockBadgeBgDark else MedLowStockBadgeBgLight,
        lowStockBadgeText = if (isDark) MedLowStockBadgeTextDark else MedLowStockBadgeTextLight,
        lowStockAccent = if (isDark) MedLowStockAccentDark else MedLowStockAccentLight,
        lowStockTrack = if (isDark) MedLowStockTrackDark else MedLowStockTrackLight,
        lowStockItemSurface = if (isDark) MedLowStockItemSurfaceDark else MedLowStockItemSurfaceLight
    )

    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * (textSizeScale * 0.88f)
    )

    CompositionLocalProvider(
        LocalMedicoCustomColors provides customColors,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object MedicoTheme {
    val colors: MedicoCustomColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMedicoCustomColors.current
}

typealias MedtrackTheme = MedicoTheme
