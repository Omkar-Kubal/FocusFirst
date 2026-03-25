package com.focusfirst.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FreeBreakfast
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.BuildConfig
import com.focusfirst.billing.BillingViewModel
import com.focusfirst.data.db.SessionEntity
import com.focusfirst.data.model.getNextStageAt
import com.focusfirst.data.model.getStageForSessions
import com.focusfirst.data.model.getStageLabel
import com.focusfirst.data.model.modelPath
import com.focusfirst.ui.components.PlanetView
import com.focusfirst.ui.components.SkinSelectorSheet
import com.focusfirst.viewmodel.SettingsViewModel
import com.focusfirst.viewmodel.TimerViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// StatsScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatsScreen(
    timerViewModel:       TimerViewModel    = hiltViewModel(),
    billingViewModel:     BillingViewModel  = hiltViewModel(),
    settingsViewModel:    SettingsViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit        = {},
) {
    val totalCompleted by timerViewModel.totalCompleted.collectAsStateWithLifecycle()
    val todayCount     by timerViewModel.todayCount.collectAsStateWithLifecycle()
    val weeklySummary  by timerViewModel.weeklySummary.collectAsStateWithLifecycle()
    val recentSessions by timerViewModel.recentSessions.collectAsStateWithLifecycle()
    val streakDays     by timerViewModel.streakDays.collectAsStateWithLifecycle()
    val isPro          by billingViewModel.isPro.collectAsStateWithLifecycle()
    val selectedSkin   by settingsViewModel.planetSkin.collectAsStateWithLifecycle()
    val allSessions    by timerViewModel.allSessions.collectAsStateWithLifecycle()

    // ── Debug state (DEBUG builds only) ──────────────────────────────────────
    var debugMode            by remember { mutableStateOf(false) }
    var debugSessionOverride by remember { mutableStateOf<Int?>(null) }

    // ── Computed ─────────────────────────────────────────────────────────────
    val effectiveSessions = debugSessionOverride ?: totalCompleted
    val currentStage  = getStageForSessions(effectiveSessions)
    val stageLabel    = getStageLabel(selectedSkin, currentStage)
    val nextStageAt   = getNextStageAt(effectiveSessions)
    val sessionsLeft  = nextStageAt?.minus(effectiveSessions)
    val stageProgress = computeStageProgress(effectiveSessions)
    val modelPath     = selectedSkin.modelPath(currentStage)
    val weeklyTotal   = weeklySummary.sumOf { it.sessionCount }
    val todayEpochDay = System.currentTimeMillis() / 86_400_000L

    val daysChart = (6 downTo 0).map { offset ->
        val day = todayEpochDay - offset
        val row = weeklySummary.find { it.date == day }
        Triple(day, row?.sessionCount ?: 0, row?.totalMinutes ?: 0)
    }
    val maxSessions = daysChart.maxOfOrNull { it.second }.let { if (it != null && it > 0) it else 1 }
    val maxMinutes  = daysChart.maxOfOrNull { it.third  }.let { if (it != null && it > 0) it else 1 }

    var showSkinSheet by rememberSaveable { mutableStateOf(false) }
    var showMinutes   by remember { mutableStateOf(false) }

    // ── Layout ───────────────────────────────────────────────────────────────
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // 1 · Planet hero — edge to edge
        item {
            PlanetHeroSection(
                modelPath        = modelPath,
                stageLabel       = stageLabel,
                sessionsLeft     = sessionsLeft,
                stageProgress    = stageProgress,
                currentStage     = currentStage,
                onShowSkin       = { showSkinSheet = true },
                onActivateDebug  = { debugMode = !debugMode },
            )
        }

        // 2 · Stat cards
        item {
            StatCardsRow(
                totalCompleted = totalCompleted,
                weeklyTotal    = weeklyTotal,
                streakDays     = streakDays,
            )
        }

        // 3 · Focus heatmap
        item {
            FocusHeatmapSection(allSessions = allSessions)
        }

        // 4 · Bar chart
        item {
            BarChartSection(
                days          = daysChart,
                maxSessions   = maxSessions,
                maxMinutes    = maxMinutes,
                todayEpochDay = todayEpochDay,
                showMinutes   = showMinutes,
                onToggle      = { showMinutes = !showMinutes },
            )
        }

        // 5 · Recent intervals (asymmetric)
        item {
            RecentIntervalsSection(recentSessions = recentSessions)
        }

        // Debug panel — hidden behind 5-tap on stage pill
        if (BuildConfig.DEBUG && debugMode) {
            item {
                DebugPanel(
                    totalCompleted       = totalCompleted,
                    debugSessionOverride = debugSessionOverride,
                    onOverrideSelected   = { debugSessionOverride = it },
                    onClearOverride      = { debugSessionOverride = null },
                )
            }
        }
    }

    // Skin selector sheet
    if (showSkinSheet) {
        SkinSelectorSheet(
            currentSkin    = selectedSkin,
            isPro          = isPro,
            onSkinSelected = { skin ->
                settingsViewModel.updatePlanetSkin(skin)
                showSkinSheet = false
            },
            onDismiss      = { showSkinSheet = false },
            onUpgradeClick = {
                showSkinSheet = false
                billingViewModel.openUpgradeSheet()
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Item 1 · Planet hero
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlanetHeroSection(
    modelPath:       String,
    stageLabel:      String,
    sessionsLeft:    Int?,
    stageProgress:   Float,
    currentStage:    Int,
    onShowSkin:      () -> Unit,
    onActivateDebug: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .pointerInput(Unit) {
                var tapCount = 0
                var lastTap  = 0L
                detectTapGestures {
                    val now = System.currentTimeMillis()
                    if (now - lastTap < 500L) {
                        tapCount++
                        if (tapCount >= 5) {
                            onActivateDebug()
                            tapCount = 0
                        }
                    } else {
                        tapCount = 1
                    }
                    lastTap = now
                }
            },
    ) {
        // Dark green ambient glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color  = Color(0xFF0A2A0A),
                radius = size.width * 0.55f,
                center = Offset(size.width / 2f, size.height * 0.45f),
                alpha  = 0.8f,
            )
        }

        // Star field — seed 42 keeps positions stable across recompositions
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rng = Random(42)
            repeat(50) {
                val x    = rng.nextFloat() * size.width
                val y    = rng.nextFloat() * size.height
                val dist = sqrt(
                    (x - size.width / 2f).pow(2) + (y - size.height * 0.4f).pow(2),
                )
                if (dist > size.width * 0.35f) {
                    drawCircle(
                        color  = Color.White,
                        radius = rng.nextFloat() * 1.5f + 0.5f,
                        center = Offset(x, y),
                        alpha  = rng.nextFloat() * 0.6f + 0.2f,
                    )
                }
            }
        }

        // Planet
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .height(260.dp),
            contentAlignment = Alignment.Center,
        ) {
            PlanetView(
                modelPath = modelPath,
                modifier  = Modifier.size(240.dp),
            )
        }

        // Palette icon — top right
        IconButton(
            onClick  = onShowSkin,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp),
        ) {
            Icon(
                imageVector        = Icons.Outlined.Palette,
                contentDescription = "Choose world skin",
                tint               = Color.White.copy(alpha = 0.7f),
            )
        }

        // Bottom overlay: stage pill → progress bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Stage pill
            Surface(
                color  = Color.Black.copy(alpha = 0.6f),
                shape  = RoundedCornerShape(50.dp),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
            ) {
                Text(
                    text     = stageLabel,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    fontSize = 13.sp,
                    color    = Color.White.copy(alpha = 0.9f),
                )
            }

            if (sessionsLeft != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text          = "$sessionsLeft MORE SESSIONS TO EVOLVE",
                    fontSize      = 10.sp,
                    color         = Color.White.copy(alpha = 0.35f),
                    letterSpacing = 0.08.em,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (currentStage < 6) {
                LinearProgressIndicator(
                    progress   = { stageProgress },
                    modifier   = Modifier
                        .width(160.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    color      = Color.White,
                    trackColor = Color.White.copy(alpha = 0.15f),
                    strokeCap  = StrokeCap.Round,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Item 2 · Stat cards
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatCardsRow(
    totalCompleted: Int,
    weeklyTotal:    Int,
    streakDays:     Int,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
    Surface(
        modifier = modifier,
        color    = Color(0xFF0D0D0D),
        shape    = RoundedCornerShape(12.dp),
        border   = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text       = value,
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
                if (showFlame) {
                    Text(text = " 🔥", fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text          = label,
                fontSize      = 9.sp,
                color         = Color.White.copy(alpha = 0.35f),
                letterSpacing = 0.12.em,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Item 3 · Focus heatmap (GitHub style, 26 weeks)
// ─────────────────────────────────────────────────────────────────────────────

private val MONTH_NAMES = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

@Composable
private fun FocusHeatmapSection(allSessions: List<SessionEntity>) {
    val completedCount = allSessions.count { it.wasCompleted }
    val year           = Calendar.getInstance().get(Calendar.YEAR)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        Text(
            text       = "Focus history",
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text     = "$completedCount sessions in $year",
            fontSize = 12.sp,
            color    = Color.White.copy(alpha = 0.4f),
        )
        Spacer(modifier = Modifier.height(12.dp))
        FocusHeatmap(sessions = allSessions)
    }
}

@Composable
private fun FocusHeatmap(sessions: List<SessionEntity>) {
    val sessionsByDay = sessions
        .filter { it.wasCompleted }
        .groupBy { it.startedAt / 86_400_000L }
        .mapValues { it.value.size }

    val today       = System.currentTimeMillis() / 86_400_000L
    val weeksToShow = 26
    val startDay    = today - weeksToShow * 7L
    val cellSize    = 11.dp
    val cellGap     = 2.dp
    val totalCell   = cellSize + cellGap    // 13 dp per week column
    val dayLabelW   = 20.dp

    Column {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {

            // Day-of-week labels — M / W / F only, offset by month-label row height
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
                            Text(text = label, fontSize = 8.sp, color = Color.White.copy(alpha = 0.3f))
                        }
                    }
                }
            }

            // Month labels + cell grid
            Column {
                // Month label row — one slot per week, labels appear at month boundaries
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
                                color    = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.width(totalCell * 4),
                            )
                        } else {
                            Spacer(modifier = Modifier.width(totalCell))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Cell grid
                Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
                    for (week in 0 until weeksToShow) {
                        Column(verticalArrangement = Arrangement.spacedBy(cellGap)) {
                            for (dayOfWeek in 0..6) {
                                val epochDay = startDay + (week * 7).toLong() + dayOfWeek
                                val count    = sessionsByDay[epochDay] ?: 0
                                val isToday  = epochDay == today
                                val isFuture = epochDay > today

                                val cellColor = when {
                                    isFuture   -> Color(0xFF111111)
                                    count == 0 -> Color(0xFF1A1A1A)
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
                                                0.8.dp, Color.White, RoundedCornerShape(2.dp),
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
            Text(text = "Less", fontSize = 9.sp, color = Color.White.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.width(4.dp))
            listOf(
                Color(0xFF1A1A1A),
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
            Text(text = "More", fontSize = 9.sp, color = Color.White.copy(alpha = 0.3f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Item 4 · Bar chart with COUNT / MINUTES toggle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BarChartSection(
    days:          List<Triple<Long, Int, Int>>,
    maxSessions:   Int,
    maxMinutes:    Int,
    todayEpochDay: Long,
    showMinutes:   Boolean,
    onToggle:      () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text       = "Last 7 Days",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ChartToggleChip(label = "COUNT",   selected = !showMinutes, onClick = { if (showMinutes)  onToggle() })
                ChartToggleChip(label = "MINUTES", selected = showMinutes,  onClick = { if (!showMinutes) onToggle() })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                    color     = Color.White.copy(alpha = 0.5f),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Bars — rounded top corners only
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
                val barColor = if (isToday) Color.White else Color.White.copy(alpha = 0.15f)
                val x        = index * (barWidth + gap)

                if (count > 0) {
                    val barH = (count.toFloat() / maxVal.toFloat()) * size.height
                    val capH = cornerPx.coerceAtMost(barH / 2f)
                    // Full roundRect for the top curves, then a plain rect covers the bottom corners
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
                        color        = Color.White.copy(alpha = 0.07f),
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
                    color      = if (isToday) Color.White else Color.White.copy(alpha = 0.4f),
                )
            }
        }
    }
}

@Composable
private fun ChartToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (selected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text       = label,
            fontSize   = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (selected) Color.White else Color.White.copy(alpha = 0.4f),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Item 5 · Recent intervals (asymmetric)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentIntervalsSection(recentSessions: List<SessionEntity>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 120.dp),
    ) {
        Text(
            text       = "Recent Intervals",
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (recentSessions.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text      = "Complete your first session to see history",
                    fontSize  = 13.sp,
                    color     = Color.White.copy(alpha = 0.3f),
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = Color(0xFF111111),
        shape    = RoundedCornerShape(12.dp),
        border   = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
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
                    color      = Color.White,
                )
                Text(
                    text     = formatTimeAgo(session.startedAt),
                    fontSize = 12.sp,
                    color    = Color.White.copy(alpha = 0.4f),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = "${session.durationSeconds / 60}m",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
                Spacer(modifier = Modifier.height(2.dp))
                CompletionBadge(wasCompleted = session.wasCompleted)
            }
        }
    }
}

@Composable
private fun SmallSessionCard(session: SessionEntity, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.aspectRatio(1f),
        color    = Color(0xFF111111),
        shape    = RoundedCornerShape(12.dp),
        border   = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
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
                    color      = Color.White,
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
                        color      = Color.White,
                    )
                    CompletionBadge(wasCompleted = session.wasCompleted, compact = true)
                }
            }
        }
    }
}

@Composable
private fun SessionIcon(tag: String, size: Dp) {
    Surface(
        modifier = Modifier.size(size),
        color    = Color.White.copy(alpha = 0.06f),
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
                tint               = Color.White.copy(alpha = 0.6f),
                modifier           = Modifier.size(size * 0.5f),
            )
        }
    }
}

@Composable
private fun CompletionBadge(wasCompleted: Boolean, compact: Boolean = false) {
    Surface(
        color = if (wasCompleted) Color(0xFF1A9E5F).copy(alpha = 0.2f)
                else Color.White.copy(alpha = 0.06f),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text     = if (wasCompleted) "COMPLETED" else "STOPPED",
            modifier = Modifier.padding(
                horizontal = if (compact) 4.dp else 6.dp,
                vertical   = if (compact) 2.dp else 3.dp,
            ),
            fontSize      = if (compact) 8.sp else 9.sp,
            color         = if (wasCompleted) Color(0xFF1A9E5F) else Color.White.copy(alpha = 0.3f),
            letterSpacing = 0.06.em,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Debug panel (DEBUG builds only)
// ─────────────────────────────────────────────────────────────────────────────

private val debugStageThresholds = listOf(0, 5, 15, 30, 60, 100)
private val debugRed = Color(0xFFFF3B30)

@Composable
private fun DebugPanel(
    totalCompleted:       Int,
    debugSessionOverride: Int?,
    onOverrideSelected:   (Int) -> Unit,
    onClearOverride:      () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color  = Color(0xFF1A0000),
        shape  = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, debugRed.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text          = "⚠ DEBUG MODE ACTIVE",
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.sp,
                color         = debugRed,
            )
            Text(
                text     = "Override session count for stage testing",
                fontSize = 11.sp,
                color    = Color.White.copy(alpha = 0.5f),
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                debugStageThresholds.forEachIndexed { index, threshold ->
                    val stage = index + 1
                    OutlinedButton(
                        onClick        = { onOverrideSelected(threshold) },
                        modifier       = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                        border         = BorderStroke(
                            1.dp,
                            if (debugSessionOverride == threshold) debugRed
                            else debugRed.copy(alpha = 0.35f),
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (debugSessionOverride == threshold) debugRed
                                           else Color.White.copy(alpha = 0.7f),
                        ),
                    ) {
                        Text(text = "S$stage", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            TextButton(
                onClick  = onClearOverride,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.5f)),
            ) {
                Text(text = "Clear override", fontSize = 12.sp)
            }
            Text(
                text     = "Active: ${debugSessionOverride?.let { "$it (override)" } ?: "real ($totalCompleted)"}",
                fontSize = 11.sp,
                color    = Color.White.copy(alpha = 0.4f),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun computeStageProgress(sessions: Int): Float {
    val thresholds = listOf(0, 5, 15, 30, 60, 100, 200)
    val stage = getStageForSessions(sessions)
    if (stage == 6) return 1f
    val low  = thresholds[stage - 1]
    val high = thresholds[stage]
    return ((sessions - low).toFloat() / (high - low)).coerceIn(0f, 1f)
}

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
