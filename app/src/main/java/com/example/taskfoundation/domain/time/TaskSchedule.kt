package com.example.taskfoundation.domain.time

import com.example.taskfoundation.domain.model.*
import java.time.*
import java.time.temporal.ChronoUnit

object TaskSchedule {
    fun validate(task: Task) {
        require(task.dueTimeMinutes == null || task.dueTimeMinutes in 0..1439) { "Choose a valid due time" }
        require(task.reminderMinutes == null || task.reminderMinutes in 0..10080) { "Invalid reminder offset" }
        require(task.dueDateTime != null || (task.dueTimeMinutes == null && task.reminderMinutes == null && task.repeatRule == RepeatRule.NONE)) { "Choose a due date for a time, reminder, or repeat" }
        require(task.reminderMinutes == null || task.dueTimeMinutes != null) { "Choose a due time for reminders" }
        require(task.repeatInterval in 1..365) { "Repeat interval must be between 1 and 365" }
    }

    fun reminderAt(task: Task, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (task.isCompleted || task.reminderMinutes == null) return null
        task.snoozedUntil?.let { return it }
        val date = task.dueDateTime ?: return null
        val minutes = task.dueTimeMinutes ?: return null
        return CalendarDates.decode(date).atTime(minutes / 60, minutes % 60).atZone(zone)
            .minusMinutes(task.reminderMinutes.toLong()).toInstant().toEpochMilli()
    }

    /** Advance from the original schedule, skipping missed occurrences, preserving month-end anchors. */
    fun next(task: Task, now: Long, zone: ZoneId = ZoneId.systemDefault()): Task {
        val due = CalendarDates.decode(requireNotNull(task.dueDateTime))
        val anchor = CalendarDates.decode(task.repeatAnchor ?: task.dueDateTime)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val after = maxOf(due, today)
        val unit = when (task.repeatRule) {
            RepeatRule.DAILY -> ChronoUnit.DAYS
            RepeatRule.WEEKLY -> ChronoUnit.WEEKS
            RepeatRule.MONTHLY -> ChronoUnit.MONTHS
            RepeatRule.YEARLY -> ChronoUnit.YEARS
            RepeatRule.NONE -> error("Task does not repeat")
        }
        var step = (unit.between(anchor, after) / task.repeatInterval).coerceAtLeast(0) + 1
        var next = anchor.plus(step * task.repeatInterval, unit)
        while (!next.isAfter(after)) { step++; next = anchor.plus(step * task.repeatInterval, unit) }
        require(next.year in 1..9999) { "Next occurrence is outside the supported date range" }
        val shift = ChronoUnit.DAYS.between(due, next)
        return task.copy(dueDateTime = CalendarDates.encode(next),
            startDateTime = task.startDateTime?.let { CalendarDates.encode(CalendarDates.decode(it).plusDays(shift)) },
            repeatAnchor = CalendarDates.encode(anchor), isCompleted = false, status = TaskStatus.TODO,
            progress = 0, snoozedUntil = null, lastNotifiedAt = null, updatedAt = now)
    }
}
