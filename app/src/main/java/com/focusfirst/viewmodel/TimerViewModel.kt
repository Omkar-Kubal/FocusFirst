package com.focusfirst.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.focusfirst.data.db.DailySummary
import com.focusfirst.data.db.SessionDao
import com.focusfirst.data.db.SessionEntity
import com.focusfirst.data.model.IntervalPreset
import com.focusfirst.data.model.TimerPhase
import com.focusfirst.data.model.TimerState
import com.focusfirst.service.TimerAlarmWorker
import com.focusfirst.service.TimerForegroundService
import com.focusfirst.service.cancelAlarm
import com.focusfirst.service.scheduleAlarm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// ---------------------------------------------------------------------------
// TimerViewModel
//
// Single source of truth for all timer UI state.  Coordinates between:
//   - TimerForegroundService (issues commands, receives tick/finish broadcasts)
//   - Room / SessionDao           (persists completed + partial sessions)
//   - Compose UI                  (exposes StateFlows)
//
// Broadcast wiring: the service emits via LocalBroadcastManager (process-local,
// inherently equivalent to RECEIVER_NOT_EXPORTED).  The ViewModel registers
// with the same LocalBroadcastManager in init{} and unregisters in onCleared().
// ---------------------------------------------------------------------------

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val application: Application,
    private val sessionDao: SessionDao,
) : ViewModel() {

    // ── LocalBroadcastManager ─────────────────────────────────────────────────
    // Cached at construction; used to register/unregister timerReceiver.
    private val localBroadcastManager: LocalBroadcastManager =
        LocalBroadcastManager.getInstance(application)

    // ========================================================================
    // Exposed StateFlows
    // ========================================================================

    // Primary timer state — mutated by broadcast ticks, action functions,
    // and the phase-lifecycle logic.
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    /**
     * Number of sessions (any completion status) that started today.
     * Derived from Room; re-emits on every insert / delete.
     *
     * [todayStartMs] is computed once at ViewModel creation.  If the app
     * stays alive past midnight the value drifts by at most one day —
     * acceptable for V1; a DateChangeReceiver can fix this in V2.
     */
    val todayCount: StateFlow<Int> = sessionDao
        .observeTodayCount(todayStartMs())
        .stateIn(
            scope         = viewModelScope,
            started       = SharingStarted.WhileSubscribed(5_000L),
            initialValue  = 0,
        )

    /** Running total of all-time completed focus sessions. */
    val totalCompleted: StateFlow<Int> = sessionDao
        .observeTotalCompleted()
        .stateIn(
            scope         = viewModelScope,
            started       = SharingStarted.WhileSubscribed(5_000L),
            initialValue  = 0,
        )

    /** Five most-recent sessions (any status), newest first. */
    val recentSessions: StateFlow<List<SessionEntity>> = sessionDao
        .observeRecentSessions(limit = 5)
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    /**
     * Per-day aggregates for the trailing 7 days.
     * Ordered newest-first by [DailySummary.date] (epoch-day).
     */
    val weeklySummary: StateFlow<List<DailySummary>> = sessionDao
        .observeWeeklySummary(sinceEpochMs = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    // ========================================================================
    // Private session-tracking fields
    // ========================================================================

    /**
     * Epoch-ms recorded when the current session began.
     * 0 means no session is active (idle or just after save).
     */
    private var sessionStartMs: Long = 0L

    /**
     * Cumulative count of FOCUS phases completed in the current continuous run.
     * Drives the short-break / long-break decision.
     */
    private var sessionCount: Int = 0

    /** A long break is inserted every N focus sessions. */
    private val sessionsBeforeLongBreak: Int = 4

    // ========================================================================
    // Broadcast receiver
    // ========================================================================

    /**
     * Receives second-by-second ticks and phase-finished signals from
     * [TimerForegroundService] via [LocalBroadcastManager].
     */
    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                TimerForegroundService.BROADCAST_TICK     -> handleTick(intent)
                TimerForegroundService.BROADCAST_FINISHED -> {
                    val phaseString =
                        intent.getStringExtra(TimerForegroundService.EXTRA_PHASE) ?: return
                    onPhaseFinished(phaseString)
                }
            }
        }
    }

    init {
        // Register for both broadcast actions with a single receiver instance.
        // LocalBroadcastManager is process-local — no exported flag is needed.
        val filter = IntentFilter().apply {
            addAction(TimerForegroundService.BROADCAST_TICK)
            addAction(TimerForegroundService.BROADCAST_FINISHED)
        }
        // NOTE: LocalBroadcastManager.registerReceiver is used (not
        // ContextCompat.registerReceiver) because the service sends via
        // LocalBroadcastManager.sendBroadcast().  The two mechanisms are
        // separate channels — mixing them would silently drop broadcasts.
        // Process-locality provides the same security guarantee as
        // RECEIVER_NOT_EXPORTED.
        localBroadcastManager.registerReceiver(timerReceiver, filter)
        Log.d(TAG, "init: BroadcastReceiver registered")
    }

    // ========================================================================
    // Public API — timer commands
    // ========================================================================

    /**
     * Starts a fresh FOCUS session using [preset].
     *
     * If the timer is already running the caller should [stop] first; this
     * function does not guard against double-starts (the service is
     * idempotent on ACTION_START).
     */
    fun start(preset: IntervalPreset = _timerState.value.preset) {
        val durationSeconds = preset.focusMinutes * 60
        sessionStartMs = System.currentTimeMillis()

        _timerState.update { current ->
            current.copy(
                phase            = TimerPhase.FOCUS,
                preset           = preset,
                totalSeconds     = durationSeconds,
                remainingSeconds = durationSeconds,
                isRunning        = true,
                isPaused         = false,
            )
        }

        TimerAlarmWorker.resetCompletionFlag(application)                          // LINE 1
        scheduleAlarm(application, durationSeconds, TimerPhase.FOCUS)              // LINE 2

        ContextCompat.startForegroundService(
            application,
            TimerForegroundService.buildStartIntent(application, durationSeconds, TimerPhase.FOCUS),
        )
        Log.d(TAG, "start preset=$preset duration=${durationSeconds}s")
    }

    /**
     * Pauses the running timer.
     * The service cancels its coroutine and broadcasts a final tick with
     * isRunning=false so the UI updates immediately.
     */
    fun pause() {
        Log.d(TAG, "pause")
        sendServiceAction(TimerForegroundService.ACTION_PAUSE)
    }

    /**
     * Resumes a paused timer from the remaining seconds saved in the service.
     */
    fun resume() {
        Log.d(TAG, "resume")
        sendServiceAction(TimerForegroundService.ACTION_RESUME)
    }

    /**
     * Stops the timer unconditionally.
     *
     * If the session has been running for more than 30 seconds it is saved
     * to Room as an incomplete session ([wasCompleted] = false).
     * Sessions shorter than 30 s are considered accidental starts and are
     * discarded to keep the stats clean.
     */
    fun stop() {
        if (sessionStartMs > 0L) {
            val elapsedSeconds = ((System.currentTimeMillis() - sessionStartMs) / 1000L).toInt()
            if (elapsedSeconds > 30) {
                saveSession(wasCompleted = false, phase = _timerState.value.phase)
            }
        }
        // Unconditional reset — saveSession nulls sessionStartMs if it ran,
        // but we guard here in case it didn't (elapsed ≤ 30 s).
        sessionStartMs = 0L

        cancelAlarm(application)                                                   // LINE 3

        sendServiceAction(TimerForegroundService.ACTION_STOP)
        _timerState.value = TimerState()  // Return to idle defaults
        Log.d(TAG, "stop")
    }

    /**
     * Switches the active preset.  Only applies when the timer is idle
     * (not running and not paused) — otherwise silently ignored.
     */
    fun selectPreset(preset: IntervalPreset) {
        val state = _timerState.value
        if (state.isRunning || state.isPaused) return

        _timerState.update { current ->
            current.copy(
                preset           = preset,
                totalSeconds     = preset.focusMinutes * 60,
                remainingSeconds = preset.focusMinutes * 60,
            )
        }
        Log.d(TAG, "selectPreset $preset")
    }

    // ========================================================================
    // Private — broadcast handling
    // ========================================================================

    /**
     * Processes a BROADCAST_TICK from the service.
     *
     * [isPaused] is set only when the clock has time remaining but the
     * coroutine is not running — ruling out the idle-at-zero case where
     * [isRunning]=false and [remainingSeconds]=0 (e.g. post-stop tick).
     */
    private fun handleTick(intent: Intent) {
        val remaining = intent.getIntExtra(TimerForegroundService.EXTRA_REMAINING_SECONDS, 0)
        val running   = intent.getBooleanExtra(TimerForegroundService.EXTRA_IS_RUNNING, false)
        val phase     = intent.getStringExtra(TimerForegroundService.EXTRA_PHASE)
            ?.let { runCatching { TimerPhase.valueOf(it) }.getOrNull() }
            ?: _timerState.value.phase

        _timerState.update { current ->
            current.copy(
                remainingSeconds = remaining,
                isRunning        = running,
                isPaused         = !running && remaining > 0,
                phase            = phase,
            )
        }
    }

    // ========================================================================
    // Private — Pomodoro phase lifecycle
    // ========================================================================

    /**
     * Called when [TimerForegroundService] signals that a phase completed
     * (either naturally or via ACTION_SKIP).
     *
     * Sequence:
     *   1. Persist the completed FOCUS session (breaks are not recorded).
     *   2. Advance the focus counter.
     *   3. Choose the next phase based on the counter modulo [sessionsBeforeLongBreak].
     *   4. Auto-start the next phase.
     */
    private fun onPhaseFinished(phaseString: String) {
        val phase = runCatching { TimerPhase.valueOf(phaseString) }.getOrElse {
            Log.e(TAG, "Unknown phase in BROADCAST_FINISHED: '$phaseString'")
            return
        }

        if (phase == TimerPhase.FOCUS) {
            saveSession(wasCompleted = true, phase = phase)
            sessionCount++
            _timerState.update { it.copy(sessionsCompleted = it.sessionsCompleted + 1) }
        }

        val nextPhase = when (phase) {
            TimerPhase.FOCUS ->
                if (sessionCount % sessionsBeforeLongBreak == 0) TimerPhase.LONG_BREAK
                else TimerPhase.SHORT_BREAK
            TimerPhase.SHORT_BREAK,
            TimerPhase.LONG_BREAK -> TimerPhase.FOCUS
        }

        val nextDurationSeconds = when (nextPhase) {
            TimerPhase.FOCUS       -> _timerState.value.preset.focusMinutes * 60
            TimerPhase.SHORT_BREAK -> BREAK_SHORT_SECONDS
            TimerPhase.LONG_BREAK  -> BREAK_LONG_SECONDS
        }

        Log.d(TAG, "onPhaseFinished phase=$phase → next=$nextPhase (${nextDurationSeconds}s) sessionCount=$sessionCount")
        launchPhase(nextPhase, nextDurationSeconds)
    }

    /**
     * Updates [_timerState] and starts [TimerForegroundService] for any phase.
     *
     * Called by [onPhaseFinished] to auto-advance through the Pomodoro cycle.
     * Unlike the public [start] (which is FOCUS-only and preset-driven),
     * this function accepts any [TimerPhase] and an explicit duration — allowing
     * break phases to use their fixed durations.
     */
    private fun launchPhase(phase: TimerPhase, durationSeconds: Int) {
        sessionStartMs = System.currentTimeMillis()

        _timerState.update { current ->
            current.copy(
                phase            = phase,
                totalSeconds     = durationSeconds,
                remainingSeconds = durationSeconds,
                isRunning        = true,
                isPaused         = false,
            )
        }

        ContextCompat.startForegroundService(
            application,
            TimerForegroundService.buildStartIntent(application, durationSeconds, phase),
        )
    }

    // ========================================================================
    // Private — persistence
    // ========================================================================

    /**
     * Inserts a [SessionEntity] into Room for the session that began at
     * [sessionStartMs].
     *
     * [sessionStartMs] is zeroed out *before* the coroutine launches so that
     * a rapid double-call (e.g. stop() + onPhaseFinished race) cannot produce
     * a duplicate insert.
     *
     * Duration is wall-clock elapsed time, so it correctly accounts for any
     * paused intervals that weren't tracked by the service.
     *
     * @param wasCompleted true for a session that ran to natural completion.
     * @param phase        the phase being saved (captured by caller to avoid
     *                     a TOCTOU race with [_timerState] updates).
     */
    private fun saveSession(wasCompleted: Boolean, phase: TimerPhase) {
        val startMs = sessionStartMs
        sessionStartMs = 0L            // Zero before launch — prevents double-insert
        if (startMs == 0L) return      // Guard: no active session to save

        val durationSeconds = ((System.currentTimeMillis() - startMs) / 1000L).toInt()
        val tag = when (phase) {
            TimerPhase.FOCUS       -> "Focus"
            TimerPhase.SHORT_BREAK -> "Short Break"
            TimerPhase.LONG_BREAK  -> "Long Break"
        }

        viewModelScope.launch {
            runCatching {
                sessionDao.insert(
                    SessionEntity(
                        startedAt       = startMs,
                        durationSeconds = durationSeconds,
                        wasCompleted    = wasCompleted,
                        tag             = tag,
                    )
                )
            }.onFailure { Log.e(TAG, "Failed to save session", it) }
                .onSuccess { Log.d(TAG, "Saved session tag=$tag completed=$wasCompleted duration=${durationSeconds}s") }
        }
    }

    // ========================================================================
    // Private — service communication
    // ========================================================================

    /**
     * Sends an action intent to [TimerForegroundService].
     *
     * Uses [Context.startService] (not startForegroundService) for non-start
     * commands: the service is already running as a foreground service, so
     * only ACTION_START needs the foreground-service guarantee.
     */
    private fun sendServiceAction(action: String) {
        val intent = Intent(application, TimerForegroundService::class.java).apply {
            this.action = action
        }
        application.startService(intent)
    }

    // ========================================================================
    // Lifecycle
    // ========================================================================

    override fun onCleared() {
        localBroadcastManager.unregisterReceiver(timerReceiver)
        Log.d(TAG, "onCleared: BroadcastReceiver unregistered")
        super.onCleared()
    }

    // ========================================================================
    // Companion object — constants + helpers
    // ========================================================================

    companion object {
        private const val TAG = "TimerViewModel"

        /** Fixed break durations for V1. Expose as settings in V2. */
        private const val BREAK_SHORT_SECONDS = 5  * 60   // 5 minutes
        private const val BREAK_LONG_SECONDS  = 15 * 60   // 15 minutes

        /**
         * Returns the epoch-millisecond timestamp for today's midnight
         * in the device's local time zone.
         *
         * Used to filter [SessionDao.observeTodayCount] on ViewModel creation.
         * Computed once per ViewModel instance — acceptable for V1.
         */
        fun todayStartMs(): Long = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE,      0)
            set(Calendar.SECOND,      0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
