package com.example.taskfoundation.domain.stats

import com.example.taskfoundation.domain.model.Task
import java.time.LocalDate
import java.time.LocalTime

object GreetingProvider {
    fun headline(tasks: List<Task>, today: LocalDate = LocalDate.now(), hour: Int = LocalTime.now().hour): String {
        val overdue = TaskStats.overdueCount(tasks, today)
        return when {
            tasks.isEmpty() -> "Your next chapter starts with one task."
            tasks.all { it.isCompleted } -> "You crushed it today. 🎉"
            overdue > 0 -> "$overdue ${if (overdue == 1) "task needs" else "tasks need"} your attention."
            hour in 5..11 -> "Good morning. Let's make today count."
            hour in 12..16 -> "Good afternoon. You've got this."
            hour in 17..20 -> "Good evening. Wrap up strong."
            else -> "Still at it? You're dedicated. 🌙"
        }
    }
    fun subtitle(tasks: List<Task>) = if (tasks.isEmpty()) "Tap New task to add one."
        else "${tasks.count { it.isCompleted }} of ${tasks.size} tasks complete"
}
