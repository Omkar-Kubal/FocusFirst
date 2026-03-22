package com.focusfirst.data.model

// ── Phase ─────────────────────────────────────────────────────────────────────

enum class TimerPhase {
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK,
}

// ── Preset ────────────────────────────────────────────────────────────────────

enum class IntervalPreset(val label: String, val focusMinutes: Int) {
    QUICK("Quick", 15),
    CLASSIC("Classic", 25),
    DEEP("Deep", 35),
    FLOW("Flow", 45),
}

// ── State ─────────────────────────────────────────────────────────────────────

data class TimerState(
    val phase: TimerPhase = TimerPhase.FOCUS,
    val preset: IntervalPreset = IntervalPreset.CLASSIC,
    /** Total seconds for the current phase (set when phase starts). */
    val totalSeconds: Int = 25 * 60,
    /** Seconds remaining — counts down while running. */
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val sessionsCompleted: Int = 0,
    /** Stored separately so the user's custom value survives preset switches. */
    val customFocusMinutes: Int = 25,
) {
    /**
     * Linear progress from 0.0 (start) → 1.0 (complete).
     * Guards against divide-by-zero on a freshly reset state.
     */
    val progress: Float
        get() = if (totalSeconds == 0) 0f
                else (totalSeconds - remainingSeconds) / totalSeconds.toFloat()

    /** "MM:SS" string suitable for display on the timer face. */
    val displayTime: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }
}
