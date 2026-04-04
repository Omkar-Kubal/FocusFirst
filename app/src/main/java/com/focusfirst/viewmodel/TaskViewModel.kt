package com.focusfirst.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfirst.billing.BillingManager
import com.focusfirst.data.db.TaskDao
import com.focusfirst.data.model.TaskEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskDao: TaskDao,
    private val billingManager: BillingManager,
) : ViewModel() {

    val activeTasks = taskDao.observeActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeCount = taskDao.observeActiveCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val isPro = billingManager.isPro
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val canAddTask: Boolean
        get() = isPro.value || activeCount.value < 3

    /** ID of the task currently linked to the running timer session. */
    var selectedTaskId by mutableStateOf<Int?>(null)

    fun addTask(title: String, target: Int = 4) {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskDao.insert(TaskEntity(title = title, targetPomodoros = target))
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { taskDao.delete(task) }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch { taskDao.update(task.copy(isCompleted = true)) }
    }

    fun incrementPomodoro(taskId: Int) {
        viewModelScope.launch { taskDao.incrementPomodoro(taskId) }
    }
}
