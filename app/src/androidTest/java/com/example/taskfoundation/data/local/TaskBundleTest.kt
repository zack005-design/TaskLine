package com.example.taskfoundation.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.taskfoundation.data.repository.OfflineTaskRepository
import com.example.taskfoundation.data.mapper.toDomain
import com.example.taskfoundation.data.mapper.toEntity
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.time.SystemClock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class TaskBundleTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TaskDatabase::class.java).build()
    private val repository = OfflineTaskRepository(db.taskDao(), SystemClock)
    @After fun close() = db.close()

    @Test fun pendingSubtasksAreTrimmedAndAppendedOnEdit() = runBlocking {
        val id = repository.saveTaskWithSubtasks(Task(title = "Parent", createdAt = 0, updatedAt = 0), listOf(" One ", " ", "Two"))
        val task = db.taskDao().get(id)!!.toDomain()
        repository.saveTaskWithSubtasks(task.copy(title = "Edited"), listOf("Three"))
        assertEquals(1, db.taskDao().all().size)
        val children = db.taskDao().allSubtasks().sortedBy { it.sortOrder }
        assertEquals(listOf("One", "Two", "Three"), children.map { it.title })
        assertEquals(listOf(0, 1, 2), children.map { it.sortOrder })
        assertTrue(children.all { it.taskId == id && it.createdAt > 0 && !it.isCompleted })
    }

    @Test fun childFailureRollsBackParentAndEarlierChildren() = runBlocking {
        try {
            db.taskDao().saveWithSubtasks(Task(title = "Rollback", createdAt = 1, updatedAt = 1).toEntity(), listOf("Valid", ""), 1)
            fail("Expected invalid child to fail the bundle")
        } catch (_: IllegalArgumentException) { }
        assertTrue(db.taskDao().all().isEmpty())
        assertTrue(db.taskDao().allSubtasks().isEmpty())
    }
}
