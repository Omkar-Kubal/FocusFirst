package com.focusfirst.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// ============================================================================
// BootReceiver — V1 stub
//
// Declared in AndroidManifest.xml to receive BOOT_COMPLETED.
// Full restart-on-boot logic (re-schedule active timers) comes in V2.
// ============================================================================

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // TODO V2: if a timer was active when the device was rebooted,
        //          re-schedule the alarm via TimerAlarmWorker.scheduleAlarm().
    }
}
