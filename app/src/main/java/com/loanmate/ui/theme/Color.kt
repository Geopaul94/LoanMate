package com.loanmate.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * LoanMate brand palette.
 * Dominant: deep emerald-teal (growth, money, calm).
 * Accent: warm gold (milestones, optimism).
 * Neutrals are warm-tinted — never flat white / flat grey.
 * Contrast verified for WCAG AA in both light and dark.
 */

// ---- Light ----
val EmeraldLight = Color(0xFF0E6B57)
val OnEmeraldLight = Color(0xFFFFFFFF)
val EmeraldContainerLight = Color(0xFFA7F0DD)
val OnEmeraldContainerLight = Color(0xFF00201A)

val GoldLight = Color(0xFF7C5800)
val OnGoldLight = Color(0xFFFFFFFF)
val GoldContainerLight = Color(0xFFFFDF9E)
val OnGoldContainerLight = Color(0xFF271900)

val TerracottaLight = Color(0xFF8C4A2F)
val OnTerracottaLight = Color(0xFFFFFFFF)
val TerracottaContainerLight = Color(0xFFFFDBCC)
val OnTerracottaContainerLight = Color(0xFF351000)

val BackgroundLight = Color(0xFFF8F7F3)
val OnBackgroundLight = Color(0xFF1A1C19)
val SurfaceLight = Color(0xFFFCFBF7)
val OnSurfaceLight = Color(0xFF1A1C19)
val SurfaceVariantLight = Color(0xFFDBE5DE)
val OnSurfaceVariantLight = Color(0xFF404943)
val OutlineLight = Color(0xFF707973)
val OutlineVariantLight = Color(0xFFBFC9C1)

// Warm-tinted surface container roles (override M3's purple-ish baseline)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF2F1EB)
val SurfaceContainerLight = Color(0xFFECEBE4)
val SurfaceContainerHighLight = Color(0xFFE6E5DE)
val SurfaceContainerHighestLight = Color(0xFFE1E0D9)
val SurfaceDimLight = Color(0xFFD9D9D0)
val SurfaceBrightLight = Color(0xFFF8F7F3)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

// ---- Dark ----
val EmeraldDark = Color(0xFF5AD9BC)
val OnEmeraldDark = Color(0xFF00382D)
val EmeraldContainerDark = Color(0xFF005142)
val OnEmeraldContainerDark = Color(0xFF76F6D9)

val GoldDark = Color(0xFFF2C24B)
val OnGoldDark = Color(0xFF412D00)
val GoldContainerDark = Color(0xFF5E4200)
val OnGoldContainerDark = Color(0xFFFFDF9E)

val TerracottaDark = Color(0xFFFFB599)
val OnTerracottaDark = Color(0xFF542100)
val TerracottaContainerDark = Color(0xFF703314)
val OnTerracottaContainerDark = Color(0xFFFFDBCC)

val BackgroundDark = Color(0xFF101512)
val OnBackgroundDark = Color(0xFFE2E3DD)
val SurfaceDark = Color(0xFF191C19)
val OnSurfaceDark = Color(0xFFE2E3DD)
val SurfaceVariantDark = Color(0xFF404943)
val OnSurfaceVariantDark = Color(0xFFBFC9C1)
val OutlineDark = Color(0xFF8A938C)
val OutlineVariantDark = Color(0xFF404943)

val SurfaceContainerLowestDark = Color(0xFF0B0F0C)
val SurfaceContainerLowDark = Color(0xFF191C19)
val SurfaceContainerDark = Color(0xFF1D211D)
val SurfaceContainerHighDark = Color(0xFF272B27)
val SurfaceContainerHighestDark = Color(0xFF323632)
val SurfaceDimDark = Color(0xFF101512)
val SurfaceBrightDark = Color(0xFF363A35)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// ---- Shared semantic accents (used by cards, charts, status) ----
// These stay distinct from brand colors so status never reads as "brand".
val SuccessGreen = Color(0xFF2E7D32)
val WarningAmber = Color(0xFFE8890C)
val DangerRed = Color(0xFFD32F2F)

// Debt-free card gradient — warm gold, tied to the accent family
val DebtFreeGradientStart = Color(0xFFF2A93B)
val DebtFreeGradientEnd = Color(0xFFF6C14E)
val DebtFreeGradientCalm = Color(0xFF14A085)   // used when < 1 year left
