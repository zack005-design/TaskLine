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
fun TaskDetailsDialog(task: Task, viewModel: TasksViewModel, onStartFocus: ((Task, Int) -> Unit)? = null,
    libraryViewModel: LibraryViewModel? = null, onDismiss: () -> Unit) {
    val state by viewModel.detailsState.collectAsStateWithLifecycle()
    val tasks by viewModel.uiState.collectAsStateWithLifecycle()
    var title by rememberSaveable(task.id) { mutableStateOf("") }
    var tag by rememberSaveable(task.id) { mutableStateOf("") }
    var comment by rememberSaveable(task.id) { mutableStateOf("") }
    var focusMinutes by rememberSaveable(task.id) { mutableIntStateOf(25) }
    val library = libraryViewModel?.state?.collectAsStateWithLifecycle()?.value
    var editingId by rememberSaveable(task.id) { mutableStateOf<Long?>(null) }
    var deletingId by rememberSaveable(task.id) { mutableStateOf<Long?>(null) }
    LaunchedEffect(task.id) { viewModel.selectDetails(task.id) }
    DisposableEffect(task.id) { onDispose { viewModel.selectDetails(null) } }
    val busy = tasks.isSaving
    TaskLineSheet(
        onDismiss = { if (!busy) onDismiss() },
        title = task.title,
        busy = busy,
        content = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (task.description.isNotBlank()) Text(task.description)
                task.durationMinutes?.let { Text("Planned duration · $it minutes", color = MaterialTheme.colorScheme.primary) }
                task.deadline?.let { Text("Deadline · ${com.example.taskfoundation.domain.time.CalendarDates.decode(it)}") }
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
                    TaskLineTextField(title, { title = it }, enabled = !busy, label = { Text("Subtask title") }, modifier = Modifier.fillMaxWidth())
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
                    TaskLineTextField(tag, { tag = it }, enabled = !busy, label = { Text("Tag name") }, modifier = Modifier.fillMaxWidth())
                    TextButton(enabled = !busy && tag.isNotBlank(), onClick = {
                        viewModel.attachTag(task.id, tag) { tag = "" }
                    }) { Text("Add tag") }
                    if (onStartFocus != null && !task.isCompleted) Choice("Focus length", "$focusMinutes minutes", listOf(5, 15, 25, 45, 60, 90),
                        !busy, label = { "$it minutes" }) { focusMinutes = it }
                    if (libraryViewModel != null && library != null) TaskAttachments(task.id, library, libraryViewModel)
                    if (libraryViewModel != null && library != null) CollapsibleSection("Comments", "Private to this device") {
                        library.items.filter { it.kind == "Comment" && it.id.startsWith("${task.id}:") }.forEach { item ->
                            Text(org.json.JSONObject(item.payload).getString("text"))
                            Text(java.time.Instant.ofEpochMilli(item.createdAt).atZone(java.time.ZoneId.systemDefault())
                                .format(java.time.format.DateTimeFormatter.ofPattern("d MMM, HH:mm")), style = MaterialTheme.typography.labelSmall)
                            TextButton(enabled = !library.busy, onClick = { libraryViewModel.delete(item.id) }) { Text("Remove comment") }
                        }
                        TaskLineTextField(comment, { comment = it }, enabled = !library.busy, label = { Text("Write a comment") })
                        library.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        TextButton(enabled = !library.busy && comment.isNotBlank(), onClick = {
                            libraryViewModel.comment(task.id, comment) { comment = "" }
                        }) { Text("Add comment") }
                    }
                }
            }
        },
        footer = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (onStartFocus != null && !task.isCompleted) TaskLinePrimaryButton("Focus", enabled = !busy, onClick = { onStartFocus(task, focusMinutes) })
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
