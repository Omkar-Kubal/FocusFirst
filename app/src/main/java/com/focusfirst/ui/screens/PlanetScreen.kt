package com.focusfirst.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.layer.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.BuildConfig
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    // ── Debug mode ────────────────────────────────────────────────────────────
    var debugTapCount       by remember { mutableStateOf(0) }
    var debugMode           by remember { mutableStateOf(false) }
    var debugSessionOverride by remember { mutableStateOf<Int?>(null) }

    // All stage calculations use the override when active, real data otherwise.
    val effectiveSessions = debugSessionOverride ?: totalCompleted
    val currentStage  = getStageForSessions(effectiveSessions)
    val stageLabel    = getStageLabel(selectedSkin, currentStage)
    val nextStageAt   = getNextStageAt(effectiveSessions)
    val modelPath     = selectedSkin.modelPath(currentStage)
    val stageProgress = computeStageProgress(effectiveSessions)
    val sessionsLeft  = nextStageAt?.minus(effectiveSessions)

    var showSkinSheet by rememberSaveable { mutableStateOf(false) }

    // ── Stage-up celebration ──────────────────────────────────────────────────
    var previousStage by rememberSaveable { mutableStateOf(currentStage) }
    var showStageUp   by remember { mutableStateOf(false) }

    LaunchedEffect(currentStage) {
        if (currentStage > previousStage) {
            showStageUp   = true
            previousStage = currentStage
            delay(3000)
            showStageUp = false
        }
    }

    // ── Share via GraphicsLayer ───────────────────────────────────────────────
    val graphicsLayer  = rememberGraphicsLayer()
    val coroutineScope = rememberCoroutineScope()
    val context        = LocalContext.current

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
                    modifier = Modifier.clickable {
                        debugTapCount++
                        if (debugTapCount >= 5) {
                            debugMode     = true
                            debugTapCount = 0
                        }
                    },
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
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(graphicsLayer)
                    },
            ) {
                PlanetView(
                    modelPath = modelPath,
                    size      = 280.dp,
                )
            }

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
                        .then(Modifier.padding(horizontal = 0.dp)),
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

        // ── Stage-up banner ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showStageUp,
            enter   = fadeIn() + slideInVertically { it },
            exit    = fadeOut() + slideOutVertically { -it },
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                color  = Color.White.copy(alpha = 0.1f),
                shape  = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier              = Modifier.padding(16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text       = "Your world evolved — $stageLabel",
                        color      = Color.White,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // ── Stage progress bar ────────────────────────────────────────────────
        if (currentStage < 6) {
            LinearProgressIndicator(
                progress   = { stageProgress },
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                color      = Color.White,
                trackColor = Color.White.copy(alpha = 0.10f),
                strokeCap  = StrokeCap.Round,
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
                label    = "THIS WEEK",
                value    = "${weeklyTotal(weeklySummary)}",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label    = "BEST DAY",
                value    = "${bestDayCount(weeklySummary)}",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label    = "STREAK",
                value    = "$streakDays days",
                modifier = Modifier.weight(1f),
            )
        }

        // ── Debug panel (DEBUG builds only, hidden by 5-tap gesture) ─────────
        if (BuildConfig.DEBUG && debugMode) {
            DebugPanel(
                totalCompleted       = totalCompleted,
                debugSessionOverride = debugSessionOverride,
                onOverrideSelected   = { debugSessionOverride = it },
                onClearOverride      = { debugSessionOverride = null },
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
                    coroutineScope.launch {
                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()

                        val file = File(context.cacheDir, "toki_world.png")
                        file.outputStream().use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                        }

                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file,
                        )

                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "My world has $totalCompleted focus sessions " +
                                    "with Toki ✨ — Zero ads, one tap timer",
                            )
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share your world"))
                    }
                },
                modifier       = Modifier
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

// ── Debug panel ───────────────────────────────────────────────────────────────

private val debugStageThresholds = listOf(0, 5, 15, 30, 60, 100)
private val debugRed = Color(0xFFFF3B30)

@Composable
private fun DebugPanel(
    totalCompleted: Int,
    debugSessionOverride: Int?,
    onOverrideSelected: (Int) -> Unit,
    onClearOverride: () -> Unit,
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
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text          = "DEBUG MODE",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.sp,
                color         = debugRed,
            )
            Text(
                text     = "Override session count for stage testing",
                fontSize = 11.sp,
                color    = Color.White.copy(alpha = 0.5f),
            )

            // Stage jump buttons — S1 through S6
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                debugStageThresholds.forEachIndexed { index, threshold ->
                    val stage = index + 1
                    OutlinedButton(
                        onClick          = { onOverrideSelected(threshold) },
                        modifier         = Modifier.weight(1f),
                        contentPadding   = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                        border           = BorderStroke(
                            1.dp,
                            if (debugSessionOverride == threshold)
                                debugRed
                            else
                                debugRed.copy(alpha = 0.35f),
                        ),
                        colors           = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (debugSessionOverride == threshold)
                                debugRed
                            else
                                Color.White.copy(alpha = 0.7f),
                        ),
                    ) {
                        Text(text = "S$stage", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Clear override
            TextButton(
                onClick  = onClearOverride,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.textButtonColors(
                    contentColor = Color.White.copy(alpha = 0.5f),
                ),
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
