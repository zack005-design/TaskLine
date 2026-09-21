package com.example.taskfoundation.data.repository

import com.example.taskfoundation.data.local.dao.ProjectDao
import com.example.taskfoundation.data.mapper.toDomain
import com.example.taskfoundation.data.mapper.toEntity
import com.example.taskfoundation.domain.model.Project
import com.example.taskfoundation.domain.repository.ProjectRepository
import com.example.taskfoundation.domain.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineProjectRepository(
    private val projectDao: ProjectDao,
    private val clock: Clock,
) : ProjectRepository {
    override fun observeProjects(): Flow<List<Project>> =
        projectDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeProject(projectId: Long): Flow<Project?> =
        projectDao.observeById(projectId).map { it?.toDomain() }

    override suspend fun saveProject(project: Project): Long {
        require(project.name.isNotBlank()) { "Project name cannot be blank" }
        val now = clock.nowMillis()
        val normalized = project.copy(
            name = project.name.trim(),
            createdAt = if (project.id == 0L) now else project.createdAt,
            updatedAt = now,
        )
        val rowId = projectDao.upsert(normalized.toEntity())
        return if (project.id == 0L) rowId else project.id
    }

    override suspend fun deleteProject(projectId: Long): Boolean = projectDao.deleteById(projectId) == 1
}
