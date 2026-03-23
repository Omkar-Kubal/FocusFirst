package com.focusfirst.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.data.model.IntervalPreset
import com.focusfirst.data.model.TimerPhase
import com.focusfirst.data.model.TimerState
import com.focusfirst.ui.theme.FocusColors
import com.focusfirst.util.BatteryPromptDialog
import com.focusfirst.viewmodel.SettingsViewModel
import com.focusfirst.viewmodel.TimerViewModel

// ============================================================================
// HomeScreen
// ============================================================================

@Composable
fun HomeScreen(
    viewModel:            TimerViewModel   = hiltViewModel(),
    settingsViewModel:    SettingsViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit        = {},
) {
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val todayCount by viewModel.todayCount.collectAsStateWithLifecycle()

    val context = LocalContext.current

    var showStopDialog by rememberSaveable { mutableStateOf(false) }

    // ── Stop confirmation dialog ───────────────────────────────────────────
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Stop session?") },
            text  = {
                Text("Your progress will be saved if you've focused for 30+ seconds.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.stop()
                    showStopDialog = false
                }) {
                    Text("Stop")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Keep going")
                }
            },
        )
    }

    // ── Battery prompt (shown once on first launch) ────────────────────────
    BatteryPromptDialog(
        settingsViewModel = settingsViewModel,
        context           = context,
    )

    // ── Screen layout ──────────────────────────────────────────────────────
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .background(FocusColors.Background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        // 1. Glass top bar
        HomeTopBar(onSettingsClick = onNavigateToSettings)

        // 2. Preset pill row
        PresetPillRow(
            activePreset = timerState.preset,
            isRunning    = timerState.isRunning,
            onSelect     = { viewModel.selectPreset(it) },
        )

        // 3. Timer ring — fills remaining vertical space
        Box(
            modifier         = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            VoidTimerRing(timerState = timerState)
        }

        Spacer(Modifier.height(24.dp))

        // 4. Primary FAB
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

        Surface(
            modifier        = Modifier.size(80.dp),
            shape           = CircleShape,
            color           = Color.White,
            onClick         = fabAction,
            shadowElevation = 8.dp,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier.fillMaxSize(),
            ) {
                Icon(
                    imageVector        = fabIcon,
                    contentDescription = fabLabel,
                    tint               = Color.Black,
                    modifier           = Modifier.size(36.dp),
                )
            }
        }

        // 5. Stop button — visible only when timer is not idle
        AnimatedVisibility(visible = !timerState.isIdle) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(12.dp))
                IconButton(onClick = { showStopDialog = true }) {
                    Icon(
                        imageVector        = Icons.Outlined.Stop,
                        contentDescription = "Stop session",
                        tint               = Color.White.copy(alpha = 0.7f),
                        modifier           = Modifier.size(28.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 6. Bento stats row
        BentoStatsRow(
            todayCount = todayCount,
            modifier   = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(16.dp))
    }
}

// ============================================================================
// Sub-composables
// ============================================================================

// ── Glass top bar ─────────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar(onSettingsClick: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector        = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint               = Color.White.copy(alpha = 0.7f),
            )
        }

        Text(
            text       = "Focus",
            fontSize   = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color.White,
        )

        IconButton(onClick = { /* reserved */ }) {
            Icon(
                imageVector        = Icons.Filled.MoreHoriz,
                contentDescription = "More",
                tint               = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

// ── Preset pill row ───────────────────────────────────────────────────────────

@Composable
private fun PresetPillRow(
    activePreset: IntervalPreset,
    isRunning:    Boolean,
    onSelect:     (IntervalPreset) -> Unit,
) {
    LazyRow(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(IntervalPreset.entries) { preset ->
            val isSelected = preset == activePreset

            Box(
                modifier = Modifier
                    .alpha(if (isRunning && !isSelected) 0.4f else 1f)
                    .background(
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(50.dp),
                    )
                    .clickable(enabled = !isRunning) { onSelect(preset) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = "Focus ${preset.focusMinutes}m",
                    fontSize   = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (isSelected) Color.Black else Color.White.copy(alpha = 0.6f),
                )
            }
        }
    }
}

// ── Void timer ring ───────────────────────────────────────────────────────────

@Composable
private fun VoidTimerRing(timerState: TimerState) {
    val animatedProgress by animateFloatAsState(
        targetValue   = timerState.progress,
        animationSpec = tween(durationMillis = 800),
        label         = "voidProgress",
    )

    val phaseLabel = when (timerState.phase) {
        TimerPhase.FOCUS       -> "FOCUS"
        TimerPhase.SHORT_BREAK -> "SHORT BREAK"
        TimerPhase.LONG_BREAK  -> "LONG BREAK"
    }

    Box(
        modifier         = Modifier.size(280.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val strokePx = 3.dp.toPx()
            val padPx    = 8.dp.toPx()
            val radius   = size.width / 2f - padPx

            val arcTopLeft = Offset(
                x = size.width  / 2f - radius,
                y = size.height / 2f - radius,
            )
            val arcSize = Size(radius * 2f, radius * 2f)

            // Background track — full 360° at low opacity
            drawArc(
                color      = Color.White.copy(alpha = 0.1f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter  = false,
                topLeft    = arcTopLeft,
                size       = arcSize,
                style      = Stroke(strokePx),
            )

            // Progress arc — sweeps clockwise from 12 o'clock
            if (animatedProgress > 0f) {
                drawArc(
                    color      = Color.White,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter  = false,
                    topLeft    = arcTopLeft,
                    size       = arcSize,
                    style      = Stroke(strokePx, cap = StrokeCap.Round),
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text          = timerState.displayTime,
                fontSize      = 80.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = (-1.6).sp,
                color         = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text          = phaseLabel,
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 2.sp,
                color         = Color.White.copy(alpha = 0.4f),
            )
        }
    }
}

// ── Bento stats row ───────────────────────────────────────────────────────────

private val BentoShape  = RoundedCornerShape(16.dp)
private val BentoBg     = FocusColors.SurfaceContainerLow
private val BentoBorder = Color.White.copy(alpha = 0.08f)

@Composable
private fun BentoStatsRow(
    todayCount: Int,
    modifier:   Modifier = Modifier,
) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        // Daily goal card
        Column(
            modifier = Modifier
                .weight(1f)
                .background(BentoBg, BentoShape)
                .border(1.dp, BentoBorder, BentoShape)
                .padding(16.dp),
        ) {
            Text(
                text          = "DAILY GOAL",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                color         = Color.White.copy(alpha = 0.4f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text       = "$todayCount / 8",
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
            )
        }

        // Streak card — TODO: derive from weeklySummary in V2
        Column(
            modifier = Modifier
                .weight(1f)
                .background(BentoBg, BentoShape)
                .border(1.dp, BentoBorder, BentoShape)
                .padding(16.dp),
        ) {
            Text(
                text          = "STREAK",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                color         = Color.White.copy(alpha = 0.4f),
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = "0",
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text     = "\uD83D\uDD25",
                    fontSize = 18.sp,
                )
            }
        }
    }
}
