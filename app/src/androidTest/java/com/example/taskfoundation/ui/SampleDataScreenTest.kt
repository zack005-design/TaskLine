package com.example.taskfoundation.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.taskfoundation.calendar.CalendarImport
import com.example.taskfoundation.data.backup.BackupFiles
import com.example.taskfoundation.data.backup.BackupRepository
import com.example.taskfoundation.data.local.TaskDatabase
import com.example.taskfoundation.data.repository.OfflineProjectRepository
import com.example.taskfoundation.data.repository.OfflineTaskRepository
import com.example.taskfoundation.data.sample.SampleDataRepository
import com.example.taskfoundation.domain.time.SystemClock
import com.example.taskfoundation.ui.backup.BackupViewModel
import com.example.taskfoundation.ui.projects.ProjectsViewModel
import com.example.taskfoundation.ui.tasks.TasksViewModel
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class SampleDataScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
    private val store = ViewModelStore()
    @After fun cleanup() { compose.runOnIdle { store.clear() }; db.close() }

    @Test fun toolsClickSeedsDataAndSecondClickKeepsIt() {
        val tasks = TasksViewModel(OfflineTaskRepository(db.taskDao(), SystemClock))
        val projects = ProjectsViewModel(OfflineProjectRepository(db.projectDao(), SystemClock))
        val backup = BackupViewModel(BackupRepository(db), BackupFiles(context.contentResolver))
        val tools = DataToolsViewModel(context, CalendarImport(context, db), SampleDataRepository(db))
        val library = LibraryViewModel(db, OfflineTaskRepository(db.taskDao(), SystemClock))
        store.put("tasks", tasks); store.put("projects", projects)
        store.put("backup", backup); store.put("tools", tools)
        store.put("library", library)
        compose.setContent { TaskLineTheme { TaskLineScreen(tasks, projects, backup, tools, libraryViewModel = library) } }
        compose.waitUntil(10000) { !tasks.uiState.value.isLoading && !projects.uiState.value.isLoading }
        compose.onNodeWithText("Tools").performClick()
        compose.onNodeWithText("Add test data").performClick()
        compose.waitUntil(10000) { tools.uiState.value.sampleMessage?.startsWith("17 sample tasks") == true }
        compose.onNodeWithText("Test data").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.waitUntil(10000) { tasks.uiState.value.tasks.size == 17 && projects.uiState.value.projects.size == 3 }
        compose.onNodeWithText("Tools").performClick()
        compose.onNodeWithText("Add test data").performClick()
        compose.waitUntil(10000) { tools.uiState.value.sampleMessage?.startsWith("Sample data is already present") == true }
        compose.onNodeWithText("Close").performClick()
        assertEquals(17, runBlocking { db.taskDao().all().size })
        assertNull("Sample feedback must not carry over into calendar import", tools.uiState.value.message)
        compose.onNodeWithText("Library").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Sample · Drink water").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Sample · Drink water").assertIsDisplayed()
        compose.onNodeWithText("Note").performClick()
        compose.onNodeWithText("Sample · Project ideas").assertIsDisplayed()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithText("Tools").performClick()
        compose.onNodeWithText("Calendar import").performClick()
        compose.onNodeWithTag("calendar_content").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Tools").performClick()
        compose.onNodeWithText("Backup & restore").performClick()
        compose.onNodeWithText("Export backup").assertExists()
    }
}
