package com.example.taskfoundation.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.taskfoundation.data.local.entity.ProjectEntity
import com.example.taskfoundation.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectDeletionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val database = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun deletingProject_detachesOnlyItsTasks() = runBlocking {
        val now = 1_000L
        val firstProjectId = database.projectDao().upsert(ProjectEntity(name = "First", description = "", color = null, createdAt = now, updatedAt = now))
        val secondProjectId = database.projectDao().upsert(ProjectEntity(name = "Second", description = "", color = null, createdAt = now, updatedAt = now))
        val firstTaskId = database.taskDao().upsert(task(projectId = firstProjectId, title = "First task"))
        val secondTaskId = database.taskDao().upsert(task(projectId = secondProjectId, title = "Second task"))

        assertEquals(1, database.projectDao().deleteById(firstProjectId))

        val tasks = database.taskDao().observeAll().first().associateBy { it.id }
        assertNull(tasks.getValue(firstTaskId).projectId)
        assertEquals(secondProjectId, tasks.getValue(secondTaskId).projectId)
    }

    private fun task(projectId: Long, title: String) = TaskEntity(
        title = title,
        description = "",
        projectId = projectId,
        startDateTime = null,
        dueDateTime = null,
        priority = "MEDIUM",
        status = "TODO",
        progress = 0,
        isCompleted = false,
        createdAt = 1_000,
        updatedAt = 1_000,
    )
}
