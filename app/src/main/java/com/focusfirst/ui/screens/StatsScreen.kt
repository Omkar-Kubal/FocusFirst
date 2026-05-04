package com.focusfirst.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FreeBreakfast
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.data.db.SessionEntity
import com.focusfirst.data.model.Badge
import com.focusfirst.viewmodel.BadgeViewModel
import com.focusfirst.viewmodel.TimerViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

private val TokiGreen     = Color(0xFF1A9E5F)
// M3 Card shape: Medium token = 12dp (was 18dp) per material_design_skills.md §3.2
private val TokiCardShape = RoundedCornerShape(12.dp)

// Backport of Modifier.grayscale() (added in Compose 1.8, not in BOM 2024.12.01)
private fun Modifier.grayscale(): Modifier = drawWithContent {
    val matrix = ColorMatrix().apply { setToSaturation(0f) }
    val paint  = Paint().apply { colorFilter = ColorFilter.colorMatrix(matrix) }
    drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
    drawContent()
    drawContext.canvas.restore()
}

// ─────────────────────────────────────────────────────────────────────────────
// StatsScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatsScreen(
    timerViewModel:       TimerViewModel = hiltViewModel(),
    badgeViewModel:       BadgeViewModel = hiltViewModel(),
    billingViewModel:     com.focusfirst.billing.BillingViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit     = {},
) {
    val totalCompleted by timerViewModel.totalCompleted.collectAsStateWithLifecycle()
    val weeklySummary  by timerViewModel.weeklySummary.collectAsStateWithLifecycle()
    val recentSessions by timerViewModel.recentSessions.collectAsStateWithLifecycle()
    val streakDays     by timerViewModel.streakDays.collectAsStateWithLifecycle()
    val allSessions    by timerViewModel.allSessions.collectAsStateWithLifecycle()
    val badges         by badgeViewModel.badges.collectAsStateWithLifecycle()
    val isPro          by billingViewModel.isPro.collectAsStateWithLifecycle()

    val weeklyTotal   = weeklySummary.sumOf { it.sessionCount }
    val todayEpochDay = System.currentTimeMillis() / 86_400_000L

    val daysChart = (6 downTo 0).map { offset ->
        val day = todayEpochDay - offset
        val row = weeklySummary.find { it.date == day }
        Triple(day, row?.sessionCount ?: 0, row?.totalMinutes ?: 0)
    }
    val maxSessions = daysChart.maxOfOrNull { it.second }.let { if (it != null && it > 0) it else 1 }
    val maxMinutes  = daysChart.maxOfOrNull { it.third  }.let { if (it != null && it > 0) it else 1 }

    var showMinutes by remember { mutableStateOf(false) }

    val cs = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background),
        contentPadding = PaddingValues(bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        item {
            StatsHeader(
                totalCompleted = totalCompleted,
                weeklyTotal = weeklyTotal,
                onNavigateToSettings = onNavigateToSettings,
            )
        }

        // 1 · Stat cards
        item {
            StatCardsRow(
                totalCompleted = totalCompleted,
                weeklyTotal    = weeklyTotal,
                streakDays     = streakDays,
            )
        }

        // 1b · Empty state
        if (totalCompleted == 0) {
            item {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🍅", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text       = "No sessions yet",
                            fontSize   = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = cs.onBackground,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text      = "Start your first focus session\nto see your stats here",
                            fontSize  = 13.sp,
                            color     = cs.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // 2 · Focus heatmap
        item {
            FocusHeatmapSection(allSessions = allSessions)
        }

        // 3 · Bar chart
        item {
            Box {
                BarChartSection(
                    days          = daysChart,
                    maxSessions   = maxSessions,
                    maxMinutes    = maxMinutes,
                    todayEpochDay = todayEpochDay,
                    showMinutes   = showMinutes,
                    onToggle      = { showMinutes = !showMinutes },
                    modifier      = if (!isPro) Modifier.grayscale().clip(RoundedCornerShape(12.dp)) else Modifier
                )
                if (!isPro) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(cs.background.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { billingViewModel.openUpgradeSheet() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Lock, contentDescription = "Pro Required", tint = cs.onSurface, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Pro Feature", fontWeight = FontWeight.Bold, color = cs.onSurface)
                        }
                    }
                }
            }
        }

        // 4 · Recent intervals
        item {
            RecentIntervalsSection(recentSessions = recentSessions)
        }

        // 5 · Achievements / Badges
        item {
            BadgesSection(badges = badges)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 1 · Stat cards
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatsHeader(
    totalCompleted: Int,
    weeklyTotal: Int,
    onNavigateToSettings: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 46.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Stats",
                fontSize = 34.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = cs.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "$totalCompleted total sessions / $weeklyTotal this week",
                fontSize = 13.sp,
                lineHeight = 16.sp,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .border(1.dp, cs.outline, CircleShape)
                .clickable { onNavigateToSettings() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = cs.onBackground,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun TokiSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(TokiCardShape)
            .background(cs.surfaceContainerLow)
            .border(1.dp, cs.outline, TokiCardShape)
            .padding(18.dp),
        content = content,
    )
}

@Composable
private fun SectionTitle(
    title: String,
    detail: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.uppercase(Locale.US),
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.8.sp,
                color = cs.onSurfaceVariant,
            )
            if (detail != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = detail,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun StatCardsRow(
    totalCompleted: Int,
    weeklyTotal:    Int,
    streakDays:     Int,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatCard(value = "$totalCompleted", label = "TOTAL",     modifier = Modifier.weight(1f))
        StatCard(value = "$weeklyTotal",    label = "THIS WEEK", modifier = Modifier.weight(1f))
        StatCard(
            value     = "$streakDays",
            label     = "DAY STREAK",
            modifier  = Modifier.weight(1f),
            showFlame = streakDays >= 5,
        )
    }
}

@Composable
private fun StatCard(
    value:     String,
    label:     String,
    modifier:  Modifier = Modifier,
    showFlame: Boolean  = false,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        color    = cs.surfaceContainerLow,
        shape    = TokiCardShape,
        border   = BorderStroke(1.dp, cs.outline),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text       = value,
                    fontSize   = 30.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color      = cs.onSurface,
                )
                if (showFlame) {
                    Text(text = " 🔥", fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text          = label,
                fontSize      = 9.sp,
                color         = cs.onSurfaceVariant,
                letterSpacing = 1.2.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 2 · Focus heatmap (GitHub style, 26 weeks)
// ─────────────────────────────────────────────────────────────────────────────

private val MONTH_NAMES = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

@Composable
private fun FocusHeatmapSection(allSessions: List<SessionEntity>) {
    val completedCount = allSessions.count { it.wasCompleted }
    val year           = Calendar.getInstance().get(Calendar.YEAR)

    TokiSectionCard {
        SectionTitle(
            title = "Focus history",
            detail = "$completedCount sessions in $year",
        )
        Spacer(modifier = Modifier.height(16.dp))
        FocusHeatmap(sessions = allSessions)
    }
}

@Composable
private fun FocusHeatmap(sessions: List<SessionEntity>) {
    val cs = MaterialTheme.colorScheme
    val sessionsByDay = sessions
        .filter { it.wasCompleted }
        .groupBy { it.startedAt / 86_400_000L }
        .mapValues { it.value.size }

    val today       = System.currentTimeMillis() / 86_400_000L
    val weeksToShow = 26
    val startDay    = today - weeksToShow * 7L
    val cellSize    = 11.dp
    val cellGap     = 2.dp
    val totalCell   = cellSize + cellGap
    val dayLabelW   = 20.dp

    val cellFuture = cs.surfaceContainerLow
    val cellZero   = cs.surfaceVariant
    val todayBorder = cs.onSurface

    Column {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {

            // Day-of-week labels — M / W / F only
            Column(
                modifier            = Modifier
                    .width(dayLabelW)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(cellGap),
            ) {
                listOf("M", "", "W", "", "F", "", "").forEach { label ->
                    Box(
                        modifier         = Modifier.height(cellSize),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (label.isNotEmpty()) {
                            Text(text = label, fontSize = 8.sp, color = cs.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            // Month labels + cell grid
            Column {
                Row {
                    var lastMonth = -1
                    for (week in 0 until weeksToShow) {
                        val weekEpochDay = startDay + week * 7L
                        val cal          = Calendar.getInstance().apply {
                            timeInMillis = weekEpochDay * 86_400_000L
                        }
                        val month      = cal.get(Calendar.MONTH)
                        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                        if (month != lastMonth && dayOfMonth <= 7) {
                            lastMonth = month
                            Text(
                                text     = MONTH_NAMES[month],
                                fontSize = 9.sp,
                                color    = cs.onSurfaceVariant,
                                modifier = Modifier.width(totalCell * 4),
                            )
                        } else {
                            Spacer(modifier = Modifier.width(totalCell))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
                    for (week in 0 until weeksToShow) {
                        Column(verticalArrangement = Arrangement.spacedBy(cellGap)) {
                            for (dayOfWeek in 0..6) {
                                val epochDay = startDay + (week * 7).toLong() + dayOfWeek
                                val count    = sessionsByDay[epochDay] ?: 0
                                val isToday  = epochDay == today
                                val isFuture = epochDay > today

                                val cellColor = when {
                                    isFuture   -> cellFuture
                                    count == 0 -> cellZero
                                    count <= 2 -> Color(0xFF1A4A2A)
                                    count <= 4 -> Color(0xFF2D7A3D)
                                    count <= 6 -> Color(0xFF3A9A50)
                                    else       -> Color(0xFF4AAA60)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(cellColor)
                                        .then(
                                            if (isToday) Modifier.border(
                                                0.8.dp, todayBorder, RoundedCornerShape(2.dp),
                                            ) else Modifier,
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(text = "Less", fontSize = 9.sp, color = cs.onSurfaceVariant.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.width(4.dp))
            listOf(
                cellZero,
                Color(0xFF1A4A2A),
                Color(0xFF2D7A3D),
                Color(0xFF3A9A50),
                Color(0xFF4AAA60),
            ).forEach { color ->
                Spacer(modifier = Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color),
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "More", fontSize = 9.sp, color = cs.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 3 · Bar chart with COUNT / MINUTES toggle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BarChartSection(
    days:          List<Triple<Long, Int, Int>>,
    maxSessions:   Int,
    maxMinutes:    Int,
    todayEpochDay: Long,
    showMinutes:   Boolean,
    onToggle:      () -> Unit,
    modifier:      Modifier = Modifier,
) {
    TokiSectionCard(modifier = modifier) {
        SectionTitle(
            title = "Last 7 days",
            detail = if (showMinutes) "Minutes focused" else "Sessions completed",
            trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ChartToggleChip(label = "COUNT",   selected = !showMinutes, onClick = { if (showMinutes)  onToggle() })
                ChartToggleChip(label = "MINUTES", selected = showMinutes,  onClick = { if (!showMinutes) onToggle() })
            }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        val cs = MaterialTheme.colorScheme
        val maxVal = if (showMinutes) maxMinutes else maxSessions

        // Value labels above bars
        Row(modifier = Modifier.fillMaxWidth()) {
            days.forEach { (_, sessions, minutes) ->
                val v = if (showMinutes) minutes else sessions
                Text(
                    text      = if (v > 0) v.toString() else "",
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize  = 10.sp,
                    color     = cs.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Bars — rounded top corners only
        val barPrimary = cs.primary
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            val gap      = 6.dp.toPx()
            val barWidth = (size.width - gap * 6f) / 7f
            val cornerPx = 6.dp.toPx()

            days.forEachIndexed { index, (epochDay, sessions, minutes) ->
                val isToday  = epochDay == todayEpochDay
                val count    = if (showMinutes) minutes else sessions
                val barColor = if (isToday) barPrimary else barPrimary.copy(alpha = 0.15f)
                val x        = index * (barWidth + gap)

                if (count > 0) {
                    val barH = (count.toFloat() / maxVal.toFloat()) * size.height
                    val capH = cornerPx.coerceAtMost(barH / 2f)
                    drawRoundRect(
                        color        = barColor,
                        topLeft      = Offset(x, size.height - barH),
                        size         = Size(barWidth, barH),
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                    )
                    drawRect(
                        color   = barColor,
                        topLeft = Offset(x, size.height - capH),
                        size    = Size(barWidth, capH),
                    )
                } else {
                    val floorH = 3.dp.toPx()
                    drawRoundRect(
                        color        = barPrimary.copy(alpha = 0.07f),
                        topLeft      = Offset(x, size.height - floorH),
                        size         = Size(barWidth, floorH),
                        cornerRadius = CornerRadius(floorH / 2f, floorH / 2f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Day-of-week labels
        Row(modifier = Modifier.fillMaxWidth()) {
            days.forEach { (epochDay, _, _) ->
                val isToday  = epochDay == todayEpochDay
                val dayLabel = LocalDate.ofEpochDay(epochDay)
                    .dayOfWeek.name.take(3).uppercase(Locale.US)
                Text(
                    text       = dayLabel,
                    modifier   = Modifier.weight(1f),
                    textAlign  = TextAlign.Center,
                    fontSize   = 10.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color      = if (isToday) cs.onSurface else cs.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChartToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (selected) cs.primary else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) cs.primary else cs.outline,
                shape = RoundedCornerShape(50.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text       = label,
            fontSize   = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (selected) cs.onPrimary else cs.onSurfaceVariant,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 4 · Recent intervals (asymmetric)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentIntervalsSection(recentSessions: List<SessionEntity>) {
    TokiSectionCard {
        SectionTitle(title = "Recent intervals")
        Spacer(modifier = Modifier.height(14.dp))

        if (recentSessions.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text      = "Complete your first session to see history",
                    fontSize  = 13.sp,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            // First — full width
            FullWidthSessionCard(session = recentSessions.first())

            // Next two — side by side
            val pair = recentSessions.drop(1).take(2)
            if (pair.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    pair.forEach { session ->
                        SmallSessionCard(session = session, modifier = Modifier.weight(1f))
                    }
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Remaining — full width
            recentSessions.drop(3).forEach { session ->
                Spacer(modifier = Modifier.height(8.dp))
                FullWidthSessionCard(session = session)
            }
        }
    }
}

@Composable
private fun FullWidthSessionCard(session: SessionEntity) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = cs.surfaceContainerHigh,
        shape    = RoundedCornerShape(14.dp),
        border   = BorderStroke(1.dp, cs.outline),
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SessionIcon(tag = session.tag, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = session.tag,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color      = cs.onSurface,
                )
                Text(
                    text     = formatTimeAgo(session.startedAt),
                    fontSize = 12.sp,
                    color    = cs.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = "${session.durationSeconds / 60}m",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = cs.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                CompletionBadge(wasCompleted = session.wasCompleted)
            }
        }
    }
}

@Composable
private fun SmallSessionCard(session: SessionEntity, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.aspectRatio(1f),
        color    = cs.surfaceContainerHigh,
        shape    = RoundedCornerShape(14.dp),
        border   = BorderStroke(1.dp, cs.outline),
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            SessionIcon(tag = session.tag, size = 32.dp)
            Column {
                Text(
                    text       = session.tag,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color      = cs.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    val mins    = session.durationSeconds / 60
                    val display = if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"
                    Text(
                        text       = display,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color      = cs.onSurface,
                    )
                    CompletionBadge(wasCompleted = session.wasCompleted, compact = true)
                }
            }
        }
    }
}

@Composable
private fun SessionIcon(tag: String, size: Dp) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.size(size),
        color    = cs.surfaceVariant,
        shape    = RoundedCornerShape(size * 0.25f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = when {
                    tag.contains("Break",  ignoreCase = true)  -> Icons.Outlined.FreeBreakfast
                    tag.contains("Code",   ignoreCase = true) ||
                    tag.contains("Sprint", ignoreCase = true)  -> Icons.Outlined.Code
                    else                                       -> Icons.Outlined.Edit
                },
                contentDescription = null,
                tint               = cs.onSurface,
                modifier           = Modifier.size(if (size >= 40.dp) 24.dp else 20.dp),
            )
        }
    }
}

@Composable
private fun CompletionBadge(wasCompleted: Boolean, compact: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Surface(
        color = if (wasCompleted) TokiGreen.copy(alpha = 0.18f) else cs.surfaceVariant,
        shape = RoundedCornerShape(50.dp),
    ) {
        Text(
            text     = if (wasCompleted) "COMPLETED" else "STOPPED",
            modifier = Modifier.padding(
                horizontal = if (compact) 4.dp else 6.dp,
                vertical   = if (compact) 2.dp else 3.dp,
            ),
            fontSize      = if (compact) 8.sp else 9.sp,
            color         = if (wasCompleted) TokiGreen else cs.onSurfaceVariant.copy(alpha = 0.7f),
            letterSpacing = 0.7.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun formatTimeAgo(startedAtMs: Long): String {
    val zone   = ZoneId.systemDefault()
    val zdt    = Instant.ofEpochMilli(startedAtMs).atZone(zone)
    val today  = LocalDate.now(zone)
    val day    = zdt.toLocalDate()
    val prefix = when (day) {
        today              -> "Today"
        today.minusDays(1) -> "Yesterday"
        else               -> day.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
    }
    val time = zdt.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
    return "$prefix at $time"
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 5 · Achievement Badges
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BadgesSection(badges: List<Badge>) {
    var selectedBadge by remember { mutableStateOf<Badge?>(null) }

    TokiSectionCard {
        val cs = MaterialTheme.colorScheme
        // Header
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text       = "ACHIEVEMENTS",
                fontSize   = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.8.sp,
                color      = cs.onSurfaceVariant,
            )
            val unlockedCount = badges.count { it.isUnlocked }
            Text(
                text     = "$unlockedCount / ${badges.size}",
                fontSize = 12.sp,
                color    = cs.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))

        // 4-column grid
        val columns = 4
        val rows = (badges.size + columns - 1) / columns
        for (row in 0 until rows) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index < badges.size) {
                        BadgeCard(
                            badge     = badges[index],
                            modifier  = Modifier.weight(1f),
                            onClick   = { selectedBadge = badges[index] },
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (row < rows - 1) Spacer(Modifier.height(10.dp))
        }
    }

    // Detail dialog
    selectedBadge?.let { badge ->
        BadgeDetailDialog(
            badge     = badge,
            onDismiss = { selectedBadge = null },
        )
    }
}

@Composable
private fun BadgeCard(
    badge:    Badge,
    modifier: Modifier = Modifier,
    onClick:  () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .aspectRatio(0.85f)
            .clickable(onClick = onClick)
            .then(if (!badge.isUnlocked) Modifier.grayscale() else Modifier),
        color  = if (badge.isUnlocked) cs.surfaceContainerHigh else cs.surfaceContainerLow,
        shape  = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (badge.isUnlocked) cs.outline else cs.outline.copy(alpha = 0.55f),
        ),
    ) {
        Column(
            modifier            = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text     = if (badge.isUnlocked) badge.emoji else "🔒",
                fontSize = 22.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text       = if (badge.isUnlocked) badge.name else "???",
                fontSize   = 9.sp,
                fontWeight = FontWeight.Medium,
                color      = if (badge.isUnlocked) cs.onSurface else cs.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign  = TextAlign.Center,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                lineHeight = 12.sp,
            )
        }
    }
}

@Composable
private fun BadgeDetailDialog(badge: Badge, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = cs.surfaceContainerLow,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = badge.emoji, fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = if (badge.isUnlocked) badge.name else "???",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = cs.onSurface,
                    textAlign  = TextAlign.Center,
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text      = badge.description,
                    fontSize  = 14.sp,
                    color     = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (badge.isUnlocked && badge.unlockedAt != null) {
                    Spacer(Modifier.height(12.dp))
                    val date = Instant.ofEpochMilli(badge.unlockedAt)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
                    Surface(
                        color = cs.surfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text     = "Unlocked $date",
                            fontSize = 11.sp,
                            color    = cs.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                } else if (!badge.isUnlocked) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text     = "Keep focusing to unlock this badge",
                        fontSize = 11.sp,
                        color    = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = cs.onSurface)
            }
        },
    )
}

