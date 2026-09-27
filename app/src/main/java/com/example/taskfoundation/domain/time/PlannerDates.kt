package com.example.taskfoundation.domain.time

import com.example.taskfoundation.domain.model.Task
import java.time.LocalDate
import java.time.YearMonth

/** A Monday-first month grid, including adjacent dates to complete each week. */
fun monthGrid(month: YearMonth): List<LocalDate> {
    val first = month.atDay(1)
    val start = first.minusDays((first.dayOfWeek.value - 1).toLong())
    val count = ((first.dayOfWeek.value - 1 + month.lengthOfMonth() + 6) / 7) * 7
    return List(count) { start.plusDays(it.toLong()) }
}

/** Tasks appear on their due day, or start day when they have no due date. */
fun Task.plannerDate(): LocalDate? = (dueDateTime ?: startDateTime)?.let(CalendarDates::decode)

fun agendaFor(tasks: List<Task>, day: LocalDate): List<Task> = tasks
    .filter { it.plannerDate() == day }
    .sortedWith(compareBy<Task> { it.isCompleted }
        .thenBy { it.dueTimeMinutes ?: -1 }
        .thenByDescending { it.priority.ordinal }.thenBy { it.id })
