package com.example.taskfoundation.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.taskfoundation.data.backup.BackupJson
import com.example.taskfoundation.data.backup.BackupRepository
import com.example.taskfoundation.data.local.entity.ProjectEntity
import com.example.taskfoundation.data.local.entity.TaskEntity
import com.example.taskfoundation.data.sample.SampleDataRepository
import com.example.taskfoundation.domain.time.CalendarDates
import com.example.taskfoundation.domain.time.Clock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SampleDataTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TaskDatabase::class.java).build()
    private val now = Instant.parse("2026-10-02T20:00:00Z").toEpochMilli()
    private val samples = SampleDataRepository(db, Clock { now }, ZoneId.of("Asia/Kolkata"))
    private val backups = BackupRepository(db)
    @After fun close() = db.close()

    @Test fun coversPlannerStatesAndPreservesExistingDataAndBackupCompatibility() = runBlocking {
        val project = ProjectEntity(name = "Sample · Work", description = "My real project", color = null, createdAt = 1, updatedAt = 1)
        val projectId = db.projectDao().upsert(project)
        val original = TaskEntity(title = "My task", description = "Keep me", projectId = projectId,
            startDateTime = null, dueDateTime = null, priority = "HIGH", status = "TODO",
            progress = 0, isCompleted = false, createdAt = 1, updatedAt = 1)
        val originalId = db.taskDao().upsert(original)
        assertEquals(17, samples.add())
        assertEquals(original.copy(id = originalId), db.taskDao().get(originalId))
        assertEquals(project.copy(id = projectId), db.projectDao().all().first { it.id == projectId })
        val fixture = db.taskDao().all().filter { it.id != originalId }
        val today = CalendarDates.encode(LocalDate.of(2026, 10, 3))
        assertTrue(fixture.any { it.dueDateTime == today && !it.isCompleted })
        assertTrue(fixture.any { it.dueDateTime != null && it.dueDateTime < today && !it.isCompleted })
        assertTrue(fixture.any { it.dueDateTime == null })
        assertEquals(setOf("TODO", "IN_PROGRESS", "BLOCKED", "DONE"), fixture.map { it.status }.toSet())
        assertEquals(setOf("NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"), fixture.map { it.repeatRule }.toSet())
        assertEquals(7, fixture.count { it.isCompleted })
        assertTrue(fixture.all { it.reminderMinutes == null && it.snoozedUntil == null })
        assertEquals(34, db.taskDao().allSubtasks().size)
        assertEquals(34, db.taskDao().allLinks().size)
        assertEquals(10, db.libraryDao().all().size)
        assertEquals(setOf("Habit", "Note", "Countdown", "Template", "Filter", "Comment", "Attachment", "Activity"), db.libraryDao().all().map { it.kind }.toSet())
        assertTrue(fixture.any { it.deadline != null && it.durationMinutes != null })
        val snapshot = backups.snapshot()
        assertEquals(snapshot, BackupJson.decode(BackupJson.encode(snapshot)))
        backups.restore(snapshot)
        assertEquals(0, samples.add())
        assertEquals(snapshot, backups.snapshot())
    }

    @Test fun concurrentClicksAndEditedSamplesDoNotDuplicateOrOverwrite() = runBlocking {
        assertEquals(listOf(0, 17), listOf(async { samples.add() }, async { samples.add() }).awaitAll().sorted())
        val task = db.taskDao().all().first()
        db.taskDao().upsert(task.copy(title = "My edited sample", progress = 30))
        val snapshot = backups.snapshot()
        assertEquals(0, samples.add())
        assertEquals(snapshot, backups.snapshot())
    }

    @Test fun insertionFailureRollsBackEveryRecordAndCanRetry() = runBlocking {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_sample BEFORE INSERT ON subtasks BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        try { samples.add(); fail("Expected transaction failure") } catch (_: android.database.sqlite.SQLiteException) { }
        assertTrue(backups.snapshot().let { it.tasks.isEmpty() && it.projects.isEmpty() && it.tags.isEmpty() && it.subtasks.isEmpty() && it.links.isEmpty() && it.library.isEmpty() })
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_sample")
        assertEquals(17, samples.add())
    }
}
