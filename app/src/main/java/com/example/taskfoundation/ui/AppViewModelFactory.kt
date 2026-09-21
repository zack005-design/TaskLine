package com.example.taskfoundation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.taskfoundation.AppContainer
import com.example.taskfoundation.ui.projects.ProjectsViewModel
import com.example.taskfoundation.ui.tasks.TasksViewModel

class AppViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(TasksViewModel::class.java) ->
            TasksViewModel(container.taskRepository) as T

        modelClass.isAssignableFrom(ProjectsViewModel::class.java) ->
            ProjectsViewModel(container.projectRepository) as T

        else -> error("Unknown ViewModel class: ${modelClass.name}")
    }
}
