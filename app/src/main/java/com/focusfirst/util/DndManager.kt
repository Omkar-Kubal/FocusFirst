package com.focusfirst.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

class DndManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)

    private val prefs = context.getSharedPreferences("focusfirst_prefs", Context.MODE_PRIVATE)

    // Persisted across process kills so DND is always restored to the correct filter
    private var previousFilter: Int
        get() = prefs.getInt("dnd_previous_filter", NotificationManager.INTERRUPTION_FILTER_ALL)
        set(value) { prefs.edit().putInt("dnd_previous_filter", value).apply() }

    fun isDndPermissionGranted(): Boolean =
        notificationManager.isNotificationPolicyAccessGranted

    fun requestDndPermission() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun enableDnd() {
        if (!isDndPermissionGranted()) return
        previousFilter = notificationManager.currentInterruptionFilter
        // INTERRUPTION_FILTER_PRIORITY allows alarms and high-priority calls through
        notificationManager.setInterruptionFilter(
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
        )
    }

    fun disableDnd() {
        if (!isDndPermissionGranted()) return
        notificationManager.setInterruptionFilter(previousFilter)
    }

    fun isCurrentlyInDnd(): Boolean =
        notificationManager.currentInterruptionFilter !=
            NotificationManager.INTERRUPTION_FILTER_ALL
}
