package com.example.taskfoundation.domain.stats

import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.time.CalendarDates
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DailyCount(val date: LocalDate, val completed: Int)

/** Estimates activity from currently completed tasks' last update; not a completion history. */
object TaskStats {
    fun completionDay(task: Task, zone: ZoneId): LocalDate? =
        if (task.isCompleted && task.updatedAt > 0) Instant.ofEpochMilli(task.updatedAt).atZone(zone).toLocalDate() else null

    fun dailyCompletions(tasks: List<Task>, days: Int = 7, today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()): List<DailyCount> {
        require(days in 1..366)
        val counts = tasks.mapNotNull { completionDay(it, zone) }.groupingBy { it }.eachCount()
        return (days - 1 downTo 0).map { today.minusDays(it.toLong()) }.map { DailyCount(it, counts[it] ?: 0) }
    }
    fun totalCompleted(tasks: List<Task>) = tasks.count { it.isCompleted }
    fun totalActive(tasks: List<Task>) = tasks.count { !it.isCompleted }
    fun overdueCount(tasks: List<Task>, today: LocalDate = LocalDate.now()) = tasks.count {
        !it.isCompleted && it.dueDateTime?.let { value -> CalendarDates.decode(value).isBefore(today) } == true
    }
}
