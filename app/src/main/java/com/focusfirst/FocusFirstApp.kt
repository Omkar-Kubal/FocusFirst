package com.focusfirst

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FocusFirstApp : Application() {

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
