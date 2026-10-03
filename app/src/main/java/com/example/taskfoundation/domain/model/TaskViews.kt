package com.example.taskfoundation.domain.model

import com.example.taskfoundation.domain.time.CalendarDates
import java.time.LocalDate

enum class MatrixQuadrant(val title: String, val hint: String) {
    DO("Do first", "Important · due or deadline reached"),
    PLAN("Schedule", "Important · no immediate deadline"),
    DELEGATE("Delegate", "Lower priority · due or deadline reached"),
    LATER("Later", "Lower priority · no immediate deadline")
}

fun Task.quadrant(today: LocalDate): MatrixQuadrant {
    val important = priority >= TaskPriority.HIGH
    val urgent = listOfNotNull(dueDateTime, deadline).any { !CalendarDates.decode(it).isAfter(today) }
    return when {
        important && urgent -> MatrixQuadrant.DO
        important -> MatrixQuadrant.PLAN
        urgent -> MatrixQuadrant.DELEGATE
        else -> MatrixQuadrant.LATER
    }
}
