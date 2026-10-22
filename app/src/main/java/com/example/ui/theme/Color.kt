package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// --- STATIC COLORS ---

// App Brand Accent - Mint Green
val MedtrackMint = Color(0xFF10B981)
val MedicoMint = MedtrackMint

// Default Palette Bases
val DefaultMedAccent = Color(0xFF1D5FD6)
val DefaultMedAccentTint = Color(0xFFEFF6FF)

// Light Mode Static
val MedLightBg = Color(0xFFF8FAFC)
val MedLightSurface = Color(0xFFFFFFFF)
val MedLightTextPrimary = Color(0xFF0F172A)
val MedLightTextSecondary = Color(0xFF475569)
val MedLightLines = Color(0xFFE2E8F0)

// Status Colors - Light
val MedLightStatusOverdueText = Color(0xFFDC2626)
val MedLightStatusOverdueBg = Color(0xFFFEF2F2)
val MedLightStatusRefillText = Color(0xFFD97706)
val MedLightStatusRefillBg = Color(0xFFFFFBEB)
val MedLightStatusOkText = Color(0xFF059669)
val MedLightStatusOkBg = Color(0xFFECFDF5)

// --- DYNAMIC ACCESSORS ---

val MedTextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val MedTextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val MedBorder: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
val MedBorderTeal: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
val MedTealBg: Color @Composable get() = MaterialTheme.colorScheme.background
val MedSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
val MedCardBg: Color @Composable get() = MaterialTheme.colorScheme.surface
val MedTealPrimary: Color @Composable get() = MaterialTheme.colorScheme.primary
val MedTealSoft: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
val MedTealDark: Color @Composable get() = MaterialTheme.colorScheme.onPrimaryContainer
val MedDanger: Color @Composable get() = MaterialTheme.colorScheme.error

val MedSuccess = Color(0xFF059669)
val MedWarning = Color(0xFFD97706)
val MedBgGray = Color(0xFFF1F5F9)

val MedLightAccent = Color(0xFF1D5FD6)
val MedLightAccentTint = Color(0xFFEFF6FF)
val MedSuccessGreen = Color(0xFF059669)
val MedSuccessBg = Color(0xFFECFDF5)

// --- NEUTRAL LOW-STOCK TOKENS (Light & Dark Mode) ---

val MedLowStockSurfaceLight = Color(0xFFF5F5F4)
val MedLowStockBorderLight = Color(0xFFE7E5E4)
val MedLowStockTextLight = Color(0xFF292524)
val MedLowStockSecondaryTextLight = Color(0xFF78716C)
val MedLowStockIconLight = Color(0xFF57534E)
val MedLowStockIconBgLight = Color(0xFFE7E5E4)
val MedLowStockBadgeBgLight = Color(0xFFE7E5E4)
val MedLowStockBadgeTextLight = Color(0xFF44403C)
val MedLowStockAccentLight = Color(0xFF78716C)
val MedLowStockTrackLight = Color(0xFFE7E5E4)
val MedLowStockItemSurfaceLight = Color(0xFFFFFFFF)

val MedLowStockSurfaceDark = Color(0xFF1C1917)
val MedLowStockBorderDark = Color(0xFF292524)
val MedLowStockTextDark = Color(0xFFF5F5F4)
val MedLowStockSecondaryTextDark = Color(0xFFA8A29E)
val MedLowStockIconDark = Color(0xFFD6D3D1)
val MedLowStockIconBgDark = Color(0xFF292524)
val MedLowStockBadgeBgDark = Color(0xFF292524)
val MedLowStockBadgeTextDark = Color(0xFFE7E5E4)
val MedLowStockAccentDark = Color(0xFFA8A29E)
val MedLowStockTrackDark = Color(0xFF292524)
val MedLowStockItemSurfaceDark = Color(0xFF292524)

// Shared Theme Token Accessors
val lowStockSurface: Color @Composable get() = MedicoTheme.colors.lowStockSurface
val lowStockBorder: Color @Composable get() = MedicoTheme.colors.lowStockBorder
val lowStockText: Color @Composable get() = MedicoTheme.colors.lowStockText
val lowStockSecondaryText: Color @Composable get() = MedicoTheme.colors.lowStockSecondaryText
val lowStockIcon: Color @Composable get() = MedicoTheme.colors.lowStockIcon
val lowStockIconBg: Color @Composable get() = MedicoTheme.colors.lowStockIconBg
val lowStockBadgeBg: Color @Composable get() = MedicoTheme.colors.lowStockBadgeBg
val lowStockBadgeText: Color @Composable get() = MedicoTheme.colors.lowStockBadgeText
val lowStockAccent: Color @Composable get() = MedicoTheme.colors.lowStockAccent
val lowStockTrack: Color @Composable get() = MedicoTheme.colors.lowStockTrack
val lowStockItemSurface: Color @Composable get() = MedicoTheme.colors.lowStockItemSurface

