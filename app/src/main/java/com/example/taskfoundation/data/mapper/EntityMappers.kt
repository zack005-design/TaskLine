package com.example.taskfoundation.data.mapper

import com.example.taskfoundation.data.local.entity.ProjectEntity
import com.example.taskfoundation.data.local.entity.SubtaskEntity
import com.example.taskfoundation.data.local.entity.TagEntity
import com.example.taskfoundation.data.local.entity.TaskEntity
import com.example.taskfoundation.domain.model.Project
import com.example.taskfoundation.domain.model.Subtask
import com.example.taskfoundation.domain.model.Tag
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.model.TaskPriority
import com.example.taskfoundation.domain.model.TaskStatus
import com.example.taskfoundation.domain.model.RepeatRule

fun TaskEntity.toDomain() = Task(
    id = id,
    title = title,
    description = description,
    projectId = projectId,
    startDateTime = startDateTime,
    dueDateTime = dueDateTime,
    priority = TaskPriority.valueOf(priority),
    status = TaskStatus.valueOf(status),
    progress = progress,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
    dueTimeMinutes = dueTimeMinutes, reminderMinutes = reminderMinutes,
    repeatRule = RepeatRule.valueOf(repeatRule), repeatInterval = repeatInterval, repeatAnchor = repeatAnchor,
    snoozedUntil = snoozedUntil, lastNotifiedAt = lastNotifiedAt, importKey = importKey,
)

fun Task.toEntity() = TaskEntity(
    id = id,
    title = title,
    description = description,
    projectId = projectId,
    startDateTime = startDateTime,
    dueDateTime = dueDateTime,
    priority = priority.name,
    status = status.name,
    progress = progress,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
    dueTimeMinutes = dueTimeMinutes, reminderMinutes = reminderMinutes,
    repeatRule = repeatRule.name, repeatInterval = repeatInterval, repeatAnchor = repeatAnchor,
    snoozedUntil = snoozedUntil, lastNotifiedAt = lastNotifiedAt, importKey = importKey,
)

fun SubtaskEntity.toDomain() = Subtask(id, taskId, title, isCompleted, sortOrder, createdAt, updatedAt)

fun Subtask.toEntity() = SubtaskEntity(id, taskId, title, isCompleted, sortOrder, createdAt, updatedAt)

fun ProjectEntity.toDomain() = Project(id, name, description, color, createdAt, updatedAt)

fun Project.toEntity() = ProjectEntity(id, name, description, color, createdAt, updatedAt)

fun TagEntity.toDomain() = Tag(id, name, color, createdAt)

fun Tag.toEntity() = TagEntity(id, name, color, createdAt)
