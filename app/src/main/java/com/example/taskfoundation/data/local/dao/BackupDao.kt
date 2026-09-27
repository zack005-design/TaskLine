package com.example.taskfoundation.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.taskfoundation.data.local.entity.*

@Dao
interface BackupDao {
    @Query("SELECT * FROM projects ORDER BY id") suspend fun projects(): List<ProjectEntity>
    @Query("SELECT * FROM tasks ORDER BY id") suspend fun tasks(): List<TaskEntity>
    @Query("SELECT * FROM subtasks ORDER BY id") suspend fun subtasks(): List<SubtaskEntity>
    @Query("SELECT * FROM tags ORDER BY id") suspend fun tags(): List<TagEntity>
    @Query("SELECT * FROM task_tags ORDER BY taskId, tagId") suspend fun links(): List<TaskTagCrossRef>

    @Insert suspend fun insertProjects(rows: List<ProjectEntity>)
    @Insert suspend fun insertTasks(rows: List<TaskEntity>)
    @Insert suspend fun insertSubtasks(rows: List<SubtaskEntity>)
    @Insert suspend fun insertTags(rows: List<TagEntity>)
    @Insert suspend fun insertLinks(rows: List<TaskTagCrossRef>)

    @Query("DELETE FROM tasks") suspend fun deleteTasks()
    @Query("DELETE FROM projects") suspend fun deleteProjects()
    @Query("DELETE FROM tags") suspend fun deleteTags()
}
