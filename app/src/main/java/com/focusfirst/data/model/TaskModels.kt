package com.focusfirst.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val status: String = "TODO", // "TODO", "IN_PROGRESS", "DONE"
    /** Epoch-milliseconds for the scheduled date. */
    val dueDate: Long? = null,
    /** Milliseconds since start of day for the scheduled time. */
    val dueTime: Long? = null,
    val targetPomodoros: Int = 4,
    val durationMinutes: Int = 25,
    val completedPomodoros: Int = 0,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val tag: String = "Focus",
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey
    val name: String,
    val colorHex: String = "#FF6C63FF", // Default primary color
    val createdAt: Long = System.currentTimeMillis()
)
