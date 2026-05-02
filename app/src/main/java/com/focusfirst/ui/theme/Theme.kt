package com.focusfirst.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.focusfirst.data.model.TimerPhase

// ============================================================================
// Toki Dark palette — "The Focused Void"
// ============================================================================

private val DarkBackground        = Color(0xFF000000) // Absolute Night Black
private val DarkSurface           = Color(0xFF111111) // Charcoal Glass
private val DarkSurfaceHigh       = Color(0xFF1F1F1F) // Pressed Graphite
private val DarkSurfaceVariant    = Color(0xFF242424) // Dim Graphite Track
private val DarkPrimary           = Color(0xFFF7F7F7) // Soft White
private val DarkOnPrimary         = Color(0xFF050505) // Pure Black Ink
private val DarkOnSurface         = Color(0xFFF7F7F7) // Soft White
private val DarkOnSurfaceVariant  = Color(0xFFA6A6A6) // Muted Silver Text
private val DarkOutline           = Color(0xFF2A2A2A) // Fine Graphite Stroke

// ============================================================================
// Toki Light palette — "Daylight Instrument"
// ============================================================================

private val LightBackground       = Color(0xFFFFFFFF) // Pure Canvas White
private val LightSurface          = Color(0xFFF2F2F2) // Cloud Surface
private val LightSurfaceHigh      = Color(0xFFE4E4E4) // Pressed Ash
private val LightSurfaceVariant   = Color(0xFFE8E8E8) // Pale Ash Track
private val LightPrimary          = Color(0xFF0D0D0D) // Ink Black
private val LightOnPrimary        = Color(0xFFF7F7F7) // Soft White Fill
private val LightOnSurface        = Color(0xFF0D0D0D) // Ink Black
private val LightOnSurfaceVariant = Color(0xFF7A7A7A) // Muted Slate Text
private val LightOutline          = Color(0xFFD4D4D4) // Fine Ash Stroke

// ============================================================================
// The Focused Void — shared / brand colors
// ============================================================================

/**
 * Colors shared across both modes, or used outside the Material3 slot system.
 */
object FocusColors {
    val TomatoRed         = Color(0xFFE84B1A)
    /** Pro / marketing card — stays dark in both modes. */
    val ProCardBackground = Color(0xFF1A1A1A)
}

// ============================================================================
// Color schemes
// ============================================================================

private val LightColorScheme = lightColorScheme(
    primary                   = LightPrimary,
    onPrimary                 = LightOnPrimary,
    background                = LightBackground,
    onBackground              = LightOnSurface,
    surface                   = LightSurface,
    onSurface                 = LightOnSurface,
    surfaceContainerLow       = LightSurface,
    surfaceContainerHigh      = LightSurfaceHigh,
    surfaceVariant            = LightSurfaceVariant,
    onSurfaceVariant          = LightOnSurfaceVariant,
    outline                   = LightOutline,
    outlineVariant            = LightOutline,
)

private val DarkColorScheme = darkColorScheme(
    primary                   = DarkPrimary,
    onPrimary                 = DarkOnPrimary,
    background                = DarkBackground,
    onBackground              = DarkOnSurface,
    surface                   = DarkSurface,
    onSurface                 = DarkOnSurface,
    surfaceContainerLow       = DarkSurface,
    surfaceContainerHigh      = DarkSurfaceHigh,
    surfaceVariant            = DarkSurfaceVariant,
    onSurfaceVariant          = DarkOnSurfaceVariant,
    outline                   = DarkOutline,
    outlineVariant            = DarkOutline,
)

private val DarkAmoledColorScheme = DarkColorScheme.copy(
    background          = Color.Black,
    surface             = Color.Black,
    surfaceContainerLow = Color.Black,
)

// ============================================================================
// CompositionLocal — whether the app shell is using the dark scheme
// ============================================================================

val LocalFocusDarkTheme = staticCompositionLocalOf { true }

// ============================================================================
// Theme composable
// ============================================================================

/**
 * Root Material 3 theme for FocusFirst.
 *
 * @param darkTheme When false, uses Stitch light colors (#F2F2F7 background).
 * @param amoledMode When true **and** [darkTheme], forces pure-black surfaces.
 */
@Composable
fun FocusFirstTheme(
    darkTheme:  Boolean = true,
    amoledMode: Boolean = false,
    content:    @Composable () -> Unit,
) {
    val scheme = when {
        !darkTheme -> LightColorScheme
        amoledMode -> DarkAmoledColorScheme
        else       -> DarkColorScheme
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
// Phase ring color — follows active scheme (black ring in light, white in dark)
// ============================================================================

/**
 * Progress-ring colour for [TimerPhase].
 * FOCUS → Tomato Red, SHORT_BREAK → Green, LONG_BREAK → Blue.
 */
@Composable
fun TimerPhase.ringColor(): Color = when (this) {
    TimerPhase.FOCUS       -> Color(0xFFE84B1A)
    TimerPhase.SHORT_BREAK -> Color(0xFF1A9E5F)
    TimerPhase.LONG_BREAK  -> Color(0xFF2B7BE0)
}
