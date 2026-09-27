package com.example.taskfoundation.domain.time

import com.example.taskfoundation.domain.model.Task
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class PlannerDatesTest {
    @Test fun monthGridIncludesLeapDayAndCompleteMondayFirstWeeks() {
        val days = monthGrid(YearMonth.of(2024, 2))
        assertEquals(LocalDate.of(2024, 1, 29), days.first())
        assertEquals(LocalDate.of(2024, 3, 3), days.last())
        assertTrue(days.contains(LocalDate.of(2024, 2, 29)))
        assertEquals(0, days.size % 7)
        assertEquals(days.size, days.distinct().size)
        assertEquals(42, monthGrid(YearMonth.of(2026, 8)).size)
    }

    @Test fun agendaUsesDueDateThenStartDateAndSortsAllDayBeforeTimed() {
        val day = LocalDate.of(2026, 9, 27)
        val date = CalendarDates.encode(day)
        val base = Task(id = 1, title = "Morning", dueDateTime = date,
            dueTimeMinutes = 540, createdAt = 0, updatedAt = 0)
        val tasks = listOf(base.copy(id = 2, dueTimeMinutes = 900),
            base.copy(id = 3, dueTimeMinutes = null), base,
            base.copy(id = 4, isCompleted = true, dueTimeMinutes = 480),
            base.copy(id = 5, dueDateTime = null, startDateTime = date, dueTimeMinutes = null),
            base.copy(id = 6, startDateTime = date, dueDateTime = CalendarDates.encode(day.plusDays(1))),
            base.copy(id = 7, dueDateTime = null))
        assertEquals(listOf(3L, 5L, 1L, 2L, 4L), agendaFor(tasks, day).map { it.id })
    }
}
