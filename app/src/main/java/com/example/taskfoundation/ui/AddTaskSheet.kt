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
    var pending by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        confirmValueChange = { !busy })
    LaunchedEffect(Unit) { focus.requestFocus() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState,
        sheetGesturesEnabled = !busy) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.94f).imePadding().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(if (original == null) "Log a task" else "Edit task", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth().focusRequester(focus))
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, enabled = !busy)
                ProjectChipRow(projects, projectId, !busy) { projectId = it }
                PrioritySelector(TaskPriority.valueOf(priority), !busy) { priority = it.name }
                Text("Status", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SingleChoiceSegmentedButtonRow {
                        TaskStatus.entries.forEachIndexed { index, option ->
                            SegmentedButton(selected = status == option.name, enabled = !busy,
                                shape = SegmentedButtonDefaults.itemShape(index, TaskStatus.entries.size),
                                onClick = {
                                    status = option.name
                                    if (option == TaskStatus.DONE) progress = 100f else if (progress == 100f) progress = 0f
                                }) { Text(option.display()) }
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
                InlineSubtaskList(pending, !busy) { pending = ArrayList(it) }
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
        FilterChip(selected == null, { onSelect(null) }, enabled = enabled, label = { Text("No project") })
        projects.forEach { project ->
            FilterChip(selected == project.id, { onSelect(project.id) }, enabled = enabled, label = { Text(project.name) })
        }
    }
}

@Composable
fun PrioritySelector(selected: TaskPriority, enabled: Boolean = true, onSelect: (TaskPriority) -> Unit) {
    Text("Priority", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskPriority.entries.forEach { priority ->
            val color = priorityColor(priority)
            FilterChip(selected == priority, { onSelect(priority) }, enabled = enabled,
                label = { Text(priority.display()) },
                leadingIcon = { Box(Modifier.size(8.dp).background(color, MaterialTheme.shapes.small)) })
        }
    }
}

@Composable
fun InlineSubtaskList(titles: List<String>, enabled: Boolean, onChange: (List<String>) -> Unit) {
    titles.forEachIndexed { index, title ->
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(title, { value -> onChange(titles.toMutableList().also { it[index] = value }) },
                label = { Text("Subtask ${index + 1}") }, enabled = enabled, modifier = Modifier.weight(1f))
            IconButton(enabled = enabled, onClick = { onChange(titles.filterIndexed { i, _ -> i != index }) },
                modifier = Modifier.semantics { contentDescription = "Remove subtask ${index + 1}" }) { Text("×") }
        }
    }
    TextButton(enabled = enabled, onClick = { onChange(titles + "") }) { Text("+ Add subtask") }
}

@Composable
fun CollapsibleSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "sectionChevron")
    Column {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()
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
