package com.focusfirst

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FocusFirstApp : Application(), Configuration.Provider {

    // Injected by Hilt; used to build a WorkManager configuration that knows
    // how to construct @HiltWorker classes (e.g. TimerAlarmWorker).
    @Inject lateinit var workerFactory: HiltWorkerFactory

    // WorkManager reads this property instead of auto-initializing, because the
    // default initializer is removed from AndroidManifest.xml (see provider block).
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Silent, persistent notification shown while the timer is running.
            // IMPORTANCE_LOW = no sound, no heads-up, no vibration — intentional.
            val timerChannel = NotificationChannel(
                CHANNEL_TIMER,
                "Focus Timer",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows the active focus or break timer"
                setSound(null, null)
                enableVibration(false)
            }

            // High-importance channel for session-end alerts (break / focus ready).
            // IMPORTANCE_HIGH = plays sound + shows heads-up notification.
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Session Alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Plays a sound when a focus or break session ends"
            }

            manager.createNotificationChannels(listOf(timerChannel, alertsChannel))
        }
    }

    companion object {
        const val CHANNEL_TIMER  = "focusfirst_timer"
        const val CHANNEL_ALERTS = "focusfirst_alerts"
    }
}
