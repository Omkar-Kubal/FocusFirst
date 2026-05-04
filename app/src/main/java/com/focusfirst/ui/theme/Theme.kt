package com.focusfirst.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.focusfirst.data.model.TimerPhase

// ============================================================================
// Toki Dark palette — "The Focused Void"
// ============================================================================
// NOTE: This palette is PRESERVED for easy revert if dynamic color is disabled.
// To disable dynamic color: set dynamicColor = false in FocusFirstTheme below,
// or revert to the old branch in the when{} block.

private val DarkBackground             = Color(0xFF000000) // Absolute Night Black
private val DarkSurface                = Color(0xFF111111) // Charcoal Glass
private val DarkSurfaceHigh            = Color(0xFF1F1F1F) // Pressed Graphite
private val DarkSurfaceVariant         = Color(0xFF242424) // Dim Graphite Track
private val DarkPrimary                = Color(0xFFF7F7F7) // Soft White
private val DarkOnPrimary              = Color(0xFF050505) // Pure Black Ink
private val DarkOnSurface              = Color(0xFFF7F7F7) // Soft White
private val DarkOnSurfaceVariant       = Color(0xFFA6A6A6) // Muted Silver Text
// M3: outline = borders/text-field edges; outlineVariant = subtle dividers (dimmer)
private val DarkOutline                = Color(0xFF2A2A2A) // Fine Graphite Stroke
private val DarkOutlineVariant         = Color(0xFF1E1E1E) // Subtle Divider (dimmer)
// Additional surface container slots (M3 §1.2 Surface & Neutral Roles)
private val DarkSurfaceContainerLowest = Color(0xFF050505)
private val DarkSurfaceContainer       = Color(0xFF161616)
private val DarkSurfaceContainerHighest= Color(0xFF282828)

// ============================================================================
// Toki Light palette — "Daylight Instrument"
// ============================================================================

private val LightBackground             = Color(0xFFFFFFFF) // Pure Canvas White
private val LightSurface                = Color(0xFFF2F2F2) // Cloud Surface
private val LightSurfaceHigh            = Color(0xFFE4E4E4) // Pressed Ash
private val LightSurfaceVariant         = Color(0xFFE8E8E8) // Pale Ash Track
private val LightPrimary                = Color(0xFF0D0D0D) // Ink Black
private val LightOnPrimary              = Color(0xFFF7F7F7) // Soft White Fill
private val LightOnSurface              = Color(0xFF0D0D0D) // Ink Black
private val LightOnSurfaceVariant       = Color(0xFF737373) // Muted Slate Text
private val LightOutline                = Color(0xFFD4D4D4) // Fine Ash Stroke
private val LightOutlineVariant         = Color(0xFFEAEAEA) // Subtle Divider (lighter)
// Additional surface container slots
private val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
private val LightSurfaceContainer       = Color(0xFFF6F6F6)
private val LightSurfaceContainerHighest= Color(0xFFDEDEDE)

// ============================================================================
// The Focused Void — shared / brand colors
// ============================================================================

/**
 * Colors shared across both modes, or used outside the Material3 slot system.
 * Updated to monochrome grayscale for "The Focused Void" aesthetic.
 */
object FocusColors {
    val TomatoRed         = Color(0xFFF7F7F7) // Mapped to primary white
    val BreakGreen        = Color(0xFF8E8E93) // Muted neutral grey
    val FlowBlue          = Color(0xFFD1D1D6) // Soft grey accent
    /** Pro / marketing card — stays dark in both modes. */
    val ProCardBackground = Color(0xFF1A1A1A)
}

// ============================================================================
// Static color schemes (B&W brand palette — fallback for API < 31)
// ============================================================================

private val LightColorScheme = lightColorScheme(
    primary                     = LightPrimary,
    onPrimary                   = LightOnPrimary,
    background                  = LightBackground,
    onBackground                = LightOnSurface,
    surface                     = LightSurface,
    onSurface                   = LightOnSurface,
    surfaceContainerLowest      = LightSurfaceContainerLowest,
    surfaceContainerLow         = LightSurface,
    surfaceContainer            = LightSurfaceContainer,
    surfaceContainerHigh        = LightSurfaceHigh,
    surfaceContainerHighest     = LightSurfaceContainerHighest,
    surfaceVariant              = LightSurfaceVariant,
    onSurfaceVariant            = LightOnSurfaceVariant,
    outline                     = LightOutline,
    outlineVariant              = LightOutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary                     = DarkPrimary,
    onPrimary                   = DarkOnPrimary,
    background                  = DarkBackground,
    onBackground                = DarkOnSurface,
    surface                     = DarkSurface,
    onSurface                   = DarkOnSurface,
    surfaceContainerLowest      = DarkSurfaceContainerLowest,
    surfaceContainerLow         = DarkSurface,
    surfaceContainer            = DarkSurfaceContainer,
    surfaceContainerHigh        = DarkSurfaceHigh,
    surfaceContainerHighest     = DarkSurfaceContainerHighest,
    surfaceVariant              = DarkSurfaceVariant,
    onSurfaceVariant            = DarkOnSurfaceVariant,
    outline                     = DarkOutline,
    outlineVariant              = DarkOutlineVariant,
)

private val DarkAmoledColorScheme = DarkColorScheme.copy(
    background                  = Color.Black,
    surface                     = Color.Black,
    surfaceContainerLowest      = Color.Black,
    surfaceContainerLow         = Color.Black,
    surfaceContainer            = Color(0xFF080808),
    surfaceContainerHigh        = Color(0xFF111111), // slight lift for usability
    surfaceContainerHighest     = Color(0xFF161616),
)

// ============================================================================
// CompositionLocal — whether the app shell is using the dark scheme
// ============================================================================

val LocalFocusDarkTheme = staticCompositionLocalOf { true }

// ============================================================================
// Theme composable
// ============================================================================

/**
 * Root Material 3 theme for FocusFirst (Toki).
 *
 * The black-and-white brand palette ("Focused Void") is the primary default.
 * Dynamic color (Material You) is preserved as an optional toggle for API 31+.
 *
 * @param darkTheme    When false uses Daylight Instrument light colors.
 * @param amoledMode   When true **and** [darkTheme], forces pure-black surfaces.
 * @param dynamicColor Set true to use wallpaper-extracted colors on API 31+.
 *                     Defaults to false for the brand monochrome experience.
 */
@Composable
fun FocusFirstTheme(
    darkTheme:    Boolean = true,
    amoledMode:   Boolean = false,
    dynamicColor: Boolean = false, // Switched default to false for B&W theme
    content:      @Composable () -> Unit,
) {
    val context = LocalContext.current

    val scheme = when {
        // ── Dynamic color branch (Material You, API 31+) ─────────────────────
        // Disabled by default to preserve the brand monochrome palette.
        // Set dynamicColor = true in the call-site to enable.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else           dynamicLightColorScheme(context)
        }

        // ── Static brand palette (The Focused Void) ──────────────────────────
        amoledMode && darkTheme -> DarkAmoledColorScheme
        darkTheme               -> DarkColorScheme
        else                    -> LightColorScheme
    }

    CompositionLocalProvider(LocalFocusDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = scheme,
            typography  = Typography,
            content     = content,
        )
    }
}

// ============================================================================
// Phase ring color — follows active scheme
// ============================================================================

/**
 * Progress-ring colour for [TimerPhase].
 * FOCUS → Tomato Red, SHORT_BREAK → Break Green, LONG_BREAK → Flow Blue.
 * Colors sourced from [FocusColors] so they're centrally managed.
 */
@Composable
fun TimerPhase.ringColor(): Color = when (this) {
    TimerPhase.FOCUS       -> FocusColors.TomatoRed
    TimerPhase.SHORT_BREAK -> FocusColors.BreakGreen
    TimerPhase.LONG_BREAK  -> FocusColors.FlowBlue
}
