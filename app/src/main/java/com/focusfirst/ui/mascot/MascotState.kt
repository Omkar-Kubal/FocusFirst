package com.focusfirst.ui.mascot

import androidx.annotation.DrawableRes
import com.focusfirst.R
import com.focusfirst.data.model.TimerMode
import com.focusfirst.data.model.TimerPhase

/**
 * Maps every timer state to the mascot drawable that should be shown.
 * Add new states here — TokiMascot picks them up automatically.
 */
enum class MascotState(@DrawableRes val drawableRes: Int) {
    IDLE_POMODORO(R.drawable.ic_mascot_general),
    IDLE_FLOW    (R.drawable.ic_mascot_learn),
    PAUSED       (R.drawable.ic_mascot_peek),
    FOCUS        (R.drawable.ic_mascot_focus),
    FLOW_ACTIVE  (R.drawable.ic_mascot_plan),
    SHORT_BREAK  (R.drawable.ic_mascot_coffee),
    LONG_BREAK   (R.drawable.ic_mascot_sleep);

    companion object {
        fun from(
            isIdle:   Boolean,
            isPaused: Boolean,
            mode:     TimerMode,
            phase:    TimerPhase,
        ): MascotState = when {
            isIdle && mode == TimerMode.FLOW  -> IDLE_FLOW
            isIdle                            -> IDLE_POMODORO
            isPaused                          -> PAUSED
            mode == TimerMode.FLOW            -> FLOW_ACTIVE
            phase == TimerPhase.SHORT_BREAK   -> SHORT_BREAK
            phase == TimerPhase.LONG_BREAK    -> LONG_BREAK
            phase == TimerPhase.FOCUS         -> FOCUS
            else                              -> IDLE_POMODORO
        }
    }
}
