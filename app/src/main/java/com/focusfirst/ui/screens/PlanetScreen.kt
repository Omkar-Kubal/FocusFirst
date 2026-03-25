package com.focusfirst.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.billing.BillingViewModel
import com.focusfirst.data.db.DailySummary
import com.focusfirst.data.model.PlanetSkin
import com.focusfirst.data.model.getNextStageAt
import com.focusfirst.data.model.getStageForSessions
import com.focusfirst.data.model.getStageLabel
import com.focusfirst.data.model.modelPath
import com.focusfirst.ui.components.PlanetView
import com.focusfirst.ui.components.SkinSelectorSheet
import com.focusfirst.viewmodel.SettingsViewModel
import com.focusfirst.viewmodel.TimerViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun PlanetScreen(
    timerViewModel: TimerViewModel = hiltViewModel(),
    billingViewModel: BillingViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val totalCompleted by timerViewModel.totalCompleted.collectAsStateWithLifecycle()
    val streakDays     by timerViewModel.streakDays.collectAsStateWithLifecycle()
    val todayCount     by timerViewModel.todayCount.collectAsStateWithLifecycle()
    val weeklySummary  by timerViewModel.weeklySummary.collectAsStateWithLifecycle()
    val isPro          by billingViewModel.isPro.collectAsStateWithLifecycle()
    val selectedSkin   by settingsViewModel.planetSkin.collectAsStateWithLifecycle()

    val currentStage = getStageForSessions(totalCompleted)
    val stageLabel   = getStageLabel(selectedSkin, currentStage)
    val nextStageAt  = getNextStageAt(totalCompleted)
    val modelPath    = selectedSkin.modelPath(currentStage)
    val stageProgress = computeStageProgress(totalCompleted)
    val sessionsLeft  = nextStageAt?.minus(totalCompleted)

    var showSkinSheet by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {

        // ── Top bar ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 48.dp, bottom = 8.dp),
        ) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    text = "Your world",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "$totalCompleted sessions",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 0.12.sp,
                )
            }
            IconButton(
                onClick = { showSkinSheet = true },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = "Choose world skin",
                    tint = Color.White,
                )
            }
        }

        // ── Planet view ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            PlanetView(
                modelPath = modelPath,
                modifier  = Modifier.align(Alignment.Center),
                size      = 280.dp,
            )

            // Stage label pill
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .then(
                            Modifier.padding(horizontal = 0.dp)
                        ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.15f),
                    ),
                ) {
                    Text(
                        text = stageLabel,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }

                if (sessionsLeft != null) {
                    Text(
                        text = "$sessionsLeft more to evolve",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.3f),
                    )
                }
            }
        }

        // ── Stage progress bar ────────────────────────────────────────────────
        if (currentStage < 6) {
            LinearProgressIndicator(
                progress = { stageProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.10f),
                strokeCap = StrokeCap.Round,
            )
        }

        // ── Stats row ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard(
                label = "THIS WEEK",
                value = "${weeklyTotal(weeklySummary)}",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "BEST DAY",
                value = "${bestDayCount(weeklySummary)}",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "STREAK",
                value = "$streakDays days",
                modifier = Modifier.weight(1f),
            )
        }

        // ── Share FAB ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            FloatingActionButton(
                onClick = {
                    sharePlanet(
                        context = context,
                        skin    = selectedSkin,
                        stage   = currentStage,
                        sessions = totalCompleted,
                        streak  = streakDays,
                    )
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(52.dp),
                containerColor = Color.White,
                contentColor   = Color.Black,
                shape          = CircleShape,
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Share,
                    contentDescription = "Share your world",
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    // ── Skin selector sheet ───────────────────────────────────────────────────
    if (showSkinSheet) {
        SkinSelectorSheet(
            currentSkin     = selectedSkin,
            isPro           = isPro,
            onSkinSelected  = { skin ->
                settingsViewModel.updatePlanetSkin(skin)
                showSkinSheet = false
            },
            onDismiss       = { showSkinSheet = false },
            onUpgradeClick  = {
                showSkinSheet = false
                billingViewModel.openUpgradeSheet()
            },
        )
    }
}

// ── StatCard ──────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color    = Color(0xFF111111),
        shape    = RoundedCornerShape(12.dp),
        border   = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            Color.White.copy(alpha = 0.08f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text          = label,
                fontSize      = 9.sp,
                color         = Color.White.copy(alpha = 0.35f),
                letterSpacing = 0.12.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text       = value,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Medium,
                color      = Color.White,
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun computeStageProgress(sessions: Int): Float {
    val thresholds = listOf(0, 5, 15, 30, 60, 100, 200)
    val stage = getStageForSessions(sessions)
    if (stage == 6) return 1f
    val low  = thresholds[stage - 1]
    val high = thresholds[stage]
    return ((sessions - low).toFloat() / (high - low)).coerceIn(0f, 1f)
}

private fun weeklyTotal(summary: List<DailySummary>): Int =
    summary.sumOf { it.sessionCount }

private fun bestDayCount(summary: List<DailySummary>): Int =
    summary.maxOfOrNull { it.sessionCount } ?: 0

private fun sharePlanet(
    context: android.content.Context,
    skin: PlanetSkin,
    stage: Int,
    sessions: Int,
    streak: Int,
) {
    val shareText = buildString {
        append("My ${skin.displayName} is at stage $stage on Toki! ")
        append("$sessions focus sessions completed")
        if (streak > 0) append(" • $streak day streak")
        append(" 🌍")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share your world"))
}
