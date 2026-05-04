package com.focusfirst.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusfirst.data.model.TaskEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSheet(
    tasks: List<TaskEntity>,
    selectedTaskId: Int?,
    isPro: Boolean,
    activeCount: Int,
    onTaskSelected: (TaskEntity?) -> Unit,
    onAddTask: (String) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onCompleteTask: (TaskEntity) -> Unit,
    onUpgradeClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF111111),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text       = "Tasks",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
                if (!isPro) {
                    Text(
                        text     = "$activeCount/3 free",
                        fontSize = 12.sp,
                        color    = Color.White.copy(alpha = 0.5f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Quick-add input ───────────────────────────────────────────────
            var newTask by remember { mutableStateOf("") }
            val canAdd  = isPro || activeCount < 3

            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value         = newTask,
                    onValueChange = { newTask = it },
                    placeholder   = {
                        Text(
                            text  = if (canAdd) "Add a task…" else "Upgrade for more tasks",
                            color = Color.White.copy(alpha = 0.5f),
                        )
                    },
                    modifier        = Modifier.weight(1f),
                    enabled         = canAdd,
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newTask.isNotBlank() && canAdd) {
                                onAddTask(newTask.trim())
                                newTask = ""
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color.White.copy(alpha = 0.3f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        disabledTextColor    = Color.White.copy(alpha = 0.5f),
                        disabledBorderColor  = Color.White.copy(alpha = 0.06f),
                    ),
                )

                Spacer(Modifier.width(8.dp))

                if (!canAdd) {
                    TextButton(onClick = onUpgradeClick) {
                        Text("Pro", color = Color(0xFF1A9E5F))
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (newTask.isNotBlank()) {
                                onAddTask(newTask.trim())
                                newTask = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.Add,
                            contentDescription = "Add task",
                            tint               = Color.White,
                            modifier           = Modifier.size(24.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── "No task" option ──────────────────────────────────────────────
            TaskRow(
                title               = "No task selected",
                completedPomodoros  = 0,
                targetPomodoros     = 0,
                isSelected          = selectedTaskId == null,
                onClick             = { onTaskSelected(null) },
                onComplete          = {},
                onDelete            = {},
                showActions         = false,
            )

            Spacer(Modifier.height(8.dp))

            // ── Task list ─────────────────────────────────────────────────────
            tasks.forEach { task ->
                TaskRow(
                    title              = task.title,
                    completedPomodoros = task.completedPomodoros,
                    targetPomodoros    = task.targetPomodoros,
                    isSelected         = selectedTaskId == task.id,
                    onClick            = { onTaskSelected(task) },
                    onComplete         = { onCompleteTask(task) },
                    onDelete           = { onDeleteTask(task) },
                    showActions        = true,
                )
                Spacer(Modifier.height(6.dp))
            }

            if (tasks.isEmpty()) {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text     = "No tasks yet — add one above",
                        fontSize = 13.sp,
                        color    = Color.White.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    title: String,
    completedPomodoros: Int,
    targetPomodoros: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    showActions: Boolean,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color  = if (isSelected) Color.White.copy(alpha = 0.12f) else Color(0xFF1A1A1A),
        shape  = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (isSelected) 1.dp else 0.5.dp,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.08f),
        ),
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Selection dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color  = if (isSelected) Color(0xFFE84B1A) else Color.Transparent,
                        shape  = CircleShape,
                    )
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = title,
                    fontSize   = 14.sp,
                    color      = Color.White,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                )
                if (showActions && targetPomodoros > 0) {
                    Text(
                        text     = "$completedPomodoros/$targetPomodoros 🍅",
                        fontSize = 11.sp,
                        color    = Color.White.copy(alpha = 0.5f),
                    )
                }
            }

            if (showActions) {
                IconButton(
                    onClick  = onComplete,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.CheckCircle,
                        contentDescription = "Complete task",
                        tint               = Color.White.copy(alpha = 0.4f),
                        modifier           = Modifier.size(24.dp),
                    )
                }
                IconButton(
                    onClick  = onDelete,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Delete,
                        contentDescription = "Delete task",
                        tint               = Color.White.copy(alpha = 0.4f),
                        modifier           = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
