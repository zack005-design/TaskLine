package com.example.taskfoundation.domain

import com.example.taskfoundation.domain.model.*
import com.example.taskfoundation.domain.time.CalendarDates
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class SmartEntryTest {
    private val today = LocalDate.of(2026, 10, 2)
    @Test fun parsesScheduleWithoutChangingTaskMeaning() {
        val parsed = SmartEntryParser.parse("Read tomorrow at 6:30pm p2 every week", today)
        assertEquals("Read", parsed.title)
        assertEquals(today.plusDays(1), parsed.date)
        assertEquals(1110, parsed.minutes)
        assertEquals(TaskPriority.HIGH, parsed.priority)
        assertEquals(RepeatRule.WEEKLY, parsed.repeat)
    }
    @Test fun leavesInvalidAndAmbiguousInputIntact() {
        val invalid = "Meet 2026-02-30 at 25:99"
        assertEquals(invalid, SmartEntryParser.parse(invalid, today).title)
        assertFalse(SmartEntryParser.parse(invalid, today).recognized)
        assertEquals("tomorrow", SmartEntryParser.parse("tomorrow", today).title)
        assertFalse(SmartEntryParser.parse("tomorrow", today).recognized)
        assertEquals("Ship2 prototype", SmartEntryParser.parse("Ship2 prototype", today).title)
    }
    @Test fun handlesMidnightNoonAndFutureWeekdays() {
        assertEquals(0, SmartEntryParser.parse("Read at 12am", today).minutes)
        assertEquals(720, SmartEntryParser.parse("Read at 12pm", today).minutes)
        assertEquals(today.plusWeeks(1), SmartEntryParser.parse("Read friday", today).date)
    }
    @Test fun matrixUsesPriorityAndCalendarBoundaryIncludingDeadline() {
        val task = Task(title = "Task", priority = TaskPriority.HIGH, createdAt = 0, updatedAt = 0)
        assertEquals(MatrixQuadrant.PLAN, task.quadrant(today))
        assertEquals(MatrixQuadrant.DO, task.copy(deadline = CalendarDates.encode(today)).quadrant(today))
        assertEquals(MatrixQuadrant.DELEGATE, task.copy(priority = TaskPriority.LOW, dueDateTime = CalendarDates.encode(today.minusDays(1))).quadrant(today))
        assertEquals(MatrixQuadrant.LATER, task.copy(priority = TaskPriority.LOW, dueDateTime = CalendarDates.encode(today.plusDays(1))).quadrant(today))
    }
}
