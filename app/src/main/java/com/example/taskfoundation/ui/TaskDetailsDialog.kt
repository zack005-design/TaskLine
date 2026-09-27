package com.example.taskfoundation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskfoundation.domain.model.Subtask
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.ui.tasks.TasksViewModel

@Composable
fun TaskDetailsDialog(task: Task, viewModel: TasksViewModel, onStartFocus: ((Task) -> Unit)? = null, onDismiss: () -> Unit) {
    val state by viewModel.detailsState.collectAsStateWithLifecycle()
    val tasks by viewModel.uiState.collectAsStateWithLifecycle()
    var title by rememberSaveable(task.id) { mutableStateOf("") }
    var tag by rememberSaveable(task.id) { mutableStateOf("") }
    var editingId by rememberSaveable(task.id) { mutableStateOf<Long?>(null) }
    var deletingId by rememberSaveable(task.id) { mutableStateOf<Long?>(null) }
    LaunchedEffect(task.id) { viewModel.selectDetails(task.id) }
    DisposableEffect(task.id) { onDispose { viewModel.selectDetails(null) } }
    val busy = tasks.isSaving
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(task.title) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tasks.errorMessage != null) {
                    Text(tasks.errorMessage!!, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::clearError) { Text("Dismiss error") }
                }
                if (state.taskId != task.id || state.isLoading) {
                    CircularProgressIndicator()
                } else if (state.error != null) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::retryDetails) { Text("Retry") }
                } else {
                    Text("Subtasks (${state.subtasks.count { it.isCompleted }}/${state.subtasks.size})", style = MaterialTheme.typography.titleMedium)
                    Text("Subtask completion is tracked separately from task progress.")
                    if (state.subtasks.isEmpty()) Text("No subtasks yet")
                    state.subtasks.forEach { subtask ->
                        Column {
                            Row {
                                Checkbox(subtask.isCompleted, enabled = !busy,
                                    onCheckedChange = { viewModel.saveSubtask(subtask.copy(isCompleted = it)) },
                                    modifier = Modifier.semantics { contentDescription = "Complete subtask ${subtask.title}" })
                                Text(subtask.title, Modifier.weight(1f).padding(top = 12.dp))
                            }
                            Row {
                                TextButton(enabled = !busy, onClick = { editingId = subtask.id; title = subtask.title },
                                    modifier = Modifier.semantics { contentDescription = "Edit subtask ${subtask.title}" }) { Text("Edit") }
                                TextButton(enabled = !busy, onClick = { deletingId = subtask.id },
                                    modifier = Modifier.semantics { contentDescription = "Delete subtask ${subtask.title}" }) { Text("Delete") }
                            }
                        }
                    }
                    OutlinedTextField(title, { title = it }, enabled = !busy, label = { Text("Subtask title") }, modifier = Modifier.fillMaxWidth())
                    TextButton(enabled = !busy && title.isNotBlank(), onClick = {
                        val original = state.subtasks.find { it.id == editingId }
                        val subtask = original ?: Subtask(taskId = task.id, title = title,
                            sortOrder = (state.subtasks.maxOfOrNull { it.sortOrder } ?: -1) + 1, createdAt = 0, updatedAt = 0)
                        viewModel.saveSubtask(subtask.copy(title = title)) { title = ""; editingId = null }
                    }) { Text(if (editingId == null) "Add subtask" else "Save subtask") }
                    if (editingId != null) TextButton(enabled = !busy, onClick = { editingId = null; title = "" }) { Text("Cancel edit") }
                    HorizontalDivider()
                    Text("Tags", style = MaterialTheme.typography.titleMedium)
                    if (state.tags.isEmpty()) Text("No tags yet")
                    state.tags.forEach { item ->
                        Row {
                            Text(item.name, Modifier.weight(1f).padding(top = 12.dp))
                            TextButton(enabled = !busy, onClick = { viewModel.detachTag(task.id, item.id) },
                                modifier = Modifier.semantics { contentDescription = "Remove tag ${item.name}" }) { Text("Remove") }
                        }
                    }
                    OutlinedTextField(tag, { tag = it }, enabled = !busy, label = { Text("Tag name") }, modifier = Modifier.fillMaxWidth())
                    TextButton(enabled = !busy && tag.isNotBlank(), onClick = {
                        viewModel.attachTag(task.id, tag) { tag = "" }
                    }) { Text("Add tag") }
                }
            }
        },
        confirmButton = { FlowRow {
            if (onStartFocus != null && !task.isCompleted) FilledTonalButton(enabled = !busy, onClick = { onStartFocus(task) }) { Text("Focus") }
            TextButton(enabled = !busy, onClick = onDismiss) { Text("Close") }
        } },
    )
    deletingId?.let { id ->
        AlertDialog(onDismissRequest = { if (!busy) deletingId = null },
            title = { Text("Delete subtask?") }, text = { Text("This cannot be undone.") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                viewModel.deleteSubtask(id) {
                    deletingId = null
                    if (editingId == id) { editingId = null; title = "" }
                }
            }) { Text("Delete subtask permanently") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deletingId = null }) { Text("Cancel") } })
    }
}
