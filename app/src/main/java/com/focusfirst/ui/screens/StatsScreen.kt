package com.focusfirst.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.data.db.SessionEntity
import com.focusfirst.data.model.Badge
import com.focusfirst.ui.components.FocusedVoid
import com.focusfirst.ui.components.TokiMetric
import com.focusfirst.ui.components.TokiSection
import com.focusfirst.viewmodel.BadgeViewModel
import com.focusfirst.viewmodel.TimerViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatsScreen(
    timerViewModel: TimerViewModel = hiltViewModel(),
    badgeViewModel: BadgeViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
) {
    val totalCompleted by timerViewModel.totalCompleted.collectAsStateWithLifecycle()
    val weeklySummary by timerViewModel.weeklySummary.collectAsStateWithLifecycle()
    val recentSessions by timerViewModel.recentSessions.collectAsStateWithLifecycle()
    val streakDays by timerViewModel.streakDays.collectAsStateWithLifecycle()
    val badges by badgeViewModel.badges.collectAsStateWithLifecycle()

    val todayEpochDay = remember { System.currentTimeMillis() / 86_400_000L }
    val days = remember(weeklySummary, todayEpochDay) {
        (6 downTo 0).map { offset ->
            val day = todayEpochDay - offset
            val row = weeklySummary.find { it.date == day }
            InsightDay(day, row?.sessionCount ?: 0, row?.totalMinutes ?: 0)
        }
    }
    val today = days.lastOrNull()
    val weeklyMinutes = days.sumOf { it.minutes }
    val unlockedBadges = badges.count { it.isUnlocked }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 22.dp, top = 42.dp, end = 22.dp, bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            InsightsHeader(onNavigateToSettings)
        }
        item {
            TokiSection(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TokiMetric("today", "${today?.minutes ?: 0} min", Modifier.weight(1f))
                    TokiMetric("sessions", "${today?.sessions ?: 0}", Modifier.weight(1f))
                    TokiMetric("streak", if (streakDays == 0) "-" else "$streakDays days", Modifier.weight(1f))
                }
            }
        }
        item {
            TokiSection(Modifier.fillMaxWidth()) {
                SectionTitle("Last 7 days", "$weeklyMinutes focused minutes")
                WeekBars(days)
            }
        }
        item {
            TokiSection(Modifier.fillMaxWidth()) {
                SectionTitle("History", "${recentSessions.size} recent intervals")
                if (recentSessions.isEmpty()) {
                    EmptyInsights()
                } else {
                    recentSessions.forEach { SessionRow(it) }
                }
            }
        }
        item {
            TokiSection(Modifier.fillMaxWidth()) {
                SectionTitle("Achievements", "$unlockedBadges of ${badges.size} unlocked")
                BadgePreview(badges)
            }
        }
    }
}

@Composable
private fun InsightsHeader(onNavigateToSettings: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Insights",
                color = cs.onBackground,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Useful patterns, not dashboard noise",
                color = cs.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(1.dp, cs.outline, CircleShape),
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = cs.onBackground,
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun WeekBars(days: List<InsightDay>) {
    val cs = MaterialTheme.colorScheme
    val maxMinutes = days.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp),
    ) {
        val gap = 8.dp.toPx()
        val barWidth = (size.width - gap * 6f) / 7f
        val corner = 8.dp.toPx()
        days.forEachIndexed { index, day ->
            val x = index * (barWidth + gap)
            val height = if (day.minutes == 0) 4.dp.toPx()
            else (day.minutes / maxMinutes.toFloat()) * size.height
            val color = if (day.minutes == 0) {
                cs.surfaceVariant
            } else if (index == days.lastIndex) {
                FocusedVoid.FocusRed
            } else {
                cs.primary.copy(alpha = 0.42f)
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(corner, corner),
            )
        }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Text(
                text = day.label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EmptyInsights() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Start one focus session to see today's progress.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SessionRow(session: SessionEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (session.wasCompleted) FocusedVoid.Success else MaterialTheme.colorScheme.outline),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.tag,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = formatTimeAgo(session.startedAt),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = "${session.durationSeconds / 60}m",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun BadgePreview(badges: List<Badge>) {
    if (badges.isEmpty()) {
        EmptyInsights()
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        badges.take(5).forEach { badge ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = if (badge.isUnlocked) badge.emoji else "?", fontSize = 20.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (badge.isUnlocked) badge.name else "Locked",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private data class InsightDay(
    val epochDay: Long,
    val sessions: Int,
    val minutes: Int,
) {
    val label: String
        get() = LocalDate.ofEpochDay(epochDay).dayOfWeek.name.take(1)
}

private fun formatTimeAgo(startedAtMs: Long): String {
    val zone = ZoneId.systemDefault()
    val zdt = Instant.ofEpochMilli(startedAtMs).atZone(zone)
    val today = LocalDate.now(zone)
    val day = zdt.toLocalDate()
    val prefix = when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> day.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
    }
    val time = zdt.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
    return "$prefix at $time"
}
