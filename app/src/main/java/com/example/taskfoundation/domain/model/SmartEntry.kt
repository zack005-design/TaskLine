package com.example.taskfoundation.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class SmartEntry(val title: String, val date: LocalDate? = null, val minutes: Int? = null,
    val priority: TaskPriority? = null, val repeat: RepeatRule = RepeatRule.NONE) {
    val recognized: Boolean get() = date != null || minutes != null || priority != null || repeat != RepeatRule.NONE
    fun summary(): String = listOfNotNull(date?.toString(), minutes?.let { "%02d:%02d".format(it / 60, it % 60) },
        priority?.name?.lowercase(), repeat.takeIf { it != RepeatRule.NONE }?.name?.lowercase()).joinToString(" · ")
}

/** Deliberately small, predictable English grammar. Suggestions require an explicit Apply tap. */
object SmartEntryParser {
    fun parse(input: String, today: LocalDate): SmartEntry {
        var text = input
        var date: LocalDate? = null
        var time: Int? = null
        var priority: TaskPriority? = null
        var repeat = RepeatRule.NONE
        fun consume(pattern: String, action: (MatchResult) -> Boolean) {
            val match = Regex(pattern, RegexOption.IGNORE_CASE).find(text) ?: return
            if (action(match)) text = text.removeRange(match.range)
        }
        consume("\\bevery (day|week|month|year)\\b") {
            repeat = when (it.groupValues[1].lowercase()) { "day" -> RepeatRule.DAILY; "week" -> RepeatRule.WEEKLY; "month" -> RepeatRule.MONTHLY; else -> RepeatRule.YEARLY }; true
        }
        consume("\\b(today|tomorrow|next week)\\b") {
            date = today.plusDays(when (it.value.lowercase()) { "tomorrow" -> 1; "next week" -> 7; else -> 0 }); true
        }
        if (date == null) consume("\\b(next )?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b") {
            date = today.with(TemporalAdjusters.next(DayOfWeek.valueOf(it.groupValues[2].uppercase()))); true
        }
        if (date == null) consume("\\b\\d{4}-\\d{2}-\\d{2}\\b") {
            val parsed = runCatching { LocalDate.parse(it.value) }.getOrNull()
            if (parsed != null && parsed.year in 1..9999) { date = parsed; true } else false
        }
        consume("\\bat (\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b") {
            val hour = it.groupValues[1].toInt()
            val minute = it.groupValues[2].ifBlank { "0" }.toInt()
            val period = it.groupValues[3].lowercase()
            if (minute !in 0..59 || (period.isEmpty() && hour !in 0..23) || (period.isNotEmpty() && hour !in 1..12)) false
            else { time = (if (period.isEmpty()) hour else hour % 12 + if (period == "pm") 12 else 0) * 60 + minute; true }
        }
        consume("(?<!\\S)p([1-4])(?!\\S)") {
            priority = listOf(TaskPriority.URGENT, TaskPriority.HIGH, TaskPriority.MEDIUM, TaskPriority.LOW)[it.groupValues[1].toInt() - 1]; true
        }
        if (date == null && (time != null || repeat != RepeatRule.NONE)) date = today
        val title = text.replace(Regex("\\s+"), " ").trim()
        return if (title.isBlank()) SmartEntry(input) else SmartEntry(title, date, time, priority, repeat)
    }
}
