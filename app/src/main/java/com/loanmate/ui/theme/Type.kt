package com.loanmate.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.loanmate.R

/**
 * Variable-font families. The same .ttf is loaded at multiple weights via
 * FontVariation (API 26+, our minSdk). Plus Jakarta Sans gives headings a
 * distinctive, premium geometric character; Inter keeps body text crisp.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun jakarta(weight: Int) = Font(
    R.font.plus_jakarta_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

val DisplayFont = FontFamily(
    jakarta(500), jakarta(600), jakarta(700), jakarta(800)
)

val BodyFont = FontFamily(
    inter(400), inter(500), inter(600), inter(700)
)

val AppTypography = Typography(
    // Display — big hero numbers (countdown, balances)
    displayLarge = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight(800),
        fontSize = 52.sp, lineHeight = 58.sp, letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight(700),
        fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight(700),
        fontSize = 32.sp, lineHeight = 38.sp
    ),
    // Headlines — screen titles, greeting
    headlineLarge = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight(700),
        fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight(700),
        fontSize = 26.sp, lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight(700),
        fontSize = 22.sp, lineHeight = 28.sp
    ),
    // Titles — card headers, section labels
    titleLarge = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight(700),
        fontSize = 20.sp, lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight(600),
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight(600),
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    // Body — descriptions, list rows
    bodyLarge = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight(400),
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight(400),
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight(400),
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp
    ),
    // Labels — buttons, chips, captions
    labelLarge = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight(600),
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight(600),
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight(500),
        fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.4.sp
    )
)
