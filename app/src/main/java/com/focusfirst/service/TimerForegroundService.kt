package com.focusfirst.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.focusfirst.MainActivity
import com.focusfirst.R
import com.focusfirst.data.db.SessionDao
import com.focusfirst.data.model.TimerPhase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// ---------------------------------------------------------------------------
// TimerForegroundService
//
// Two-layer reliability model:
//   Layer 1 – This ForegroundService owns the live countdown coroutine and
//              broadcasts second-by-second ticks to the UI.
//   Layer 2 – WorkManager (TimerSyncWorker, wired separately) acts as a
//              watchdog: if the process is killed it reschedules completion.
//
// NOTE: SessionDao is injected but not yet used in V1.  Wire it up when the
//       Room database layer (AppDatabase / SessionDao) is implemented.
// ---------------------------------------------------------------------------

@AndroidEntryPoint
class TimerForegroundService : Service() {

    // ── Hilt injection ──────────────────────────────────────────────────────
    @Inject lateinit var sessionDao: SessionDao

    // ── Coroutine scope ─────────────────────────────────────────────────────
    // SupervisorJob: child coroutine failures don't cancel the scope.
    // Dispatchers.IO: keeps the countdown off the main thread.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var timerJob: Job? = null

    // ── Mutable timer state ──────────────────────────────────────────────────
    private var remainingSeconds: Int = 0
    private var currentPhase: TimerPhase = TimerPhase.FOCUS
    private var isRunning: Boolean = false

    // ── Notification manager (cached) ────────────────────────────────────────
    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    // ── LocalBroadcastManager (cached) ──────────────────────────────────────
    private val localBroadcastManager: LocalBroadcastManager by lazy {
        LocalBroadcastManager.getInstance(this)
    }

    // ========================================================================
    // Companion object — all public constants + helper
    // ========================================================================

    companion object {
        // Actions (sent as intent.action to onStartCommand)
        const val ACTION_START  = "com.focusfirst.ACTION_START"
        const val ACTION_PAUSE  = "com.focusfirst.ACTION_PAUSE"
        const val ACTION_RESUME = "com.focusfirst.ACTION_RESUME"
        const val ACTION_STOP   = "com.focusfirst.ACTION_STOP"
        const val ACTION_SKIP   = "com.focusfirst.ACTION_SKIP"

        // Broadcast actions (received by UI via LocalBroadcastManager)
        const val BROADCAST_TICK     = "com.focusfirst.TICK"
        const val BROADCAST_FINISHED = "com.focusfirst.FINISHED"

        // Extras for ACTION_START
        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"

        // Shared extras (start input + broadcast output)
        const val EXTRA_PHASE = "extra_phase"

        // Extras carried in BROADCAST_TICK
        const val EXTRA_REMAINING_SECONDS = "extra_remaining_seconds"
        const val EXTRA_IS_RUNNING        = "extra_is_running"

        // Notification
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID      = "focusfirst_timer" // created in FocusFirstApp

        private const val TAG = "TimerForegroundService"

        /**
         * Convenience builder for the ACTION_START intent.
         *
         * Usage:
         *   val intent = TimerForegroundService.buildStartIntent(context, 25 * 60, TimerPhase.FOCUS)
         *   ContextCompat.startForegroundService(context, intent)
         */
        fun buildStartIntent(
            context: Context,
            durationSeconds: Int,
            phase: TimerPhase,
        ): Intent = Intent(context, TimerForegroundService::class.java).apply {
            action = ACTION_START
            putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            putExtra(EXTRA_PHASE, phase.name)
        }
    }

    // ========================================================================
    // Service lifecycle
    // ========================================================================

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

    /**
     * START_STICKY: if the OS kills the service, it will be restarted with a
     * null intent (no action).  The null branch in the when() below is a
     * safe no-op — WorkManager watchdog handles re-scheduling.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action=${intent?.action}")
        when (intent?.action) {
            ACTION_START  -> handleStart(intent)
            ACTION_PAUSE  -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_STOP   -> handleStop()
            ACTION_SKIP   -> handleSkip()
            null          -> Log.w(TAG, "Restarted with null intent (START_STICKY); waiting for next command")
        }
        return START_STICKY
    }

    /** Not a bound service. */
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "onDestroy — scope cancelled")
    }

    // ========================================================================
    // Action handlers
    // ========================================================================

    private fun handleStart(intent: Intent) {
        val durationSeconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, 25 * 60)
        val phaseName       = intent.getStringExtra(EXTRA_PHASE) ?: TimerPhase.FOCUS.name
        currentPhase    = TimerPhase.valueOf(phaseName)
        remainingSeconds = durationSeconds
        isRunning        = true

        Log.d(TAG, "handleStart phase=$currentPhase duration=${durationSeconds}s")

        // Must call startForeground() before anything else on API 26+.
        startForegroundCompat()
        startTimerCoroutine()
    }

    private fun handlePause() {
        Log.d(TAG, "handlePause remaining=${remainingSeconds}s")
        timerJob?.cancel()
        timerJob   = null
        isRunning  = false
        // Show "Resume" in the notification and let the UI know we've paused.
        updateNotification()
        sendTickBroadcast(isRunning = false)
    }

    private fun handleResume() {
        Log.d(TAG, "handleResume remaining=${remainingSeconds}s")
        if (remainingSeconds <= 0) return // Guard against spurious resume.
        isRunning = true
        startTimerCoroutine()
    }

    private fun handleStop() {
        Log.d(TAG, "handleStop")
        timerJob?.cancel()
        timerJob      = null
        isRunning     = false
        remainingSeconds = 0
        sendTickBroadcast(isRunning = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Skip: treat the current phase as immediately finished.
     * Plays completion feedback and stops the service.  The UI / ViewModel
     * is responsible for starting the next phase.
     */
    private fun handleSkip() {
        Log.d(TAG, "handleSkip phase=$currentPhase")
        timerJob?.cancel()
        timerJob  = null
        isRunning = false
        sendFinishedBroadcast()
        serviceScope.launch(Dispatchers.Main) { onPhaseFinished() }
    }

    // ========================================================================
    // Countdown coroutine
    // ========================================================================

    /**
     * Core timer loop.  Runs on Dispatchers.IO (serviceScope).
     *
     * Tick sequence per second:
     *   1. Broadcast current remainingSeconds to UI.
     *   2. Wait 1 000 ms.
     *   3. Decrement remainingSeconds.
     *   4. If a 30-second boundary has passed, refresh the notification.
     *
     * On natural completion (remainingSeconds reaches 0):
     *   • Send a final TICK with isRunning=false (so UI shows 00:00).
     *   • Send BROADCAST_FINISHED.
     *   • Call onPhaseFinished() on Main.
     */
    private fun startTimerCoroutine() {
        timerJob?.cancel()
        var ticksSinceLastNotificationUpdate = 0

        timerJob = serviceScope.launch {
            // Immediately reflect "Pause" button in the notification.
            updateNotification()

            while (remainingSeconds > 0 && isActive) {
                sendTickBroadcast(isRunning = true)
                delay(1_000L)
                remainingSeconds--
                ticksSinceLastNotificationUpdate++

                if (ticksSinceLastNotificationUpdate >= 30) {
                    updateNotification()
                    ticksSinceLastNotificationUpdate = 0
                }
            }

            // Only run completion logic if the coroutine was not externally cancelled
            // (handlePause / handleStop cancel the job before reaching here).
            if (isActive) {
                isRunning = false
                sendTickBroadcast(isRunning = false)   // UI shows 00:00
                sendFinishedBroadcast()
                withContext(Dispatchers.Main) { onPhaseFinished() }
            }
        }
    }

    // ========================================================================
    // Completion callbacks
    // ========================================================================

    /**
     * Called on Main thread when a phase completes naturally or is skipped.
     * Plays haptic + audio feedback, then tears down the foreground service.
     */
    private fun onPhaseFinished() {
        Log.d(TAG, "onPhaseFinished phase=$currentPhase")
        TimerAlarmWorker.setCompleted(this)
        vibrate()
        playSound()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ========================================================================
    // Broadcasts (via LocalBroadcastManager)
    // ========================================================================

    private fun sendTickBroadcast(isRunning: Boolean) {
        val intent = Intent(BROADCAST_TICK).apply {
            putExtra(EXTRA_REMAINING_SECONDS, remainingSeconds)
            putExtra(EXTRA_IS_RUNNING, isRunning)
            putExtra(EXTRA_PHASE, currentPhase.name)
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
    // Notification helpers
    // ========================================================================

    /**
     * Calls the correct startForeground() overload.
     * API 34+ (UPSIDE_DOWN_CAKE) requires the foreground service type to be
     * declared explicitly in code to match the manifest's foregroundServiceType.
     */
    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /** Posts an updated notification to the system — thread-safe. */
    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    /**
     * Builds the persistent timer notification.
     *
     * Pause/Resume action: label toggles based on [isRunning].
     * Stop action: always present.
     * Content tap: opens MainActivity.
     *
     * FOREGROUND_SERVICE_IMMEDIATE ensures Android 12+ does not delay the
     * initial display (critical for a user-visible timer).
     */
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

        val pauseResumeLabel = if (isRunning) "Pause" else "Resume"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(currentPhase.displayName())
            .setContentText(formatTime(remainingSeconds))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, pauseResumeLabel, pauseResumePendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    // ========================================================================
    // Haptic + audio feedback on completion
    // ========================================================================

    /**
     * Pattern: silence → 300 ms buzz → 100 ms pause → 300 ms buzz.
     * -1 = do not repeat.
     *
     * Uses VibratorManager on API 31+; falls back to the deprecated Vibrator
     * service on API 26–30.  VibrationEffect is safe on all supported API
     * levels since minSdk = 26.
     */
    @Suppress("DEPRECATION")
    private fun vibrate() {
        val waveform = longArrayOf(0, 300, 100, 300)
        val effect   = VibrationEffect.createWaveform(waveform, /* repeat= */ -1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(effect)
        } else {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(effect)
        }
    }

    /**
     * Plays the default notification sound once.
     * Uses the system notification ringtone so the user's own sound preference
     * is respected automatically.
     */
    private fun playSound() {
        val uri      = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(this, uri)
        ringtone?.play()
    }

    // ========================================================================
    // Utility
    // ========================================================================

    private fun TimerPhase.displayName(): String = when (this) {
        TimerPhase.FOCUS       -> "Focus"
        TimerPhase.SHORT_BREAK -> "Short Break"
        TimerPhase.LONG_BREAK  -> "Long Break"
    }

    /** Formats a raw second count as "MM:SS". */
    private fun formatTime(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}

// ============================================================================
// BootReceiver
//
// Declared in AndroidManifest with RECEIVE_BOOT_COMPLETED permission.
// V1: logs receipt only.
// V2: will restore an in-progress session from DataStore / WorkManager.
// ============================================================================

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot received — timer restore logic comes in V2")
        }
    }
}
