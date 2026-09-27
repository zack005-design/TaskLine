package com.example.taskfoundation.domain.stats

import com.example.taskfoundation.domain.model.Task
import java.time.LocalDate
import java.time.ZoneId

object StreakCalculator {
    fun calculate(tasks: List<Task>, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Int {
        val days = tasks.mapNotNull { TaskStats.completionDay(it, zone) }.toSet()
        var day = if (today in days) today else today.minusDays(1)
        var count = 0
        while (day in days) { count++; day = day.minusDays(1) }
        return count
    }
}
