package com.focusfirst.ui.home

/**
 * Non-dimensional constants for the Home screen and timer face.
 * Dp/sp values that may need per-density overrides live in res/values/dimens.xml.
 */
internal object HomeDefaults {
    /** Standard M3 Medium2 transition duration used throughout the home screen. */
    const val TRANSITION_DURATION_MS      = 300

    /** Large timer countdown display. */
    const val TIMER_TEXT_SIZE_SP          = 76

    /** Phase label below the countdown ("FOCUS", "SHORT BREAK", …). */
    const val PHASE_LABEL_SIZE_SP         = 14

    /** Wide letter-spacing on the phase label for the all-caps aesthetic. */
    const val PHASE_LETTER_SPACING_SP     = 7
}
