package com.example.taskfoundation.ui

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.taskfoundation.data.backup.*
import com.example.taskfoundation.data.local.TaskDatabase
import com.example.taskfoundation.data.local.entity.ProjectEntity
import com.example.taskfoundation.ui.backup.BackupDialog
import com.example.taskfoundation.ui.backup.BackupViewModel
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class BackupDialogTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
    private val repository = BackupRepository(database)
    private val files = BackupFiles(context.contentResolver)
    private val model = BackupViewModel(repository, files)
    private val store = ViewModelStore().apply { put("backup", model) }
    private val backupFile = File.createTempFile("backup-test", ".json", context.cacheDir)

    @After fun close() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
        database.close()
        backupFile.delete()
    }

    @Test fun fileRoundTripRequiresConfirmationAndCancelKeepsCurrentData() {
        val imported = TaskBackup(listOf(ProjectEntity(9, "Imported", "", null, 1, 2)),
            emptyList(), emptyList(), emptyList(), emptyList())
        val current = imported.copy(projects = listOf(ProjectEntity(1, "Current", "", null, 1, 2)))
        runBlocking { repository.restore(current) }
        val uri = Uri.fromFile(backupFile)
        files.write(uri, imported)
        assertEquals(imported, files.read(uri))
        compose.setContent { TaskLineTheme { BackupDialog(model) {} } }
        compose.runOnIdle { model.inspect(uri) }
        compose.waitUntil(5000) { model.uiState.value.preview != null && !model.uiState.value.busy }
        compose.onNodeWithText("Replace current data?").assertIsDisplayed()
        assertEquals(current, runBlocking { repository.snapshot() })
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertNull(model.uiState.value.preview) }
        assertEquals(current, runBlocking { repository.snapshot() })

        compose.runOnIdle { model.inspect(uri) }
        compose.waitUntil(5000) { model.uiState.value.preview != null && !model.uiState.value.busy }
        compose.onNodeWithText("Replace and restore").performClick()
        compose.waitUntil(5000) { model.uiState.value.message == "Backup restored." && !model.uiState.value.busy }
        assertEquals(imported, runBlocking { repository.snapshot() })

        backupFile.writeText("not a backup")
        compose.runOnIdle { model.inspect(uri) }
        compose.waitUntil(5000) { model.uiState.value.error != null && !model.uiState.value.busy }
        compose.runOnIdle { assertNull(model.uiState.value.preview) }
        assertEquals(imported, runBlocking { repository.snapshot() })
    }
}
