package com.focusfirst.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.data.model.TimerPhase
import com.focusfirst.data.model.TimerState
import com.focusfirst.ui.theme.FocusColors
import com.focusfirst.viewmodel.TimerViewModel

// ============================================================================
// HomeScreen — "The Focused Void" redesign
// ============================================================================

@Composable
fun HomeScreen(
    viewModel:           TimerViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit    = {},
) {
    val timerState    by viewModel.timerState.collectAsStateWithLifecycle()
    val todayCount    by viewModel.todayCount.collectAsStateWithLifecycle()
    val weeklySummary by viewModel.weeklySummary.collectAsStateWithLifecycle()

    // Streak = consecutive days (ending today) with at least one session,
    // computed from the 7-day window already held in weeklySummary.
    val streakDays = remember(weeklySummary) {
        val todayEpoch = System.currentTimeMillis() / 86_400_000L
        var count = 0
        for (daysBack in 0..6) {
            if (weeklySummary.any { it.date == todayEpoch - daysBack && it.sessionCount > 0 }) {
                count++
            } else {
                break
            }
        }
        count
    }

    val isRunning = timerState.isRunning
    val isPaused  = timerState.isPaused

    val fabIcon  = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow
    val fabLabel = when {
        isRunning -> "Pause"
        isPaused  -> "Resume"
        else      -> "Start"
    }
    val fabAction: () -> Unit = when {
        isRunning -> ({ viewModel.pause() })
        isPaused  -> ({ viewModel.resume() })
        else      -> ({ viewModel.start() })
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .background(FocusColors.Background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        // ── 1. Glass top bar ──────────────────────────────────────────────────
        HomeTopBar(onSettingsClick = onNavigateToSettings)

        // ── 2. Phase pill selector ────────────────────────────────────────────
        PhasePillRow(activePhase = timerState.phase)

        // ── 3. Timer ring (centred, fills remaining vertical space) ───────────
        Box(
            modifier         = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            VoidTimerRing(timerState = timerState)
        }

        Spacer(Modifier.height(24.dp))

        // ── 4. Primary FAB — white circle, black icon ─────────────────────────
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

        Spacer(Modifier.height(28.dp))

        // ── 5. Bento stats row ────────────────────────────────────────────────
        BentoStatsRow(
            todayCount = todayCount,
            streakDays = streakDays,
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

// ── Phase pill selector ───────────────────────────────────────────────────────

/**
 * Horizontal pill row reflecting the current timer phase.
 *
 * Phases are display-only in V1: the timer auto-advances through Focus →
 * Short Break → Long Break; tapping a pill has no effect while the timer is
 * running.  The highlighted pill always tracks [activePhase] so the user
 * sees which phase is currently active.
 */
@Composable
private fun PhasePillRow(activePhase: TimerPhase) {
    val pills = listOf(
        "Focus"       to TimerPhase.FOCUS,
        "Short Break" to TimerPhase.SHORT_BREAK,
        "Long Break"  to TimerPhase.LONG_BREAK,
    )

    LazyRow(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(pills) { (label, phase) ->
            val isActive = phase == activePhase

            Box(
                modifier = Modifier
                    .background(
                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(50.dp),
                    )
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = label,
                    fontSize   = 14.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (isActive) Color.Black else Color.White.copy(alpha = 0.6f),
                )
            }
        }
    }
}

// ── Void timer ring ───────────────────────────────────────────────────────────

/**
 * Monochrome countdown ring matching the "Focused Void" spec:
 *   - Track: white at 10 % opacity, 3 dp stroke
 *   - Progress arc: pure white, 3 dp stroke, round caps
 *   - Centre: 80 sp bold timer text + 10 sp ALL-CAPS phase label
 */
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
        // Canvas — ring drawing
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

        // Text overlay — participates in accessibility tree + font scale
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text          = timerState.displayTime,
                fontSize      = 80.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = (-1.6).sp,   // ≈ -0.02 em at 80 sp
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
    streakDays: Int,
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

        // Streak card
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
                    text       = "$streakDays",
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text     = "🔥",
                    fontSize = 18.sp,
                )
            }
        }
    }
}
