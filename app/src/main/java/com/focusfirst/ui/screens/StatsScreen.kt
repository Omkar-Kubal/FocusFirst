package com.focusfirst.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.focusfirst.ui.theme.LocalFocusDarkTheme
import com.focusfirst.viewmodel.SettingsViewModel
import com.focusfirst.viewmodel.TimerViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// ============================================================================
// StatsScreen
// ============================================================================

private val TrendGreen = Color(0xFF34C759)

@Composable
fun StatsScreen(
    viewModel: TimerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
) {
    val todayCount     by viewModel.todayCount.collectAsStateWithLifecycle()
    val totalCompleted by viewModel.totalCompleted.collectAsStateWithLifecycle()
    val weeklySummary  by viewModel.weeklySummary.collectAsStateWithLifecycle()
    val summaries14    by viewModel.dailySummaries14d.collectAsStateWithLifecycle()
    val recentSessions by viewModel.recentSessions.collectAsStateWithLifecycle()
    val dailyGoal      by settingsViewModel.dailyGoal.collectAsStateWithLifecycle()
    val streakDays     by viewModel.streakDays.collectAsStateWithLifecycle()

    val goalDen = dailyGoal.coerceAtLeast(1)
    val masteryPct =
        (todayCount / goalDen.toFloat() * 100f).coerceAtMost(100f).toInt()

    val todayEpochDay = System.currentTimeMillis() / 86_400_000L

    val thisWeekSessions = (0..6).sumOf { offset ->
        val day = todayEpochDay - offset
        summaries14.find { it.date == day }?.sessionCount ?: 0
    }
    val lastWeekSessions = (7..13).sumOf { offset ->
        val day = todayEpochDay - offset
        summaries14.find { it.date == day }?.sessionCount ?: 0
    }
    val trendPct = when {
        lastWeekSessions == 0 && thisWeekSessions > 0 -> 100
        lastWeekSessions == 0                         -> 0
        else ->
            ((thisWeekSessions - lastWeekSessions).toFloat() / lastWeekSessions * 100f)
                .roundToInt()
    }
    val trendLabel = if (trendPct >= 0) "+$trendPct%" else "$trendPct%"

    val daysChart = (6 downTo 0).map { offset ->
        val day = todayEpochDay - offset
        val row = weeklySummary.find { it.date == day }
        Triple(day, row?.sessionCount ?: 0, row?.totalMinutes ?: 0)
    }
    val maxSessions = daysChart.maxOfOrNull { it.second }.let { m -> if (m != null && m > 0) m else 1 }
    val maxMinutes  = daysChart.maxOfOrNull { it.third }.let { m -> if (m != null && m > 0) m else 1 }

    val scheme = MaterialTheme.colorScheme
    val dark   = LocalFocusDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        StatsTopBar(onSettingsClick = onNavigateToSettings)

        if (totalCompleted == 0) {
            EmptyStatsState()
        } else {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                Spacer(Modifier.height(8.dp))

                WeeklyMasteryCard(
                    masteryPct = masteryPct,
                    trendLabel = trendLabel,
                    dark       = dark,
                    scheme     = scheme,
                )

                Spacer(Modifier.height(16.dp))

                SummaryCardsRow(
                    todayCount     = todayCount,
                    dailyGoal      = goalDen,
                    totalCompleted = totalCompleted,
                    dark           = dark,
                    scheme         = scheme,
                )

                Spacer(Modifier.height(12.dp))

                StreakCard(
                    streakDays = streakDays,
                    dark       = dark,
                    scheme     = scheme,
                )

                Spacer(Modifier.height(20.dp))

                var showMinutes by remember { mutableStateOf(false) }
                BarChartSection(
                    days          = daysChart,
                    maxSessions   = maxSessions,
                    maxMinutes    = maxMinutes,
                    todayEpochDay = todayEpochDay,
                    showMinutes   = showMinutes,
                    onToggleChart = { showMinutes = !showMinutes },
                    dark          = dark,
                    scheme        = scheme,
                )

                if (recentSessions.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    RecentIntervalsSection(
                        sessions = recentSessions,
                        scheme   = scheme,
                        dark     = dark,
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ============================================================================
// Sub-composables
// ============================================================================

@Composable
private fun StatsTopBar(onSettingsClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val barBg  = if (LocalFocusDarkTheme.current) {
        Color.White.copy(alpha = 0.08f)
    } else {
        scheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(barBg)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text       = "Stats",
            fontSize   = 28.sp,
            fontWeight = FontWeight.Bold,
            color      = scheme.onSurface,
            modifier   = Modifier.padding(start = 8.dp),
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector        = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint               = scheme.onSurface,
            )
        }
    }
}

@Composable
private fun EmptyStatsState() {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector        = Icons.Outlined.Timer,
            contentDescription = null,
            tint               = scheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier           = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text       = "No sessions yet",
            fontSize   = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color      = scheme.onSurface.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "Complete your first focus session\nto see your stats here.",
            fontSize  = 14.sp,
            color     = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val CardShape = RoundedCornerShape(16.dp)

@Composable
private fun WeeklyMasteryCard(
    masteryPct: Int,
    trendLabel: String,
    dark:       Boolean,
    scheme:     androidx.compose.material3.ColorScheme,
) {
    val cardBg = if (dark) FocusColors.SurfaceContainerLow else scheme.surface
    val border = if (dark) Color.White.copy(alpha = 0.08f) else scheme.outlineVariant.copy(alpha = 0.35f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, border, CardShape)
            .background(cardBg, CardShape)
            .padding(20.dp),
    ) {
        Icon(
            imageVector        = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint               = TrendGreen,
            modifier           = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp),
        )
        Column {
            Text(
                text          = "WEEKLY MASTERY",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color         = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = "$masteryPct%",
                fontSize   = 48.sp,
                fontWeight = FontWeight.Bold,
                color      = scheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text       = "$trendLabel vs last week",
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = TrendGreen,
            )
        }
    }
}

@Composable
private fun SummaryCardsRow(
    todayCount:     Int,
    dailyGoal:      Int,
    totalCompleted: Int,
    dark:           Boolean,
    scheme:         androidx.compose.material3.ColorScheme,
) {
    val cardBg = if (dark) Color.White.copy(alpha = 0.07f) else scheme.surface
    val border = if (dark) Color.White.copy(alpha = 0.1f) else scheme.outlineVariant.copy(alpha = 0.35f)

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryMiniCard(
            modifier = Modifier.weight(1f),
            label    = "SESSIONS TODAY",
            value    = todayCount.toString(),
            subLabel = "Goal: $dailyGoal",
            cardBg   = cardBg,
            border   = border,
            scheme   = scheme,
        )
        SummaryMiniCard(
            modifier = Modifier.weight(1f),
            label    = "TOTAL SESSIONS",
            value    = totalCompleted.toString(),
            subLabel = "All time",
            cardBg   = cardBg,
            border   = border,
            scheme   = scheme,
        )
    }
}

@Composable
private fun SummaryMiniCard(
    modifier: Modifier,
    label:    String,
    value:    String,
    subLabel: String,
    cardBg:   Color,
    border:   Color,
    scheme:   androidx.compose.material3.ColorScheme,
) {
    Column(
        modifier = modifier
            .border(1.dp, border, CardShape)
            .background(cardBg, CardShape)
            .padding(16.dp),
    ) {
        Text(
            text          = label,
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            color         = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text       = value,
            fontSize   = 32.sp,
            fontWeight = FontWeight.Bold,
            color      = scheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text     = subLabel,
            fontSize = 12.sp,
            color    = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StreakCard(
    streakDays: Int,
    dark:       Boolean,
    scheme:     androidx.compose.material3.ColorScheme,
) {
    val cardBg = if (dark) Color.White.copy(alpha = 0.07f) else scheme.surface
    val border = if (dark) Color.White.copy(alpha = 0.1f) else scheme.outlineVariant.copy(alpha = 0.35f)
    val showFlame = streakDays >= 7

    val motivational = when {
        streakDays == 0   -> "Start your streak today"
        streakDays < 7    -> "Keep it going!"
        streakDays < 14   -> "One week strong!"
        streakDays < 30   -> "Two weeks of focus!"
        else              -> "Elite focuser!"
    }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .border(1.dp, border, CardShape)
            .background(cardBg, CardShape)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text          = "STREAK",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color         = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = if (streakDays == 0) "—" else "$streakDays",
                    fontSize   = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color      = scheme.onSurface,
                )
                if (streakDays > 0) {
                    androidx.compose.foundation.layout.Spacer(
                        Modifier.size(6.dp)
                    )
                    Text(
                        text     = if (streakDays == 1) "day" else "days",
                        fontSize = 16.sp,
                        color    = scheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text     = motivational,
                fontSize = 13.sp,
                color    = if (showFlame) Color(0xFFFF9500) else scheme.onSurfaceVariant,
            )
        }

        if (showFlame) {
            Text(
                text     = "\uD83D\uDD25",   // 🔥
                fontSize = 36.sp,
            )
        }
    }
}

@Composable
private fun BarChartSection(
    days:          List<Triple<Long, Int, Int>>,
    maxSessions:   Int,
    maxMinutes:    Int,
    todayEpochDay: Long,
    showMinutes:   Boolean,
    onToggleChart: () -> Unit,
    dark:          Boolean,
    scheme:        androidx.compose.material3.ColorScheme,
) {
    val cardBg = if (dark) Color.White.copy(alpha = 0.07f) else scheme.surface
    val border = if (dark) Color.White.copy(alpha = 0.1f) else scheme.outlineVariant.copy(alpha = 0.35f)

    val otherBarLight = Color(0xFFE5E5EA)
    val todayBarLight = Color.Black
    val todayBarDark  = Color.White
    val otherBarDark  = Color.White.copy(alpha = 0.2f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, border, CardShape)
            .background(cardBg, CardShape)
            .padding(16.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text       = "Last 7 Days",
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = scheme.onSurface,
            )
            Text(
                text = if (showMinutes) "SESSIONS" else "MINUTES",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (showMinutes) scheme.onSurface else scheme.onPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (showMinutes) scheme.surfaceVariant.copy(alpha = 0.5f) else scheme.primary,
                    )
                    .clickable(onClick = onToggleChart)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(16.dp))

        val maxVal = if (showMinutes) maxMinutes else maxSessions

        Row(modifier = Modifier.fillMaxWidth()) {
            days.forEach { (_, sessions, minutes) ->
                val v = if (showMinutes) minutes else sessions
                Text(
                    text      = if (v > 0) v.toString() else "",
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize  = 10.sp,
                    color     = scheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            val gap      = 6.dp.toPx()
            val barWidth = (size.width - gap * 6f) / 7f
            val cornerPx = barWidth / 2f

            days.forEachIndexed { index, (epochDay, sessions, minutes) ->
                val isToday = epochDay == todayEpochDay
                val count   = if (showMinutes) minutes else sessions
                val otherColor = if (dark) otherBarDark else otherBarLight
                val highlight  = if (dark) todayBarDark else todayBarLight
                val barColor   = if (isToday) highlight else otherColor

                val x = index * (barWidth + gap)
                if (count > 0) {
                    val barHeight = (count.toFloat() / maxVal.toFloat()) * size.height
                    drawRoundRect(
                        color        = barColor,
                        topLeft      = Offset(x, size.height - barHeight),
                        size         = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                    )
                } else {
                    val floorH = 3.dp.toPx()
                    drawRoundRect(
                        color        = otherColor.copy(alpha = 0.35f),
                        topLeft      = Offset(x, size.height - floorH),
                        size         = Size(barWidth, floorH),
                        cornerRadius = CornerRadius(floorH / 2f, floorH / 2f),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            days.forEach { (epochDay, _, _) ->
                val isToday  = epochDay == todayEpochDay
                val dayLabel = LocalDate.ofEpochDay(epochDay)
                    .dayOfWeek
                    .name
                    .take(3)
                    .uppercase(Locale.US)

                Text(
                    text       = dayLabel,
                    modifier   = Modifier.weight(1f),
                    textAlign  = TextAlign.Center,
                    fontSize   = 10.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color      = if (isToday) scheme.onSurface else scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecentIntervalsSection(
    sessions: List<SessionEntity>,
    scheme:   androidx.compose.material3.ColorScheme,
    dark:     Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text       = "Recent Intervals",
            fontSize   = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color      = scheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))

        sessions.forEach { session ->
            RecentIntervalCard(session = session, scheme = scheme, dark = dark)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun RecentIntervalCard(
    session: SessionEntity,
    scheme:  androidx.compose.material3.ColorScheme,
    dark:    Boolean,
) {
    val border = if (dark) Color.White.copy(alpha = 0.12f) else scheme.outlineVariant.copy(alpha = 0.45f)
    val cardBg = if (dark) Color.White.copy(alpha = 0.05f) else scheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, border, CardShape)
            .background(cardBg, CardShape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(scheme.surfaceVariant.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = if (session.tag == "Focus") Icons.Outlined.Edit
                                    else Icons.Outlined.Timer,
                contentDescription = null,
                tint               = scheme.onSurface,
                modifier           = Modifier.size(22.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text       = session.tag,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = scheme.onSurface,
            )
            Text(
                text     = formatSessionSubtitle(session.startedAt),
                fontSize = 12.sp,
                color    = scheme.onSurfaceVariant,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text       = "${session.durationSeconds / 60} min",
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = scheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = if (session.wasCompleted) "COMPLETED" else "STOPPED",
                fontSize   = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                color      = if (session.wasCompleted) TrendGreen else scheme.onSurfaceVariant,
            )
        }
    }
}

// ============================================================================
// Helpers
// ============================================================================

private fun formatSessionSubtitle(startedAtMs: Long): String {
    val zone    = ZoneId.systemDefault()
    val instant = Instant.ofEpochMilli(startedAtMs)
    val zdt     = instant.atZone(zone)
    val today   = LocalDate.now(zone)
    val day     = zdt.toLocalDate()
    val prefix  = when (day) {
        today            -> "Today"
        today.minusDays(1) -> "Yesterday"
        else             -> day.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
    }
    val time = zdt.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
    return "$prefix, $time"
}
