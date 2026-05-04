package com.focusfirst.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfirst.billing.BillingManager
import com.focusfirst.data.db.TaskDao
import com.focusfirst.data.db.TagDao
import com.focusfirst.data.model.TaskEntity
import com.focusfirst.data.model.TagEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskDao: TaskDao,
    private val tagDao: TagDao,
    private val billingManager: BillingManager,
) : ViewModel() {

    private val _selectedDate = mutableStateOf(java.time.LocalDate.now())
    val selectedDate: androidx.compose.runtime.State<java.time.LocalDate> = _selectedDate

    val activeTasks = taskDao.observeActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeCount = taskDao.observeActiveCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val isPro = billingManager.isPro
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val canAddTask: Boolean
        get() = isPro.value || activeCount.value < 10 // Increased limit for free users slightly

    val tags = tagDao.observeAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Tasks for the currently selected calendar date. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val tasksForSelectedDate = androidx.compose.runtime.derivedStateOf {
        val start = _selectedDate.value.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end   = _selectedDate.value.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        taskDao.observeTasksForDate(start, end)
    }.value.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Incomplete tasks from previous days. */
    val overdueTasks = taskDao.observeOverdueTasks(
        java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** ID of the task currently linked to the running timer session. */
    var selectedTaskId by mutableStateOf<Int?>(null)

    fun selectDate(date: java.time.LocalDate) {
        _selectedDate.value = date
    }

    fun addTask(
        title: String,
        description: String = "",
        status: String = "TODO",
        date: java.time.LocalDate? = null,
        timeMillis: Long? = null,
        target: Int = 4,
        durationMinutes: Int = 25,
        tagName: String = "Focus"
    ) {
        if (title.isBlank()) return
        val dueDate = date?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        viewModelScope.launch {
            taskDao.insert(
                TaskEntity(
                    title       = title,
                    description = description,
                    status      = status,
                    dueDate     = dueDate,
                    dueTime     = timeMillis,
                    targetPomodoros = target,
                    durationMinutes = durationMinutes,
                    tag         = tagName
                )
            )
        }
    }

    fun addTag(name: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            tagDao.insertTag(TagEntity(name = name, colorHex = colorHex))
        }
    }

    fun updateTaskStatus(task: TaskEntity, newStatus: String) {
        viewModelScope.launch {
            taskDao.update(task.copy(status = newStatus, isCompleted = newStatus == "DONE"))
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { taskDao.delete(task) }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch { 
            taskDao.update(task.copy(status = "DONE", isCompleted = true)) 
        }
    }

    fun incrementPomodoro(taskId: Int) {
        viewModelScope.launch { taskDao.incrementPomodoro(taskId) }
    }
}
