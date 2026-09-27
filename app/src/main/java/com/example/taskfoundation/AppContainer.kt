package com.example.taskfoundation

import android.content.Context
import com.example.taskfoundation.data.backup.BackupFiles
import com.example.taskfoundation.data.backup.BackupRepository
import com.example.taskfoundation.data.local.TaskDatabase
import com.example.taskfoundation.data.repository.OfflineProjectRepository
import com.example.taskfoundation.data.repository.OfflineTaskRepository
import com.example.taskfoundation.domain.repository.ProjectRepository
import com.example.taskfoundation.domain.repository.TaskRepository
import com.example.taskfoundation.domain.time.SystemClock

class AppContainer(val context: Context) {
    val database = TaskDatabase.getInstance(context)
    val reminders = com.example.taskfoundation.reminders.TaskReminders(context.applicationContext, database)
    val backupRepository = BackupRepository(database)
    val backupFiles = BackupFiles(context.applicationContext.contentResolver)

    val taskRepository: TaskRepository = OfflineTaskRepository(database.taskDao(), SystemClock)
    val projectRepository: ProjectRepository = OfflineProjectRepository(database.projectDao(), SystemClock)
}
