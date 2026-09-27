package com.example.taskfoundation.ui

import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.taskfoundation.data.local.TaskDatabase
import com.example.taskfoundation.data.repository.OfflineProjectRepository
import com.example.taskfoundation.data.repository.OfflineTaskRepository
import com.example.taskfoundation.domain.model.Project
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.repository.ProjectRepository
import com.example.taskfoundation.domain.repository.TaskRepository
import com.example.taskfoundation.domain.time.SystemClock
import com.example.taskfoundation.ui.projects.ProjectsViewModel
import com.example.taskfoundation.ui.tasks.TasksViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class LoadingRecoveryTest {
    private val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TaskDatabase::class.java).build()
    private val store = ViewModelStore()
    @After fun close() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
        database.close()
    }

    @Test fun tasksRetryAfterReadFailureAndContinueObservingChanges() = runBlocking {
        val real = OfflineTaskRepository(database.taskDao(), SystemClock)
        var attempts = 0
        val failingOnce = object : TaskRepository by real {
            override fun observeTasks(projectId: Long?): Flow<List<Task>> = flow {
                if (attempts++ == 0) error("Temporary read failure")
                emitAll(real.observeTasks(projectId))
            }
        }
        val model = TasksViewModel(failingOnce)
        store.put("tasks", model)
        withTimeout(5000) { model.uiState.first { it.loadFailed } }
        model.clearError()
        assertTrue(model.uiState.value.loadFailed)
        assertNotNull(model.uiState.value.errorMessage)
        model.retryLoading()
        withTimeout(5000) { model.uiState.first { !it.isLoading && !it.loadFailed } }
        real.saveTask(Task(title = "After retry", createdAt = 0, updatedAt = 0))
        val state = withTimeout(5000) { model.uiState.first { it.tasks.size == 1 } }
        assertEquals("After retry", state.tasks.single().title)
        assertNull(state.errorMessage)
    }

    @Test fun projectsRetryAfterReadFailureAndContinueObservingChanges() = runBlocking {
        val real = OfflineProjectRepository(database.projectDao(), SystemClock)
        var attempts = 0
        val failingOnce = object : ProjectRepository by real {
            override fun observeProjects(): Flow<List<Project>> = flow {
                if (attempts++ == 0) error("Temporary read failure")
                emitAll(real.observeProjects())
            }
        }
        val model = ProjectsViewModel(failingOnce)
        store.put("projects", model)
        withTimeout(5000) { model.uiState.first { it.loadFailed } }
        model.clearError()
        assertTrue(model.uiState.value.loadFailed)
        model.retryLoading()
        withTimeout(5000) { model.uiState.first { !it.isLoading && !it.loadFailed } }
        real.saveProject(Project(name = "Recovered", createdAt = 0, updatedAt = 0))
        val state = withTimeout(5000) { model.uiState.first { it.projects.size == 1 } }
        assertEquals("Recovered", state.projects.single().name)
        assertNull(state.errorMessage)
    }
}
