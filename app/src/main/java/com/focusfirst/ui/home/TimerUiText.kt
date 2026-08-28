package com.focusfirst.ui.home

import com.focusfirst.data.model.TimerMode
import com.focusfirst.data.model.TimerPhase
import com.focusfirst.data.model.TimerState

data class TimerUiText(
    val phase: String,
    val primaryAction: String,
    val cycle: String,
    val nextAction: String,
    val accessibilitySummary: String,
) {
    companion object {
        fun from(
            state: TimerState,
            selectedMode: TimerMode,
            sessionsBeforeLongBreak: Int = 4,
        ): TimerUiText {
            val visualMode = if (state.isIdle) selectedMode else state.timerMode
            val phase = when {
                visualMode == TimerMode.FLOW -> "Flow 45"
                state.phase == TimerPhase.SHORT_BREAK -> "Short break"
                state.phase == TimerPhase.LONG_BREAK -> "Long break"
                else -> "Focus"
            }
            val primaryAction = when {
                state.isRunning -> "Pause"
                state.isPaused -> "Resume"
                else -> "Start"
            }
            val currentCycle = (state.sessionsCompleted % sessionsBeforeLongBreak) + 1
            val cycle = when {
                state.isIdle -> "Ready when you are"
                visualMode == TimerMode.FLOW -> "Open focus session"
                state.phase == TimerPhase.FOCUS ->
                    "Session $currentCycle of $sessionsBeforeLongBreak before long break"
                state.phase == TimerPhase.SHORT_BREAK -> "Next up: focus"
                else -> "Full reset before the next round"
            }
            val nextAction = when {
                state.isPaused -> "Resume when ready"
                state.isRunning && state.phase == TimerPhase.FOCUS -> "Stay with this session"
                state.isRunning -> "Rest now, focus next"
                else -> "Start a focus session"
            }
            val remaining = if (state.isIdle && selectedMode == TimerMode.FLOW) {
                45 * 60
            } else {
                if (state.timerMode == TimerMode.FLOW && state.totalSeconds == 0) {
                    state.elapsedSeconds
                } else {
                    state.remainingSeconds
                }
            }
            val stateLabel = when {
                state.isRunning -> "running"
                state.isPaused -> "paused"
                else -> "ready"
            }
            val summary = buildString {
                append("$phase $stateLabel, ")
                append(formatAccessibilityDuration(remaining))
                if (state.isRunning || state.isPaused) {
                    append(" remaining")
                } else {
                    append(" remaining")
                }
                if (!state.isIdle && visualMode == TimerMode.POMODORO && state.phase == TimerPhase.FOCUS) {
                    append(", session $currentCycle of $sessionsBeforeLongBreak before long break")
                }
            }
            return TimerUiText(
                phase = phase,
                primaryAction = primaryAction,
                cycle = cycle,
                nextAction = nextAction,
                accessibilitySummary = summary,
            )
        }

        private fun formatAccessibilityDuration(totalSeconds: Int): String {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return when {
                seconds == 0 -> "$minutes minutes"
                minutes == 0 -> "$seconds seconds"
                else -> "$minutes minutes $seconds seconds"
            }
        }
    }
}
