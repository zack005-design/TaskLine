package com.example.taskfoundation.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.taskfoundation.data.backup.*
import com.example.taskfoundation.data.local.entity.*
import com.example.taskfoundation.domain.time.CalendarDates
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class BackupTest {
    private val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TaskDatabase::class.java).build()
    private val repository = BackupRepository(database)
    private val date = CalendarDates.encode(LocalDate.of(2026, 9, 26))
    private fun sample() = TaskBackup(
        listOf(ProjectEntity(3, "Project", "Description", 42, 1, 2)),
        listOf(TaskEntity(7, "Task \"one\"", "Multiline\n✓", 3, null, date, "HIGH", "IN_PROGRESS", 45, false, 1, 2,
            dueTimeMinutes = 600, reminderMinutes = 30, repeatRule = "WEEKLY", repeatInterval = 2,
            repeatAnchor = date, snoozedUntil = date + 3600000, lastNotifiedAt = date, importKey = "event-1")),
        listOf(SubtaskEntity(8, 7, "Child", true, 2, 1, 2)),
        listOf(TagEntity(9, "Work", null, 1)),
        listOf(TaskTagCrossRef(7, 9)),
    )
    @After fun close() = database.close()

    @Test fun jsonRoundTripRestoresAllRelationshipsAndReplacesExistingData() = runBlocking {
        val backup = sample()
        val decoded = BackupJson.decode(BackupJson.encode(backup))
        assertEquals(backup, decoded)
        database.backupDao().insertProjects(listOf(ProjectEntity(50, "Old", "", null, 0, 0)))
        repository.restore(decoded)
        assertEquals(backup, repository.snapshot())
        repository.restore(TaskBackup(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
        assertTrue(repository.snapshot().tasks.isEmpty())
        assertTrue(repository.snapshot().projects.isEmpty())
    }

    @Test fun invalidDataNeverChangesCurrentDatabase() = runBlocking {
        val backup = sample()
        repository.restore(backup)
        val invalid = listOf(
            backup.copy(tasks = backup.tasks.map { it.copy(projectId = 999) }),
            backup.copy(tasks = backup.tasks + backup.tasks),
            backup.copy(tasks = backup.tasks.map { it.copy(dueDateTime = date + 1) }),
            backup.copy(tasks = backup.tasks.map { it.copy(progress = 101) }),
            backup.copy(links = listOf(TaskTagCrossRef(999, 9))),
            backup.copy(tasks = backup.tasks.map { it.copy(repeatInterval = 0) }),
        )
        invalid.forEach {
            try { repository.restore(it); fail("Invalid backup accepted") }
            catch (_: IllegalArgumentException) { }
            assertEquals(backup, repository.snapshot())
        }
        val unsupported = JSONObject(BackupJson.encode(backup)).put("version", 100).toString()
        try { BackupJson.decode(unsupported); fail("Unsupported format accepted") }
        catch (_: IllegalArgumentException) { }
        val wrongType = JSONObject(BackupJson.encode(backup)).apply {
            getJSONArray("tasks").getJSONObject(0).put("progress", "45")
        }.toString()
        try { BackupJson.decode(wrongType); fail("Wrong field type accepted") }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun olderBackupFormatDefaultsSchedulingFields() {
        val old = JSONObject(BackupJson.encode(sample())).put("version", 1)
        val task = old.getJSONArray("tasks").getJSONObject(0)
        listOf("dueTimeMinutes", "reminderMinutes", "repeatRule", "repeatInterval", "repeatAnchor",
            "snoozedUntil", "lastNotifiedAt", "importKey").forEach(task::remove)
        val restored = BackupJson.decode(old.toString()).tasks.single()
        assertEquals("NONE", restored.repeatRule)
        assertNull(restored.reminderMinutes)
        assertEquals(date, restored.dueDateTime)
    }

    @Test fun writeFailureRollsBackDeletionAndPartialInserts() = runBlocking {
        val original = sample()
        repository.restore(original)
        database.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER reject_test_task BEFORE INSERT ON tasks
            WHEN NEW.title = 'Reject' BEGIN SELECT RAISE(ABORT, 'Test write failure'); END
        """.trimIndent())
        val replacement = original.copy(tasks = original.tasks.map { it.copy(title = "Reject") })
        try { repository.restore(replacement); fail("Expected insertion to fail") }
        catch (_: android.database.sqlite.SQLiteException) { }
        assertEquals(original, repository.snapshot())
    }
}
