package com.example.taskfoundation.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.taskfoundation.data.local.TaskDatabase
import com.example.taskfoundation.data.repository.OfflineProjectRepository
import com.example.taskfoundation.data.repository.OfflineTaskRepository
import com.example.taskfoundation.domain.model.*
import com.example.taskfoundation.domain.time.CalendarDates
import com.example.taskfoundation.domain.time.SystemClock
import com.example.taskfoundation.ui.projects.ProjectsViewModel
import com.example.taskfoundation.ui.tasks.TasksViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** Real-window visual evidence and navigation checks; fixtures never touch the user's database. */
class TaskLineDesignTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
    private val store = ViewModelStore()

    @After fun close() {
        compose.runOnIdle { store.clear() }
        database.close()
    }

    @Test fun summaryClearsSearchAndProjectContextAndScreensRemainUsable() {
        val repository = OfflineTaskRepository(database.taskDao(), SystemClock)
        val projectRepository = OfflineProjectRepository(database.projectDao(), SystemClock)
        runBlocking {
            val work = projectRepository.saveProject(Project(name = "Design studio", description = "Make space for your best work.",
                color = 0xFF4C61D6, createdAt = 0, updatedAt = 0))
            val life = projectRepository.saveProject(Project(name = "Life & little things", description = "A little more intention, every day.",
                color = 0xFF298575, createdAt = 0, updatedAt = 0))
            repository.saveTask(Task(title = "Refine the new homepage", projectId = work,
                dueDateTime = CalendarDates.encode(LocalDate.now()), dueTimeMinutes = 600,
                status = TaskStatus.IN_PROGRESS, progress = 40, createdAt = 0, updatedAt = 0))
            repository.saveTask(Task(title = "A little time outside", projectId = life,
                dueDateTime = CalendarDates.encode(LocalDate.now()), dueTimeMinutes = 1080,
                priority = TaskPriority.LOW, createdAt = 0, updatedAt = 0))
            repository.saveTask(Task(title = "Collect ideas for next week", projectId = work,
                dueDateTime = CalendarDates.encode(LocalDate.now().plusDays(1)), createdAt = 0, updatedAt = 0))
            repository.saveTask(Task(title = "Book a table for Saturday", projectId = life, createdAt = 0, updatedAt = 0))
            repository.saveTask(Task(title = "Send the first draft", projectId = work, isCompleted = true,
                status = TaskStatus.DONE, progress = 100, createdAt = 0, updatedAt = 0))
        }
        val tasks = TasksViewModel(repository)
        val projects = ProjectsViewModel(projectRepository)
        store.put("tasks", tasks); store.put("projects", projects)
        compose.setContent { TaskLineTheme { TaskLineScreen(tasks, projects) } }
        compose.waitUntil(10_000) { !tasks.uiState.value.isLoading && !projects.uiState.value.isLoading }
        top()
        capture("tasks")
        compose.onNodeWithText("Projects", useUnmergedTree = true).performClick()
        top()
        capture("projects")
        compose.onAllNodesWithText("View tasks")[0].performScrollTo().performClick()
        top()
        compose.onNodeWithTag("task_search").performTextInput("No such task")
        compose.onNodeWithText("Due today").performScrollTo().performClick()
        compose.onNodeWithTag("task_search").assert(SemanticsMatcher.expectValue(
            androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
        compose.onNodeWithTag("taskline_content").performScrollToNode(hasText("A little time outside"))
        compose.onNodeWithText("A little time outside").assertExists()
        top()
        capture("today")
        compose.onNodeWithText("Calendar", useUnmergedTree = true).performClick()
        top()
        capture("calendar")
        compose.onNodeWithText("Week").performScrollTo().performClick()
        top()
        capture("week")
        compose.onNodeWithText("Stats", useUnmergedTree = true).performClick()
        top()
        capture("stats")
        compose.onNodeWithText("New task", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Title").performTextInput("Make room for a good idea")
        compose.onNodeWithContentDescription("Task progress").assertDoesNotExist()
        compose.onNodeWithText("Status", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Save task").assertIsDisplayed().assertIsEnabled()
        capture("editor")
        compose.onNodeWithText("Cancel").performClick()
    }

    private fun top() { compose.onNodeWithTag("taskline_content").performScrollToIndex(0); compose.waitForIdle() }

    private fun capture(name: String) {
        compose.waitForIdle()
        val mode = if (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES) "dark" else "light"
        val scale = context.resources.configuration.fontScale
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), "redesign-$name-$mode-$scale.png").outputStream().use {
            check(image.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        image.recycle()
    }
}

