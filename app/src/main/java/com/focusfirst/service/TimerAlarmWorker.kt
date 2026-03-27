package com.focusfirst.service

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.focusfirst.FocusFirstApp
import com.focusfirst.R
import com.focusfirst.data.model.TimerPhase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

// ============================================================================
// TimerAlarmWorker
//
// BACKUP alarm layer — fires a completion notification if the
// TimerForegroundService is killed by the Android OS or an OEM battery
// optimiser before the countdown reaches zero.
//
// Two-layer reliability model:
//   Layer 1 – TimerForegroundService: foreground-service coroutine with
//              START_STICKY, owns the live countdown.
//   Layer 2 – This worker: scheduled via WorkManager for durationSeconds
//              in the future.  If the service fires first it writes
//              KEY_TIMER_COMPLETED = true into SharedPreferences; the
//              worker reads this flag and skips the notification.
//
// ── HiltWorker wiring (required one-time setup) ───────────────────────────────
//
// 1. FocusFirstApp must implement Configuration.Provider:
//
//    @HiltAndroidApp
//    class FocusFirstApp : Application(), Configuration.Provider {
//        @Inject lateinit var workerFactory: HiltWorkerFactory
//        override val workManagerConfiguration: Configuration
//            get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
//    }
//
// 2. Remove the default WorkManager initializer from AndroidManifest.xml so
//    Hilt's factory is used instead:
//
//    <provider
//        android:name="androidx.startup.InitializationProvider"
//        android:authorities="${applicationId}.androidx-startup"
//        android:exported="false"
//        tools:node="merge">
//        <meta-data
//            android:name="androidx.work.WorkManagerInitializer"
//            android:value="androidx.startup.InitializationProvider"
//            tools:node="remove" />
//    </provider>
//
// ============================================================================

@HiltWorker
class TimerAlarmWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val durationSeconds = inputData.getInt(KEY_DURATION_SECONDS, 0)
        val phaseString     = inputData.getString(KEY_PHASE) ?: TimerPhase.FOCUS.name

        Log.d(TAG, "doWork: phase=$phaseString duration=${durationSeconds}s")

        // ── Guard: skip if ForegroundService already handled completion ────────
        // TimerForegroundService.onPhaseFinished() calls setCompleted() before
        // it stops itself.  If the flag is set, the service beat us here.
        val alreadyCompleted = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_TIMER_COMPLETED, false)

        if (alreadyCompleted) {
            Log.d(TAG, "doWork: ForegroundService already completed — skipping notification")
            return Result.success()
        }

        // ── Service was killed; post the fallback notification ────────────────
        val phase = runCatching { TimerPhase.valueOf(phaseString) }.getOrElse { TimerPhase.FOCUS }
        val contentText = when (phase) {
            TimerPhase.FOCUS                          -> "Time for a break!"
            TimerPhase.SHORT_BREAK, TimerPhase.LONG_BREAK -> "Back to focus!"
        }

        val notification = NotificationCompat.Builder(context, FocusFirstApp.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Toki — Session complete")
            .setContentText(contentText)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ALARM_NOTIFICATION_ID, notification)

        Log.d(TAG, "doWork: fallback notification posted")
        return Result.success()
    }

    // ========================================================================
    // Companion — constants + SharedPreference helpers
    // ========================================================================

    companion object {
        private const val TAG = "TimerAlarmWorker"

        // WorkManager unique work name — REPLACE policy ensures only one alarm
        // exists at a time even if start() is called mid-session.
        private const val WORK_NAME = "timer_alarm"

        // InputData keys
        const val KEY_DURATION_SECONDS = "duration_seconds"
        const val KEY_PHASE            = "phase"

        // Notification
        private const val ALARM_NOTIFICATION_ID = 1002

        // SharedPreferences
        private const val PREFS_NAME        = "focusfirst_prefs"
        const val KEY_TIMER_COMPLETED       = "timer_completed"

        /**
         * Called by [com.focusfirst.service.TimerForegroundService] inside
         * [com.focusfirst.service.TimerForegroundService.onPhaseFinished] when
         * the countdown naturally reaches zero.
         *
         * Setting this flag tells the worker that no fallback notification
         * is needed — the service handled completion successfully.
         *
         * Add to TimerForegroundService.onPhaseFinished():
         *   TimerAlarmWorker.setCompleted(this)
         */
        fun setCompleted(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_TIMER_COMPLETED, true)
                .apply()
            Log.d(TAG, "setCompleted: flag written")
        }

        /**
         * Called by [com.focusfirst.viewmodel.TimerViewModel.start] at the
         * beginning of every new session so the worker from the previous
         * session does not suppress this session's notification.
         *
         * Add to TimerViewModel.start() — LINE 1:
         *   TimerAlarmWorker.resetCompletionFlag(application)
         */
        fun resetCompletionFlag(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_TIMER_COMPLETED, false)
                .apply()
            Log.d(TAG, "resetCompletionFlag: flag cleared")
        }
    }
}

// ============================================================================
// Top-level scheduling helpers
// (called from TimerViewModel — top-level so they don't require a class ref)
// ============================================================================

/**
 * Schedules the backup alarm for [durationSeconds] seconds from now.
 *
 * Uses [ExistingWorkPolicy.REPLACE] so pausing + resuming never accumulates
 * stale work requests.
 *
 * Add to TimerViewModel.start() — LINE 2:
 *   scheduleAlarm(application, durationSeconds, TimerPhase.FOCUS)
 *
 * And to TimerViewModel.launchPhase() for break phases:
 *   scheduleAlarm(application, durationSeconds, phase)
 */
fun scheduleAlarm(context: Context, durationSeconds: Int, phase: TimerPhase) {
    val request = OneTimeWorkRequestBuilder<TimerAlarmWorker>()
        .setInputData(
            workDataOf(
                TimerAlarmWorker.KEY_DURATION_SECONDS to durationSeconds,
                TimerAlarmWorker.KEY_PHASE            to phase.name,
            )
        )
        .setInitialDelay(durationSeconds.toLong(), TimeUnit.SECONDS)
        .build()

    WorkManager.getInstance(context)
        .enqueueUniqueWork("timer_alarm", ExistingWorkPolicy.REPLACE, request)

    Log.d("TimerAlarmWorker", "scheduleAlarm: phase=$phase delay=${durationSeconds}s")
}

/**
 * Cancels any pending backup alarm.
 *
 * Call when the user explicitly stops the timer so no stale notification
 * fires later.
 *
 * Add to TimerViewModel.stop() — LINE 3:
 *   cancelAlarm(application)
 */
fun cancelAlarm(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork("timer_alarm")
    Log.d("TimerAlarmWorker", "cancelAlarm: work cancelled")
}
