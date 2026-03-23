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
// ---------------------------------------------------------------------------

@AndroidEntryPoint
class TimerForegroundService : Service() {

    @Inject lateinit var sessionDao: SessionDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var timerJob: Job? = null

    private var remainingSeconds: Int = 0
    private var currentPhase: TimerPhase = TimerPhase.FOCUS
    private var isRunning: Boolean = false

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
        const val FOCUS_FIRST_PREFS = "focusfirst_prefs"
        const val PREF_VIBRATE_ENABLED = "vibrate_enabled"
        const val PREF_SOUND_TYPE = "sound_type"

        const val ACTION_START  = "com.focusfirst.ACTION_START"
        const val ACTION_PAUSE  = "com.focusfirst.ACTION_PAUSE"
        const val ACTION_RESUME = "com.focusfirst.ACTION_RESUME"
        const val ACTION_STOP   = "com.focusfirst.ACTION_STOP"
        const val ACTION_SKIP   = "com.focusfirst.ACTION_SKIP"

        const val BROADCAST_TICK     = "com.focusfirst.TICK"
        const val BROADCAST_FINISHED = "com.focusfirst.FINISHED"

        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"

        const val EXTRA_PHASE = "extra_phase"

        const val EXTRA_REMAINING_SECONDS = "extra_remaining_seconds"
        const val EXTRA_IS_RUNNING        = "extra_is_running"

        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID      = "focusfirst_timer"

        private const val TAG = "TimerForegroundService"

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

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "onDestroy — scope cancelled")
    }

    private fun handleStart(intent: Intent) {
        val durationSeconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, 25 * 60)
        val phaseName       = intent.getStringExtra(EXTRA_PHASE) ?: TimerPhase.FOCUS.name
        currentPhase    = TimerPhase.valueOf(phaseName)
        remainingSeconds = durationSeconds
        isRunning        = true

        Log.d(TAG, "handleStart phase=$currentPhase duration=${durationSeconds}s")

        startForegroundCompat()
        startTimerCoroutine()
    }

    private fun handlePause() {
        Log.d(TAG, "handlePause remaining=${remainingSeconds}s")
        timerJob?.cancel()
        timerJob   = null
        isRunning  = false
        updateNotification()
        sendTickBroadcast(isRunning = false)
    }

    private fun handleResume() {
        Log.d(TAG, "handleResume remaining=${remainingSeconds}s")
        if (remainingSeconds <= 0) return
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

    private fun handleSkip() {
        Log.d(TAG, "handleSkip phase=$currentPhase")
        timerJob?.cancel()
        timerJob  = null
        isRunning = false
        sendFinishedBroadcast()
        serviceScope.launch(Dispatchers.Main) { onPhaseFinished() }
    }

    private fun startTimerCoroutine() {
        timerJob?.cancel()
        var ticksSinceLastNotificationUpdate = 0

        timerJob = serviceScope.launch {
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

            if (isActive) {
                isRunning = false
                sendTickBroadcast(isRunning = false)
                sendFinishedBroadcast()
                withContext(Dispatchers.Main) { onPhaseFinished() }
            }
        }
    }

    private fun onPhaseFinished() {
        Log.d(TAG, "onPhaseFinished phase=$currentPhase")
        TimerAlarmWorker.setCompleted(this)
        vibrate()
        playSound()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

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

    private fun isVibrateEnabled(): Boolean =
        completionPrefs.getBoolean(PREF_VIBRATE_ENABLED, true)

    @Suppress("DEPRECATION")
    private fun vibrate() {
        if (!isVibrateEnabled()) return

        val waveform = longArrayOf(0, 300, 100, 300)
        val effect   = VibrationEffect.createWaveform(waveform, -1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(effect)
        } else {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(effect)
        }
    }

    private fun readSoundType(): String =
        completionPrefs.getString(PREF_SOUND_TYPE, "Bell") ?: "Bell"

    private fun playSound() {
        val type = readSoundType()
        if (type.equals("None", ignoreCase = true)) return

        try {
            val uri      = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(this, uri)
            ringtone?.play()
        } catch (_: Exception) {
            Log.w(TAG, "playSound failed")
        }
    }

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
