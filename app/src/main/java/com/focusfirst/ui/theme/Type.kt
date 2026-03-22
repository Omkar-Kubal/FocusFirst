package com.focusfirst.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── V2 font swap — change this one line to apply a custom typeface everywhere ─
// Example:  private val AppFontFamily = FontFamily(Font(R.font.inter_variable))
private val AppFontFamily = FontFamily.Default

/**
 * Material 3 typography scale for FocusFirst.
 *
 * Every TextStyle references [AppFontFamily], so swapping to a custom font
 * in V2 requires editing exactly one line above — all scale slots update
 * automatically.  Add or override individual slots below as the design evolves
 * (e.g. a bold condensed style for the large timer display face).
 */
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 57.sp,
        lineHeight    = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 45.sp,
        lineHeight    = 52.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 36.sp,
        lineHeight    = 44.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 32.sp,
        lineHeight    = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 28.sp,
        lineHeight    = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 24.sp,
        lineHeight    = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 22.sp,
        lineHeight    = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 11.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)
