package com.focusfirst.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.viewmodel.SettingsViewModel

// ============================================================================
// Helpers
// ============================================================================

/**
 * Returns true when FocusFirst is already on the system's battery-optimisation
 * whitelist and the OS will not restrict background execution.
 */
fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

/**
 * Opens the most direct battery-optimisation settings page available on the
 * current device.
 *
 * Xiaomi/MIUI and Realme/OPPO ship proprietary battery-manager UIs that must
 * be opened via vendor Intents.  Every branch has a fallback to the standard
 * AOSP screen in case the vendor Intent cannot be resolved or the Activity
 * is missing.  Every [startActivity] call is wrapped so a missing component
 * never crashes the app.
 */
fun openBatterySettings(context: Context) {
    val manufacturer = Build.MANUFACTURER.lowercase()

    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun tryStart(intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    when {
        manufacturer.contains("xiaomi") -> {
            val miui = Intent("miui.intent.action.POWER_HIDE_MODE_APP_LIST").apply {
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            if (!tryStart(miui)) tryStart(fallback)
        }

        manufacturer.contains("realme") || manufacturer.contains("oppo") -> {
            val oppoIntent = runCatching {
                context.packageManager.getLaunchIntentForPackage("com.coloros.oppoguardelf")
            }.getOrNull()
            if (oppoIntent != null) {
                if (!tryStart(oppoIntent)) tryStart(fallback)
            } else {
                tryStart(fallback)
            }
        }

        else -> tryStart(fallback)
    }
}

// ============================================================================
// Composable dialog
// ============================================================================

/**
 * Modal alert that asks the user to disable battery optimisation for FocusFirst.
 *
 * Visibility is driven internally: the dialog only appears when
 *   1. FocusFirst is NOT already on the battery-optimisation whitelist, AND
 *   2. The user has not previously dismissed this prompt.
 *
 * Both confirmation and dismissal write [dismissed = true] to [SettingsViewModel]
 * so the dialog is never shown again after the user interacts with it.
 */
@Composable
fun BatteryPromptDialog(
    settingsViewModel: SettingsViewModel,
    context: Context,
) {
    val dismissed by settingsViewModel.batteryPromptDismissed.collectAsStateWithLifecycle()

    if (!dismissed && !isIgnoringBatteryOptimizations(context)) {
        AlertDialog(
            onDismissRequest = {
                settingsViewModel.updateBatteryPromptDismissed(true)
            },
            title = {
                Text("Enable reliable timer")
            },
            text = {
                Text(
                    "On Xiaomi, Realme and OnePlus, background restrictions " +
                    "can stop your timer. Tap Fix Now to allow Toki " +
                    "to run in the background."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openBatterySettings(context)
                        settingsViewModel.updateBatteryPromptDismissed(true)
                    },
                ) {
                    Text("Fix now")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.updateBatteryPromptDismissed(true)
                    },
                ) {
                    Text("Maybe later")
                }
            },
        )
    }
}
