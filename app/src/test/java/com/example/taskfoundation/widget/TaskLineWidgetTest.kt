package com.example.taskfoundation.widget

import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.model.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskLineWidgetTest {
    @Test fun upcomingExcludesCompletedAndOrdersDatedTasksBeforeUndated() {
        val tasks = listOf(
            task(1, null), task(2, 200), task(3, 100),
            task(4, 50).copy(isCompleted = true), task(5, 300))
        assertEquals(listOf(3L, 2L, 5L), upcomingTasks(tasks).map { it.id })
    }

    @Test fun tiesUsePriorityThenStableId() {
        val tasks = listOf(task(4, 100), task(3, 100).copy(priority = TaskPriority.URGENT), task(2, 100))
        assertEquals(listOf(3L, 2L, 4L), upcomingTasks(tasks).map { it.id })
        assertEquals(emptyList<Task>(), upcomingTasks(emptyList()))
    }

    private fun task(id: Long, due: Long?) = Task(id = id, title = "Task $id", dueDateTime = due, createdAt = 0, updatedAt = 0)
}
