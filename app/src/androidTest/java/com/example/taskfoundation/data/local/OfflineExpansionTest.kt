package com.example.taskfoundation.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.taskfoundation.data.backup.*
import com.example.taskfoundation.data.local.entity.LibraryItem
import com.example.taskfoundation.data.repository.OfflineTaskRepository
import com.example.taskfoundation.domain.model.*
import com.example.taskfoundation.domain.time.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class OfflineExpansionTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), TaskDatabase::class.java)
    @Test fun versionThreeTasksSurviveBothMigrations() {
        helper.createDatabase("expansion-migration", 3).apply {
            execSQL("INSERT INTO tasks (id,title,description,priority,status,progress,isCompleted,createdAt,updatedAt,repeatRule,repeatInterval) VALUES (7,'Keep me','','HIGH','TODO',0,0,1,2,'NONE',1)")
            close()
        }
        helper.runMigrationsAndValidate("expansion-migration", 5, true, TaskDatabase.MIGRATION_3_4, TaskDatabase.MIGRATION_4_5).use { db ->
            db.query("SELECT title, durationMinutes, deadline FROM tasks WHERE id=7").use {
                assertTrue(it.moveToFirst()); assertEquals("Keep me", it.getString(0)); assertTrue(it.isNull(1)); assertTrue(it.isNull(2))
            }
        }
    }
    @Test fun libraryCommentsHistoryAndTaskPlanningRoundTripAtomically() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TaskDatabase::class.java).build()
        try {
            val tasks = OfflineTaskRepository(db.taskDao(), SystemClock)
            val id = tasks.saveTask(Task(title = "Planning", durationMinutes = 45, deadline = CalendarDates.encode(LocalDate.of(2026, 10, 9)), createdAt = 0, updatedAt = 0))
            val habit = LibraryItem("habit", "Habit", "Water", "{\"goal\":2,\"days\":{\"2026-10-01\":2}}", 1, 1)
            val comment = LibraryItem("$id:comment", "Comment", "Planning", "{\"taskId\":$id,\"text\":\"Keep this\"}", 1, 1)
            val attachment = LibraryItem("$id:file", "Attachment", "Planning", "{\"taskId\":$id,\"name\":\"notes.txt\",\"mime\":\"text/plain\",\"bytes\":\"SGVsbG8=\"}", 1, 1)
            db.libraryDao().save(habit); db.libraryDao().save(comment)
            db.libraryDao().save(attachment)
            tasks.setTaskCompleted(id, true)
            val repo = BackupRepository(db)
            val snapshot = repo.snapshot()
            assertEquals(2, snapshot.library.count { it.kind == "Activity" })
            assertEquals(snapshot, BackupJson.decode(BackupJson.encode(snapshot)))
            tasks.deleteTask(id)
            assertTrue(db.libraryDao().all().none { it.kind in listOf("Comment", "Attachment") })
            repo.restore(snapshot)
            assertEquals(snapshot, repo.snapshot())
            try {
                repo.restore(snapshot.copy(library = snapshot.library + habit.copy(id = "bad", payload = "{\"goal\":0,\"days\":{}}")))
                fail("Invalid habit accepted")
            } catch (_: IllegalArgumentException) { }
            assertEquals(snapshot, repo.snapshot())
        } finally { db.close() }
    }
}
