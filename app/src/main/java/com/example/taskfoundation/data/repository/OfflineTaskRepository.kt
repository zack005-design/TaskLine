package com.example.taskfoundation.data.repository

import com.example.taskfoundation.data.local.dao.TaskDao
import com.example.taskfoundation.data.mapper.toDomain
import com.example.taskfoundation.data.mapper.toEntity
import com.example.taskfoundation.domain.model.Subtask
import com.example.taskfoundation.domain.model.Tag
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.model.TaskStatus
import com.example.taskfoundation.domain.repository.TaskRepository
import com.example.taskfoundation.domain.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineTaskRepository(
    private val taskDao: TaskDao,
    private val clock: Clock,
) : TaskRepository {
    override fun observeTasks(projectId: Long?): Flow<List<Task>> =
        (projectId?.let(taskDao::observeByProject) ?: taskDao.observeAll())
            .map { entities -> entities.map { it.toDomain() } }

    override fun observeTask(taskId: Long): Flow<Task?> =
        taskDao.observeById(taskId).map { it?.toDomain() }

    override fun observeSubtasks(taskId: Long): Flow<List<Subtask>> =
        taskDao.observeSubtasks(taskId).map { entities -> entities.map { it.toDomain() } }

    override fun observeTags(taskId: Long): Flow<List<Tag>> =
        taskDao.observeTags(taskId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveTask(task: Task): Long {
        require(task.title.isNotBlank()) { "Task title cannot be blank" }
        require(task.progress in 0..100) { "Task progress must be between 0 and 100" }
        require(task.startDateTime == null || task.dueDateTime == null || task.startDateTime <= task.dueDateTime) {
            "Task due date cannot be earlier than its start date"
        }
        val now = clock.nowMillis()
        val completed = task.isCompleted || task.status == TaskStatus.DONE
        val normalized = task.copy(
            title = task.title.trim(),
            progress = if (completed) 100 else task.progress,
            status = if (completed) TaskStatus.DONE else task.status,
            isCompleted = completed,
            createdAt = if (task.id == 0L) now else task.createdAt,
            updatedAt = now,
        )
        val rowId = taskDao.upsert(normalized.toEntity())
        return if (task.id == 0L) rowId else task.id
    }

    override suspend fun deleteTask(taskId: Long): Boolean = taskDao.deleteById(taskId) == 1

    override suspend fun setTaskCompleted(taskId: Long, isCompleted: Boolean): Boolean =
        taskDao.setCompleted(taskId, isCompleted, clock.nowMillis()) == 1

    override suspend fun saveSubtask(subtask: Subtask): Long {
        require(subtask.title.isNotBlank()) { "Subtask title cannot be blank" }
        val now = clock.nowMillis()
        val normalized = subtask.copy(
            title = subtask.title.trim(),
            createdAt = if (subtask.id == 0L) now else subtask.createdAt,
            updatedAt = now,
        )
        val rowId = taskDao.upsertSubtask(normalized.toEntity())
        return if (subtask.id == 0L) rowId else subtask.id
    }

    override suspend fun deleteSubtask(subtaskId: Long): Boolean = taskDao.deleteSubtask(subtaskId) == 1

    override suspend fun attachTag(taskId: Long, tag: Tag): Long {
        require(tag.name.isNotBlank()) { "Tag name cannot be blank" }
        return taskDao.createOrAttachTag(
            taskId = taskId,
            tag = tag.copy(name = tag.name.trim(), createdAt = tag.createdAt.takeIf { it > 0 } ?: clock.nowMillis())
                .toEntity(),
        )
    }

    override suspend fun detachTag(taskId: Long, tagId: Long): Boolean =
        taskDao.removeTaskTag(taskId, tagId) == 1
}
