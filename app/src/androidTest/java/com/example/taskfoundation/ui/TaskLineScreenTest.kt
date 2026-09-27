package com.example.taskfoundation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.taskfoundation.data.local.TaskDatabase
import com.example.taskfoundation.data.repository.OfflineProjectRepository
import com.example.taskfoundation.data.repository.OfflineTaskRepository
import com.example.taskfoundation.domain.time.SystemClock
import com.example.taskfoundation.ui.projects.ProjectsViewModel
import com.example.taskfoundation.ui.tasks.TasksViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TaskLineScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: TaskDatabase
    private val store = ViewModelStore()

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TaskDatabase::class.java).build()
        val tasks = TasksViewModel(OfflineTaskRepository(database.taskDao(), SystemClock))
        val projects = ProjectsViewModel(OfflineProjectRepository(database.projectDao(), SystemClock))
        store.put("tasks", tasks)
        store.put("projects", projects)
        compose.setContent { TaskLineTheme { TaskLineScreen(tasks, projects) } }
        awaitText("No tasks here yet")
    }

    @After fun tearDown() {
        compose.runOnIdle { store.clear() }
        database.close()
    }

    @Test fun createEditCompleteAndDetachTaskThroughUi() {
        compose.onNodeWithText("Projects", useUnmergedTree = true).performClick()
        compose.onNodeWithText("New project", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Save project").assertIsNotEnabled()
        compose.onNodeWithText("Project name").performTextInput("Release plan")
        compose.onNodeWithText("Save project").performClick()
        awaitText("View tasks")
        compose.onNodeWithText("View tasks").performClick()
        compose.onNodeWithText("New task", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Save task").assertIsNotEnabled()
        compose.onNodeWithText("Title").performTextInput("Check persistence")
        compose.onNodeWithText("Description").performTextInput("Created by a UI test")
        compose.onNodeWithText("Save task").performClick()
        awaitText("Check persistence")
        compose.onNodeWithContentDescription("Edit Check persistence").performScrollTo().performClick()
        compose.onNodeWithText("Title").performTextReplacement("Check saved task")
        compose.onNodeWithText("Save task").performClick()
        awaitText("Check saved task")
        compose.onNodeWithContentDescription("Complete Check saved task").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            runBlocking { database.taskDao().observeAll().first().single().isCompleted }
        }
        compose.onNodeWithText("Projects", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Delete Release plan").performScrollTo().performClick()
        compose.onNodeWithText("Delete permanently").performClick()
        awaitText("Make room for a project")
        compose.onNodeWithText("Tasks", useUnmergedTree = true).performClick()
        awaitText("Check saved task")
        runBlocking {
            val task = database.taskDao().observeAll().first().single()
            assertNull(task.projectId)
            assertTrue(task.isCompleted)
            assertEquals("Created by a UI test", task.description)
        }
    }

    @Test fun manageSubtasksAndTagsThroughDetails() {
        compose.onNodeWithText("New task", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Title").performTextInput("Plan release")
        compose.onNodeWithText("Save task").performClick()
        awaitText("Plan release")
        compose.onNodeWithContentDescription("Details Plan release").performScrollTo().performClick()
        awaitText("No subtasks yet")
        compose.onNodeWithText("Subtask title").performScrollTo().performTextInput("Check build")
        compose.onNodeWithText("Add subtask").performScrollTo().performClick()
        awaitText("Check build")
        compose.onNodeWithContentDescription("Complete subtask Check build").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            runBlocking {
                val id = database.taskDao().observeAll().first().single().id
                database.taskDao().observeSubtasks(id).first().single().isCompleted
            }
        }
        compose.onNodeWithText("Tag name").performScrollTo().performTextInput("Release")
        compose.onNodeWithText("Add tag").performScrollTo().performClick()
        awaitText("Release")
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithContentDescription("Details Plan release").performScrollTo().performClick()
        awaitText("Check build")
        compose.onNodeWithContentDescription("Complete subtask Check build").assertIsOn()
        compose.onNodeWithContentDescription("Remove tag Release").performScrollTo().performClick()
        awaitText("No tags yet")
        compose.onNodeWithContentDescription("Delete subtask Check build").performScrollTo().performClick()
        compose.onNodeWithText("Delete subtask permanently").performClick()
        awaitText("No subtasks yet")
    }

    @Test fun createFromTimelineAndSearchTasks() {
        compose.onNodeWithText("Calendar", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Timeline").performClick()
        compose.onNodeWithText("New task", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Title").performTextInput("Sketch new dashboard")
        compose.onNodeWithText("Save task").performClick()
        awaitText("Sketch new dashboard")
        compose.onNodeWithText("Tasks", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("taskline_content").performScrollToNode(hasText("Search tasks"))
        compose.onNodeWithText("Search tasks").performTextInput("Sketch")
        awaitText("Sketch new dashboard")
        compose.onNodeWithTag("taskline_content").performScrollToNode(hasText("Search tasks"))
        compose.onNodeWithText("Search tasks").performTextReplacement("No matching task")
        awaitText("No tasks here yet")
    }

    @Test fun dateTimeReminderAndCustomRepeatAreSavedThroughEditor() {
        compose.onNodeWithText("New task", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Title").performTextInput("Weekly planning")
        compose.onNodeWithText("Due date: Not set").performScrollTo().performClick()
        compose.onNodeWithText("Set date").performClick()
        compose.onNodeWithText("Due time: Not set").performScrollTo().performClick()
        compose.onNodeWithText("Set time").performClick()
        compose.onNodeWithText("Reminder: Off").performScrollTo().performClick()
        compose.onNodeWithText("15 minutes before").performClick()
        compose.onNodeWithText("Repeat: None").performScrollTo().performClick()
        compose.onNodeWithText("Weekly").performClick()
        compose.onNodeWithText("Repeat interval").performScrollTo().performTextReplacement("2")
        compose.onNodeWithText("Save task").performClick()
        awaitText("Weekly planning")
        runBlocking {
            val task = database.taskDao().all().single()
            assertNotNull(task.dueDateTime)
            assertEquals(540, task.dueTimeMinutes)
            assertEquals(15, task.reminderMinutes)
            assertEquals("WEEKLY", task.repeatRule)
            assertEquals(2, task.repeatInterval)
        }
    }

    @Test fun calendarDateCreatesPersistentTaskAndCompletionUpdatesAgenda() {
        val day = java.time.LocalDate.now().plusMonths(1).withDayOfMonth(15)
        compose.onNodeWithText("Calendar", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Next calendar period").performScrollTo().performClick()
        val dateLabel = day.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
        compose.onNodeWithContentDescription("$dateLabel, 0 active tasks").performScrollTo().performClick()
        compose.onNodeWithText("New task", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Title").performTextInput("Plan the selected day")
        compose.onNodeWithText("Save task").performClick()
        awaitText("Plan the selected day")
        runBlocking {
            assertEquals(com.example.taskfoundation.domain.time.CalendarDates.encode(day),
                database.taskDao().all().single().dueDateTime)
        }
        compose.onNodeWithContentDescription("Complete Plan the selected day").performScrollTo().performClick()
        awaitText("0 remaining · 1 completed")
        compose.onNodeWithTag("taskline_content").performScrollToNode(hasText("Week"))
        compose.onNodeWithText("Week").performClick()
        compose.onNodeWithText("Tasks", useUnmergedTree = true).performClick()
        awaitText("Plan the selected day")
        compose.onNodeWithContentDescription("Complete Plan the selected day").assertIsOn()
    }

    @Test fun quickEntryIsSavedAndFoundInUnscheduledFilter() {
        compose.onNodeWithText("Add a task").performTextInput("Capture an idea")
        compose.onNodeWithText("Add", substring = false).performClick()
        awaitText("Capture an idea")
        runBlocking {
            assertNull(database.taskDao().all().single().dueDateTime)
        }
        compose.onNodeWithTag("taskline_content").performScrollToNode(hasText("Unscheduled"))
        compose.onNodeWithText("Unscheduled").performScrollTo().performClick()
        awaitText("Capture an idea")
    }

    private fun awaitText(text: String) {
        compose.waitUntil(10_000) {
            if (compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()) {
                runCatching { compose.onNodeWithTag("taskline_content").performScrollToNode(hasText(text)) }
            }
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}

