package com.focusfirst.ui.mascot

/**
 * Animation and sizing constants for [TokiMascot].
 * All durations in milliseconds; sizes as fractions or raw floats.
 */
internal object MascotDefaults {
    /** Duration of the idle floating / breathing loop (ms). */
    const val FLOAT_DURATION_MS     = 1500

    /** Fade + scale in duration when switching mascot states (ms). */
    const val TRANSITION_IN_MS      = 300

    /** Fade + scale out duration when switching mascot states (ms). */
    const val TRANSITION_OUT_MS     = 200

    /** Initial / target scale used in the bouncy state-change transition. */
    const val TRANSITION_SCALE      = 0.8f

    /** Mascot renders at this fraction of its container's width. */
    const val SIZE_FRACTION         = 0.70f

    /**
     * Divides the float offset to derive a subtle breathing scale delta.
     * breathScale = 1f + (floatOffset / BREATH_SCALE_DIVISOR)
     */
    const val BREATH_SCALE_DIVISOR  = 100f
}
