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

    @Inject lateinit var workerFactory: HiltWorkerFactory

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

            // ── Timer channel — silent live countdown notification ─────────────
            // IMPORTANCE_LOW: no sound, no heads-up, no vibration — intentional.
            // setShowBadge(false): a persistent timer notification must not add
            // a badge dot to the launcher icon.
            if (manager.getNotificationChannel(CHANNEL_TIMER) == null) {
                val timerChannel = NotificationChannel(
                    CHANNEL_TIMER,
                    "Toki Timer",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Shows the active focus or break timer"
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                }
                manager.createNotificationChannel(timerChannel)
            }

            // ── Alerts channel — session-end completion notification ───────────
            // IMPORTANCE_HIGH: plays sound + shows heads-up notification.
            // enableVibration(true): tactile feedback on session completion.
            if (manager.getNotificationChannel(CHANNEL_ALERTS) == null) {
                val alertsChannel = NotificationChannel(
                    CHANNEL_ALERTS,
                    "Session Alerts",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Plays a sound when a focus or break session ends"
                    enableVibration(true)
                }
                manager.createNotificationChannel(alertsChannel)
            }
        }
    }

    companion object {
        /** Live countdown notification — silent, persistent. */
        const val CHANNEL_TIMER  = "focusfirst_timer"
        /** Session-end alert — sound + vibration. */
        const val CHANNEL_ALERTS = "focusfirst_alerts"
    }
}
