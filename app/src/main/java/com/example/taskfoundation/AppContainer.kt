package com.example.taskfoundation

import android.content.Context
import com.example.taskfoundation.data.local.TaskDatabase
import com.example.taskfoundation.data.repository.OfflineProjectRepository
import com.example.taskfoundation.data.repository.OfflineTaskRepository
import com.example.taskfoundation.domain.repository.ProjectRepository
import com.example.taskfoundation.domain.repository.TaskRepository
import com.example.taskfoundation.domain.time.SystemClock

class AppContainer(context: Context) {
    private val database = TaskDatabase.getInstance(context)

    val taskRepository: TaskRepository = OfflineTaskRepository(database.taskDao(), SystemClock)
    val projectRepository: ProjectRepository = OfflineProjectRepository(database.projectDao(), SystemClock)
}
