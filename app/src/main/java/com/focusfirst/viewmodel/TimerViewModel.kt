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
import androidx.preference.PreferenceManager
import com.focusfirst.analytics.TokiAnalytics
import com.focusfirst.data.SettingsRepository
import com.focusfirst.data.db.DailySummary
import com.focusfirst.data.db.SessionDao
import com.focusfirst.data.db.SessionEntity
import com.focusfirst.data.model.AmbientSound
import com.focusfirst.data.model.AllBadges
import com.focusfirst.data.model.Badge
import com.focusfirst.data.model.IntervalPreset
import com.focusfirst.data.model.TimerMode
import com.focusfirst.data.model.TimerPhase
import com.focusfirst.data.model.TimerState
import com.focusfirst.data.remote.FirestoreRepository
import com.focusfirst.data.repository.FocusGuardRepository
import com.focusfirst.service.FocusGuardAccessibilityService
import com.focusfirst.service.SoundManager
import com.focusfirst.service.TimerAlarmWorker
import com.focusfirst.service.TimerForegroundService
import com.focusfirst.service.cancelAlarm
import com.focusfirst.service.scheduleAlarm
import com.focusfirst.util.BadgeEvaluator
import com.focusfirst.util.DndManager
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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
    private val focusGuardRepository: FocusGuardRepository,
    private val firestoreRepository: FirestoreRepository,
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

    /** Emits Unit each time a FOCUS phase (Pomodoro or Flow) completes successfully. */
    private val _focusSessionCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val focusSessionCompleted: SharedFlow<Unit> = _focusSessionCompleted.asSharedFlow()

    /** Emits list of newly unlocked badges after a session completes. */
    private val _newBadges = MutableSharedFlow<List<Badge>>(extraBufferCapacity = 4)
    val newBadges: SharedFlow<List<Badge>> = _newBadges.asSharedFlow()

    private val todayStartFlow = flow {
        while (true) {
            val start = todayStartMs()
            emit(start)
            val msUntilMidnight = start + 86_400_000L - System.currentTimeMillis()
            delay(msUntilMidnight.coerceAtLeast(1_000L))
        }
    }

    val todayCount: StateFlow<Int> = todayStartFlow
        .flatMapLatest { sessionDao.observeTodayCount(it) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000L),
            initialValue = 0,
        )

    val totalCompleted: StateFlow<Int> = sessionDao
        .observeTotalCompleted()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.Eagerly,
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
     */
    val streakDays: StateFlow<Int> = sessionDao
        .observeCompletedDays()
        .map { days -> computeStreak(days) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.Eagerly,
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

    /** Whether the user has an active Pro purchase. Used to gate auto-sync. */
    val isPro: StateFlow<Boolean> = settingsRepository.proUnlocked
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
     * Starts a fresh FOCUS session in Pomodoro (count-down) mode.
     */
    fun start(preset: IntervalPreset = _timerState.value.preset) {
        val durationSeconds = preset.focusMinutes * 60
        sessionStartMs = System.currentTimeMillis()

        _timerState.update { current ->
            current.copy(
                phase            = TimerPhase.FOCUS,
                preset           = preset,
                timerMode        = TimerMode.POMODORO,
                totalSeconds     = durationSeconds,
                remainingSeconds = durationSeconds,
                elapsedSeconds   = 0,
                isRunning        = true,
                isPaused         = false,
            )
        }

        setFocusGuardActive(true, durationSeconds)
        TimerAlarmWorker.resetCompletionFlag(application)
        scheduleAlarm(application, durationSeconds, TimerPhase.FOCUS)

        ContextCompat.startForegroundService(
            application,
            TimerForegroundService.buildStartIntent(application, durationSeconds, TimerPhase.FOCUS),
        )

        startSoundAndDnd()

        // Analytics
        TokiAnalytics.logSessionStarted(
            durationMinutes = durationSeconds / 60,
            soundEnabled    = ambientSound.value != AmbientSound.NONE,
            dndEnabled      = dndEnabled.value,
            hasTask         = false,
        )
        Firebase.crashlytics.log("Timer started: $durationSeconds seconds")

        Log.d(TAG, "start preset=$preset duration=${durationSeconds}s")
    }

    /**
     * Starts a Flow session — count-up timer with no automatic phase transitions.
     * The session is saved when the user manually stops.
     */
    fun startFlow() {
        com.focusfirst.analytics.TokiAnalytics.logSessionStarted(
            durationMinutes = 0, // flow mode = no fixed duration
            soundEnabled = ambientSound.value != AmbientSound.NONE,
            dndEnabled = dndEnabled.value,
            hasTask = false
        )
        sessionStartMs = System.currentTimeMillis()

        _timerState.update { current ->
            current.copy(
                phase          = TimerPhase.FOCUS,
                timerMode      = TimerMode.FLOW,
                totalSeconds   = 0,
                remainingSeconds = 0,
                elapsedSeconds = 0,
                isRunning      = true,
                isPaused       = false,
            )
        }

        setFocusGuardActive(true, 0)
        // No WorkManager alarm for Flow — the user decides when to stop.
        ContextCompat.startForegroundService(
            application,
            Intent(application, TimerForegroundService::class.java).apply {
                action = TimerForegroundService.ACTION_START_FLOW
            },
        )

        startSoundAndDnd()
        Log.d(TAG, "startFlow")
    }

    fun pause() {
        Log.d(TAG, "pause")
        cancelAlarm(application)
        soundManager.pause()
        sendServiceAction(TimerForegroundService.ACTION_PAUSE)
    }

    fun resume() {
        Log.d(TAG, "resume")
        val state = _timerState.value
        if (state.timerMode == TimerMode.POMODORO && state.remainingSeconds > 0) {
            scheduleAlarm(application, state.remainingSeconds, state.phase)
        }
        soundManager.resume()
        sendServiceAction(TimerForegroundService.ACTION_RESUME)
    }

    /**
     * Stops the timer unconditionally.
     *
     * Pomodoro: saves a partial session if the user focused for > 30 s.
     * Flow: saves a completed session with actual elapsed time if > 30 s.
     */
    fun stop() {
        val state = _timerState.value

        if (state.timerMode == TimerMode.FLOW) {
            val elapsed = state.elapsedSeconds
            if (elapsed > 30) {
                viewModelScope.launch {
                    runCatching {
                        sessionDao.insert(
                            SessionEntity(
                                startedAt       = sessionStartMs,
                                durationSeconds = elapsed,
                                wasCompleted    = true,
                                tag             = "Flow",
                            )
                        )
                    }.onSuccess {
                        _focusSessionCompleted.tryEmit(Unit)
                        evaluateBadgesAfterSave()
                        autoSyncIfPro()
                        TokiAnalytics.logSessionCompleted(
                            durationMinutes = elapsed / 60,
                            sessionNumber   = totalCompleted.value,
                        )
                        Log.d(TAG, "Flow session saved: ${elapsed}s")
                    }
                }
            } else if (elapsed > 0) {
                // Short flow session treated as abandoned
                TokiAnalytics.logSessionAbandoned(
                    durationMinutes = 0,
                    elapsedMinutes  = elapsed / 60,
                )
            }
        } else if (sessionStartMs > 0L) {
            val elapsedSeconds = ((System.currentTimeMillis() - sessionStartMs) / 1000L).toInt()
            if (elapsedSeconds > 30) {
                val totalDuration = state.totalSeconds
                saveSession(wasCompleted = false, phase = state.phase)
                TokiAnalytics.logSessionAbandoned(
                    durationMinutes = totalDuration / 60,
                    elapsedMinutes  = elapsedSeconds / 60,
                )
            }
        }

        sessionStartMs = 0L
        setFocusGuardActive(false, 0)
        soundManager.stop()
        dndManager.disableDnd()
        cancelAlarm(application)
        sendServiceAction(TimerForegroundService.ACTION_STOP)
        _timerState.value = TimerState()
        Firebase.crashlytics.log("Timer stopped")
        Log.d(TAG, "stop")
    }

    /**
     * Switches the active preset when the timer is idle.
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

    fun updateSound(sound: AmbientSound, volume: Float) {
        viewModelScope.launch {
            settingsRepository.updateAmbientSound(sound)
            val state = _timerState.value
            if (state.isRunning && state.phase == TimerPhase.FOCUS) {
                if (sound == AmbientSound.NONE) soundManager.stop()
                else soundManager.play(sound, volume)
            }
        }
    }

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
        val isFlowMode = intent.getBooleanExtra(TimerForegroundService.EXTRA_IS_FLOW_MODE, false)

        if (isFlowMode) {
            val elapsed = intent.getIntExtra(TimerForegroundService.EXTRA_ELAPSED_SECONDS, 0)
            val running = intent.getBooleanExtra(TimerForegroundService.EXTRA_IS_RUNNING, false)
            _timerState.update { current ->
                current.copy(
                    elapsedSeconds = elapsed,
                    isRunning      = running,
                    isPaused       = !running && elapsed > 0,
                    timerMode      = TimerMode.FLOW,
                    phase          = TimerPhase.FOCUS,
                )
            }
        } else {
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
                    timerMode        = TimerMode.POMODORO,
                )
            }
        }
    }

    // ========================================================================
    // Private — Pomodoro phase lifecycle
    // ========================================================================

    private fun onPhaseFinished(phaseString: String) {
        val phase = runCatching { TimerPhase.valueOf(phaseString) }.getOrElse {
            Log.e(TAG, "Unknown phase in BROADCAST_FINISHED: '$phaseString'")
            return
        }

        if (phase == TimerPhase.FOCUS) {
            saveSession(wasCompleted = true, phase = phase)
            sessionCount++
            _timerState.update { it.copy(sessionsCompleted = it.sessionsCompleted + 1) }
            _focusSessionCompleted.tryEmit(Unit)
        }

        val sessionsBeforeLong = sessionsBeforeLongBreakFlow.value

        val nextPhase = when (phase) {
            TimerPhase.FOCUS ->
                if (sessionCount % sessionsBeforeLong == 0) TimerPhase.LONG_BREAK
                else TimerPhase.SHORT_BREAK

            TimerPhase.SHORT_BREAK -> TimerPhase.FOCUS

            TimerPhase.LONG_BREAK  -> {
                sessionCount = 0
                TimerPhase.FOCUS
            }
        }

        val nextDurationSeconds = when (nextPhase) {
            TimerPhase.FOCUS       -> focusMinutesFlow.value * 60
            TimerPhase.SHORT_BREAK -> shortBreakMinutesFlow.value * 60
            TimerPhase.LONG_BREAK  -> longBreakMinutesFlow.value * 60
        }

        if (nextPhase == TimerPhase.FOCUS) {
            startSoundAndDnd()
            setFocusGuardActive(true, nextDurationSeconds)
        } else {
            soundManager.pause()
            dndManager.disableDnd()
            setFocusGuardActive(false, 0)
        }

        Log.d(TAG, "onPhaseFinished phase=$phase → next=$nextPhase (${nextDurationSeconds}s) sessionCount=$sessionCount")
        launchPhase(nextPhase, nextDurationSeconds)
    }

    private fun launchPhase(phase: TimerPhase, durationSeconds: Int) {
        sessionStartMs = System.currentTimeMillis()

        _timerState.update { current ->
            current.copy(
                phase            = phase,
                timerMode        = TimerMode.POMODORO,
                totalSeconds     = durationSeconds,
                remainingSeconds = durationSeconds,
                elapsedSeconds   = 0,
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
        sessionStartMs = 0L
        if (startMs == 0L) return

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
             .onSuccess {
                 Log.d(TAG, "Saved session tag=$tag completed=$wasCompleted duration=${durationSeconds}s")
                 if (wasCompleted && phase == TimerPhase.FOCUS) {
                     evaluateBadgesAfterSave()
                     autoSyncIfPro()
                     TokiAnalytics.logSessionCompleted(
                         durationMinutes = durationSeconds / 60,
                         sessionNumber   = totalCompleted.value,
                     )
                 }
             }
        }
    }

    // ========================================================================
    // Private — helpers
    // ========================================================================

    private fun startSoundAndDnd() {
        val sound = ambientSound.value
        if (sound != AmbientSound.NONE) soundManager.play(sound, ambientVolume.value)
        if (dndEnabled.value && dndManager.isDndPermissionGranted()) dndManager.enableDnd()
    }

    // ── Focus Guard helpers ───────────────────────────────────────────────────

    private fun setFocusGuardActive(active: Boolean, remainingSeconds: Int) {
        PreferenceManager.getDefaultSharedPreferences(application)
            .edit()
            .putBoolean(FocusGuardAccessibilityService.PREF_FOCUS_GUARD_ACTIVE, active)
            .putInt(FocusGuardAccessibilityService.PREF_REMAINING_SECONDS, remainingSeconds)
            .apply()
    }

    // ── Badge evaluation ──────────────────────────────────────────────────────

    private fun evaluateBadgesAfterSave() {
        viewModelScope.launch {
            runCatching {
                val sessions        = sessionDao.observeAll().first()
                val streak          = streakDays.value
                val alreadyUnlocked = settingsRepository.unlockedBadges.first()
                val newIds          = BadgeEvaluator.evaluate(sessions, streak, alreadyUnlocked)
                if (newIds.isNotEmpty()) {
                    settingsRepository.unlockBadges(newIds.toSet())
                    val newBadgeObjects = newIds.mapNotNull { AllBadges.byId[it] }
                    _newBadges.tryEmit(newBadgeObjects)
                    Log.d(TAG, "Unlocked badges: $newIds")
                }
                if (streak in listOf(3, 7, 14, 30, 60, 100)) {
                    TokiAnalytics.logStreakMilestone(streak)
                }
            }.onFailure { Log.e(TAG, "Badge evaluation failed", it) }
        }
    }

    // ── Cloud auto-sync ───────────────────────────────────────────────────

    /** Automatically syncs unsynced sessions after a completed session. Pro only. */
    private fun autoSyncIfPro() {
        if (!isPro.value) return
        viewModelScope.launch {
            runCatching {
                firestoreRepository.ensureAuthenticated()
                firestoreRepository.syncSessionsToCloud()
            }.onFailure { e ->
                Firebase.crashlytics.recordException(e)
                Log.e(TAG, "Auto-sync failed: ${e.message}")
            }
        }
    }

    // ── Deep link entry point ─────────────────────────────────────────────────

    /**
     * Start a focus session from a deep link or home-screen shortcut.
     * Selects the closest matching preset, or uses a custom duration.
     */
    fun startFromDeepLink(durationMinutes: Int, taskName: String?) {
        // If the timer is already running, do nothing (avoid interrupt)
        val state = _timerState.value
        if (state.isRunning) return

        // Find the closest built-in preset or fall back to the default 25-min one
        val matchingPreset = IntervalPreset.entries.firstOrNull {
            it.focusMinutes == durationMinutes
        }

        if (matchingPreset != null) {
            start(matchingPreset)
        } else {
            // Custom duration — start with the default preset but override the duration
            val durationSeconds = durationMinutes * 60
            sessionStartMs = System.currentTimeMillis()
            val preset = _timerState.value.preset

            _timerState.update { current ->
                current.copy(
                    phase            = TimerPhase.FOCUS,
                    preset           = preset,
                    timerMode        = TimerMode.POMODORO,
                    totalSeconds     = durationSeconds,
                    remainingSeconds = durationSeconds,
                    elapsedSeconds   = 0,
                    isRunning        = true,
                    isPaused         = false,
                )
            }

            setFocusGuardActive(true, durationSeconds)
            TimerAlarmWorker.resetCompletionFlag(application)
            scheduleAlarm(application, durationSeconds, TimerPhase.FOCUS)
            ContextCompat.startForegroundService(
                application,
                TimerForegroundService.buildStartIntent(application, durationSeconds, TimerPhase.FOCUS),
            )
            startSoundAndDnd()
        }

        Log.d(TAG, "startFromDeepLink duration=${durationMinutes}m task=$taskName")
    }

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
        // Snapshot user properties so the final state is recorded before ViewModel dies
        TokiAnalytics.setUserProperties(
            isPro         = isPro.value,
            totalSessions = totalCompleted.value,
            streakDays    = streakDays.value,
        )
        Log.d(TAG, "onCleared: receiver unregistered, sound + DND released")
        super.onCleared()
    }

    // ========================================================================
    // Private — streak computation
    // ========================================================================

    private fun computeStreak(epochDays: List<Long>): Int {
        if (epochDays.isEmpty()) return 0
        val todayEpochDay     = System.currentTimeMillis() / 86_400_000L
        val yesterdayEpochDay = todayEpochDay - 1
        val sortedDays        = epochDays.sortedDescending()
        val mostRecent        = sortedDays.first()

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
