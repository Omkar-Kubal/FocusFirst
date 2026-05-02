package com.focusfirst.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.billing.BillingViewModel
import com.focusfirst.data.model.AmbientSound
import com.focusfirst.data.model.IntervalPreset
import com.focusfirst.data.model.TimerMode
import com.focusfirst.data.model.TimerPhase
import com.focusfirst.data.model.TimerState
import com.focusfirst.ui.components.BreakSuggestionSheet
import com.focusfirst.ui.components.SoundSelectorSheet
import com.focusfirst.ui.components.TaskSheet
import com.focusfirst.util.BatteryPromptDialog
import com.focusfirst.viewmodel.SettingsViewModel
import com.focusfirst.viewmodel.TaskViewModel
import com.focusfirst.viewmodel.TimerViewModel
import kotlin.math.cos
import kotlin.math.sin

private val PomodoroRed = Color(0xFFFF2727)

@Composable
fun HomeScreen(
    viewModel: TimerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    billingViewModel: BillingViewModel = hiltViewModel(),
    taskViewModel: TaskViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
) {
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val todayCount by viewModel.todayCount.collectAsStateWithLifecycle()
    val streakDays by viewModel.streakDays.collectAsStateWithLifecycle()
    val totalCompleted by viewModel.totalCompleted.collectAsStateWithLifecycle()
    val dailyGoal by settingsViewModel.dailyGoal.collectAsStateWithLifecycle()
    val ambientSound by settingsViewModel.ambientSound.collectAsStateWithLifecycle()
    val ambientVolume by settingsViewModel.ambientVolume.collectAsStateWithLifecycle()
    val isPro by billingViewModel.isPro.collectAsStateWithLifecycle()
    val activeTasks by taskViewModel.activeTasks.collectAsStateWithLifecycle()
    val activeCount by taskViewModel.activeCount.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var selectedMode by rememberSaveable { mutableStateOf(TimerMode.POMODORO) }
    var showStopDialog by rememberSaveable { mutableStateOf(false) }
    var showSoundSheet by remember { mutableStateOf(false) }
    var showBreakSheet by remember { mutableStateOf(false) }
    var showTaskSheet by remember { mutableStateOf(false) }
    var isLongBreak by remember { mutableStateOf(false) }

    val selectedTask = activeTasks.find { it.id == taskViewModel.selectedTaskId }
    val timerActive = timerState.isRunning || timerState.isPaused

    // Keep screen on while timer is running
    val window = (context as? ComponentActivity)?.window
    DisposableEffect(timerActive) {
        if (timerActive) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(timerState.timerMode, timerActive) {
        if (timerActive) selectedMode = timerState.timerMode
    }

    LaunchedEffect(Unit) {
        viewModel.focusSessionCompleted.collect {
            taskViewModel.selectedTaskId?.let { id ->
                taskViewModel.incrementPomodoro(id)
            }
        }
    }

    LaunchedEffect(timerState.phase) {
        if (timerState.isRunning && timerState.timerMode == TimerMode.POMODORO) {
            when (timerState.phase) {
                TimerPhase.SHORT_BREAK -> {
                    isLongBreak = false
                    showBreakSheet = true
                }
                TimerPhase.LONG_BREAK -> {
                    isLongBreak = true
                    showBreakSheet = true
                }
                TimerPhase.FOCUS -> Unit
            }
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Stop session?") },
            text = { Text("Your progress will be saved if you've focused for 30+ seconds.") },
            confirmButton = {
                TextButton(onClick = { viewModel.stop(); showStopDialog = false }) {
                    Text("Stop")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) { Text("Keep going") }
            },
        )
    }

    if (showSoundSheet) {
        SoundSelectorSheet(
            currentSound = ambientSound,
            currentVolume = ambientVolume,
            isPro = isPro,
            onSoundSelected = { sound ->
                settingsViewModel.updateAmbientSound(sound)
                com.focusfirst.analytics.TokiAnalytics.logSoundSelected(sound.displayName)
                viewModel.updateSound(sound, ambientVolume)
            },
            onVolumeChanged = { volume ->
                settingsViewModel.updateAmbientVolume(volume)
                viewModel.updateVolume(volume)
            },
            onDismiss = { showSoundSheet = false },
            onUpgradeClick = { showSoundSheet = false; billingViewModel.openUpgradeSheet() },
        )
    }

    if (showBreakSheet) {
        BreakSuggestionSheet(
            isLongBreak = isLongBreak,
            breakDurationSeconds = timerState.totalSeconds,
            breakSessionCount = totalCompleted,
            onDismiss = {
                showBreakSheet = false
                com.focusfirst.analytics.TokiAnalytics.logBreakSuggestionDismissed()
            },
        )
    }

    if (showTaskSheet) {
        TaskSheet(
            tasks = activeTasks,
            selectedTaskId = taskViewModel.selectedTaskId,
            isPro = isPro,
            activeCount = activeCount,
            onTaskSelected = { task ->
                taskViewModel.selectedTaskId = task?.id
                showTaskSheet = false
            },
            onAddTask = { taskViewModel.addTask(it) },
            onDeleteTask = { taskViewModel.deleteTask(it) },
            onCompleteTask = { taskViewModel.completeTask(it) },
            onUpgradeClick = { showTaskSheet = false; billingViewModel.openUpgradeSheet() },
            onDismiss = { showTaskSheet = false },
        )
    }

    BatteryPromptDialog(settingsViewModel = settingsViewModel, context = context)

    val fabIcon = if (timerState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow
    val fabLabel = when {
        timerState.isRunning -> "Pause"
        timerState.isPaused -> "Resume"
        else -> "Start"
    }
    val fabAction: () -> Unit = when {
        timerState.isRunning -> ({ viewModel.pause() })
        timerState.isPaused -> ({ viewModel.resume() })
        selectedMode == TimerMode.FLOW -> ({ viewModel.startFlow() })
        else -> ({ viewModel.start() })
    }

    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 46.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TokiHeader(onSettingsClick = onNavigateToSettings)
        Spacer(Modifier.height(34.dp))
        ModeSwitch(
            selectedMode = selectedMode,
            isTimerActive = timerActive,
            onModeSelected = { selectedMode = it },
        )
        Spacer(Modifier.height(26.dp))
        DurationSelector(
            activePreset = if (selectedMode == TimerMode.FLOW) IntervalPreset.FLOW else timerState.preset,
            isRunning = timerActive,
            enabled = selectedMode == TimerMode.POMODORO,
            onSelect = { viewModel.selectPreset(it) },
        )
        Spacer(Modifier.height(22.dp))
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val timerSize = maxWidth.coerceAtMost(330.dp)
            TokiTimerFace(
                timerState = timerState,
                selectedMode = selectedMode,
                faceSize = timerSize,
            )
        }
        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlineActionPill(
                icon = Icons.Outlined.MusicNote,
                label = if (ambientSound == AmbientSound.NONE) "Add sound" else ambientSound.displayName,
                contentDescription = "Select ambient sound",
                modifier = Modifier.weight(1f),
                onClick = { showSoundSheet = true },
            )
            OutlineActionPill(
                icon = Icons.Outlined.TaskAlt,
                label = selectedTask?.title ?: "No task",
                contentDescription = "Select task",
                modifier = Modifier.weight(1f),
                onClick = { showTaskSheet = true },
            )
        }
        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(cs.primary)
                .clickable { fabAction() }
                .semantics { contentDescription = fabLabel },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = fabIcon,
                contentDescription = fabLabel,
                tint = cs.onPrimary,
                modifier = Modifier.size(42.dp),
            )
        }
        AnimatedVisibility(visible = !timerState.isIdle) {
            IconButton(
                onClick = { showStopDialog = true },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Stop,
                    contentDescription = "Stop session",
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Spacer(Modifier.height(if (timerState.isIdle) 30.dp else 14.dp))
        MetricsRow(
            todayCount = todayCount,
            dailyGoal = dailyGoal,
            streakDays = streakDays,
        )
    }
}

@Composable
private fun TokiHeader(onSettingsClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TokiLogoMark(Modifier.size(50.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            text = "Toki",
            color = cs.onBackground,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .border(1.dp, cs.outline, CircleShape)
                .clickable { onSettingsClick() }
                .semantics { contentDescription = "Settings" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = cs.onBackground,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun TokiLogoMark(modifier: Modifier = Modifier) {
    val primary    = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background
    Canvas(modifier = modifier) {
        val stroke = 4.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.36f
        drawCircle(
            color = primary,
            radius = radius,
            center = center,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawCircle(
            color = background,
            radius = radius * 0.56f,
            center = center,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = background,
            startAngle = -102f,
            sweepAngle = 24f,
            useCenter = false,
            topLeft = Offset(center.x - radius - stroke, center.y - radius - stroke),
            size = Size((radius + stroke) * 2f, (radius + stroke) * 2f),
            style = Stroke(stroke + 2.dp.toPx(), cap = StrokeCap.Round),
        )
        drawCircle(
            color = primary,
            radius = radius * 0.42f,
            center = center,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawCircle(
            color = background,
            radius = radius * 0.12f,
            center = center,
        )
        drawCircle(
            color = PomodoroRed,
            radius = 4.dp.toPx(),
            center = Offset(center.x + radius * 0.64f, center.y - radius * 1.02f),
        )
    }
}

@Composable
private fun ModeSwitch(
    selectedMode: TimerMode,
    isTimerActive: Boolean,
    onModeSelected: (TimerMode) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .border(1.dp, cs.outline, RoundedCornerShape(50.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ModeSegment(
            icon = "🍅",
            label = "Pomodoro",
            selected = selectedMode == TimerMode.POMODORO,
            enabled = !isTimerActive,
            modifier = Modifier.weight(1f),
            onClick = { onModeSelected(TimerMode.POMODORO) },
        )
        ModeSegment(
            icon = "🌊",
            label = "Flow",
            selected = selectedMode == TimerMode.FLOW,
            enabled = !isTimerActive,
            modifier = Modifier.weight(1f),
            onClick = { onModeSelected(TimerMode.FLOW) },
        )
    }
}

@Composable
private fun ModeSegment(
    icon: String,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val textColor = if (selected) cs.onSurface else cs.onSurfaceVariant.copy(alpha = 0.7f)
    Row(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(42.dp))
            .background(if (selected) cs.surfaceContainerHigh else Color.Transparent)
            .clickable(enabled = enabled) { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = icon, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun DurationSelector(
    activePreset: IntervalPreset,
    isRunning: Boolean,
    enabled: Boolean,
    onSelect: (IntervalPreset) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IntervalPreset.entries.forEach { preset ->
            val selected = preset == activePreset
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (selected) cs.primary else cs.surfaceContainerLow)
                    .border(
                        width = if (selected) 0.dp else 1.dp,
                        color = if (enabled) cs.outline else cs.outline.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(50.dp),
                    )
                    .clickable(enabled = enabled && !isRunning) { onSelect(preset) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${preset.focusMinutes}m",
                    color = when {
                        selected -> cs.onPrimary
                        enabled  -> cs.onSurfaceVariant
                        else     -> cs.onSurfaceVariant.copy(alpha = 0.45f)
                    },
                    fontSize = 21.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TokiTimerFace(
    timerState: TimerState,
    selectedMode: TimerMode,
    faceSize: Dp,
) {
    val visualMode = if (timerState.isIdle) selectedMode else timerState.timerMode
    val targetProgress = if (timerState.isIdle && selectedMode == TimerMode.FLOW) {
        0f
    } else {
        timerState.progress
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 800),
        label = "tokiTimerProgress",
    )
    val displayTime = if (timerState.isIdle && selectedMode == TimerMode.FLOW) {
        "45:00"
    } else {
        timerState.displayTime
    }
    val phaseLabel = when {
        visualMode == TimerMode.FLOW -> "FLOW"
        timerState.phase == TimerPhase.SHORT_BREAK -> "SHORT BREAK"
        timerState.phase == TimerPhase.LONG_BREAK -> "LONG BREAK"
        else -> "FOCUS"
    }

    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(faceSize)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 6.dp.toPx()
            val inset = strokePx / 2f + 3.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = cs.surfaceVariant,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokePx, cap = StrokeCap.Round),
            )
            if (animatedProgress > 0f) {
                drawArc(
                    color = cs.primary,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(strokePx, cap = StrokeCap.Round),
                )
            }
            val angle = Math.toRadians((-90f + 360f * animatedProgress).toDouble())
            val radius = (size.minDimension - inset * 2f) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val handleCenter = Offset(
                x = center.x + cos(angle).toFloat() * radius,
                y = center.y + sin(angle).toFloat() * radius,
            )
            drawCircle(
                color = cs.primary,
                radius = 5.dp.toPx(),
                center = handleCenter,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = displayTime,
                color = cs.onBackground,
                fontSize = 76.sp,
                lineHeight = 80.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = phaseLabel,
                color = cs.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 7.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun OutlineActionPill(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(50.dp))
            .border(1.dp, cs.outline, RoundedCornerShape(50.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp)
            .semantics { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = cs.onSurface,
            modifier = Modifier.size(25.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = cs.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MetricsRow(
    todayCount: Int,
    dailyGoal: Int,
    streakDays: Int,
) {
    val goalDenominator = dailyGoal.coerceAtLeast(1)
    val progress = (todayCount / goalDenominator.toFloat()).coerceIn(0f, 1f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val cs = MaterialTheme.colorScheme
        MetricCard(
            label = "DAILY GOAL",
            value = "$todayCount / $goalDenominator",
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(cs.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(cs.onSurface.copy(alpha = 0.3f)),
                )
            }
        }
        MetricCard(
            label = "STREAK",
            value = if (streakDays == 0) "-" else streakDays.toString(),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "day streak",
                color = cs.onSurfaceVariant,
                fontSize = 15.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingContent: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cs.surfaceContainerLow)
            .border(1.dp, cs.outline, RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = cs.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
            maxLines = 1,
        )
        Text(
            text = value,
            color = cs.onSurface,
            fontSize = 32.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        supportingContent()
    }
}
