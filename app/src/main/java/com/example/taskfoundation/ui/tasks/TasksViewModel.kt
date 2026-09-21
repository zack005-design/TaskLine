package com.example.taskfoundation.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.model.TaskPriority
import com.example.taskfoundation.domain.model.TaskStatus
import com.example.taskfoundation.domain.repository.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskListItemUiModel(
    val id: Long,
    val title: String,
    val projectId: Long?,
    val dueDateTime: Long?,
    val priority: TaskPriority,
    val status: TaskStatus,
    val progress: Int,
    val isCompleted: Boolean,
)

data class TasksUiState(
    val tasks: List<TaskListItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModel(
    private val repository: TaskRepository,
) : ViewModel() {
    private val selectedProjectId = MutableStateFlow<Long?>(null)
    private val mutationError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TasksUiState> = selectedProjectId
        .flatMapLatest(repository::observeTasks)
        .map { tasks ->
            tasks.map {
                TaskListItemUiModel(
                    id = it.id,
                    title = it.title,
                    projectId = it.projectId,
                    dueDateTime = it.dueDateTime,
                    priority = it.priority,
                    status = it.status,
                    progress = it.progress,
                    isCompleted = it.isCompleted,
                )
            }
        }
        .catch { error ->
            mutationError.value = error.message ?: "Unable to load tasks"
            emit(emptyList())
        }
        .combine(mutationError) { tasks, error ->
            TasksUiState(tasks = tasks, isLoading = false, errorMessage = error)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TasksUiState(),
        )

    fun selectProject(projectId: Long?) {
        selectedProjectId.value = projectId
    }

    fun createTask(title: String) = mutate {
        repository.saveTask(
            Task(
                title = title,
                createdAt = 0,
                updatedAt = 0,
            ),
        )
    }

    fun saveTask(task: Task) = mutate { repository.saveTask(task) }

    fun setCompleted(taskId: Long, completed: Boolean) = mutate {
        check(repository.setTaskCompleted(taskId, completed)) { "Task no longer exists" }
    }

    fun deleteTask(taskId: Long) = mutate {
        check(repository.deleteTask(taskId)) { "Task no longer exists" }
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
                mutationError.value = error.message ?: "Task operation failed"
            }
        }
    }
}
