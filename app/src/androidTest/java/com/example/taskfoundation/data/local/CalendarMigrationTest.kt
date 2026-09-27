package com.example.taskfoundation.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.example.taskfoundation.domain.time.CalendarDates
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CalendarMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), TaskDatabase::class.java)

    @Test fun versionOneDatesAndRelationshipsSurviveMigration() {
        val originalZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
            val date = LocalDate.of(2026, 9, 26)
            val legacy = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            helper.createDatabase("calendar-migration", 1).apply {
                execSQL("INSERT INTO projects VALUES (3, 'Keep project', '', NULL, 1, 2)")
                execSQL("""INSERT INTO tasks (id,title,description,projectId,startDateTime,dueDateTime,priority,status,progress,isCompleted,createdAt,updatedAt)
                    VALUES (7,'Keep task','',3,NULL,?,'MEDIUM','TODO',0,0,1,2)""", arrayOf(legacy))
                close()
            }
            helper.runMigrationsAndValidate("calendar-migration", 3, true, TaskDatabase.MIGRATION_1_2, TaskDatabase.MIGRATION_2_3).use { db ->
                db.query("SELECT projectId, startDateTime, dueDateTime, createdAt FROM tasks WHERE id = 7").use {
                    assertTrue(it.moveToFirst())
                    assertEquals(3L, it.getLong(0))
                    assertTrue(it.isNull(1))
                    assertEquals(CalendarDates.encode(date), it.getLong(2))
                    assertEquals(1L, it.getLong(3))
                }
                db.query("SELECT repeatRule, repeatInterval, dueTimeMinutes, reminderMinutes FROM tasks WHERE id = 7").use {
                    assertTrue(it.moveToFirst())
                    assertEquals("NONE", it.getString(0))
                    assertEquals(1, it.getInt(1))
                    assertTrue(it.isNull(2))
                    assertTrue(it.isNull(3))
                }
            }
        } finally { TimeZone.setDefault(originalZone) }
    }
}
