package com.focusfirst.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.focusfirst.MainActivity
import com.focusfirst.R
import com.focusfirst.data.db.SessionDao
import com.focusfirst.data.model.TimerPhase
import com.focusfirst.receiver.BootReceiver
import com.focusfirst.widget.FocusFirstWidget
import com.focusfirst.widget.WidgetKeys
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

// ---------------------------------------------------------------------------
// TimerForegroundService
//
// Two-layer reliability model:
//   Layer 1 – This ForegroundService owns the live countdown coroutine and
//              broadcasts second-by-second ticks to the UI.
//   Layer 2 – WorkManager (TimerAlarmWorker) acts as a watchdog: if the
//              process is killed it reschedules completion.
//
// Also supports Flow mode (ACTION_START_FLOW) — count-up timer with no
// automatic phase transitions.
// ---------------------------------------------------------------------------

@AndroidEntryPoint
class TimerForegroundService : Service() {

    @Inject lateinit var sessionDao: SessionDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var timerJob: Job? = null

    // ── Timer state ───────────────────────────────────────────────────────────
    private var remainingSeconds: Int    = 0
    private var currentPhase: TimerPhase = TimerPhase.FOCUS
    private var isRunning: Boolean       = false

    // ── Flow mode ─────────────────────────────────────────────────────────────
    private var isFlowMode:     Boolean = false
    private var elapsedSeconds: Int     = 0

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val localBroadcastManager: LocalBroadcastManager by lazy {
        LocalBroadcastManager.getInstance(this)
    }

    private val completionPrefs by lazy {
        getSharedPreferences(FOCUS_FIRST_PREFS, Context.MODE_PRIVATE)
    }

    companion object {
        const val FOCUS_FIRST_PREFS    = "focusfirst_prefs"
        const val PREF_VIBRATE_ENABLED = "vibrate_enabled"
        const val PREF_SOUND_TYPE      = "sound_type"

        const val ACTION_START      = "com.focusfirst.ACTION_START"
        const val ACTION_START_FLOW = "com.focusfirst.ACTION_START_FLOW"
        const val ACTION_PAUSE      = "com.focusfirst.ACTION_PAUSE"
        const val ACTION_RESUME     = "com.focusfirst.ACTION_RESUME"
        const val ACTION_STOP       = "com.focusfirst.ACTION_STOP"
        const val ACTION_SKIP       = "com.focusfirst.ACTION_SKIP"

        const val BROADCAST_TICK     = "com.focusfirst.TICK"
        const val BROADCAST_FINISHED = "com.focusfirst.FINISHED"

        const val EXTRA_DURATION_SECONDS  = "extra_duration_seconds"
        const val EXTRA_PHASE             = "extra_phase"
        const val EXTRA_REMAINING_SECONDS = "extra_remaining_seconds"
        const val EXTRA_ELAPSED_SECONDS   = "extra_elapsed_seconds"
        const val EXTRA_IS_RUNNING        = "extra_is_running"
        const val EXTRA_IS_FLOW_MODE      = "extra_is_flow_mode"

        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID      = "focusfirst_timer"

        private const val TAG = "TimerForegroundService"

        fun buildStartIntent(
            context:         Context,
            durationSeconds: Int,
            phase:           TimerPhase,
        ): Intent = Intent(context, TimerForegroundService::class.java).apply {
            action = ACTION_START
            putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            putExtra(EXTRA_PHASE, phase.name)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action=${intent?.action}")
        when (intent?.action) {
            ACTION_START      -> handleStart(intent)
            ACTION_START_FLOW -> handleStartFlow()
            ACTION_PAUSE      -> handlePause()
            ACTION_RESUME     -> handleResume()
            ACTION_STOP       -> handleStop()
            ACTION_SKIP       -> handleSkip()
            null              -> Log.w(TAG, "Restarted with null intent (START_STICKY); waiting for next command")
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "onDestroy — scope cancelled")
    }

    // ========================================================================
    // Handlers
    // ========================================================================

    private fun handleStart(intent: Intent) {
        val durationSeconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, 25 * 60)
        val phaseName       = intent.getStringExtra(EXTRA_PHASE) ?: TimerPhase.FOCUS.name
        currentPhase     = TimerPhase.valueOf(phaseName)
        remainingSeconds = durationSeconds
        isFlowMode       = false
        isRunning        = true

        Log.d(TAG, "handleStart phase=$currentPhase duration=${durationSeconds}s")

        saveBootPrefs(endTimeMs = System.currentTimeMillis() + durationSeconds * 1000L)
        startForegroundCompat()
        startTimerCoroutine()
    }

    private fun handleStartFlow() {
        isFlowMode       = true
        elapsedSeconds   = 0
        currentPhase     = TimerPhase.FOCUS
        isRunning        = true

        Log.d(TAG, "handleStartFlow")

        // Flow sessions have no fixed end time; store a sentinel so BootReceiver
        // knows a session was active even though no specific end time is set.
        saveBootPrefs(endTimeMs = Long.MAX_VALUE)
        startForegroundCompat()
        startFlowCoroutine()
    }

    private fun handlePause() {
        Log.d(TAG, "handlePause remaining=${remainingSeconds}s elapsed=${elapsedSeconds}s")
        timerJob?.cancel()
        timerJob  = null
        isRunning = false
        updateNotification()
        if (isFlowMode) sendFlowTickBroadcast(isRunning = false)
        else sendTickBroadcast(isRunning = false)
    }

    private fun handleResume() {
        Log.d(TAG, "handleResume")
        if (isFlowMode) {
            isRunning = true
            startFlowCoroutine()
        } else {
            if (remainingSeconds <= 0) return
            isRunning = true
            startTimerCoroutine()
        }
    }

    private fun handleStop() {
        Log.d(TAG, "handleStop")
        timerJob?.cancel()
        timerJob         = null
        isRunning        = false
        remainingSeconds = 0
        isFlowMode       = false
        elapsedSeconds   = 0
        clearBootPrefs()
        sendTickBroadcast(isRunning = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleSkip() {
        Log.d(TAG, "handleSkip phase=$currentPhase")
        timerJob?.cancel()
        timerJob  = null
        isRunning = false
        sendFinishedBroadcast()
        serviceScope.launch(Dispatchers.Main) { onPhaseFinished() }
    }

    // ========================================================================
    // Coroutines
    // ========================================================================

    private fun startTimerCoroutine() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            updateNotification()
            while (remainingSeconds > 0 && isActive) {
                sendTickBroadcast(isRunning = true)
                updateWidget()
                delay(1_000L)
                remainingSeconds--
                updateNotification()
            }
            if (isActive) {
                isRunning = false
                sendTickBroadcast(isRunning = false)
                sendFinishedBroadcast()
                withContext(Dispatchers.Main) { onPhaseFinished() }
            }
        }
    }

    private fun startFlowCoroutine() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            updateNotification()
            while (isActive) {
                sendFlowTickBroadcast(isRunning = true)
                updateWidget()
                delay(1_000L)
                elapsedSeconds++
                updateNotification()
            }
        }
    }

    // ========================================================================
    // Phase completion
    // ========================================================================

    private fun onPhaseFinished() {
        Log.d(TAG, "onPhaseFinished phase=$currentPhase")
        TimerAlarmWorker.setCompleted(this)
        clearBootPrefs()
        vibrate()
        playSound()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ========================================================================
    // Broadcasts
    // ========================================================================

    private fun sendTickBroadcast(isRunning: Boolean) {
        val intent = Intent(BROADCAST_TICK).apply {
            putExtra(EXTRA_REMAINING_SECONDS, remainingSeconds)
            putExtra(EXTRA_IS_RUNNING, isRunning)
            putExtra(EXTRA_PHASE, currentPhase.name)
            putExtra(EXTRA_IS_FLOW_MODE, false)
        }
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun sendFlowTickBroadcast(isRunning: Boolean) {
        val intent = Intent(BROADCAST_TICK).apply {
            putExtra(EXTRA_ELAPSED_SECONDS, elapsedSeconds)
            putExtra(EXTRA_IS_RUNNING, isRunning)
            putExtra(EXTRA_PHASE, currentPhase.name)
            putExtra(EXTRA_IS_FLOW_MODE, true)
        }
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun sendFinishedBroadcast() {
        val intent = Intent(BROADCAST_FINISHED).apply {
            putExtra(EXTRA_PHASE, currentPhase.name)
        }
        localBroadcastManager.sendBroadcast(intent)
    }

    // ========================================================================
    // Widget
    // ========================================================================

    private fun updateWidget() {
        serviceScope.launch {
            try {
                val todayCount = sessionDao.observeTodayCount(todayStartMs()).first()
                val manager    = GlanceAppWidgetManager(this@TimerForegroundService)
                val glanceIds  = manager.getGlanceIds(FocusFirstWidget::class.java)

                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(
                        this@TimerForegroundService,
                        PreferencesGlanceStateDefinition,
                        glanceId,
                    ) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[WidgetKeys.REMAINING] =
                                if (isFlowMode) elapsedSeconds else remainingSeconds
                            this[WidgetKeys.PHASE]   =
                                if (isFlowMode) "FLOW" else currentPhase.displayName()
                            this[WidgetKeys.RUNNING]  = isRunning
                            this[WidgetKeys.TODAY]    = todayCount
                        }
                    }
                }
                FocusFirstWidget().updateAll(this@TimerForegroundService)
            } catch (e: Exception) {
                Log.w(TAG, "Widget update skipped: ${e.message}")
            }
        }
    }

    // ========================================================================
    // Boot persistence
    // ========================================================================

    private fun saveBootPrefs(endTimeMs: Long) {
        completionPrefs.edit()
            .putBoolean(BootReceiver.PREF_WAS_RUNNING, true)
            .putLong(BootReceiver.PREF_END_TIME_MS, endTimeMs)
            .putString(BootReceiver.PREF_PHASE, currentPhase.name)
            .apply()
    }

    private fun clearBootPrefs() {
        completionPrefs.edit()
            .putBoolean(BootReceiver.PREF_WAS_RUNNING, false)
            .putLong(BootReceiver.PREF_END_TIME_MS, 0L)
            .apply()
    }

    private fun todayStartMs(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE,      0)
        set(Calendar.SECOND,      0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // ========================================================================
    // Notification
    // ========================================================================

    private fun startForegroundCompat() {
        val notification = buildNotification()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else
                0,
        )
    }

    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val pauseResumePendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, TimerForegroundService::class.java).apply {
                action = if (isRunning) ACTION_PAUSE else ACTION_RESUME
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, TimerForegroundService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title   = if (isFlowMode) "Flow" else currentPhase.displayName()
        val content = if (isFlowMode) formatTime(elapsedSeconds) else formatTime(remainingSeconds)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, if (isRunning) "Pause" else "Resume", pauseResumePendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    // ========================================================================
    // Vibration / sound
    // ========================================================================

    private fun isVibrateEnabled(): Boolean =
        completionPrefs.getBoolean(PREF_VIBRATE_ENABLED, true)

    @Suppress("DEPRECATION")
    private fun vibrate() {
        if (!isVibrateEnabled()) return
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300), -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(effect)
        } else {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(effect)
        }
    }

    private fun playSound() {
        val type = completionPrefs.getString(PREF_SOUND_TYPE, "Bell") ?: "Bell"
        if (type.equals("None", ignoreCase = true)) return
        try {
            val uri      = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(this, uri)
            ringtone?.play()
        } catch (_: Exception) {
            Log.w(TAG, "playSound failed")
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private fun TimerPhase.displayName(): String = when (this) {
        TimerPhase.FOCUS       -> "Focus"
        TimerPhase.SHORT_BREAK -> "Short Break"
        TimerPhase.LONG_BREAK  -> "Long Break"
    }

    private fun formatTime(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}
