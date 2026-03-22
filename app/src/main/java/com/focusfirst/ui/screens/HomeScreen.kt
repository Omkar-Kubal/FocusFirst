package com.focusfirst.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.data.model.IntervalPreset
import com.focusfirst.data.model.TimerPhase
import com.focusfirst.ui.components.TimerRing
import com.focusfirst.ui.theme.FocusColors
import com.focusfirst.ui.theme.ringColor
import com.focusfirst.viewmodel.TimerViewModel

// ============================================================================
// HomeScreen
// ============================================================================

@Composable
fun HomeScreen(
    viewModel: TimerViewModel = hiltViewModel(),
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    // ── State ─────────────────────────────────────────────────────────────────
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val todayCount by viewModel.todayCount.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val powerManager = remember {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    var showStopDialog  by remember { mutableStateOf(false) }
    var bannerDismissed by rememberSaveable { mutableStateOf(false) }

    // Battery opt check — computed once per composition session.
    // V2 improvement: re-check on Activity.onResume via LifecycleEventObserver.
    val isIgnoringBatteryOpts by remember {
        mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }
    val showBatteryBanner = !isIgnoringBatteryOpts && !bannerDismissed

    // ── Derived display values ────────────────────────────────────────────────
    val isIdle    = !timerState.isRunning && !timerState.isPaused
    val ringColor = timerState.phase.ringColor()

    val fabIcon = if (timerState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow
    val fabLabel = when {
        timerState.isRunning -> "Pause"
        timerState.isPaused  -> "Resume"
        else                 -> "Start"
    }
    val fabAction: () -> Unit = when {
        timerState.isRunning -> ({ viewModel.pause() })
        timerState.isPaused  -> ({ viewModel.resume() })
        else                 -> ({ viewModel.start() })
    }

    val phaseHint: String = when {
        timerState.isPaused  -> "Paused — tap to resume"
        timerState.isRunning -> when (timerState.phase) {
            TimerPhase.FOCUS       -> "Stay focused!"
            TimerPhase.SHORT_BREAK -> "Take a breather"
            TimerPhase.LONG_BREAK  -> "Good work, rest up"
        }
        else -> "Ready to focus?"
    }

    // ── Stop confirmation dialog ──────────────────────────────────────────────
    if (showStopDialog) {
        StopConfirmationDialog(
            onConfirm = {
                viewModel.stop()
                showStopDialog = false
            },
            onDismiss = { showStopDialog = false },
        )
    }

    // =========================================================================
    // Root layout
    // =========================================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {

        // ── 1. Top app bar ────────────────────────────────────────────────────
        TopBar(
            todayCount         = todayCount,
            onNavigateToStats  = onNavigateToStats,
            onNavigateToSettings = onNavigateToSettings,
        )

        // ── 2. Preset selector ────────────────────────────────────────────────
        PresetSelector(
            selectedPreset = timerState.preset,
            isRunning      = timerState.isRunning,
            onSelect       = viewModel::selectPreset,
        )

        // ── 3. Timer ring — weighted so it fills remaining vertical space ─────
        Box(
            modifier         = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            TimerRing(timerState = timerState, size = 280.dp)
        }

        // ── 4. Session dots (progress toward long break) ──────────────────────
        SessionDots(
            filledCount = timerState.sessionsCompleted % 4,
            ringColor   = ringColor,
        )

        // ── 5. Phase hint text ────────────────────────────────────────────────
        // AnimatedContent provides a smooth crossfade between hint strings
        // whenever the timer phase or run/pause state changes.
        AnimatedContent(
            targetState  = phaseHint,
            label        = "phaseHint",
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { hint ->
            Text(
                text      = hint,
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth(),
            )
        }

        // ── 6. Battery optimization banner ────────────────────────────────────
        AnimatedVisibility(visible = showBatteryBanner) {
            BatteryOptimizationBanner(
                context   = context,
                onDismiss = { bannerDismissed = true },
            )
        }

        // ── 7. Timer controls ─────────────────────────────────────────────────
        TimerControls(
            isIdle           = isIdle,
            fabIcon          = fabIcon,
            fabLabel         = fabLabel,
            fabAction        = fabAction,
            onStopRequested  = { showStopDialog = true },
        )
    }
}

// ============================================================================
// Sub-composables
// ============================================================================

// ── Top app bar ──────────────────────────────────────────────────────────────

@Composable
private fun TopBar(
    todayCount: Int,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Row(
        modifier             = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment    = Alignment.CenterVertically,
    ) {
        Text(
            text  = "FocusFirst",
            style = MaterialTheme.typography.titleLarge,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Today-count badge — shows how many sessions completed today
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text     = "🍅 $todayCount",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style    = MaterialTheme.typography.labelMedium,
                )
            }

            IconButton(onClick = onNavigateToStats) {
                Icon(
                    imageVector        = Icons.Outlined.BarChart,
                    contentDescription = "Stats",
                )
            }

            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector        = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                )
            }
        }
    }
}

// ── Preset selector ──────────────────────────────────────────────────────────

@Composable
private fun PresetSelector(
    selectedPreset: IntervalPreset,
    isRunning: Boolean,
    onSelect: (IntervalPreset) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding        = PaddingValues(horizontal = 0.dp),
    ) {
        items(IntervalPreset.entries.toList(), key = { it.name }) { preset ->
            FilterChip(
                selected = selectedPreset == preset,
                onClick  = { onSelect(preset) },
                enabled  = !isRunning,
                label    = { Text("${preset.label} ${preset.focusMinutes}m") },
            )
        }
    }
}

// ── Session dots ─────────────────────────────────────────────────────────────

/**
 * Row of 4 small circles that fill as the user completes focus sessions,
 * resetting every [sessionsBeforeLongBreak] (4) sessions.
 *
 * [filledCount] = `sessionsCompleted % 4` — 0..3 dots filled.
 */
@Composable
private fun SessionDots(
    filledCount: Int,
    ringColor: Color,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { index ->
                Canvas(modifier = Modifier.size(10.dp)) {
                    if (index < filledCount) {
                        // Filled: solid phase colour
                        drawCircle(color = ringColor)
                    } else {
                        // Outlined: phase colour at reduced opacity
                        drawCircle(
                            color = ringColor.copy(alpha = 0.4f),
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                    }
                }
            }
        }
    }
}

// ── Battery optimization banner ───────────────────────────────────────────────

/**
 * Dismissible warning card surfaced on OEM devices (Xiaomi, Realme, OnePlus)
 * known for aggressive battery management that can kill background services.
 *
 * Tapping the text opens the system battery-optimization exemption screen.
 * Tapping ✕ hides the banner for the remainder of the session ([rememberSaveable]
 * in the caller persists the dismissed flag across config changes).
 */
@Composable
private fun BatteryOptimizationBanner(
    context: Context,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text     = "For reliable timer on Xiaomi/Realme/OnePlus, tap to fix battery settings",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        } catch (_: Exception) {
                            // Fallback for OEMs that don't handle the specific action
                            context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
                        }
                    },
            )

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector        = Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint               = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

// ── Timer controls ────────────────────────────────────────────────────────────

/**
 * Bottom control row: Stop button (slides in when active) + primary FAB.
 *
 * The FAB is always centered in the row.  The Stop button appears to its left
 * via [AnimatedVisibility]; since it has a fixed size, the FAB shifts slightly
 * right when it appears — acceptable for V1.  A Box-based layout with
 * `Modifier.align(Alignment.Center)` on the FAB would keep it stationary
 * if that visual stability becomes a priority in V2.
 */
@Composable
private fun TimerControls(
    isIdle: Boolean,
    fabIcon: androidx.compose.ui.graphics.vector.ImageVector,
    fabLabel: String,
    fabAction: () -> Unit,
    onStopRequested: () -> Unit,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp, top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        // Stop button — only visible while a session is running or paused
        AnimatedVisibility(visible = !isIdle) {
            OutlinedButton(
                onClick  = onStopRequested,
                modifier = Modifier.padding(end = 16.dp),
            ) {
                Icon(
                    imageVector        = Icons.Filled.Stop,
                    contentDescription = "Stop",
                    modifier           = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Stop")
            }
        }

        // Primary FAB — Start / Pause / Resume
        LargeFloatingActionButton(
            onClick        = fabAction,
            containerColor = FocusColors.TomatoRed,
            contentColor   = Color.White,
        ) {
            Icon(
                imageVector        = fabIcon,
                contentDescription = fabLabel,
                modifier           = Modifier.size(36.dp),
            )
        }
    }
}

// ── Stop confirmation dialog ──────────────────────────────────────────────────

@Composable
private fun StopConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Stop session?") },
        text             = { Text("Your progress will be saved.") },
        confirmButton    = {
            TextButton(onClick = onConfirm) {
                Text("Stop")
            }
        },
        dismissButton    = {
            TextButton(onClick = onDismiss) {
                Text("Keep going")
            }
        },
    )
}
