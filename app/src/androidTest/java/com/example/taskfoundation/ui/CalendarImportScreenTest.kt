package com.example.taskfoundation.ui

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.taskfoundation.calendar.CalendarImport
import com.example.taskfoundation.data.local.TaskDatabase
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.junit.*
import org.junit.Assert.*

class CalendarImportScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val database = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
    private val store = ViewModelStore()
    private val file = File(context.cacheDir, "taskline-import-test.ics")
    private val model = DataToolsViewModel(context, CalendarImport(context, database))

    @After fun cleanup() { compose.runOnIdle { store.clear() }; database.close(); file.delete() }

    @Test fun selectedFileShowsPreviewAndImportsOnlyOnConfirmation() {
        val day = LocalDate.now().plusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE)
        file.writeText("BEGIN:VCALENDAR\nBEGIN:VEVENT\nUID:preview-test\nDTSTART;VALUE=DATE:$day\nSUMMARY:Imported planning\nEND:VEVENT\nEND:VCALENDAR")
        store.put("calendar", model)
        compose.setContent { TaskLineTheme { DataToolsScreen(model, emptyList()) {} } }
        compose.runOnIdle { model.previewFile(Uri.fromFile(file)) }
        compose.waitUntil(10000) { model.uiState.value.preview != null }
        assertTrue(runBlocking { database.taskDao().all().isEmpty() })
        compose.onNodeWithTag("calendar_content").performScrollToNode(hasText("Import 1 events"))
        compose.onNodeWithText("Import 1 events").performClick()
        compose.waitUntil(10000) { runBlocking { database.taskDao().all().size == 1 } }
        assertEquals("Imported planning", runBlocking { database.taskDao().all().single().title })
        compose.runOnIdle { model.previewFile(Uri.fromFile(file)) }
        compose.waitUntil(10000) { model.uiState.value.preview?.existing == 1 }
        assertTrue(model.uiState.value.preview!!.events.isEmpty())
    }
}
