package com.example.taskfoundation.domain.time

import com.example.taskfoundation.domain.model.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class TaskScheduleTest {
    private fun day(value: String) = CalendarDates.encode(LocalDate.parse(value))
    private fun task(date: String, rule: RepeatRule) = Task(title = "Routine", createdAt = 0, updatedAt = 0,
        dueDateTime = day(date), repeatAnchor = day(date), repeatRule = rule)

    @Test fun monthlyRepeatRetainsOriginalMonthEnd() {
        val january = task("2026-01-31", RepeatRule.MONTHLY)
        val february = TaskSchedule.next(january, day("2026-01-31"), ZoneOffset.UTC)
        assertEquals(day("2026-02-28"), february.dueDateTime)
        assertEquals(day("2026-03-31"), TaskSchedule.next(february, day("2026-02-28"), ZoneOffset.UTC).dueDateTime)
    }

    @Test fun repeatsSkipMissedDatesAndShiftStartBySameDistance() {
        val original = task("2026-09-01", RepeatRule.WEEKLY).copy(startDateTime = day("2026-08-30"),
            repeatInterval = 2, progress = 80, status = TaskStatus.IN_PROGRESS, snoozedUntil = 100)
        val next = TaskSchedule.next(original, day("2026-10-01"), ZoneOffset.UTC)
        assertEquals(day("2026-10-13"), next.dueDateTime)
        assertEquals(day("2026-10-11"), next.startDateTime)
        assertEquals(0, next.progress)
        assertEquals(TaskStatus.TODO, next.status)
        assertNull(next.snoozedUntil)
    }

    @Test fun reminderUsesLocalWallTimeAcrossDst() {
        val task = task("2026-03-08", RepeatRule.NONE).copy(dueTimeMinutes = 9 * 60, reminderMinutes = 30)
        assertEquals(Instant.parse("2026-03-08T12:30:00Z").toEpochMilli(),
            TaskSchedule.reminderAt(task, ZoneId.of("America/New_York")))
        assertEquals(123L, TaskSchedule.reminderAt(task.copy(snoozedUntil = 123), ZoneOffset.UTC))
        assertNull(TaskSchedule.reminderAt(task.copy(isCompleted = true)))
    }

    @Test fun leapYearAnchorReturnsToLeapDay() {
        var task = task("2024-02-29", RepeatRule.YEARLY)
        for (year in 2025..2028) {
            task = TaskSchedule.next(task, task.dueDateTime!!, ZoneOffset.UTC)
            assertEquals(day(if (year == 2028) "2028-02-29" else "$year-02-28"), task.dueDateTime)
        }
    }

    @Test fun rejectsIncompleteOrInvalidSchedules() {
        val original = task("2026-09-27", RepeatRule.DAILY)
        for (invalid in listOf(original.copy(repeatInterval = 0), original.copy(dueTimeMinutes = 1440),
            original.copy(dueDateTime = null), original.copy(reminderMinutes = 15))) {
            try { TaskSchedule.validate(invalid); fail("Invalid schedule accepted") } catch (_: IllegalArgumentException) { }
        }
    }
}
