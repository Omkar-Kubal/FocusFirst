package com.focusfirst.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.data.db.DailySummary
import com.focusfirst.ui.theme.FocusColors
import com.focusfirst.viewmodel.TimerViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// ============================================================================
// StatsScreen
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: TimerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    // ── State ─────────────────────────────────────────────────────────────────
    val todayCount     by viewModel.todayCount.collectAsStateWithLifecycle()
    val totalCompleted by viewModel.totalCompleted.collectAsStateWithLifecycle()
    val weeklySummary  by viewModel.weeklySummary.collectAsStateWithLifecycle()

    // ── Chart data — recomputed only when weeklySummary changes ───────────────
    // Index 0 = 6 days ago, index 6 = today (left → right on chart).
    // DailySummary.date is an epoch-day (startedAt / 86_400_000).
    val chartData: List<Pair<Long, Int>> = remember(weeklySummary) {
        buildChartData(weeklySummary)
    }
    val chartMax: Int = remember(chartData) {
        chartData.maxOfOrNull { it.second }.takeIf { it != null && it > 0 } ?: 1
    }

    // =========================================================================
    // Scaffold
    // =========================================================================

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Stats") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {

            // ── 2. Summary row ────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    value    = todayCount,
                    label    = "sessions today",
                    emoji    = "🍅",
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    value    = totalCompleted,
                    label    = "total sessions",
                    emoji    = "✅",
                )
            }

            // ── 3. Weekly chart or empty state ────────────────────────────────
            if (totalCompleted == 0) {
                EmptyState()
            } else {
                WeeklyChartSection(
                    chartData = chartData,
                    chartMax  = chartMax,
                )

                // ── 5. Motivational footer ────────────────────────────────────
                Spacer(Modifier.height(8.dp))
                Text(
                    text      = "Every session counts. Keep going! 💪",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                )
            }
        }
    }
}

// ============================================================================
// Chart data helper  (pure function — tested independently)
// ============================================================================

/**
 * Maps [weeklySummary] onto a fixed 7-day window ending today.
 *
 * Returns a list of (epochDay, sessionCount) pairs ordered oldest → newest
 * (left-to-right on the bar chart).  Days with no matching summary entry
 * get a count of 0.
 *
 * All epoch-day arithmetic is UTC-based, consistent with how the DAO computes
 * `date` via `startedAt / 86_400_000`.
 */
private fun buildChartData(weeklySummary: List<DailySummary>): List<Pair<Long, Int>> {
    val todayEpochDay = System.currentTimeMillis() / 86_400_000L
    val summaryMap    = weeklySummary.associateBy { it.date }
    return (6 downTo 0).map { daysBack ->
        val epochDay = todayEpochDay - daysBack
        epochDay to (summaryMap[epochDay]?.sessionCount ?: 0)
    }
}

/**
 * Returns a 3-letter day abbreviation for an epoch-day value.
 * Uses the device locale so "Mon" becomes "lun" in French, etc.
 */
private fun epochDayToAbbrev(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay)
        .dayOfWeek
        .getDisplayName(TextStyle.SHORT, Locale.getDefault())
        .replaceFirstChar { it.uppercaseChar() }
        .take(3)

// ============================================================================
// Sub-composables
// ============================================================================

// ── Summary card ─────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    modifier: Modifier,
    value: Int,
    label: String,
    emoji: String,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text     = emoji,
                fontSize = 24.sp,
            )
            Text(
                text  = value.toString(),
                style = MaterialTheme.typography.displayMedium,
                color = FocusColors.TomatoRed,
            )
            Text(
                text      = label,
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Weekly chart section ──────────────────────────────────────────────────────

/**
 * Section header + count row + Canvas bar chart + day-label row.
 *
 * Text elements (counts + day abbreviations) are plain Compose [Text]
 * composables in [Row]s flanking the [Canvas], avoiding the complexity of
 * TextMeasurer / drawText inside the draw scope while preserving full
 * font-scale and accessibility support.
 */
@Composable
private fun WeeklyChartSection(
    chartData: List<Pair<Long, Int>>,
    chartMax: Int,
) {
    val todayEpochDay = remember { System.currentTimeMillis() / 86_400_000L }

    Spacer(Modifier.height(24.dp))

    Text(
        text     = "Last 7 days",
        style    = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    // ── Session-count row (above bars) ────────────────────────────────────────
    Row(modifier = Modifier.fillMaxWidth()) {
        chartData.forEach { (_, count) ->
            Text(
                text      = if (count > 0) count.toString() else "",
                modifier  = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style     = MaterialTheme.typography.labelSmall,
                color     = FocusColors.TomatoRed,
            )
        }
    }

    Spacer(Modifier.height(4.dp))

    // ── Bar chart canvas ──────────────────────────────────────────────────────
    BarChart(
        chartData = chartData,
        chartMax  = chartMax,
        todayEpochDay = todayEpochDay,
        modifier  = Modifier
            .fillMaxWidth()
            .height(160.dp),
    )

    Spacer(Modifier.height(6.dp))

    // ── Day-label row (below bars) ────────────────────────────────────────────
    Row(modifier = Modifier.fillMaxWidth()) {
        chartData.forEachIndexed { index, (epochDay, _) ->
            val isToday = epochDay == todayEpochDay
            Text(
                text      = epochDayToAbbrev(epochDay),
                modifier  = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style     = MaterialTheme.typography.labelSmall,
                color     = if (isToday) FocusColors.TomatoRed
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

// ── Bar chart ─────────────────────────────────────────────────────────────────

/**
 * Canvas-based horizontal bar chart for the 7-day session summary.
 *
 * Layout inside the canvas (all values in px, converted from dp):
 *   - 7 bars, 6 gaps of [BAR_GAP] between them, no outer margin.
 *   - barWidth  = (canvasWidth − 6 × BAR_GAP) / 7
 *   - barHeight = (count / maxCount) × MAX_BAR_HEIGHT  (0 → MAX_BAR_HEIGHT px)
 *   - Bars are bottom-aligned; y-origin is at the canvas bottom.
 *   - Rounded top corners via [drawRoundRect] with a 4 dp corner radius.
 *
 * Today's bar (index 6, rightmost) uses full [FocusColors.TomatoRed].
 * Past bars use [FocusColors.TomatoRed] at 35 % opacity.
 * Days with zero sessions show a 2 dp floor mark at 15 % opacity so the
 * chart has a visible baseline even when data is sparse.
 */
@Composable
private fun BarChart(
    chartData: List<Pair<Long, Int>>,
    chartMax: Int,
    todayEpochDay: Long,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val gapPx          = 8.dp.toPx()
        val barWidth       = (size.width - gapPx * 6f) / 7f
        val maxBarHeightPx = 120.dp.toPx()
        val cornerPx       = 4.dp.toPx()
        val floorPx        = 2.dp.toPx()

        chartData.forEachIndexed { index, (epochDay, count) ->
            val isToday     = epochDay == todayEpochDay
            val barColor    = if (isToday) FocusColors.TomatoRed
                              else FocusColors.TomatoRed.copy(alpha = 0.35f)
            val x           = index * (barWidth + gapPx)

            if (count > 0) {
                val barHeightPx = (count.toFloat() / chartMax.toFloat()) * maxBarHeightPx
                drawRoundRect(
                    color        = barColor,
                    topLeft      = Offset(x, size.height - barHeightPx),
                    size         = Size(barWidth, barHeightPx),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                )
            } else {
                // Floor mark — gives visual structure on empty days
                drawRoundRect(
                    color        = FocusColors.TomatoRed.copy(alpha = 0.15f),
                    topLeft      = Offset(x, size.height - floorPx),
                    size         = Size(barWidth, floorPx),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
                )
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text     = "🍅",
            fontSize = 64.sp,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text  = "No sessions yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "Complete your first focus session\nto see stats here",
            style     = MaterialTheme.typography.bodySmall,
            color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
    }
}
