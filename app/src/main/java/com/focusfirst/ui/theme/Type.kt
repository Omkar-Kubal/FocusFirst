package com.focusfirst.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.focusfirst.R

// ── Geist font family ─────────────────────────────────────────────────────────
// To swap fonts in the future: replace R.font.geist_* references below and
// update the comment above.  Every typography slot uses AppFontFamily, so a
// single font swap here applies everywhere in the app automatically.
//
// Previous font family: FontFamily.Default (system font)
// private val AppFontFamily = FontFamily.Default
val AppFontFamily = FontFamily(
    Font(R.font.geist_regular,   FontWeight.Normal),   // 400
    Font(R.font.geist_medium,    FontWeight.Medium),   // 500
    Font(R.font.geist_semibold,  FontWeight.SemiBold), // 600
    Font(R.font.geist_bold,      FontWeight.Bold),     // 700
    Font(R.font.geist_extrabold, FontWeight.ExtraBold),// 800
)

/**
 * Material 3 typography scale for FocusFirst (Toki).
 *
 * Weight rules per M3 spec (material_design_skills.md §2.1):
 *   Display / Headline → Regular (400)
 *   Title              → Medium  (500)
 *   Body               → Regular (400)
 *   Label              → Medium  (500)
 *
 * Previous implementation used Bold/SemiBold on Display and Headline styles,
 * which deviates from the spec.  Those are corrected here; the custom
 * ExtraBold weight is preserved only where it was explicitly intentional
 * (e.g. screen titles rendered directly in HomeScreen / StatsScreen via raw
 * TextStyle, not through the typography slot system).
 */
val Typography = Typography(
    // ── Display ──────────────────────────────────────────────────────────────
    // M3: Regular (400) weight for all Display styles.
    displayLarge = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.Normal,   // was Bold — corrected per M3 §2.1
        fontSize      = 57.sp,
        lineHeight    = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.Normal,   // was Bold — corrected per M3 §2.1
        fontSize      = 45.sp,
        lineHeight    = 52.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.Normal,   // was SemiBold — corrected per M3 §2.1
        fontSize      = 36.sp,
        lineHeight    = 44.sp,
        letterSpacing = 0.sp,
    ),

    // ── Headline ─────────────────────────────────────────────────────────────
    // M3: Regular (400) weight for all Headline styles.
    headlineLarge = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.Normal,   // was SemiBold — corrected per M3 §2.1
        fontSize      = 32.sp,
        lineHeight    = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.Normal,   // was SemiBold — corrected per M3 §2.1
        fontSize      = 28.sp,
        lineHeight    = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily    = AppFontFamily,
        fontWeight    = FontWeight.Normal,   // was SemiBold — corrected per M3 §2.1
        fontSize      = 24.sp,
        lineHeight    = 32.sp,
        letterSpacing = 0.sp,
    ),

    // ── Title ─────────────────────────────────────────────────────────────────
    // M3: Medium (500) weight — already correct, kept as-is.
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

    // ── Body ─────────────────────────────────────────────────────────────────
    // M3: Regular (400) weight — already correct, kept as-is.
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

    // ── Label ────────────────────────────────────────────────────────────────
    // M3: Medium (500) weight — already correct, kept as-is.
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
