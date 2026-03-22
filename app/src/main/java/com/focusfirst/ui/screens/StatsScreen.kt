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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.focusfirst.data.db.DailySummary
import com.focusfirst.data.db.SessionEntity
import com.focusfirst.ui.theme.FocusColors
import com.focusfirst.viewmodel.TimerViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

// ============================================================================
// StatsScreen — "The Focused Void" redesign
// ============================================================================

@Composable
fun StatsScreen(viewModel: TimerViewModel = hiltViewModel()) {
    val todayCount     by viewModel.todayCount.collectAsStateWithLifecycle()
    val totalCompleted by viewModel.totalCompleted.collectAsStateWithLifecycle()
    val weeklySummary  by viewModel.weeklySummary.collectAsStateWithLifecycle()
    val recentSessions by viewModel.recentSessions.collectAsStateWithLifecycle()

    // Chart: 7-entry list ordered oldest → newest
    val chartData = remember(weeklySummary) { buildChartData(weeklySummary) }
    val chartMax  = remember(chartData) {
        chartData.maxOfOrNull { it.second }.takeIf { it != null && it > 0 } ?: 1
    }

    // Weekly mastery = fraction of last 7 days that had at least 1 session
    val masteryPct = remember(weeklySummary) {
        val activeDays = weeklySummary.count { it.sessionCount > 0 }
        ((activeDays.toFloat() / 7f) * 100).toInt()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusColors.Background)
            .verticalScroll(rememberScrollState()),
    ) {

        // ── 1. Glass top bar ──────────────────────────────────────────────────
        StatsTopBar()

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {

            Spacer(Modifier.height(12.dp))

            // ── 2. Weekly mastery ─────────────────────────────────────────────
            WeeklyMasterySection(masteryPct = masteryPct)

            Spacer(Modifier.height(20.dp))

            // ── 3. Summary cards (2-column grid) ──────────────────────────────
            SummaryCardsRow(
                todayCount     = todayCount,
                totalCompleted = totalCompleted,
            )

            Spacer(Modifier.height(24.dp))

            // ── 4. Last 7 days bar chart ──────────────────────────────────────
            VoidChartSection(
                chartData = chartData,
                chartMax  = chartMax,
            )

            // ── 5. Recent intervals ───────────────────────────────────────────
            if (recentSessions.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                RecentIntervalsSection(sessions = recentSessions)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ============================================================================
// Sub-composables
// ============================================================================

// ── Glass top bar ─────────────────────────────────────────────────────────────

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
        // Left spacer — matches width of right IconButton to keep title centred
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

        // Full-width progress bar — 2 dp, white track + white fill
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
            modifier  = Modifier.weight(1f),
            label     = "SESSIONS TODAY",
            value     = todayCount.toString(),
            subLabel  = if (todayCount > 0) "+${todayCount} today" else "Start your first",
        )
        GlassSummaryCard(
            modifier  = Modifier.weight(1f),
            label     = "TOTAL SESSIONS",
            value     = totalCompleted.toString(),
            subLabel  = if (totalCompleted >= 100) "Top 5% of users" else "Keep it up!",
        )
    }
}

@Composable
private fun GlassSummaryCard(
    modifier:  Modifier,
    label:     String,
    value:     String,
    subLabel:  String,
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
            text       = subLabel,
            fontSize   = 12.sp,
            color      = FocusColors.OnSurfaceVariant,
        )
    }
}

// ── Last 7 days bar chart ─────────────────────────────────────────────────────

@Composable
private fun VoidChartSection(
    chartData: List<Pair<Long, Int>>,
    chartMax:  Int,
) {
    val todayEpochDay = remember { System.currentTimeMillis() / 86_400_000L }

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
        Text(
            text     = "Focused hours: ${chartData.sumOf { it.second * 25 / 60 }}.${chartData.sumOf { it.second * 25 } % 60 / 6}h",
            fontSize = 12.sp,
            color    = FocusColors.OnSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        // Count row above bars
        Row(modifier = Modifier.fillMaxWidth()) {
            chartData.forEach { (_, count) ->
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

        // Bar chart
        VoidBarChart(
            chartData     = chartData,
            chartMax      = chartMax,
            todayEpochDay = todayEpochDay,
            modifier      = Modifier
                .fillMaxWidth()
                .height(120.dp),
        )
        Spacer(Modifier.height(6.dp))

        // Day labels below bars
        Row(modifier = Modifier.fillMaxWidth()) {
            chartData.forEach { (epochDay, _) ->
                val isToday = epochDay == todayEpochDay
                Text(
                    text      = epochDayAbbrev(epochDay),
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize  = 12.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color     = if (isToday) Color.White else Color.White.copy(alpha = 0.4f),
                )
            }
        }
    }
}

@Composable
private fun VoidBarChart(
    chartData:     List<Pair<Long, Int>>,
    chartMax:      Int,
    todayEpochDay: Long,
    modifier:      Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val gapPx          = 8.dp.toPx()
        val barWidth       = (size.width - gapPx * 6f) / 7f
        val maxBarHeightPx = size.height
        val cornerPx       = 4.dp.toPx()
        val floorPx        = 2.dp.toPx()

        chartData.forEachIndexed { index, (epochDay, count) ->
            val isToday = epochDay == todayEpochDay
            val x       = index * (barWidth + gapPx)

            if (count > 0) {
                val barHeightPx = (count.toFloat() / chartMax.toFloat()) * maxBarHeightPx
                drawRoundRect(
                    color        = if (isToday) Color.White else Color.White.copy(alpha = 0.4f),
                    topLeft      = Offset(x, size.height - barHeightPx),
                    size         = Size(barWidth, barHeightPx),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                )
            } else {
                // Floor mark — visible baseline on days with no sessions
                drawRoundRect(
                    color        = Color.White.copy(alpha = 0.1f),
                    topLeft      = Offset(x, size.height - floorPx),
                    size         = Size(barWidth, floorPx),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
                )
            }
        }
    }
}

// ── Recent intervals ──────────────────────────────────────────────────────────

/**
 * Asymmetric layout matching the stitch:
 *   - First session: full-width glass card
 *   - Next two sessions (if present): side-by-side 50/50 glass cards
 */
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

        // First session — full width
        sessions.getOrNull(0)?.let { session ->
            RecentIntervalCardLarge(session = session)
        }

        // Sessions 2 & 3 — side by side
        val second = sessions.getOrNull(1)
        val third  = sessions.getOrNull(2)
        if (second != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RecentIntervalCardSmall(session = second, modifier = Modifier.weight(1f))
                if (third != null) {
                    RecentIntervalCardSmall(session = third, modifier = Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RecentIntervalCardLarge(session: SessionEntity) {
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
            text       = durationLabel(session.durationSeconds),
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color.White,
        )
    }
}

@Composable
private fun RecentIntervalCardSmall(session: SessionEntity, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(GlassBg, GlassShape)
            .border(1.dp, GlassBorder, GlassShape)
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Outlined.Timer,
                contentDescription = null,
                tint               = Color.White.copy(alpha = 0.7f),
                modifier           = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text       = session.tag,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            color      = Color.White,
        )
        Text(
            text     = durationLabel(session.durationSeconds),
            fontSize = 12.sp,
            color    = FocusColors.OnSurfaceVariant,
        )
    }
}

// ============================================================================
// Pure helpers (no composition — safe to call from remember blocks)
// ============================================================================

private fun buildChartData(weeklySummary: List<DailySummary>): List<Pair<Long, Int>> {
    val todayEpochDay = System.currentTimeMillis() / 86_400_000L
    val summaryMap    = weeklySummary.associateBy { it.date }
    return (6 downTo 0).map { daysBack ->
        val epochDay = todayEpochDay - daysBack
        epochDay to (summaryMap[epochDay]?.sessionCount ?: 0)
    }
}

private fun epochDayAbbrev(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay)
        .dayOfWeek
        .getDisplayName(TextStyle.NARROW, Locale.getDefault())
        .replaceFirstChar { it.uppercaseChar() }

private fun timeAgo(startedAtMs: Long): String {
    val diffMs = System.currentTimeMillis() - startedAtMs
    return when {
        diffMs < 60_000L        -> "Just now"
        diffMs < 3_600_000L     -> "${diffMs / 60_000}m ago"
        diffMs < 86_400_000L    -> "Today · ${clockTime(startedAtMs)}"
        else                    -> "Yesterday"
    }
}

private fun clockTime(epochMs: Long): String {
    val cal  = Calendar.getInstance().also { it.timeInMillis = epochMs }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val min  = cal.get(Calendar.MINUTE)
    val amPm = if (hour < 12) "AM" else "PM"
    val h12  = if (hour % 12 == 0) 12 else hour % 12
    return "$h12:%02d $amPm".format(min)
}

private fun durationLabel(durationSeconds: Int): String {
    val minutes = durationSeconds / 60
    return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
}
