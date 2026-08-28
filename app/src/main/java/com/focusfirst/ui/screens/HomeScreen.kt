package com.focusfirst.ui.screens

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.focusfirst.ui.components.FocusedVoid
import com.focusfirst.ui.components.SoundSelectorSheet
import com.focusfirst.ui.components.TaskSheet
import com.focusfirst.ui.components.TokiChronoRing
import com.focusfirst.ui.components.TokiIconButton
import com.focusfirst.ui.components.TokiMetric
import com.focusfirst.ui.components.TokiPill
import com.focusfirst.ui.components.TokiSection
import com.focusfirst.ui.home.TimerUiText
import com.focusfirst.ui.mascot.TokiMascot
import com.focusfirst.viewmodel.SettingsViewModel
import com.focusfirst.viewmodel.TaskViewModel
import com.focusfirst.viewmodel.TimerViewModel

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
    val sessionsBeforeLongBreak by settingsViewModel.sessionsBeforeLongBreak.collectAsStateWithLifecycle()
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
    val uiText = TimerUiText.from(timerState, selectedMode, sessionsBeforeLongBreak)

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
            taskViewModel.selectedTaskId?.let { id -> taskViewModel.incrementPomodoro(id) }
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

    StopDialog(
        visible = showStopDialog,
        onStop = {
            viewModel.stop()
            showStopDialog = false
        },
        onDismiss = { showStopDialog = false },
    )

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
            onUpgradeClick = {
                showSoundSheet = false
                billingViewModel.openUpgradeSheet()
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
            onUpgradeClick = {
                showTaskSheet = false
                billingViewModel.openUpgradeSheet()
            },
            onDismiss = { showTaskSheet = false },
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

    val primaryAction: () -> Unit = when {
        timerState.isRunning -> ({ viewModel.pause() })
        timerState.isPaused -> ({ viewModel.resume() })
        selectedMode == TimerMode.FLOW -> ({ viewModel.startFlow() })
        else -> ({ viewModel.start() })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 42.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FocusTopBar(
            timerActive = timerActive,
            todayCount = todayCount,
            dailyGoal = dailyGoal,
            onSettingsClick = onNavigateToSettings,
        )
        Spacer(Modifier.height(if (timerActive) 22.dp else 28.dp))

        if (timerActive) {
            FocusActiveLayout(
                timerState = timerState,
                uiText = uiText,
                selectedTaskTitle = selectedTask?.title,
                ambientSound = ambientSound,
                onSoundClick = { showSoundSheet = true },
                onTaskClick = { showTaskSheet = true },
                onStopClick = { showStopDialog = true },
                onPrimaryAction = primaryAction,
            )
        } else {
            FocusIdleLayout(
                timerState = timerState,
                selectedMode = selectedMode,
                uiText = uiText,
                todayCount = todayCount,
                dailyGoal = dailyGoal,
                streakDays = streakDays,
                selectedTaskTitle = selectedTask?.title,
                ambientSound = ambientSound,
                onModeSelected = { selectedMode = it },
                onPresetSelected = { viewModel.selectPreset(it) },
                onSoundClick = { showSoundSheet = true },
                onTaskClick = { showTaskSheet = true },
                onPrimaryAction = primaryAction,
            )
        }
    }
}

@Composable
private fun FocusTopBar(
    timerActive: Boolean,
    todayCount: Int,
    dailyGoal: Int,
    onSettingsClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TokiLogoMark(Modifier.size(if (timerActive) 36.dp else 44.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (timerActive) "Toki" else "Focus",
                color = cs.onBackground,
                fontSize = if (timerActive) 22.sp else 30.sp,
                lineHeight = if (timerActive) 26.sp else 34.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            if (!timerActive) {
                Text(
                    text = "$todayCount of ${dailyGoal.coerceAtLeast(1)} sessions today",
                    color = cs.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(1.dp, cs.outline, CircleShape)
                .semantics { contentDescription = "Settings" },
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                tint = cs.onBackground,
            )
        }
    }
}

@Composable
private fun FocusIdleLayout(
    timerState: TimerState,
    selectedMode: TimerMode,
    uiText: TimerUiText,
    todayCount: Int,
    dailyGoal: Int,
    streakDays: Int,
    selectedTaskTitle: String?,
    ambientSound: AmbientSound,
    onModeSelected: (TimerMode) -> Unit,
    onPresetSelected: (IntervalPreset) -> Unit,
    onSoundClick: () -> Unit,
    onTaskClick: () -> Unit,
    onPrimaryAction: () -> Unit,
) {
    ModeAndPresetPicker(
        selectedMode = selectedMode,
        activePreset = if (selectedMode == TimerMode.FLOW) IntervalPreset.FLOW else timerState.preset,
        onModeSelected = onModeSelected,
        onPresetSelected = onPresetSelected,
    )
    Spacer(Modifier.height(22.dp))
    FocusTimerBlock(timerState, selectedMode, uiText, selectedTaskTitle)
    Spacer(Modifier.height(22.dp))
    FocusControls(
        primaryLabel = uiText.primaryAction,
        primaryIcon = Icons.Filled.PlayArrow,
        onPrimaryAction = onPrimaryAction,
        showStop = false,
        onStopClick = {},
        ambientSound = ambientSound,
        selectedTaskTitle = selectedTaskTitle,
        onSoundClick = onSoundClick,
        onTaskClick = onTaskClick,
    )
    Spacer(Modifier.height(22.dp))
    TodayStrip(todayCount, dailyGoal, streakDays)
}

@Composable
private fun FocusActiveLayout(
    timerState: TimerState,
    uiText: TimerUiText,
    selectedTaskTitle: String?,
    ambientSound: AmbientSound,
    onSoundClick: () -> Unit,
    onTaskClick: () -> Unit,
    onStopClick: () -> Unit,
    onPrimaryAction: () -> Unit,
) {
    PhaseChip(uiText.phase, uiText.cycle, timerState.isPaused)
    Spacer(Modifier.height(18.dp))
    FocusTimerBlock(timerState, timerState.timerMode, uiText, selectedTaskTitle)
    Spacer(Modifier.height(20.dp))
    FocusControls(
        primaryLabel = uiText.primaryAction,
        primaryIcon = if (timerState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
        onPrimaryAction = onPrimaryAction,
        showStop = true,
        onStopClick = onStopClick,
        ambientSound = ambientSound,
        selectedTaskTitle = selectedTaskTitle,
        onSoundClick = onSoundClick,
        onTaskClick = onTaskClick,
    )
}

@Composable
private fun ModeAndPresetPicker(
    selectedMode: TimerMode,
    activePreset: IntervalPreset,
    onModeSelected: (TimerMode) -> Unit,
    onPresetSelected: (IntervalPreset) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            TokiPill(
                text = "Pomodoro",
                selected = selectedMode == TimerMode.POMODORO,
                modifier = Modifier.weight(1f),
                onClick = { onModeSelected(TimerMode.POMODORO) },
            )
            TokiPill(
                text = "Flow 45",
                selected = selectedMode == TimerMode.FLOW,
                modifier = Modifier.weight(1f),
                onClick = { onModeSelected(TimerMode.FLOW) },
            )
        }
        AnimatedVisibility(
            visible = selectedMode == TimerMode.POMODORO,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                IntervalPreset.entries.filter { it != IntervalPreset.FLOW }.forEach { preset ->
                    TokiPill(
                        text = "${preset.focusMinutes}m",
                        selected = activePreset == preset,
                        modifier = Modifier.weight(1f),
                        onClick = { onPresetSelected(preset) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PhaseChip(phase: String, cycle: String, paused: Boolean) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(cs.surfaceContainerLow)
            .border(1.dp, cs.outline, RoundedCornerShape(999.dp))
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (paused) "$phase paused - $cycle" else "$phase - $cycle",
            color = cs.onSurface,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FocusTimerBlock(
    timerState: TimerState,
    selectedMode: TimerMode,
    uiText: TimerUiText,
    selectedTaskTitle: String?,
) {
    val displayTime = if (timerState.isIdle && selectedMode == TimerMode.FLOW) {
        "45:00"
    } else {
        timerState.displayTime
    }
    val visualPhase = when {
        timerState.isIdle -> TimerPhase.FOCUS
        else -> timerState.phase
    }
    val progress = if (timerState.isIdle && selectedMode == TimerMode.FLOW) 0f else timerState.progress

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val ringSize = maxWidth.coerceAtMost(342.dp)
        TokiChronoRing(
            displayTime = displayTime,
            phase = visualPhase,
            progress = progress,
            accessibilitySummary = uiText.accessibilitySummary,
            ringSize = ringSize,
            supportingText = uiText.phase,
            isPaused = timerState.isPaused,
        )
        if (timerState.isIdle) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 22.dp)
                    .size(74.dp),
                contentAlignment = Alignment.Center,
            ) {
                TokiMascot(
                    isIdle = true,
                    isPaused = false,
                    mode = selectedMode,
                    phase = TimerPhase.FOCUS,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(
        text = selectedTaskTitle?.let { "Task: $it" } ?: uiText.nextAction,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FocusControls(
    primaryLabel: String,
    primaryIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onPrimaryAction: () -> Unit,
    showStop: Boolean,
    onStopClick: () -> Unit,
    ambientSound: AmbientSound,
    selectedTaskTitle: String?,
    onSoundClick: () -> Unit,
    onTaskClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TokiIconButton(
            icon = Icons.Outlined.MusicNote,
            label = if (ambientSound == AmbientSound.NONE) "Choose ambient sound" else "Ambient sound: ${ambientSound.displayName}",
            active = ambientSound != AmbientSound.NONE,
            onClick = onSoundClick,
        )
        Spacer(Modifier.width(18.dp))
        Button(
            onClick = onPrimaryAction,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = cs.primary,
                contentColor = cs.onPrimary,
            ),
            modifier = Modifier
                .height(64.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = primaryLabel
                },
        ) {
            Icon(
                imageVector = primaryIcon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(primaryLabel, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(18.dp))
        if (showStop) {
            TokiIconButton(
                icon = Icons.Outlined.Close,
                label = "Stop and save progress",
                active = false,
                onClick = onStopClick,
            )
        } else {
            TokiIconButton(
                icon = Icons.Outlined.TaskAlt,
                label = selectedTaskTitle?.let { "Session task: $it" } ?: "Choose session task",
                active = selectedTaskTitle != null,
                onClick = onTaskClick,
            )
        }
    }
    if (showStop) {
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            TokiIconButton(
                icon = Icons.Outlined.TaskAlt,
                label = selectedTaskTitle?.let { "Session task: $it" } ?: "Choose session task",
                active = selectedTaskTitle != null,
                onClick = onTaskClick,
            )
        }
    }
}

@Composable
private fun TodayStrip(todayCount: Int, dailyGoal: Int, streakDays: Int) {
    TokiSection(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TokiMetric("today", "$todayCount/${dailyGoal.coerceAtLeast(1)}", Modifier.weight(1f))
            TokiMetric("streak", if (streakDays == 0) "-" else "$streakDays days", Modifier.weight(1f))
            TokiMetric("mode", "quiet", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StopDialog(
    visible: Boolean,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stop and save progress?") },
        text = { Text("Progress is saved after 30 seconds. You can resume with a fresh session anytime.") },
        confirmButton = {
            TextButton(onClick = onStop) { Text("Stop") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep focusing") }
        },
    )
}

@Composable
private fun TokiLogoMark(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
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
            color = FocusedVoid.FocusRed,
            radius = 4.dp.toPx(),
            center = Offset(center.x + radius * 0.64f, center.y - radius * 1.02f),
        )
    }
}
