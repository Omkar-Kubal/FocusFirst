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
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.focusfirst.ui.theme.LocalFocusDarkTheme
import com.focusfirst.ui.theme.ringColor
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
    val timerState  by viewModel.timerState.collectAsStateWithLifecycle()
    val todayCount  by viewModel.todayCount.collectAsStateWithLifecycle()
    val streakDays  by viewModel.streakDays.collectAsStateWithLifecycle()
    val dailyGoal   by settingsViewModel.dailyGoal.collectAsStateWithLifecycle()

    val context = LocalContext.current

    var showStopDialog by rememberSaveable { mutableStateOf(false) }

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

    BatteryPromptDialog(
        settingsViewModel = settingsViewModel,
        context           = context,
    )

    val scheme = MaterialTheme.colorScheme

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .background(scheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        HomeTopBar(
            onSettingsClick = onNavigateToSettings,
        )

        PresetPillRow(
            activePreset = timerState.preset,
            isRunning    = timerState.isRunning,
            onSelect     = { viewModel.selectPreset(it) },
        )

        Box(
            modifier         = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            VoidTimerRing(timerState = timerState)
        }

        Spacer(Modifier.height(24.dp))

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
            color           = scheme.primary,
            contentColor    = scheme.onPrimary,
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
                    tint               = scheme.onPrimary,
                    modifier           = Modifier.size(36.dp),
                )
            }
        }

        AnimatedVisibility(visible = !timerState.isIdle) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(12.dp))
                IconButton(onClick = { showStopDialog = true }) {
                    Icon(
                        imageVector        = Icons.Outlined.Stop,
                        contentDescription = "Stop session",
                        tint               = scheme.onSurface.copy(alpha = 0.7f),
                        modifier           = Modifier.size(28.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        BentoStatsRow(
            todayCount = todayCount,
            dailyGoal  = dailyGoal,
            streakDays = streakDays,
            modifier   = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(16.dp))
    }
}

// ============================================================================
// Sub-composables
// ============================================================================

@Composable
private fun HomeTopBar(onSettingsClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark   = LocalFocusDarkTheme.current
    val barBg  = if (dark) Color.White.copy(alpha = 0.08f) else scheme.surfaceVariant.copy(alpha = 0.5f)

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(barBg)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text       = "Toki",
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
            modifier   = Modifier.padding(start = 4.dp),
        )

        IconButton(onClick = { /* reserved */ }) {
            Icon(
                imageVector        = Icons.Filled.MoreHoriz,
                contentDescription = "More",
                tint               = scheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun PresetPillRow(
    activePreset: IntervalPreset,
    isRunning:    Boolean,
    onSelect:     (IntervalPreset) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dark   = LocalFocusDarkTheme.current

    LazyRow(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(IntervalPreset.entries) { preset ->
            val isSelected = preset == activePreset

            val (bg, fg) = when {
                isSelected && !dark ->
                    scheme.primary to scheme.onPrimary

                isSelected && dark ->
                    Color.White to Color.Black

                !isSelected && !dark ->
                    FocusColors.LightPillIdle to scheme.onSurface

                else ->
                    Color.White.copy(alpha = 0.1f) to Color.White.copy(alpha = 0.6f)
            }

            Box(
                modifier = Modifier
                    .alpha(if (isRunning && !isSelected) 0.4f else 1f)
                    .background(
                        color = bg,
                        shape = RoundedCornerShape(50.dp),
                    )
                    .clickable(enabled = !isRunning) { onSelect(preset) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = "${preset.focusMinutes}m",
                    fontSize   = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color      = fg,
                )
            }
        }
    }
}

@Composable
private fun VoidTimerRing(timerState: TimerState) {
    val animatedProgress by animateFloatAsState(
        targetValue   = timerState.progress,
        animationSpec = tween(durationMillis = 800),
        label         = "voidProgress",
    )

    val ringAccent = timerState.phase.ringColor()
    val scheme     = MaterialTheme.colorScheme
    val trackColor = ringAccent.copy(alpha = 0.12f)

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

            drawArc(
                color      = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter  = false,
                topLeft    = arcTopLeft,
                size       = arcSize,
                style      = Stroke(strokePx),
            )

            if (animatedProgress > 0f) {
                drawArc(
                    color      = ringAccent,
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
                color         = scheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text          = phaseLabel,
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 2.sp,
                color         = scheme.onSurfaceVariant,
            )
        }
    }
}

private val BentoShape = RoundedCornerShape(16.dp)

@Composable
private fun BentoStatsRow(
    todayCount: Int,
    dailyGoal:  Int,
    streakDays: Int,
    modifier:   Modifier = Modifier,
) {
    val scheme  = MaterialTheme.colorScheme
    val dark    = LocalFocusDarkTheme.current
    val cardBg  = if (dark) FocusColors.SurfaceContainerLow else scheme.surface
    val border  = if (dark) Color.White.copy(alpha = 0.08f) else scheme.outlineVariant.copy(alpha = 0.4f)
    val goalDen = dailyGoal.coerceAtLeast(1)
    val progress = (todayCount / goalDen.toFloat()).coerceIn(0f, 1f)

    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        // ── Daily goal card ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .background(cardBg, BentoShape)
                .border(1.dp, border, BentoShape)
                .padding(16.dp),
        ) {
            Text(
                text          = "DAILY GOAL",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                color         = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text       = "$todayCount / $goalDen",
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                color      = scheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        color = scheme.surfaceVariant.copy(alpha = if (dark) 0.35f else 0.6f),
                        shape = RoundedCornerShape(2.dp),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress)
                        .height(4.dp)
                        .background(scheme.primary, RoundedCornerShape(2.dp)),
                )
            }
        }

        // ── Streak card ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .background(cardBg, BentoShape)
                .border(1.dp, border, BentoShape)
                .padding(16.dp),
        ) {
            Text(
                text          = "STREAK",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                color         = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = if (streakDays == 0) "—" else "$streakDays",
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color      = scheme.onSurface,
                )
                if (streakDays >= 7) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text     = "\uD83D\uDD25",   // 🔥
                        fontSize = 18.sp,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text     = "day streak",
                fontSize = 11.sp,
                color    = scheme.onSurfaceVariant,
            )
        }
    }
}
