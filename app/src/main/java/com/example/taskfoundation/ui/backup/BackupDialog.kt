package com.example.taskfoundation.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.taskfoundation.ui.TaskLineSheet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

@Composable
fun BackupDialog(viewModel: BackupViewModel, onDismiss: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        it?.let(viewModel::export)
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let(viewModel::inspect)
    }
    TaskLineSheet(
        onDismiss = { if (!state.busy) { viewModel.cancelRestore(); onDismiss() } },
        title = if (state.preview == null) "Backup and restore" else "Replace current data?",
        busy = state.busy,
        content = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (state.preview != null) {
                    Text("This backup contains ${state.preview}.")
                    Text("Restoring replaces tasks, projects, subtasks, tags and your entire library, including habits, notes, attachments, comments and activity. Export a backup first to keep your current data. Older backups contain no library records.")
                } else {
                    Text("Save your tasks, projects, subtasks, tags and library, including habits, notes, attachments, comments and activity. Appearance and the running focus timer are device settings and are not included.")
                    Text("Backup files are readable JSON. Store them somewhere you trust.")
                    OutlinedButton(enabled = !state.busy, onClick = { export.launch("TaskLine-${LocalDate.now()}.json") }) {
                        Text("Export backup")
                    }
                    OutlinedButton(enabled = !state.busy, onClick = { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) {
                        Text("Choose backup to restore")
                    }
                }
                if (state.busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Working…")
                }
                state.message?.let { Text(it) }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        footer = { Column(Modifier.fillMaxWidth()) {
            if (state.preview != null) {
                TextButton(enabled = !state.busy, onClick = viewModel::restore) { Text("Replace and restore") }
            } else {
                TextButton(enabled = !state.busy, onClick = onDismiss) { Text("Done") }
            }
            if (state.preview != null) TextButton(enabled = !state.busy, onClick = viewModel::cancelRestore) { Text("Cancel") }
        } },
    )
}
