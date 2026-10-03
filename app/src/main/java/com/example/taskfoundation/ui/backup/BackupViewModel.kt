package com.example.taskfoundation.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskfoundation.data.backup.BackupFiles
import com.example.taskfoundation.data.backup.BackupRepository
import com.example.taskfoundation.data.backup.TaskBackup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupUiState(
    val busy: Boolean = false,
    val preview: String? = null,
    val message: String? = null,
    val error: String? = null,
    val restoreRevision: Int = 0,
)

class BackupViewModel(
    private val repository: BackupRepository,
    private val files: BackupFiles,
) : ViewModel() {
    private val state = MutableStateFlow(BackupUiState())
    val uiState = state.asStateFlow()
    private var pending: TaskBackup? = null

    fun export(uri: Uri) = runOperation("Could not save backup. Check the destination and try again.") {
        withContext(Dispatchers.IO) { files.write(uri, repository.snapshot()) }
        state.value = state.value.copy(message = "Backup saved.")
    }

    fun inspect(uri: Uri) = runOperation("Could not read this TaskLine backup. Your current data is unchanged.") {
        pending = null
        state.value = state.value.copy(preview = null)
        val backup = withContext(Dispatchers.IO) { files.read(uri) }
        pending = backup
        state.value = state.value.copy(preview =
            "${backup.projects.size} projects, ${backup.tasks.size} tasks, ${backup.subtasks.size} subtasks, ${backup.tags.size} tags and ${backup.library.size} library records")
    }

    fun cancelRestore() {
        if (state.value.busy) return
        pending = null
        state.value = state.value.copy(preview = null)
    }

    fun restore() {
        val backup = pending ?: return
        runOperation("Restore failed. Your previous data has been kept.") {
            withContext(Dispatchers.IO) { repository.restore(backup) }
            pending = null
            state.value = state.value.copy(preview = null, message = "Backup restored.",
                restoreRevision = state.value.restoreRevision + 1)
        }
    }

    private fun runOperation(failure: String, block: suspend () -> Unit) {
        if (state.value.busy) return
        state.value = state.value.copy(busy = true, message = null, error = null)
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                state.value = state.value.copy(error = if (error is IllegalArgumentException)
                    error.message ?: failure else failure)
            } finally {
                state.value = state.value.copy(busy = false)
            }
        }
    }
}
