package com.example.taskfoundation.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.model.Subtask
import com.example.taskfoundation.domain.model.Tag
import com.example.taskfoundation.domain.repository.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val loadFailed: Boolean = false,
)

data class TaskDetailsUiState(
    val taskId: Long? = null,
    val subtasks: List<Subtask> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModel(
    private val repository: TaskRepository,
) : ViewModel() {
    private val selectedProjectId = MutableStateFlow<Long?>(null)
    private val mutationError = MutableStateFlow<String?>(null)
    private val saving = MutableStateFlow(false)
    private val detailsTaskId = MutableStateFlow<Long?>(null)
    private val detailsRetry = MutableStateFlow(0)
    private val loadRetry = MutableStateFlow(0)

    val detailsState = combine(detailsTaskId, detailsRetry) { id, _ -> id }
        .flatMapLatest { id ->
            if (id == null) flowOf(TaskDetailsUiState(isLoading = false))
            else combine(repository.observeSubtasks(id), repository.observeTags(id)) { subtasks, tags ->
                TaskDetailsUiState(id, subtasks, tags, isLoading = false)
            }.onStart { emit(TaskDetailsUiState(taskId = id)) }
                .catch { emit(TaskDetailsUiState(taskId = id, isLoading = false, error = "Unable to load task details")) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskDetailsUiState())

    fun selectDetails(taskId: Long?) { detailsTaskId.value = taskId }
    fun retryDetails() { detailsRetry.value++ }

    fun saveSubtask(subtask: Subtask, onSaved: () -> Unit = {}) = mutate {
        repository.saveSubtask(subtask)
        onSaved()
    }

    fun deleteSubtask(id: Long, onDeleted: () -> Unit = {}) = mutate {
        check(repository.deleteSubtask(id)) { "Subtask no longer exists" }
        onDeleted()
    }

    fun attachTag(taskId: Long, name: String, onSaved: () -> Unit = {}) = mutate {
        repository.attachTag(taskId, Tag(name = name, createdAt = 0))
        onSaved()
    }

    fun detachTag(taskId: Long, tagId: Long) = mutate {
        check(repository.detachTag(taskId, tagId)) { "Tag is no longer attached" }
    }

    val uiState: StateFlow<TasksUiState> = combine(selectedProjectId, loadRetry) { id, _ -> id }
        .flatMapLatest { id ->
            repository.observeTasks(id)
                .map { tasks -> TasksUiState(tasks = tasks, isLoading = false) }
                .onStart { emit(TasksUiState()) }
                .catch { emit(TasksUiState(isLoading = false, loadFailed = true,
                    errorMessage = "Unable to load tasks. Please retry.")) }
        }
        .combine(mutationError) { state, error ->
            state.copy(errorMessage = state.errorMessage ?: error)
        }
        .combine(saving) { state, busy -> state.copy(isSaving = busy) }
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

    fun saveTask(task: Task, pendingSubtasks: List<String> = emptyList(), onSaved: () -> Unit = {}) = mutate {
        repository.saveTaskWithSubtasks(task, pendingSubtasks)
        onSaved()
    }

    fun setCompleted(taskId: Long, completed: Boolean, onCompleted: () -> Unit = {}) = mutate {
        check(repository.setTaskCompleted(taskId, completed)) { "Task no longer exists" }
        onCompleted()
    }

    fun deleteTask(taskId: Long) = mutate {
        check(repository.deleteTask(taskId)) { "Task no longer exists" }
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
                mutationError.value = error.message ?: "Task operation failed"
            } finally {
                saving.value = false
            }
        }
    }
}
