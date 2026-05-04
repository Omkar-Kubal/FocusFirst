package com.focusfirst.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.data.model.TaskEntity
import com.focusfirst.viewmodel.TaskViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onNavigateToHome: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel(),
    billingViewModel: com.focusfirst.billing.BillingViewModel = hiltViewModel()
) {
    val selectedDate by viewModel.selectedDate
    val tasks        by viewModel.tasksForSelectedDate.collectAsStateWithLifecycle()
    val overdueTasks by viewModel.overdueTasks.collectAsStateWithLifecycle()
    val tags         by viewModel.tags.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("TASKS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    if (viewModel.canAddTask) {
                        showAddDialog = true
                    } else {
                        billingViewModel.openUpgradeSheet()
                    }
                },
                containerColor = if (viewModel.canAddTask) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (viewModel.canAddTask) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                if (viewModel.canAddTask) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Task", modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Filled.Lock, contentDescription = "Pro Required", modifier = Modifier.size(24.dp))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Calendar Card ──────────────────────────────────────────────────
            item {
                CalendarCard(
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.selectDate(it) }
                )
            }

            // ── Overdue Tasks ─────────────────────────────────────────────────
            if (overdueTasks.isNotEmpty()) {
                item {
                    Text(
                        "OVERDUE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(overdueTasks) { task ->
                    TaskItem(
                        task = task,
                        onStatusChange = { viewModel.updateTaskStatus(task, it) },
                        onStartFocus = { 
                            viewModel.selectedTaskId = task.id
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("toki://timer/start?duration=${task.durationMinutes}&task=${android.net.Uri.encode(task.title)}"))
                            onNavigateToHome()
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // ── Tasks for Selected Date ───────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (selectedDate == LocalDate.now()) "TODAY" 
                        else selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (tasks.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No tasks for this day", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                items(tasks) { task ->
                    TaskItem(
                        task = task,
                        onStatusChange = { viewModel.updateTaskStatus(task, it) },
                        onStartFocus = { 
                            viewModel.selectedTaskId = task.id
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("toki://timer/start?duration=${task.durationMinutes}&task=${android.net.Uri.encode(task.title)}"))
                            onNavigateToHome()
                            context.startActivity(intent)
                        }
                    )
                }
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog) {
        val isPro by billingViewModel.isPro.collectAsStateWithLifecycle()
        AddTaskDialog(
            initialDate = selectedDate,
            isPro = isPro,
            tags = tags,
            onDismiss = { showAddDialog = false },
            onUpgradeClick = { 
                showAddDialog = false
                billingViewModel.openUpgradeSheet() 
            },
            onConfirm = { title, desc, status, date, time, duration, tag ->
                viewModel.addTask(title, desc, status, date, time, 4, duration, tag)
                showAddDialog = false
            },
            onCreateTag = { name, color ->
                viewModel.addTag(name, color)
            }
        )
    }
}

@Composable
fun CalendarCard(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val cs = MaterialTheme.colorScheme

    Card(
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Month Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = "Prev Month",
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    currentMonth.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Next Month",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Weekday Labels
            Row(Modifier.fillMaxWidth()) {
                DayOfWeek.entries.forEach { day ->
                    Text(
                        day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.outline
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Calendar Grid
            val daysInMonth = currentMonth.lengthOfMonth()
            val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7 // 0=Sun, 1=Mon...
            val totalCells = (daysInMonth + firstDayOfWeek + 6) / 7 * 7
            
            Column {
                for (row in 0 until totalCells / 7) {
                    Row(Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val dayIndex = row * 7 + col - firstDayOfWeek + 1
                            if (dayIndex in 1..daysInMonth) {
                                val date = currentMonth.atDay(dayIndex)
                                val isSelected = date == selectedDate
                                val isToday = date == LocalDate.now()

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) cs.primary else Color.Transparent)
                                        .clickable { onDateSelected(date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayIndex.toString(),
                                        color = when {
                                            isSelected -> cs.onPrimary
                                            isToday -> cs.primary
                                            else -> cs.onSurface
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            } else {
                                Spacer(Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    task: TaskEntity,
    onStatusChange: (String) -> Unit,
    onStartFocus: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDone = task.status == "DONE"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onStatusChange(if (isDone) "TODO" else "DONE") }) {
                Icon(
                    imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = "Toggle Complete",
                    tint = if (isDone) cs.primary else cs.outline,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                    color = if (isDone) cs.outline else cs.onSurface
                )
                if (task.description.isNotBlank()) {
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.outline,
                        maxLines = 1
                    )
                }
            }

            if (!isDone) {
                IconButton(
                    onClick = onStartFocus,
                    modifier = Modifier.background(cs.surfaceContainerHighest, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Start Focus",
                        tint = cs.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    initialDate: LocalDate,
    isPro: Boolean = true,
    tags: List<com.focusfirst.data.model.TagEntity> = emptyList(),
    onDismiss: () -> Unit,
    onUpgradeClick: () -> Unit = {},
    onConfirm: (String, String, String, LocalDate, Long?, Int, String) -> Unit,
    onCreateTag: (String, String) -> Unit = { _, _ -> }
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var durationMinutes by remember { mutableStateOf(25) }
    var selectedTag by remember { mutableStateOf("Focus") }
    var showCreateTag by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    val cs = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NEW TASK", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Text(
                    "Date: ${selectedDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.outline
                )
                Spacer(Modifier.height(8.dp))
                Text("Timer Duration", style = MaterialTheme.typography.labelMedium, color = cs.outline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 25, 45, 60).forEach { dur ->
                        val isLocked = dur == 60 && !isPro
                        FilterChip(
                            selected = durationMinutes == dur,
                            onClick = { 
                                if (isLocked) {
                                    onUpgradeClick()
                                } else {
                                    durationMinutes = dur 
                                }
                            },
                            label = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${dur}m") 
                                    if (isLocked) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Filled.Lock, contentDescription = "Pro Required", modifier = Modifier.size(12.dp))
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cs.primary,
                                selectedLabelColor = cs.onPrimary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Tag", style = MaterialTheme.typography.labelMedium, color = cs.outline)
                
                // We always ensure "Focus" is at least an option if tags list is empty or doesn't contain it
                val displayTags = if (tags.any { it.name == "Focus" }) tags else listOf(com.focusfirst.data.model.TagEntity("Focus", "#FF6C63FF")) + tags
                
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayTags.size) { index ->
                        val t = displayTags[index]
                        FilterChip(
                            selected = selectedTag == t.name,
                            onClick = { selectedTag = t.name },
                            label = { Text(t.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cs.primary,
                                selectedLabelColor = cs.onPrimary
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { 
                                if (!isPro) onUpgradeClick()
                                else showCreateTag = true
                            },
                            label = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Add, contentDescription = "New Tag", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("New")
                                    if (!isPro) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Filled.Lock, contentDescription = "Pro Required", modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        )
                    }
                }
                
                if (showCreateTag) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        label = { Text("New Tag Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                if (newTagName.isNotBlank()) {
                                    onCreateTag(newTagName.trim(), "#FF6C63FF")
                                    selectedTag = newTagName.trim()
                                    newTagName = ""
                                    showCreateTag = false
                                }
                            }) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Create", tint = cs.primary)
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, desc, "TODO", selectedDate, null, durationMinutes, selectedTag) },
                enabled = title.isNotBlank()
            ) {
                Text("ADD", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
        containerColor = cs.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp)
    )
}
