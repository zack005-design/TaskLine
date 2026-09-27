package com.example.taskfoundation.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
    AlertDialog(
        onDismissRequest = { if (!state.busy) { viewModel.cancelRestore(); onDismiss() } },
        title = { Text(if (state.preview == null) "Backup and restore" else "Replace current data?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.preview != null) {
                    Text("This backup contains ${state.preview}.")
                    Text("Restoring replaces all current tasks, projects, subtasks and tags. Export a backup first if you want to keep your current data.")
                } else {
                    Text("Save all your tasks, projects, subtasks and tags to a file, or restore a TaskLine backup.")
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
        confirmButton = {
            if (state.preview != null) {
                TextButton(enabled = !state.busy, onClick = viewModel::restore) { Text("Replace and restore") }
            } else {
                TextButton(enabled = !state.busy, onClick = onDismiss) { Text("Done") }
            }
        },
        dismissButton = {
            if (state.preview != null) TextButton(enabled = !state.busy, onClick = viewModel::cancelRestore) { Text("Cancel") }
        },
    )
}
