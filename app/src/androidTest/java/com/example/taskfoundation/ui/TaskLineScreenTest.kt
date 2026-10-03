package com.example.taskfoundation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.asAndroidBitmap
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
        compose.onNodeWithContentDescription("Title").performTextInput("Check persistence")
        compose.onNodeWithContentDescription("Description").performTextInput("Created by a UI test")
        compose.onNodeWithText("Save task").performClick()
        awaitText("Check persistence")
        compose.onNodeWithContentDescription("Actions Check persistence").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Edit Check persistence").performClick()
        compose.onNodeWithContentDescription("Title").performTextReplacement("Check saved task")
        compose.onNodeWithText("Save task").performClick()
        awaitText("Check saved task")
        compose.onNodeWithContentDescription("Complete Check saved task").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            runBlocking { database.taskDao().observeAll().first().single().isCompleted }
        }
        compose.onNodeWithText("Projects", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Actions Release plan").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Delete Release plan").performClick()
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
        compose.onNodeWithContentDescription("Title").performTextInput("Plan release")
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
        compose.onNodeWithContentDescription("Title").performTextInput("Sketch new dashboard")
        compose.onNodeWithText("Save task").performClick()
        awaitText("Sketch new dashboard")
        compose.onNodeWithText("Tasks", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("taskline_content").performScrollToNode(hasTestTag("task_search"))
        compose.onNodeWithTag("task_search").performTextInput("Sketch")
        awaitText("Sketch new dashboard")
        compose.onNodeWithTag("taskline_content").performScrollToNode(hasTestTag("task_search"))
        compose.onNodeWithTag("task_search").performTextReplacement("No matching task")
        awaitText("No matching tasks")
    }

    @Test fun dateTimeReminderAndCustomRepeatAreSavedThroughEditor() {
        compose.onNodeWithText("New task", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Title").performTextInput("Weekly planning")
        compose.onNodeWithText("Schedule").performScrollTo().performClick()
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
        compose.onNodeWithContentDescription("Title").performTextInput("Plan the selected day")
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
        compose.onNodeWithTag("taskline_content").performScrollToNode(hasText("Add a task"))
        compose.onNodeWithText("Add a task").performTextInput("Capture an idea")
        compose.onNodeWithText("New task", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Add", substring = false).assertIsEnabled()
        compose.waitForIdle()
        val capture = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        java.io.File(context.filesDir, "quick-entry-before.png").outputStream().use {
            capture.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        capture.recycle()
        compose.onNodeWithText("Add", substring = false).performClick()
        compose.waitUntil(10_000) { runBlocking { database.taskDao().all().any { it.title == "Capture an idea" } } }
        runBlocking {
            assertNull(database.taskDao().all().single().dueDateTime)
        }
        compose.onNodeWithTag("taskline_content").performScrollToNode(hasText("Unscheduled"))
        compose.onNodeWithText("Unscheduled").performScrollTo().performClick()
        awaitText("Capture an idea")
    }

    @Test fun inlineSubtasksAndProjectColorPersistThroughSheets() {
        compose.onNodeWithText("Projects", useUnmergedTree = true).performClick()
        compose.onNodeWithText("New project", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Project name").performTextInput("Colored project")
        compose.onNodeWithContentDescription("Purple").performScrollTo().performClick()
        compose.onNodeWithText("Save project").performClick()
        awaitText("View tasks")
        compose.onNodeWithText("View tasks").performClick()
        compose.onNodeWithText("New task", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Title").performTextInput("Bundled task")
        compose.onNodeWithText("+ Add subtask").performScrollTo().performClick()
        compose.onNodeWithText("Subtask 1").performScrollTo().performTextInput("First child")
        compose.onNodeWithText("+ Add subtask").performScrollTo().performClick()
        compose.onNodeWithText("Subtask 2").performScrollTo().performTextInput("Remove me")
        compose.onNodeWithContentDescription("Remove subtask 2").performClick()
        compose.onNodeWithText("Save task").performClick()
        awaitText("Bundled task")
        runBlocking {
            val task = database.taskDao().all().single()
            assertNotNull(task.projectId)
            assertEquals("First child", database.taskDao().allSubtasks().single().title)
            assertEquals(0xFF9C27B0L, database.projectDao().observeAll().first().single().color)
        }
        compose.onNodeWithText("Stats", useUnmergedTree = true).performClick()
        awaitText("Last 7 days")
        screenshot("stats")
    }

    @Test fun focusCanCloseReopenAndStopWithoutCompletingTask() {
        compose.onNodeWithText("New task", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Title").performTextInput("Focus test")
        screenshot("task-sheet")
        compose.onNodeWithText("Save task").performClick()
        awaitText("Focus test")
        compose.onNodeWithContentDescription("Details Focus test").performScrollTo().performClick()
        compose.onNodeWithText("Focus", useUnmergedTree = true).performClick()
        awaitText("Time to focus")
        screenshot("focus")
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithContentDescription("Open focus timer").performClick()
        compose.onNodeWithText("Stop focus").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Time to focus").fetchSemanticsNodes().isEmpty() }
        runBlocking { assertFalse(database.taskDao().all().single().isCompleted) }
    }

    @Test fun swipeCanCompleteReopenAndRequestDeletion() {
        compose.onNodeWithText("New task", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Title").performTextInput("Swipe test")
        compose.onNodeWithText("Save task").performClick()
        awaitText("Swipe test")
        val id = runBlocking { database.taskDao().all().single().id }
        compose.onNodeWithTag("task_card_$id").performScrollTo().performTouchInput { swipeRight() }
        compose.waitUntil(10_000) { runBlocking { database.taskDao().get(id)!!.isCompleted } }
        compose.onNodeWithTag("task_card_$id").performScrollTo().performTouchInput { swipeRight() }
        compose.waitUntil(10_000) { runBlocking { !database.taskDao().get(id)!!.isCompleted } }
        compose.onNodeWithTag("task_card_$id").performTouchInput { swipeLeft() }
        compose.onNodeWithText("Delete task?").assertIsDisplayed()
        runBlocking { assertNotNull(database.taskDao().get(id)) }
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithContentDescription("Complete Swipe test").assertIsOff()
    }

    private fun screenshot(name: String) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val heading = when (name) { "task-sheet" -> "New task"; "focus" -> "Time to focus"; else -> "Your stats" }
        val marker = if (name == "task-sheet") hasContentDescription("Title") else hasText(heading)
        val bitmap = compose.onNode(isRoot() and hasAnyDescendant(marker)).captureToImage().asAndroidBitmap()
        java.io.File(context.getExternalFilesDir(null), "taskline-$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
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
