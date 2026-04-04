package com.focusfirst.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.focusfirst.FocusFirstApp
import com.focusfirst.R
import com.focusfirst.data.model.TimerPhase
import com.focusfirst.service.TimerAlarmWorker
import java.util.concurrent.TimeUnit

// ============================================================================
// BootReceiver
//
// Fires on BOOT_COMPLETED.  If a timer was active when the device rebooted:
//   • Still has time remaining → reschedule the WorkManager alarm watchdog
//     so the user still gets a completion notification.
//   • Completed during reboot → post a "missed session" notification.
// ============================================================================

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "BOOT_COMPLETED received")

        val prefs      = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val wasRunning = prefs.getBoolean(PREF_WAS_RUNNING, false)
        val endTimeMs  = prefs.getLong(PREF_END_TIME_MS, 0L)
        val phaseName  = prefs.getString(PREF_PHASE, TimerPhase.FOCUS.name) ?: TimerPhase.FOCUS.name

        if (!wasRunning || endTimeMs == 0L) {
            Log.d(TAG, "No active session before reboot — nothing to restore")
            return
        }

        val now          = System.currentTimeMillis()
        val phase        = runCatching { TimerPhase.valueOf(phaseName) }.getOrDefault(TimerPhase.FOCUS)

        if (endTimeMs > now) {
            // Timer still has time remaining — reschedule the alarm watchdog
            val remainingMs = endTimeMs - now
            Log.d(TAG, "Rescheduling alarm: phase=$phase remaining=${remainingMs / 1000}s")
            scheduleAlarm(context, (remainingMs / 1000L).toInt(), phase)
        } else {
            // Session finished while device was rebooting — notify the user
            Log.d(TAG, "Session completed during reboot — showing missed notification")
            showMissedSessionNotification(context)
            prefs.edit()
                .putBoolean(PREF_WAS_RUNNING, false)
                .putLong(PREF_END_TIME_MS, 0L)
                .apply()
        }
    }

    // ========================================================================
    // Private helpers
    // ========================================================================

    private fun scheduleAlarm(context: Context, durationSeconds: Int, phase: TimerPhase) {
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
    }

    private fun showMissedSessionNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, FocusFirstApp.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Toki — Session complete")
            .setContentText("Your focus session finished while your phone was restarting")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(MISSED_SESSION_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "BootReceiver"

        const val PREFS_NAME  = "focusfirst_prefs"
        const val PREF_WAS_RUNNING = "timer_was_running"
        const val PREF_END_TIME_MS = "timer_end_time_ms"
        const val PREF_PHASE       = "timer_phase"

        private const val MISSED_SESSION_NOTIFICATION_ID = 9999
    }
}
