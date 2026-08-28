package com.focusfirst.ui.home

import com.focusfirst.data.model.IntervalPreset
import com.focusfirst.data.model.TimerMode
import com.focusfirst.data.model.TimerPhase
import com.focusfirst.data.model.TimerState
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerUiTextTest {

    @Test
    fun runningFocusStateDescribesPhaseActionAndAccessibilityText() {
        val state = TimerState(
            phase = TimerPhase.FOCUS,
            preset = IntervalPreset.CLASSIC,
            timerMode = TimerMode.POMODORO,
            totalSeconds = 25 * 60,
            remainingSeconds = 24 * 60 + 12,
            isRunning = true,
            isPaused = false,
            sessionsCompleted = 1,
        )

        val text = TimerUiText.from(state, selectedMode = TimerMode.POMODORO)

        assertEquals("Focus", text.phase)
        assertEquals("Pause", text.primaryAction)
        assertEquals("Session 2 of 4 before long break", text.cycle)
        assertEquals(
            "Focus running, 24 minutes 12 seconds remaining, session 2 of 4 before long break",
            text.accessibilitySummary,
        )
    }

    @Test
    fun idleFlowStateUsesFlow45Copy() {
        val text = TimerUiText.from(TimerState(), selectedMode = TimerMode.FLOW)

        assertEquals("Flow 45", text.phase)
        assertEquals("Start", text.primaryAction)
        assertEquals("Ready when you are", text.cycle)
        assertEquals("Flow 45 ready, 45 minutes remaining", text.accessibilitySummary)
    }
}
