package com.example.taskfoundation.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.taskfoundation.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun observeById(projectId: Long): Flow<ProjectEntity?>

    @Upsert
    suspend fun upsert(project: ProjectEntity): Long

    // Task.projectId uses ON DELETE SET NULL; this cannot delete tasks from any project.
    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteById(projectId: Long): Int
}
