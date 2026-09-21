package com.example.taskfoundation.domain.repository

import com.example.taskfoundation.domain.model.Subtask
import com.example.taskfoundation.domain.model.Tag
import com.example.taskfoundation.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(projectId: Long? = null): Flow<List<Task>>

    fun observeTask(taskId: Long): Flow<Task?>

    fun observeSubtasks(taskId: Long): Flow<List<Subtask>>

    fun observeTags(taskId: Long): Flow<List<Tag>>

    suspend fun saveTask(task: Task): Long

    suspend fun deleteTask(taskId: Long): Boolean

    suspend fun setTaskCompleted(taskId: Long, isCompleted: Boolean): Boolean

    suspend fun saveSubtask(subtask: Subtask): Long

    suspend fun deleteSubtask(subtaskId: Long): Boolean

    suspend fun attachTag(taskId: Long, tag: Tag): Long

    suspend fun detachTag(taskId: Long, tagId: Long): Boolean
}
