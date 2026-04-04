package com.focusfirst.data.model

// ── Timer Mode ────────────────────────────────────────────────────────────────

/** Whether the user is in classic Pomodoro (count-down) or open-ended Flow (count-up) mode. */
enum class TimerMode { POMODORO, FLOW }

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
    val timerMode: TimerMode = TimerMode.POMODORO,
    /** Total seconds for the current phase (set when phase starts). */
    val totalSeconds: Int = 25 * 60,
    /** Seconds remaining — counts down while running (Pomodoro mode). */
    val remainingSeconds: Int = 25 * 60,
    /** Seconds elapsed — counts up while running (Flow mode). */
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val sessionsCompleted: Int = 0,
    /** Stored separately so the user's custom value survives preset switches. */
    val customFocusMinutes: Int = 25,
) {
    /**
     * Linear progress from 0.0 → 1.0.
     * Pomodoro: fraction of total time consumed.
     * Flow: fills over 25 min as a visual reference (does not stop at 1.0).
     */
    val progress: Float
        get() = when (timerMode) {
            TimerMode.FLOW     -> (elapsedSeconds / (25 * 60f)).coerceIn(0f, 1f)
            TimerMode.POMODORO -> if (totalSeconds == 0) 0f
                                  else (totalSeconds - remainingSeconds) / totalSeconds.toFloat()
        }

    /** "MM:SS" string suitable for display on the timer face. */
    val displayTime: String
        get() {
            val secs    = if (timerMode == TimerMode.FLOW) elapsedSeconds else remainingSeconds
            val minutes = secs / 60
            val seconds = secs % 60
            return "%02d:%02d".format(minutes, seconds)
        }

    /** True when the timer is neither running nor paused (idle / reset state). */
    val isIdle: Boolean
        get() = !isRunning && !isPaused
}
