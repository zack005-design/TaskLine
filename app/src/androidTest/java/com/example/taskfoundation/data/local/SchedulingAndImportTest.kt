package com.example.taskfoundation.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.taskfoundation.calendar.*
import com.example.taskfoundation.data.local.entity.SubtaskEntity
import com.example.taskfoundation.data.mapper.toDomain
import com.example.taskfoundation.data.repository.OfflineTaskRepository
import com.example.taskfoundation.domain.model.*
import com.example.taskfoundation.domain.time.*
import com.example.taskfoundation.domain.time.Clock
import java.time.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class SchedulingAndImportTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val database = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
    private val now = CalendarDates.encode(LocalDate.now())
    private val repository = OfflineTaskRepository(database.taskDao(), object : Clock { override fun nowMillis() = now })
    @After fun close() = database.close()

    @Test fun completingRepeatAdvancesAtomicallyAndResetsSubtasks() = runBlocking {
        val id = repository.saveTask(Task(title = "Daily routine", dueDateTime = now, repeatRule = RepeatRule.DAILY,
            dueTimeMinutes = 600, reminderMinutes = 15, createdAt = 0, updatedAt = 0))
        database.taskDao().upsertSubtask(SubtaskEntity(taskId = id, title = "Checklist", isCompleted = true,
            sortOrder = 0, createdAt = now, updatedAt = now))
        assertTrue(repository.setTaskCompleted(id, true))
        val task = database.taskDao().get(id)!!.toDomain()
        assertFalse(task.isCompleted)
        assertEquals(CalendarDates.encode(LocalDate.now().plusDays(1)), task.dueDateTime)
        assertEquals(now, task.repeatAnchor)
        assertEquals(0, task.progress)
        assertFalse(database.taskDao().allSubtasks().single().isCompleted)
        assertEquals(15, task.reminderMinutes)
    }

    @Test fun editedReminderClearsOldDeliveryAndSnooze() = runBlocking {
        val id = repository.saveTask(Task(title = "Edit reminder", dueDateTime = now, dueTimeMinutes = 600,
            reminderMinutes = 15, createdAt = 0, updatedAt = 0))
        database.taskDao().markNotified(id, now)
        database.taskDao().snooze(id, now + 600000, now)
        repository.saveTask(database.taskDao().get(id)!!.toDomain().copy(dueTimeMinutes = 660))
        val task = database.taskDao().get(id)!!
        assertNull(task.lastNotifiedAt)
        assertNull(task.snoozedUntil)
    }

    @Test fun importedEventsAreIdempotentAndPreserveLocalDueTime() = runBlocking {
        val importer = CalendarImport(context, database)
        val start = LocalDate.now().atTime(10, 15).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val event = CalendarEvent("ics:test:$start", "Calendar task", "Notes", start, start + 3600000, false)
        assertEquals(1, importer.commit(listOf(event, event), null))
        assertEquals(0, importer.commit(listOf(event), null))
        val preview = importer.prepare(CalendarPreview(listOf(event)))
        assertEquals(1, preview.existing)
        assertTrue(preview.events.isEmpty())
        val task = database.taskDao().all().single()
        assertEquals(615, task.dueTimeMinutes)
        assertNull(task.reminderMinutes)
        assertEquals(now, task.dueDateTime)
        try { importer.commit(listOf(event.copy(key = "other")), 999); fail("Missing project accepted") }
        catch (_: IllegalArgumentException) { }
        assertEquals(1, database.taskDao().all().size)
    }
}
