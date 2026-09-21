package com.example.taskfoundation.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskfoundation.domain.model.Project
import com.example.taskfoundation.domain.repository.ProjectRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProjectListItemUiModel(
    val id: Long,
    val name: String,
    val color: Long?,
)

data class ProjectsUiState(
    val projects: List<ProjectListItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class ProjectsViewModel(
    private val repository: ProjectRepository,
) : ViewModel() {
    private val mutationError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProjectsUiState> = combine(
        repository.observeProjects()
            .map { projects -> projects.map { ProjectListItemUiModel(it.id, it.name, it.color) } }
            .catch { error ->
                mutationError.value = error.message ?: "Unable to load projects"
                emit(emptyList())
            },
        mutationError,
    ) { projects, error ->
        ProjectsUiState(projects = projects, isLoading = false, errorMessage = error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProjectsUiState(),
    )

    fun createProject(name: String) = mutate {
        repository.saveProject(Project(name = name, createdAt = 0, updatedAt = 0))
    }

    fun saveProject(project: Project) = mutate { repository.saveProject(project) }

    fun deleteProject(projectId: Long) = mutate {
        check(repository.deleteProject(projectId)) { "Project no longer exists" }
    }

    fun clearError() {
        mutationError.value = null
    }

    private fun mutate(block: suspend () -> Unit) {
        viewModelScope.launch {
            mutationError.value = null
            try {
                block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                mutationError.value = error.message ?: "Project operation failed"
            }
        }
    }
}
