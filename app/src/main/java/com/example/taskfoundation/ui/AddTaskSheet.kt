package com.example.taskfoundation.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.taskfoundation.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(original: Task?, projects: List<Project>, defaultProject: Long?, busy: Boolean,
    error: String?, defaultDate: Long?, onDismiss: () -> Unit, onSave: (Task, List<String>) -> Unit) {
    var title by rememberSaveable { mutableStateOf(original?.title ?: "") }
    var description by rememberSaveable { mutableStateOf(original?.description ?: "") }
    var projectId by rememberSaveable { mutableStateOf(if (original != null) original.projectId else defaultProject) }
    var priority by rememberSaveable { mutableStateOf(original?.priority?.name ?: TaskPriority.MEDIUM.name) }
    var status by rememberSaveable { mutableStateOf(original?.status?.name ?: TaskStatus.TODO.name) }
    var progress by rememberSaveable { mutableFloatStateOf((original?.progress ?: 0).toFloat()) }
    var startDate by rememberSaveable { mutableStateOf(original?.startDateTime) }
    var dueDate by rememberSaveable { mutableStateOf(if (original != null) original.dueDateTime else defaultDate) }
    var dueTime by rememberSaveable { mutableStateOf(original?.dueTimeMinutes) }
    var reminder by rememberSaveable { mutableStateOf(original?.reminderMinutes) }
    var repeat by rememberSaveable { mutableStateOf(original?.repeatRule?.name ?: RepeatRule.NONE.name) }
    var interval by rememberSaveable { mutableStateOf((original?.repeatInterval ?: 1).toString()) }
    var pending by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    val currentBusy by rememberUpdatedState(busy)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        confirmValueChange = { !currentBusy })
    LaunchedEffect(Unit) { focus.requestFocus() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState,
        sheetGesturesEnabled = !busy, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.94f).imePadding().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(if (original == null) "Log a task" else "Edit task", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TaskLineTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth().focusRequester(focus))
                TaskLineTextField(description, { description = it }, label = { Text("Description") }, enabled = !busy, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5)
                ProjectChipRow(projects, projectId, !busy) { projectId = it }
                PrioritySelector(TaskPriority.valueOf(priority), !busy) { priority = it.name }
                Text("Status", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskStatus.entries.forEach { option ->
                        AppleFilter(option.display(), status == option.name, enabled = !busy) {
                            status = option.name
                            if (option == TaskStatus.DONE) progress = 100f else if (progress == 100f) progress = 0f
                        }
                    }
                }

                Text("Progress: ${progress.toInt()}%")
                Slider(value = progress, onValueChange = { progress = it }, valueRange = 0f..100f, steps = 99,
                    enabled = !busy && status != TaskStatus.DONE.name,
                    modifier = Modifier.semantics { contentDescription = "Task progress" })
                CollapsibleSection("Schedule") {
                DateField("Start date", startDate, !busy) { startDate = it }
                DateField("Due date", dueDate, !busy) {
                    dueDate = it
                    if (it == null) { dueTime = null; reminder = null; repeat = RepeatRule.NONE.name }
                }
                ScheduleFields(dueDate, dueTime, reminder, RepeatRule.valueOf(repeat), interval, !busy,
                    { dueTime = it }, { reminder = it }, { repeat = it.name }, { interval = it })
                }
                Text("Subtasks", style = MaterialTheme.typography.titleMedium)
                if (original != null) Text("Add new subtasks here. Manage existing subtasks in Details.", style = MaterialTheme.typography.bodySmall)
                InlineSubtaskList(pending, !busy) { pending = it.toList() }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        SaveButton(isSaving = busy, enabled = title.isNotBlank() && !busy &&
            (repeat == RepeatRule.NONE.name || interval.toIntOrNull() in 1..365), onClick = {
            onSave((original ?: Task(title = title, createdAt = 0, updatedAt = 0)).copy(
                title = title, description = description,
                projectId = projectId?.takeIf { id -> projects.any { it.id == id } },
                startDateTime = startDate, dueDateTime = dueDate,
                dueTimeMinutes = dueTime, reminderMinutes = reminder, repeatRule = RepeatRule.valueOf(repeat),
                repeatInterval = if (repeat == RepeatRule.NONE.name) 1 else interval.toInt(),
                priority = TaskPriority.valueOf(priority), status = TaskStatus.valueOf(status),
                isCompleted = status == TaskStatus.DONE.name, progress = progress.toInt()), pending)
        })
        }
    }
}


@Composable
fun ProjectChipRow(projects: List<Project>, selected: Long?, enabled: Boolean = true, onSelect: (Long?) -> Unit) {
    Text("Project", style = MaterialTheme.typography.labelLarge)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppleFilter("No project", selected == null, enabled) { onSelect(null) }
        projects.forEach { project ->
            AppleFilter(project.name, selected == project.id, enabled) { onSelect(project.id) }
        }
    }
}

@Composable
fun PrioritySelector(selected: TaskPriority, enabled: Boolean = true, onSelect: (TaskPriority) -> Unit) {
    Text("Priority", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskPriority.entries.forEach { priority ->
            AppleFilter(priority.display(), selected == priority, enabled) { onSelect(priority) }
        }
    }
}

@Composable
fun InlineSubtaskList(titles: List<String>, enabled: Boolean, onChange: (List<String>) -> Unit) {
    titles.forEachIndexed { index, title ->
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TaskLineTextField(title, { value -> onChange(titles.toMutableList().also { it[index] = value }) },
                label = { Text("Subtask ${index + 1}") }, enabled = enabled, modifier = Modifier.weight(1f))
            IconButton(enabled = enabled, onClick = { onChange(titles.filterIndexed { i, _ -> i != index }) },
                modifier = Modifier.semantics { contentDescription = "Remove subtask ${index + 1}" }) { Text("×") }
        }
    }
    TextButton(enabled = enabled, onClick = { onChange(titles + "") }) { Text("+ Add subtask") }
}

@Composable
fun CollapsibleSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val focusManager = LocalFocusManager.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "sectionChevron")
    Column {
        TextButton(onClick = { focusManager.clearFocus(); expanded = !expanded }, modifier = Modifier.fillMaxWidth()
            .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }) {
            Text(title, Modifier.weight(1f))
            Text("⌄", Modifier.rotate(rotation))
        }
        androidx.compose.animation.AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
fun SaveButton(enabled: Boolean, isSaving: Boolean, onClick: () -> Unit) {
    Button(onClick, enabled = enabled && !isSaving, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        if (isSaving) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(if (isSaving) "Saving…" else "Save task")
    }
}
