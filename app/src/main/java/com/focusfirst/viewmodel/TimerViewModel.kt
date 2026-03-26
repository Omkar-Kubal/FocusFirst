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
import com.focusfirst.data.SettingsRepository
import com.focusfirst.data.db.DailySummary
import com.focusfirst.data.db.SessionDao
import com.focusfirst.data.db.SessionEntity
import com.focusfirst.data.model.AmbientSound
import com.focusfirst.data.model.IntervalPreset
import com.focusfirst.data.model.TimerPhase
import com.focusfirst.data.model.TimerState
import com.focusfirst.service.SoundManager
import com.focusfirst.service.TimerAlarmWorker
import com.focusfirst.service.TimerForegroundService
import com.focusfirst.service.cancelAlarm
import com.focusfirst.service.scheduleAlarm
import com.focusfirst.util.DndManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// ---------------------------------------------------------------------------
// TimerViewModel
//
// Single source of truth for all timer UI state.  Coordinates between:
//   - TimerForegroundService  (issues commands, receives tick/finish broadcasts)
//   - SettingsRepository      (drives break lengths, long-break cadence, etc.)
//   - Room / SessionDao        (persists completed + partial sessions)
//   - SoundManager            (ambient sound playback lifecycle)
//   - DndManager              (Do Not Disturb enable/restore lifecycle)
//   - Compose UI               (exposes StateFlows)
//
// Initial FOCUS duration when the user taps Start follows the selected
// [IntervalPreset]; automatic phase transitions use slider values from
// [SettingsRepository].
// ---------------------------------------------------------------------------

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val application: Application,
    private val sessionDao: SessionDao,
    private val settingsRepository: SettingsRepository,
    private val soundManager: SoundManager,
    private val dndManager: DndManager,
) : ViewModel() {

    // ── LocalBroadcastManager ─────────────────────────────────────────────────
    private val localBroadcastManager: LocalBroadcastManager =
        LocalBroadcastManager.getInstance(application)

    // ========================================================================
    // Settings — Eagerly started so .value is always up-to-date
    // ========================================================================

    private val focusMinutesFlow = settingsRepository.focusMinutes
        .stateIn(viewModelScope, SharingStarted.Eagerly, 25)

    private val shortBreakMinutesFlow = settingsRepository.shortBreakMinutes
        .stateIn(viewModelScope, SharingStarted.Eagerly, 5)

    private val longBreakMinutesFlow = settingsRepository.longBreakMinutes
        .stateIn(viewModelScope, SharingStarted.Eagerly, 15)

    private val sessionsBeforeLongBreakFlow = settingsRepository.sessionsBeforeLongBreak
        .stateIn(viewModelScope, SharingStarted.Eagerly, 4)

    // ========================================================================
    // Exposed StateFlows
    // ========================================================================

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    val todayCount: StateFlow<Int> = sessionDao
        .observeTodayCount(todayStartMs())
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000L),
            initialValue = 0,
        )

    val totalCompleted: StateFlow<Int> = sessionDao
        .observeTotalCompleted()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000L),
            initialValue = 0,
        )

    /** Five most-recent sessions (any status), newest first. */
    val recentSessions: StateFlow<List<SessionEntity>> = sessionDao
        .observeRecent()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    val weeklySummary: StateFlow<List<DailySummary>> = sessionDao
        .observeWeeklySummary(sinceEpochMs = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    /** Last 14 calendar buckets for week-over-week stats trends. */
    val dailySummaries14d: StateFlow<List<DailySummary>> = sessionDao
        .observeWeeklySummary(sinceEpochMs = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000L)
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    /** All sessions ever recorded, newest first. Used by the heatmap in StatsScreen. */
    val allSessions: StateFlow<List<SessionEntity>> = sessionDao
        .observeAll()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    /**
     * Current consecutive-day focus streak.
     *
     * A streak is valid when either today or yesterday has at least one
     * completed session — this preserves the count for users who haven't
     * started today yet.  The streak resets to 0 if the most-recent
     * completed day is older than yesterday.
     */
    val streakDays: StateFlow<Int> = sessionDao
        .observeCompletedDays()
        .map { days -> computeStreak(days) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000L),
            initialValue = 0,
        )

    val ambientSound: StateFlow<AmbientSound> = settingsRepository.ambientSound
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.Eagerly,
            initialValue = AmbientSound.NONE,
        )

    val ambientVolume: StateFlow<Float> = settingsRepository.ambientVolume
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.Eagerly,
            initialValue = 0.5f,
        )

    val dndEnabled: StateFlow<Boolean> = settingsRepository.dndEnabled
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.Eagerly,
            initialValue = false,
        )

    // ========================================================================
    // Private session-tracking fields
    // ========================================================================

    private var sessionStartMs: Long = 0L

    /**
     * Cumulative count of FOCUS phases completed in the current Pomodoro cycle.
     * Reset to 0 after every LONG_BREAK so the cycle repeats correctly.
     */
    private var sessionCount: Int = 0

    // ========================================================================
    // Broadcast receiver
    // ========================================================================

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
        val filter = IntentFilter().apply {
            addAction(TimerForegroundService.BROADCAST_TICK)
            addAction(TimerForegroundService.BROADCAST_FINISHED)
        }
        localBroadcastManager.registerReceiver(timerReceiver, filter)
        Log.d(TAG, "init: BroadcastReceiver registered")
    }

    // ========================================================================
    // Public API — timer commands
    // ========================================================================

    /**
     * Starts a fresh FOCUS session.
     *
     * Duration follows the selected [IntervalPreset]'s focus length so the
     * preset pills (15 / 25 / 35 / 45 minutes) match the running timer.
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

        TimerAlarmWorker.resetCompletionFlag(application)
        scheduleAlarm(application, durationSeconds, TimerPhase.FOCUS)

        ContextCompat.startForegroundService(
            application,
            TimerForegroundService.buildStartIntent(application, durationSeconds, TimerPhase.FOCUS),
        )

        val sound = ambientSound.value
        if (sound != AmbientSound.NONE) {
            soundManager.play(sound, ambientVolume.value)
        }

        if (dndEnabled.value && dndManager.isDndPermissionGranted()) {
            dndManager.enableDnd()
        }

        Log.d(TAG, "start preset=$preset duration=${durationSeconds}s")
    }

    fun pause() {
        Log.d(TAG, "pause")
        soundManager.pause()
        sendServiceAction(TimerForegroundService.ACTION_PAUSE)
    }

    fun resume() {
        Log.d(TAG, "resume")
        soundManager.resume()
        sendServiceAction(TimerForegroundService.ACTION_RESUME)
    }

    /**
     * Stops the timer unconditionally.
     *
     * Saves a partial session to Room if the user focused for more than 30 s.
     * Sessions shorter than 30 s are considered accidental starts and discarded.
     */
    fun stop() {
        if (sessionStartMs > 0L) {
            val elapsedSeconds = ((System.currentTimeMillis() - sessionStartMs) / 1000L).toInt()
            if (elapsedSeconds > 30) {
                saveSession(wasCompleted = false, phase = _timerState.value.phase)
            }
        }
        sessionStartMs = 0L

        soundManager.stop()
        dndManager.disableDnd()
        cancelAlarm(application)
        sendServiceAction(TimerForegroundService.ACTION_STOP)
        _timerState.value = TimerState()
        Log.d(TAG, "stop")
    }

    /**
     * Switches the active preset when the timer is idle.
     * Idle display reflects the preset length (15 / 25 / 35 / 45 minutes).
     */
    fun selectPreset(preset: IntervalPreset) {
        val state = _timerState.value
        if (state.isRunning || state.isPaused) return

        val focusSeconds = preset.focusMinutes * 60
        _timerState.update { current ->
            current.copy(
                preset           = preset,
                totalSeconds     = focusSeconds,
                remainingSeconds = focusSeconds,
            )
        }
        viewModelScope.launch {
            settingsRepository.update(SettingsRepository.KEY_FOCUS_MINUTES, preset.focusMinutes)
        }
        Log.d(TAG, "selectPreset $preset")
    }

    /**
     * Updates the ambient sound immediately — both persists the preference and
     * starts/stops playback if the timer is currently running in FOCUS phase.
     */
    fun updateSound(sound: AmbientSound, volume: Float) {
        viewModelScope.launch {
            settingsRepository.updateAmbientSound(sound)
            val state = _timerState.value
            if (state.isRunning && state.phase == TimerPhase.FOCUS) {
                if (sound == AmbientSound.NONE) {
                    soundManager.stop()
                } else {
                    soundManager.play(sound, volume)
                }
            }
        }
    }

    /**
     * Updates the playback volume immediately — both persists the preference
     * and applies it to the current MediaPlayer if active.
     */
    fun updateVolume(volume: Float) {
        viewModelScope.launch {
            settingsRepository.updateAmbientVolume(volume)
            soundManager.setVolume(volume)
        }
    }

    // ========================================================================
    // Private — broadcast handling
    // ========================================================================

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
     * Called when [TimerForegroundService] signals phase completion.
     *
     * Sequence:
     *   1. Persist the completed FOCUS session (breaks are not recorded).
     *   2. Advance the focus counter.
     *   3. Choose the next phase: SHORT_BREAK → every N sessions → LONG_BREAK.
     *   4. Manage sound and DND for the incoming phase.
     *   5. Auto-start the next phase with duration from [SettingsRepository].
     *
     * LONG_BREAK completion resets [sessionCount] so the Pomodoro cycle repeats.
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

        val sessionsBeforeLong = sessionsBeforeLongBreakFlow.value

        val nextPhase = when (phase) {
            TimerPhase.FOCUS ->
                if (sessionCount % sessionsBeforeLong == 0) TimerPhase.LONG_BREAK
                else TimerPhase.SHORT_BREAK

            TimerPhase.SHORT_BREAK -> TimerPhase.FOCUS

            TimerPhase.LONG_BREAK -> {
                // Reset the cycle so the user gets N short breaks before the next long break.
                sessionCount = 0
                TimerPhase.FOCUS
            }
        }

        // Sound + DND lifecycle: active during FOCUS, suspended during breaks
        if (nextPhase == TimerPhase.FOCUS) {
            val sound = ambientSound.value
            if (sound != AmbientSound.NONE) {
                soundManager.play(sound, ambientVolume.value)
            }
            if (dndEnabled.value && dndManager.isDndPermissionGranted()) {
                dndManager.enableDnd()
            }
        } else {
            soundManager.pause()
            dndManager.disableDnd()
        }

        val nextDurationSeconds = when (nextPhase) {
            TimerPhase.FOCUS       -> focusMinutesFlow.value * 60
            TimerPhase.SHORT_BREAK -> shortBreakMinutesFlow.value * 60
            TimerPhase.LONG_BREAK  -> longBreakMinutesFlow.value * 60
        }

        Log.d(TAG, "onPhaseFinished phase=$phase → next=$nextPhase (${nextDurationSeconds}s) sessionCount=$sessionCount")
        launchPhase(nextPhase, nextDurationSeconds)
    }

    /**
     * Updates [_timerState] and starts [TimerForegroundService] for any phase.
     * Also schedules the WorkManager alarm watchdog so phase completion is
     * reliable even if the process is killed.
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

        TimerAlarmWorker.resetCompletionFlag(application)
        scheduleAlarm(application, durationSeconds, phase)

        ContextCompat.startForegroundService(
            application,
            TimerForegroundService.buildStartIntent(application, durationSeconds, phase),
        )
    }

    // ========================================================================
    // Private — persistence
    // ========================================================================

    private fun saveSession(wasCompleted: Boolean, phase: TimerPhase) {
        val startMs = sessionStartMs
        sessionStartMs = 0L         // Zero before launch — prevents double-insert
        if (startMs == 0L) return   // Guard: no active session to save

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
        soundManager.release()
        dndManager.disableDnd()
        Log.d(TAG, "onCleared: receiver unregistered, sound + DND released")
        super.onCleared()
    }

    // ========================================================================
    // Private — streak computation
    // ========================================================================

    /**
     * Counts how many consecutive calendar days (epoch-day units) appear at
     * the head of [epochDays], which must already be sorted descending.
     *
     * The streak is considered "alive" if the most-recent completed day is
     * either **today** or **yesterday** — this covers the case where the user
     * hasn't focused yet today but maintained a streak until yesterday.
     *
     * Returns 0 when:
     *   - the list is empty
     *   - the most-recent day is older than yesterday
     */
    private fun computeStreak(epochDays: List<Long>): Int {
        if (epochDays.isEmpty()) return 0
        val todayEpochDay     = System.currentTimeMillis() / 86_400_000L
        val yesterdayEpochDay = todayEpochDay - 1
        val sortedDays        = epochDays.sortedDescending()
        val mostRecent        = sortedDays.first()

        // Streak expired if no session on today or yesterday
        if (mostRecent < yesterdayEpochDay) return 0

        var streak   = 0
        var expected = mostRecent
        for (day in sortedDays) {
            if (day == expected) {
                streak++
                expected--
            } else {
                break
            }
        }
        return streak
    }

    // ========================================================================
    // Companion
    // ========================================================================

    companion object {
        private const val TAG = "TimerViewModel"

        fun todayStartMs(): Long = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE,      0)
            set(Calendar.SECOND,      0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
