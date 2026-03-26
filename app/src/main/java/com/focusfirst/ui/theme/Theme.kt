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
// Stitch-inspired light palette
// ============================================================================

private val StitchLightBackground   = Color(0xFFF2F2F7)
private val StitchLightSurface      = Color(0xFFFFFFFF)
private val StitchLightOnSurface    = Color(0xFF000000)
private val StitchLightSecondary    = Color(0xFF8E8E93)
private val StitchLightPillIdle     = Color(0xFFE5E5EA)

// ============================================================================
// The Focused Void — dark palette (brand)
// ============================================================================

/**
 * Central color palette for FocusFirst — "The Focused Void" design system.
 *
 * [TomatoRed] is reserved for accent CTAs; the Pro card uses solid near-black.
 */
object FocusColors {

    val TomatoRed = Color(0xFFE84B1A)

    val Background              = Color(0xFF000000)
    val SurfaceContainerLow     = Color(0xFF1B1B1B)
    val SurfaceContainerHigh    = Color(0xFF2A2A2A)
    val SurfaceContainerHighest = Color(0xFF353535)
    val SurfaceBright           = Color(0xFF393939)

    val OnSurface        = Color(0xFFE2E2E2)
    val OnSurfaceVariant = Color(0xFFC6C6C6)

    val OutlineVariant = Color(0xFF474747)

    /** Pro / marketing card — same in light and dark. */
    val ProCardBackground = Color(0xFF1A1A1A)

    /** Preset pill idle (light mode). */
    val LightPillIdle = StitchLightPillIdle
}

// ============================================================================
// Color schemes
// ============================================================================

private val LightColorScheme = lightColorScheme(
    primary                   = StitchLightOnSurface,
    onPrimary                 = StitchLightSurface,
    primaryContainer          = StitchLightPillIdle,
    onPrimaryContainer        = StitchLightOnSurface,
    background                = StitchLightBackground,
    onBackground              = StitchLightOnSurface,
    surface                   = StitchLightSurface,
    onSurface                 = StitchLightOnSurface,
    surfaceContainerLow       = StitchLightSurface,
    surfaceContainerHigh      = StitchLightSurface,
    surfaceVariant            = StitchLightPillIdle,
    onSurfaceVariant          = StitchLightSecondary,
    outline                   = StitchLightPillIdle,
    outlineVariant            = StitchLightPillIdle,
)

private val DarkColorScheme = darkColorScheme(
    primary                   = Color.White,
    onPrimary                 = Color.Black,
    primaryContainer          = FocusColors.SurfaceContainerHigh,
    onPrimaryContainer        = FocusColors.OnSurface,
    background                = FocusColors.Background,
    onBackground              = Color.White,
    surface                   = FocusColors.SurfaceContainerLow,
    onSurface                 = FocusColors.OnSurface,
    surfaceContainerLow       = FocusColors.SurfaceContainerLow,
    surfaceContainerHigh      = FocusColors.SurfaceContainerHigh,
    onSurfaceVariant          = FocusColors.OnSurfaceVariant,
    outline                   = FocusColors.OutlineVariant,
    outlineVariant            = FocusColors.OutlineVariant,
)

private val DarkAmoledColorScheme = DarkColorScheme.copy(
    background           = Color.Black,
    surface              = Color.Black,
    surfaceContainerLow  = Color.Black,
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
