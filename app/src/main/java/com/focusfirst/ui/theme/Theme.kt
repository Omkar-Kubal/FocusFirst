package com.focusfirst.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.focusfirst.data.model.TimerPhase

// ============================================================================
// The Focused Void — brand palette
// ============================================================================

/**
 * Central color palette for FocusFirst — "The Focused Void" design system.
 *
 * This is a monochrome-first design.  [TomatoRed] is the ONLY coloured
 * element and is reserved exclusively for the Pro CTA button.  All other
 * interactive and decorative elements use white at varying opacities over a
 * pure-black background.
 */
object FocusColors {

    // ── Pro CTA — only coloured element in the monochrome design ─────────────
    val TomatoRed = Color(0xFFE84B1A)

    // ── Background hierarchy (darkest → brightest) ────────────────────────────
    val Background              = Color(0xFF000000)   // Pure AMOLED black
    val SurfaceContainerLow     = Color(0xFF1B1B1B)   // Card / nav bar
    val SurfaceContainerHigh    = Color(0xFF2A2A2A)   // Elevated card
    val SurfaceContainerHighest = Color(0xFF353535)   // Highest-elevation surface
    val SurfaceBright           = Color(0xFF393939)   // Active / focused element

    // ── Text hierarchy ────────────────────────────────────────────────────────
    val OnSurface        = Color(0xFFE2E2E2)   // Primary body text
    val OnSurfaceVariant = Color(0xFFC6C6C6)   // Secondary / caption text

    // ── Borders ───────────────────────────────────────────────────────────────
    val OutlineVariant = Color(0xFF474747)
}

// ============================================================================
// Color scheme — dark-first, single scheme for V1
// ============================================================================

private val VoidColorScheme = darkColorScheme(
    primary              = Color.White,
    onPrimary            = Color.Black,
    primaryContainer     = FocusColors.SurfaceContainerHigh,
    onPrimaryContainer   = FocusColors.OnSurface,
    background           = FocusColors.Background,
    onBackground         = Color.White,
    surface              = FocusColors.SurfaceContainerLow,
    onSurface            = FocusColors.OnSurface,
    surfaceVariant       = FocusColors.SurfaceContainerHigh,
    onSurfaceVariant     = FocusColors.OnSurfaceVariant,
    outline              = FocusColors.OutlineVariant,
    outlineVariant       = FocusColors.OutlineVariant,
)

// ============================================================================
// Theme composable
// ============================================================================

/**
 * Root Material 3 theme for FocusFirst — "The Focused Void" design system.
 *
 * The app is dark-first: [VoidColorScheme] (pure-black background) is used
 * regardless of the [darkTheme] flag.  [amoledMode] is preserved for the
 * Phase 2 AMOLED toggle; the background is already #000000 so it has no
 * visual effect in V1.
 */
@Composable
fun FocusFirstTheme(
    darkTheme:  Boolean = true,
    amoledMode: Boolean = false,
    content:    @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = VoidColorScheme,
        typography  = Typography,
        content     = content,
    )
}

// ============================================================================
// Phase ring color — monochrome: always pure white
// ============================================================================

/**
 * Returns the progress-ring colour for this [TimerPhase].
 *
 * In the Focused Void monochrome design all phases share the same white ring.
 * Phase differentiation is communicated by the pill selector and phase label
 * text, not by ring colour.
 */
fun TimerPhase.ringColor(): Color = Color.White
