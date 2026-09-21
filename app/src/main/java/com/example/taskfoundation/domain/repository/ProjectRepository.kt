package com.example.taskfoundation.domain.repository

import com.example.taskfoundation.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeProjects(): Flow<List<Project>>

    fun observeProject(projectId: Long): Flow<Project?>

    suspend fun saveProject(project: Project): Long

    suspend fun deleteProject(projectId: Long): Boolean
}
