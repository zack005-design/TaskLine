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
    var duration by rememberSaveable { mutableStateOf(original?.durationMinutes?.toString() ?: "") }
    var deadline by rememberSaveable { mutableStateOf(original?.deadline) }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    val currentBusy by rememberUpdatedState(busy)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        confirmValueChange = { !currentBusy })
    LaunchedEffect(Unit) { focus.requestFocus() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState,
        sheetGesturesEnabled = !busy, containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.88f).imePadding().padding(horizontal = 24.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(if (original == null) "New task" else "Edit task", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ComposerField(title, { title = it }, "Title", "What needs to get done?", true, !busy,
                    Modifier.fillMaxWidth().focusRequester(focus).padding(top = 14.dp))
                val suggestion = SmartEntryParser.parse(title, java.time.LocalDate.now())
                if (suggestion.recognized) TextButton(enabled = !busy, onClick = {
                    title = suggestion.title
                    suggestion.date?.let { dueDate = com.example.taskfoundation.domain.time.CalendarDates.encode(it) }
                    suggestion.minutes?.let { dueTime = it }
                    suggestion.priority?.let { priority = it.name }
                    if (suggestion.repeat != RepeatRule.NONE) { repeat = suggestion.repeat.name; interval = "1" }
                }) { Text("Apply: ${suggestion.summary()}") }
                else if (original == null && title.isEmpty()) Text("Try: Read tomorrow at 6pm p2", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                ComposerField(description, { description = it }, "Description", "Add a note…", false, !busy,
                    Modifier.fillMaxWidth().heightIn(min = 56.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                ComposerChoice("Project", projects.find { it.id == projectId }?.name ?: "Inbox", 1,
                    listOf<Long?>(null) + projects.map { it.id }, !busy,
                    { id -> projects.find { it.id == id }?.name ?: "Inbox" }) { projectId = it }
                ComposerChoice("Priority", TaskPriority.valueOf(priority).display(), 2,
                    TaskPriority.entries, !busy, { it.display() }) { priority = it.name }
                CollapsibleSection("Schedule", dueDate?.let {
                    com.example.taskfoundation.domain.time.CalendarDates.decode(it)
                        .format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))
                } ?: "No date") {
                DateField("Start date", startDate, !busy) { startDate = it }
                DateField("Deadline", deadline, !busy) { deadline = it }
                Text("A fixed deadline stays separate from the day you plan to work.", style = MaterialTheme.typography.bodySmall)
                TaskLineTextField(duration, { duration = it }, enabled = !busy, label = { Text("Duration in minutes (optional)") }, singleLine = true)
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
                CollapsibleSection("More options") {
                    ComposerChoice("Status", TaskStatus.valueOf(status).display(), 0,
                        TaskStatus.entries, !busy, { it.display() }) {
                        status = it.name
                        if (it == TaskStatus.DONE) progress = 100f else if (progress == 100f) progress = 0f
                    }
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        SaveButton(isSaving = busy, enabled = title.isNotBlank() && !busy &&
            (duration.isBlank() || duration.toIntOrNull() in 1..10080) &&
            (repeat == RepeatRule.NONE.name || interval.toIntOrNull() in 1..365), onClick = {
            onSave((original ?: Task(title = title, createdAt = 0, updatedAt = 0)).copy(
                title = title, description = description,
                projectId = projectId?.takeIf { id -> projects.any { it.id == id } },
                startDateTime = startDate, dueDateTime = dueDate,
                durationMinutes = duration.toIntOrNull(), deadline = deadline,
                dueTimeMinutes = dueTime, reminderMinutes = reminder, repeatRule = RepeatRule.valueOf(repeat),
                repeatInterval = if (repeat == RepeatRule.NONE.name) 1 else interval.toInt(),
                priority = TaskPriority.valueOf(priority), status = TaskStatus.valueOf(status),
                isCompleted = status == TaskStatus.DONE.name, progress = progress.toInt()), pending)
        })
        }
    }
}


@Composable
private fun ComposerField(value: String, onChange: (String) -> Unit, label: String, placeholder: String,
    prominent: Boolean, enabled: Boolean, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    val hint = MaterialTheme.colorScheme.onSurfaceVariant
    val style = if (prominent) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyLarge
    androidx.compose.foundation.text.BasicTextField(value, onChange, enabled = enabled,
        textStyle = style.copy(color = ink), maxLines = if (prominent) 3 else 5,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.heightIn(min = 48.dp).semantics { contentDescription = label },
        decorationBox = { field ->
            Box(Modifier.padding(vertical = 8.dp)) {
                if (value.isEmpty()) Text(placeholder, style = style, color = hint.copy(alpha = .8f))
                field()
            }
        })
}

@Composable
private fun <T> ComposerChoice(title: String, value: String, kind: Int, options: List<T>, enabled: Boolean,
    label: (T) -> String, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    Box {
        Surface(onClick = { focus.clearFocus(); open = true }, enabled = enabled,
            color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.small) {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) { NavigationGlyph(kind) }
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End, color = MaterialTheme.colorScheme.primary)
                Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(open, { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(label(option)) }, onClick = { open = false; onSelect(option) })
            }
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
fun CollapsibleSection(title: String, summary: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val focusManager = LocalFocusManager.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "sectionChevron")
    Column {
        Surface(onClick = { focusManager.clearFocus(); expanded = !expanded }, color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }) {
            Row(Modifier.heightIn(min = 56.dp).padding(vertical = 8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    NavigationGlyph(if (title == "Schedule") 3 else 5)
                }
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                summary?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
                Text("⌄", Modifier.rotate(rotation), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        androidx.compose.animation.AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
fun SaveButton(enabled: Boolean, isSaving: Boolean, onClick: () -> Unit) {
    TaskLinePrimaryButton(if (isSaving) "Saving…" else "Save task", enabled && !isSaving, onClick,
        Modifier.fillMaxWidth().padding(vertical = 12.dp))
}
