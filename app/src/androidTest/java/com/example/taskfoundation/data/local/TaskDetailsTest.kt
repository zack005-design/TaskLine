package com.example.taskfoundation.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.taskfoundation.data.repository.OfflineTaskRepository
import com.example.taskfoundation.domain.model.Subtask
import com.example.taskfoundation.domain.model.Tag
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.time.SystemClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class TaskDetailsTest {
    private val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TaskDatabase::class.java).build()
    private val repository = OfflineTaskRepository(database.taskDao(), SystemClock)
    @After fun close() = database.close()

    @Test fun subtaskEditsPersistAndDeletedRowsCannotBeResurrected() = runBlocking {
        val taskId = repository.saveTask(Task(title = "Parent", createdAt = 0, updatedAt = 0))
        repository.saveSubtask(Subtask(taskId = taskId, title = " First ", createdAt = 0, updatedAt = 0))
        val original = repository.observeSubtasks(taskId).first().single()
        assertEquals("First", original.title)
        repository.saveSubtask(original.copy(title = "Updated", isCompleted = true))
        val saved = repository.observeSubtasks(taskId).first().single()
        assertTrue(saved.isCompleted)
        assertEquals("Updated", saved.title)
        assertEquals(original.createdAt, saved.createdAt)
        assertFalse(repository.observeTask(taskId).first()!!.isCompleted)
        repository.deleteSubtask(saved.id)
        try {
            repository.saveSubtask(saved)
            fail("A deleted subtask must not be recreated by an edit")
        } catch (_: IllegalStateException) { }
        assertTrue(repository.observeSubtasks(taskId).first().isEmpty())
    }

    @Test fun tagsAreReusedAndTaskDeletionCascadesWithoutAffectingOtherTasks() = runBlocking {
        val first = repository.saveTask(Task(title = "First", createdAt = 0, updatedAt = 0))
        val second = repository.saveTask(Task(title = "Second", createdAt = 0, updatedAt = 0))
        val tagId = repository.attachTag(first, Tag(name = " Work ", createdAt = 0))
        assertEquals(tagId, repository.attachTag(first, Tag(name = "work", createdAt = 0)))
        assertEquals(tagId, repository.attachTag(second, Tag(name = "WORK", createdAt = 0)))
        assertEquals(1, repository.observeTags(first).first().size)
        repository.saveSubtask(Subtask(taskId = first, title = "Child", createdAt = 0, updatedAt = 0))
        repository.deleteTask(first)
        assertTrue(repository.observeSubtasks(first).first().isEmpty())
        assertTrue(repository.observeTags(first).first().isEmpty())
        assertEquals(tagId, repository.observeTags(second).first().single().id)
        assertTrue(repository.detachTag(second, tagId))
        assertTrue(repository.observeTags(second).first().isEmpty())
    }
}
