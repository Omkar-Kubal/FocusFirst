package com.focusfirst.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.data.db.SessionEntity
import com.focusfirst.ui.theme.FocusColors
import com.focusfirst.viewmodel.TimerViewModel
import java.time.LocalDate

// ============================================================================
// StatsScreen
// ============================================================================

@Composable
fun StatsScreen(viewModel: TimerViewModel = hiltViewModel()) {

    val todayCount     by viewModel.todayCount.collectAsStateWithLifecycle()
    val totalCompleted by viewModel.totalCompleted.collectAsStateWithLifecycle()
    val weeklySummary  by viewModel.weeklySummary.collectAsStateWithLifecycle()
    val recentSessions by viewModel.recentSessions.collectAsStateWithLifecycle()

    // Weekly mastery: progress toward today's goal of 8 sessions
    val masteryPct = (todayCount / 8f * 100).coerceAtMost(100f).toInt()

    // 7-day chart data — ordered oldest (index 0) → today (index 6)
    val todayEpochDay = System.currentTimeMillis() / 86_400_000L
    val days = (6 downTo 0).map { offset ->
        val day   = todayEpochDay - offset
        val count = weeklySummary.find { it.date == day }?.sessionCount ?: 0
        Pair(day, count)
    }
    val maxCount = days.maxOfOrNull { it.second }.let { if (it != null && it > 0) it else 1 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusColors.Background)
            .verticalScroll(rememberScrollState()),
    ) {
        StatsTopBar()

        if (totalCompleted == 0) {
            EmptyStatsState()
        } else {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                Spacer(Modifier.height(12.dp))

                WeeklyMasterySection(masteryPct = masteryPct)

                Spacer(Modifier.height(20.dp))

                SummaryCardsRow(
                    todayCount     = todayCount,
                    totalCompleted = totalCompleted,
                )

                Spacer(Modifier.height(24.dp))

                BarChartSection(
                    days         = days,
                    maxCount     = maxCount,
                    todayEpochDay = todayEpochDay,
                )

                if (recentSessions.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    RecentIntervalsSection(sessions = recentSessions)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ============================================================================
// Sub-composables
// ============================================================================

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun StatsTopBar() {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(modifier = Modifier.size(48.dp))

        Text(
            text       = "Stats",
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

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyStatsState() {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector        = Icons.Outlined.BarChart,
            contentDescription = null,
            tint               = Color.White.copy(alpha = 0.2f),
            modifier           = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text       = "No sessions yet",
            fontSize   = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color.White.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "Complete your first focus session\nto see your stats here.",
            fontSize  = 14.sp,
            color     = Color.White.copy(alpha = 0.35f),
            textAlign = TextAlign.Center,
        )
    }
}

// ── Weekly mastery ────────────────────────────────────────────────────────────

@Composable
private fun WeeklyMasterySection(masteryPct: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text          = "WEEKLY MASTERY",
            fontSize      = 10.sp,
            fontWeight    = FontWeight.Medium,
            letterSpacing = 1.5.sp,
            color         = Color.White.copy(alpha = 0.4f),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text       = "$masteryPct%",
            fontSize   = 56.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(1.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (masteryPct / 100f).coerceIn(0f, 1f))
                    .height(2.dp)
                    .background(Color.White, RoundedCornerShape(1.dp)),
            )
        }
    }
}

// ── Summary cards ─────────────────────────────────────────────────────────────

private val GlassShape  = RoundedCornerShape(16.dp)
private val GlassBg     = Color.White.copy(alpha = 0.07f)
private val GlassBorder = Color.White.copy(alpha = 0.1f)

@Composable
private fun SummaryCardsRow(todayCount: Int, totalCompleted: Int) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GlassSummaryCard(
            modifier = Modifier.weight(1f),
            label    = "SESSIONS TODAY",
            value    = todayCount.toString(),
            subLabel = if (todayCount > 0) "+$todayCount today" else "Start your first",
        )
        GlassSummaryCard(
            modifier = Modifier.weight(1f),
            label    = "TOTAL SESSIONS",
            value    = totalCompleted.toString(),
            subLabel = if (totalCompleted >= 100) "Top 5% of users" else "Keep it up!",
        )
    }
}

@Composable
private fun GlassSummaryCard(
    modifier: Modifier,
    label:    String,
    value:    String,
    subLabel: String,
) {
    Column(
        modifier = modifier
            .background(GlassBg, GlassShape)
            .border(1.dp, GlassBorder, GlassShape)
            .padding(16.dp),
    ) {
        Text(
            text          = label,
            fontSize      = 10.sp,
            fontWeight    = FontWeight.Medium,
            letterSpacing = 1.5.sp,
            color         = Color.White.copy(alpha = 0.4f),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text       = value,
            fontSize   = 32.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text     = subLabel,
            fontSize = 12.sp,
            color    = FocusColors.OnSurfaceVariant,
        )
    }
}

// ── 7-day bar chart ───────────────────────────────────────────────────────────

@Composable
private fun BarChartSection(
    days:          List<Pair<Long, Int>>,
    maxCount:      Int,
    todayEpochDay: Long,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassBg, GlassShape)
            .border(1.dp, GlassBorder, GlassShape)
            .padding(16.dp),
    ) {
        Text(
            text       = "Last 7 Days",
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color.White,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text     = "${days.sumOf { it.second }} sessions this week",
            fontSize = 12.sp,
            color    = FocusColors.OnSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        // Session count labels above bars
        Row(modifier = Modifier.fillMaxWidth()) {
            days.forEach { (_, count) ->
                Text(
                    text      = if (count > 0) count.toString() else "",
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize  = 10.sp,
                    color     = Color.White.copy(alpha = 0.6f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // Bars
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
        ) {
            val gap      = 6.dp.toPx()
            val barWidth = (size.width - gap * 6f) / 7f
            val cornerPx = 4.dp.toPx()
            val floorPx  = 2.dp.toPx()

            days.forEachIndexed { index, (epochDay, count) ->
                val isToday = epochDay == todayEpochDay
                val x       = index * (barWidth + gap)
                val color   = if (isToday) Color.White else Color.White.copy(alpha = 0.4f)

                if (count > 0) {
                    val barHeight = (count.toFloat() / maxCount.toFloat()) * size.height
                    drawRoundRect(
                        color        = color,
                        topLeft      = Offset(x, size.height - barHeight),
                        size         = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                    )
                } else {
                    // Floor mark — baseline indicator on empty days
                    drawRoundRect(
                        color        = Color.White.copy(alpha = 0.1f),
                        topLeft      = Offset(x, size.height - floorPx),
                        size         = Size(barWidth, floorPx),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        // Day-of-week labels below bars
        Row(modifier = Modifier.fillMaxWidth()) {
            days.forEach { (epochDay, _) ->
                val isToday  = epochDay == todayEpochDay
                val dayLabel = LocalDate.ofEpochDay(epochDay)
                    .dayOfWeek
                    .name
                    .take(3)
                    .lowercase()
                    .replaceFirstChar { it.uppercaseChar() }

                Text(
                    text       = dayLabel,
                    modifier   = Modifier.weight(1f),
                    textAlign  = TextAlign.Center,
                    fontSize   = 11.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color      = if (isToday) Color.White else Color.White.copy(alpha = 0.4f),
                )
            }
        }
    }
}

// ── Recent intervals ──────────────────────────────────────────────────────────

@Composable
private fun RecentIntervalsSection(sessions: List<SessionEntity>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text       = "Recent Intervals",
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color.White,
        )
        Spacer(Modifier.height(12.dp))

        sessions.forEach { session ->
            RecentIntervalCard(session = session)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RecentIntervalCard(session: SessionEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassBg, GlassShape)
            .border(1.dp, GlassBorder, GlassShape)
            .padding(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = if (session.tag == "Focus") Icons.Outlined.Edit
                                        else Icons.Outlined.Timer,
                    contentDescription = null,
                    tint               = Color.White.copy(alpha = 0.7f),
                    modifier           = Modifier.size(16.dp),
                )
            }

            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text       = session.tag,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color      = Color.White,
                )
                Text(
                    text     = timeAgo(session.startedAt),
                    fontSize = 12.sp,
                    color    = FocusColors.OnSurfaceVariant,
                )
            }
        }

        Text(
            text       = "${session.durationSeconds / 60}m",
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color.White,
        )
    }
}

// ============================================================================
// Pure helpers
// ============================================================================

private fun timeAgo(startedAtMs: Long): String {
    val diffMs = System.currentTimeMillis() - startedAtMs
    return when {
        diffMs < 60_000L     -> "Just now"
        diffMs < 3_600_000L  -> "${diffMs / 60_000}m ago"
        diffMs < 86_400_000L -> "${diffMs / 3_600_000}h ago"
        else                 -> "${diffMs / 86_400_000}d ago"
    }
}
