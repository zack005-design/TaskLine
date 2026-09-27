package com.example.taskfoundation.domain.model

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT,
}

enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    BLOCKED,
    DONE,
}

enum class RepeatRule { NONE, DAILY, WEEKLY, MONTHLY, YEARLY }

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val projectId: Long? = null,
    /** UTC-midnight encoding of a calendar date, independent of the device time zone. */
    val startDateTime: Long? = null,
    /** UTC-midnight encoding of a calendar date, independent of the device time zone. */
    val dueDateTime: Long? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.TODO,
    val progress: Int = 0,
    val isCompleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val dueTimeMinutes: Int? = null,
    val reminderMinutes: Int? = null,
    val repeatRule: RepeatRule = RepeatRule.NONE,
    val repeatInterval: Int = 1,
    val repeatAnchor: Long? = null,
    val snoozedUntil: Long? = null,
    val lastNotifiedAt: Long? = null,
    val importKey: String? = null,
)

data class Subtask(
    val id: Long = 0,
    val taskId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)

data class Project(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val color: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

data class Tag(
    val id: Long = 0,
    val name: String,
    val color: Long? = null,
    val createdAt: Long,
)
