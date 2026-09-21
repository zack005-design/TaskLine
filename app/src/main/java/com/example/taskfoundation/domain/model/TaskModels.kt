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

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val projectId: Long? = null,
    val startDateTime: Long? = null,
    val dueDateTime: Long? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.TODO,
    val progress: Int = 0,
    val isCompleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
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
