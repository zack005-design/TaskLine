package com.example.taskfoundation.domain.stats

import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.time.CalendarDates
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class TaskStatsTest {
    private val today = LocalDate.of(2026, 9, 27)
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun completed(day: LocalDate) = Task(title = "Done", isCompleted = true,
        createdAt = 1, updatedAt = day.atTime(0, 15).atZone(zone).toInstant().toEpochMilli())

    @Test fun localMidnightUsesInstantTimezoneAndDailyCountsIncludeZeroDays() {
        val tasks = listOf(completed(today), completed(today), completed(today.minusDays(2)))
        val result = TaskStats.dailyCompletions(tasks, 3, today, zone)
        assertEquals(listOf(1, 0, 2), result.map { it.completed })
        assertEquals(today, result.last().date)
    }

    @Test fun streakAllowsYesterdayAnchorAndStopsAtGap() {
        val days = listOf(today.minusDays(1), today.minusDays(2), today.minusDays(4))
        assertEquals(2, StreakCalculator.calculate(days.map(::completed), today, zone))
        assertEquals(3, StreakCalculator.calculate(days.map(::completed) + completed(today), today, zone))
        assertEquals(0, StreakCalculator.calculate(listOf(completed(today.minusDays(2))), today, zone))
    }

    @Test fun futureUncompletedAndUnsetTimestampsDoNotCreateCurrentActivity() {
        val tasks = listOf(completed(today).copy(isCompleted = false), completed(today).copy(updatedAt = 0), completed(today.plusDays(1)))
        assertEquals(0, TaskStats.dailyCompletions(tasks, 7, today, zone).sumOf { it.completed })
        assertEquals(0, StreakCalculator.calculate(tasks, today, zone))
    }

    @Test fun overdueUsesCalendarDateRatherThanTimestampZone() {
        val task = Task(title = "Due", createdAt = 0, updatedAt = 0, dueDateTime = CalendarDates.encode(today))
        assertEquals(0, TaskStats.overdueCount(listOf(task), today))
        assertEquals(1, TaskStats.overdueCount(listOf(task), today.plusDays(1)))
        assertEquals(0, TaskStats.overdueCount(listOf(task.copy(isCompleted = true)), today.plusDays(1)))
        assertEquals("1 task needs your attention.", GreetingProvider.headline(listOf(task), today.plusDays(1), 8))
    }
}
