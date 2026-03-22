package com.focusfirst.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.focusfirst.data.model.TimerPhase

// ============================================================================
// Brand palette
// ============================================================================

/**
 * Central color palette for FocusFirst.
 *
 * Phase ring aliases ([FocusRing], [ShortBreakRing], [LongBreakRing]) are
 * intentionally indirected through named vals: retheme a phase ring by
 * changing its assignment here — one line, zero hunt-and-replace.
 */
object FocusColors {

    // ── Core brand ───────────────────────────────────────────────────────────
    val TomatoRed   = Color(0xFFE84B1A)
    val GreenAccent = Color(0xFF1A9E5F)
    val BlueAccent  = Color(0xFF2B7BE0)

    // ── Phase ring colors — change these to retheme individual phases ────────
    val FocusRing      = TomatoRed
    val ShortBreakRing = GreenAccent
    val LongBreakRing  = BlueAccent

    // ── Surface palette ───────────────────────────────────────────────────────
    val AmoledBlack  = Color(0xFF000000)
    val SurfaceDark  = Color(0xFF1A1A1A)
    val SurfaceLight = Color(0xFFF5F5F5)
}

// ============================================================================
// Color schemes
// ============================================================================

private val LightColorScheme = lightColorScheme(
    primary      = FocusColors.TomatoRed,
    background   = Color.White,
    surface      = FocusColors.SurfaceLight,
    onPrimary    = Color.White,
    onBackground = Color(0xFF1A1A1A),
    onSurface    = Color(0xFF1A1A1A),
)

private val DarkColorScheme = darkColorScheme(
    primary      = FocusColors.TomatoRed,
    background   = FocusColors.SurfaceDark,
    surface      = Color(0xFF2A2A2A),
    onPrimary    = Color.White,
    onBackground = Color.White,
    onSurface    = Color.White,
)

/**
 * True-black variant of [DarkColorScheme] for OLED/AMOLED displays.
 *
 * [background] = pure black saves measurable power on pixel-level dimming
 * panels.  [surface] is kept slightly lifted so cards/sheets are
 * distinguishable from the background without a colour tint.
 */
private val AmoledColorScheme = darkColorScheme(
    primary      = FocusColors.TomatoRed,
    background   = FocusColors.AmoledBlack,
    surface      = Color(0xFF0D0D0D),
    onPrimary    = Color.White,
    onBackground = Color.White,
    onSurface    = Color.White,
)

// ============================================================================
// Theme composable
// ============================================================================

/**
 * Root Material 3 theme for FocusFirst.
 *
 * @param darkTheme  Defaults to the system dark-mode setting.  Pass an
 *                   explicit value to override (e.g. from a user preference).
 * @param amoledMode When true and [darkTheme] is also true, switches to the
 *                   pure-black [AmoledColorScheme].  Has no effect in light
 *                   mode because AMOLED savings only apply to dark pixels.
 */
@Composable
fun FocusFirstTheme(
    darkTheme:  Boolean = isSystemInDarkTheme(),
    amoledMode: Boolean = false,
    content:    @Composable () -> Unit,
) {
    val colorScheme = when {
        darkTheme && amoledMode -> AmoledColorScheme
        darkTheme               -> DarkColorScheme
        else                    -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content,
    )
}

// ============================================================================
// Phase color helper
// ============================================================================

/**
 * Returns the progress-ring colour associated with this [TimerPhase].
 *
 * Usage in Compose:
 *   val ringColor = timerState.phase.ringColor()
 */
fun TimerPhase.ringColor(): Color = when (this) {
    TimerPhase.FOCUS       -> FocusColors.FocusRing
    TimerPhase.SHORT_BREAK -> FocusColors.ShortBreakRing
    TimerPhase.LONG_BREAK  -> FocusColors.LongBreakRing
}
