package com.focusfirst.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
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

private val LightBackground             = Color(0xFFF8F6F1)
private val LightSurface                = Color(0xFFFFFCF7)
private val LightSurfaceHigh            = Color(0xFFF0EDE6)
private val LightSurfaceVariant         = Color(0xFFE1DDD3)
private val LightPrimary                = Color(0xFF161616)
private val LightOnPrimary              = Color(0xFFFFFFFF)
private val LightOnSurface              = Color(0xFF161616)
private val LightOnSurfaceVariant       = Color(0xFF666158)
private val LightOutline                = Color(0xFFD5D0C7)

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
    val FocusRed          = Color(0xFFE21B16)
    val ShortBreak        = Color(0xFF8FAAA0)
    val LongBreak         = Color(0xFF8C9EBC)
    val Success           = Color(0xFF65A882)
    val Warning           = Color(0xFFE1A95F)
}

// ============================================================================
// Static color schemes (B&W brand palette — fallback for API < 31)
// ============================================================================

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

private val LightColorScheme = lightColorScheme(
    primary                     = LightPrimary,
    onPrimary                   = LightOnPrimary,
    background                  = LightBackground,
    onBackground                = LightOnSurface,
    surface                     = LightSurface,
    onSurface                   = LightOnSurface,
    surfaceContainerLowest      = Color.White,
    surfaceContainerLow         = LightSurface,
    surfaceContainer            = LightSurfaceHigh,
    surfaceContainerHigh        = LightSurfaceHigh,
    surfaceContainerHighest     = Color(0xFFE8E3DA),
    surfaceVariant              = LightSurfaceVariant,
    onSurfaceVariant            = LightOnSurfaceVariant,
    outline                     = LightOutline,
    outlineVariant              = LightOutline.copy(alpha = 0.65f),
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
 * Toki is a dark-only app. The Focused Void palette is always active.
 * Dynamic color (Material You) is available as an optional toggle for API 31+.
 *
 * @param amoledMode   When true, forces pure-black surfaces for OLED screens.
 * @param dynamicColor Set true to use wallpaper-extracted colors on API 31+.
 *                     Defaults to false for the brand monochrome experience.
 */
@Composable
fun FocusFirstTheme(
    amoledMode:   Boolean = false,
    dynamicColor: Boolean = false,
    darkTheme:    Boolean = true,
    content:      @Composable () -> Unit,
) {
    val context = LocalContext.current

    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicDarkColorScheme(context)
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
