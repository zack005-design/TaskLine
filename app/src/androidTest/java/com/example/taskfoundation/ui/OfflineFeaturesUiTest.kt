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
import com.example.taskfoundation.data.local.entity.LibraryItem
import com.example.taskfoundation.data.repository.OfflineTaskRepository
import com.example.taskfoundation.data.repository.OfflineProjectRepository
import com.example.taskfoundation.ui.tasks.TasksViewModel
import com.example.taskfoundation.ui.projects.ProjectsViewModel
import com.example.taskfoundation.domain.model.*
import com.example.taskfoundation.domain.time.SystemClock
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.json.JSONObject
import java.io.File

class OfflineFeaturesUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
    private val store = ViewModelStore()
    private val repository = OfflineTaskRepository(db.taskDao(), SystemClock)
    private val vm = LibraryViewModel(db, repository)
    @After fun close() { compose.runOnIdle { store.clear() }; db.close() }

    @Test fun habitsSaveCheckInUndoAndTemplatesCreateRealTasks() {
        store.put("library", vm)
        runBlocking { db.libraryDao().save(LibraryItem("template", "Template", "Weekly reset", "{\"tasks\":[\"Review the week\",\"Plan next week\"]}", 1, 1)) }
        compose.setContent { TaskLineTheme { LibraryScreen(vm, emptyList(), {}, {}) } }
        compose.waitUntil(10000) { !vm.state.value.loading }
        compose.onNodeWithText("New habit").performClick()
        compose.onNodeWithText("Name").performTextInput("Read a chapter")
        compose.onNodeWithText("Save habit").performClick()
        compose.waitUntil(10000) { vm.state.value.items.any { it.kind == "Habit" } }
        compose.onNodeWithText("Check in").performScrollTo().performClick()
        compose.waitUntil(10000) { vm.state.value.items.filter { it.kind == "Habit" }.any { JSONObject(it.payload).getJSONObject("days").length() == 1 } }
        compose.onNodeWithText("1 / 1 today · 1 day streak").assertExists()
        capture("habits")
        compose.onNodeWithText("Undo").performClick()
        compose.waitUntil(10000) { vm.state.value.items.filter { it.kind == "Habit" }.all { JSONObject(it.payload).getJSONObject("days").length() == 0 } }
        compose.onNodeWithText("Template", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithText("Use template").performScrollTo().performClick()
        compose.onNodeWithText("Create tasks").performClick()
        compose.waitUntil(10000) { !vm.state.value.busy && runBlocking { db.taskDao().all().size == 2 } }
        assertEquals(listOf("Review the week", "Plan next week"), runBlocking { db.taskDao().all().map { it.title } })
    }

    @Test fun smartComposerAppliesSuggestedSchedule() {
        var saved: Task? = null
        compose.setContent { TaskLineTheme { AddTaskSheet(null, emptyList(), null, false, null, null, {}, { task, _ -> saved = task }) } }
        compose.onNodeWithContentDescription("Title").performTextInput("Read tomorrow at 6pm p2")
        compose.onNodeWithText("Apply:", substring = true).performClick()
        compose.onNodeWithText("Save task").performClick()
        compose.runOnIdle {
            assertEquals("Read", saved?.title)
            assertEquals(TaskPriority.HIGH, saved?.priority)
            assertEquals(1080, saved?.dueTimeMinutes)
            assertNotNull(saved?.dueDateTime)
        }
    }
    @Test fun boardMovesPersistAndMatrixShowsPriorityQuadrants() {
        val id = runBlocking { repository.saveTask(Task(title = "Prepare the launch", priority = TaskPriority.HIGH,
            dueDateTime = com.example.taskfoundation.domain.time.CalendarDates.encode(java.time.LocalDate.now()), createdAt = 0, updatedAt = 0)) }
        val tasks = TasksViewModel(repository)
        val projects = ProjectsViewModel(OfflineProjectRepository(db.projectDao(), SystemClock))
        store.put("tasks", tasks); store.put("projects", projects); store.put("library", vm)
        compose.setContent { TaskLineTheme { TaskLineScreen(tasks, projects, libraryViewModel = vm) } }
        compose.waitUntil(10000) { !tasks.uiState.value.isLoading && !projects.uiState.value.isLoading }
        compose.onNodeWithText("Board", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("taskline_content").performScrollToNode(hasText("Move work forward"))
        capture("board")
        compose.onNodeWithText("Move to: Todo").performScrollTo().performClick()
        compose.onNode(hasText("In progress") and hasClickAction()).performClick()
        compose.waitUntil(10000) { runBlocking { db.taskDao().get(id)?.status == "IN_PROGRESS" } }
        compose.onNodeWithTag("taskline_content").performScrollToIndex(0)
        compose.onNodeWithText("Matrix", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("taskline_content").performScrollToNode(hasText("Make space for what matters"))
        compose.onNodeWithText("Do first").assertExists()
        compose.onNodeWithText("Prepare the launch").assertExists()
        capture("matrix")
        compose.onNodeWithText("Calendar", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Year", useUnmergedTree = true).performClick()
        capture("year")
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.filesDir, "offline-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
