package com.example.taskfoundation.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskfoundation.domain.model.Project
import com.example.taskfoundation.domain.repository.ProjectRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val loadFailed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsViewModel(
    private val repository: ProjectRepository,
) : ViewModel() {
    private val mutationError = MutableStateFlow<String?>(null)
    private val saving = MutableStateFlow(false)
    private val loadRetry = MutableStateFlow(0)

    val uiState: StateFlow<ProjectsUiState> = combine(
        loadRetry.flatMapLatest {
            repository.observeProjects()
                .map { ProjectsUiState(projects = it, isLoading = false) }
                .onStart { emit(ProjectsUiState()) }
                .catch { emit(ProjectsUiState(isLoading = false, loadFailed = true,
                    errorMessage = "Unable to load projects. Please retry.")) }
            },
        mutationError,
    ) { state, error ->
        state.copy(errorMessage = state.errorMessage ?: error)
    }.combine(saving) { state, busy -> state.copy(isSaving = busy) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProjectsUiState(),
    )

    fun createProject(name: String) = mutate {
        repository.saveProject(Project(name = name, createdAt = 0, updatedAt = 0))
    }

    fun saveProject(project: Project, onSaved: () -> Unit = {}) = mutate {
        repository.saveProject(project)
        onSaved()
    }

    fun deleteProject(projectId: Long) = mutate {
        check(repository.deleteProject(projectId)) { "Project no longer exists" }
    }

    fun clearError() {
        mutationError.value = null
    }

    fun retryLoading() { loadRetry.value++ }

    private fun mutate(block: suspend () -> Unit) {
        if (saving.value) return
        saving.value = true
        viewModelScope.launch {
            mutationError.value = null
            try {
                block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                mutationError.value = error.message ?: "Project operation failed"
            } finally {
                saving.value = false
            }
        }
    }
}
