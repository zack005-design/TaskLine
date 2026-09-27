package com.example.taskfoundation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.delay
import com.example.taskfoundation.ui.components.CompletionBurst
import com.example.taskfoundation.focus.FocusViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskfoundation.R
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskfoundation.domain.model.*
import com.example.taskfoundation.domain.time.CalendarDates
import com.example.taskfoundation.domain.time.agendaFor
import com.example.taskfoundation.domain.time.plannerDate
import com.example.taskfoundation.ui.projects.ProjectsViewModel
import com.example.taskfoundation.ui.tasks.TasksViewModel
import com.example.taskfoundation.ui.backup.BackupViewModel
import com.example.taskfoundation.ui.backup.BackupDialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskLineScreen(tasksViewModel: TasksViewModel, projectsViewModel: ProjectsViewModel,
    backupViewModel: BackupViewModel? = null, dataToolsViewModel: DataToolsViewModel? = null,
    requestedTaskId: Long? = null, onRequestedTaskOpened: () -> Unit = {}) {
    val tasks by tasksViewModel.uiState.collectAsStateWithLifecycle()
    val projects by projectsViewModel.uiState.collectAsStateWithLifecycle()
    val focusViewModel: FocusViewModel = viewModel()
    val focusState by focusViewModel.state.collectAsStateWithLifecycle()
    var showFocus by rememberSaveable { mutableStateOf(false) }
    var celebration by remember { mutableIntStateOf(0) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableStateOf("All") }
    var selectedDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var editorDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var quickTitle by rememberSaveable { mutableStateOf("") }
    var calendarMode by rememberSaveable { mutableStateOf("Agenda") }
    var search by rememberSaveable { mutableStateOf("") }
    var projectFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    // Null means closed, zero means new, and a positive ID means edit.
    var taskEditor by rememberSaveable { mutableStateOf<Long?>(null) }
    var projectEditor by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteProjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    var detailsId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showBackup by rememberSaveable { mutableStateOf(false) }
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    var showTools by remember { mutableStateOf(false) }
    var entryMessage by remember { mutableStateOf<String?>(null) }
    val backupState = backupViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val busy = tasks.isSaving || projects.isSaving || backupState?.busy == true
    LaunchedEffect(backupState?.restoreRevision) {
        if ((backupState?.restoreRevision ?: 0) > 0) {
            projectFilter = null
            filter = "All"
            search = ""
            detailsId = null
            taskEditor = null
            projectEditor = null
            deleteTaskId = null
            deleteProjectId = null
            tasksViewModel.retryLoading()
            projectsViewModel.retryLoading()
        }
    }
    if (showBackup && backupViewModel != null) {
        BackupDialog(backupViewModel) { showBackup = false }
    }
    if (showCalendar && dataToolsViewModel != null) {
        DataToolsScreen(dataToolsViewModel, projects.projects) { showCalendar = false }
    }
    LaunchedEffect(requestedTaskId, tasks.isLoading, tasks.tasks) {
        if (requestedTaskId != null && !tasks.isLoading && !tasks.loadFailed) {
            tab = 0
            if (tasks.tasks.any { it.id == requestedTaskId }) detailsId = requestedTaskId
            else entryMessage = "This task is no longer available."
            onRequestedTaskOpened()
        }
    }
    BackHandler(tab != 0 && taskEditor == null && projectEditor == null) { tab = 0 }
    LaunchedEffect(projects.projects, projects.isLoading) {
        if (!projects.isLoading && projects.projects.none { it.id == projectFilter }) projectFilter = null
    }

    // Scroll state shared between LazyColumn and top bar — enables the Liquid Glass
    // scroll-edge effect: top bar is transparent at the top and frosts in as the user scrolls.
    val taskListState = rememberLazyListState()
    val projectListState = rememberLazyListState()
    val calendarListState = rememberLazyListState()
    val statsListState = rememberLazyListState()
    val listStates = listOf(taskListState, projectListState, calendarListState, statsListState)
    val listState = listStates[tab]
    val scrolled by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 8
        }
    }
    val topBarAlpha by animateFloatAsState(
        targetValue = if (scrolled) 0.88f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "topBarAlpha"
    )

    val fabRotation by animateFloatAsState(if (taskEditor != null || projectEditor != null) 45f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "fabRotation")
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.background(glassBackdrop()),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            // Liquid Glass scroll-edge effect: transparent at list top, frosted glass on scroll.
            // Mirrors Apple's "Optimize for legibility when content scrolls beneath controls."
            TopAppBar(title = {
                Text(if (scrolled) when (tab) { 0 -> "Your tasks"; 1 -> "Your projects"; 3 -> "Your stats"; else -> "Your calendar" } else "TaskLine",
                    style = MaterialTheme.typography.titleMedium)
            }, actions = {
                if (focusState.taskId != null) TextButton(onClick = { showFocus = true }) {
                    Text(if (focusState.isActive) "Focus ${focusState.remainingSeconds / 60}:${(focusState.remainingSeconds % 60).toString().padStart(2, '0')}" else "Focus done")
                }
                if (backupViewModel != null) {
                    Box {
                        TextButton(enabled = !busy, onClick = { showTools = true }) { Text("Tools") }
                        DropdownMenu(expanded = showTools, onDismissRequest = { showTools = false }) {
                            if (dataToolsViewModel != null) DropdownMenuItem(text = { Text("Calendar import") },
                                onClick = { showTools = false; showCalendar = true })
                            DropdownMenuItem(text = { Text("Backup & restore") },
                                onClick = { showTools = false; showBackup = true })
                        }
                    }
                } else {
                    Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM")),
                        Modifier.padding(end = 16.dp), style = MaterialTheme.typography.labelMedium)
                }
            }, colors = TopAppBarDefaults.topAppBarColors(
                // Animated glass: clear at top, frosted when scrolled
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = topBarAlpha),
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.90f),
            ))
        },
        bottomBar = {
            GlassPill(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppleTab("Tasks", 0, tab == 0, { tab = 0 }, Modifier.weight(1f))
                    AppleTab("Calendar", 3, tab == 2, { tab = 2 }, Modifier.weight(1f))
                    AppleTab("Projects", 1, tab == 1, { tab = 1 }, Modifier.weight(1f))
                    AppleTab("Stats", 4, tab == 3, { tab = 3 }, Modifier.weight(1f))
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                shape = androidx.compose.foundation.shape.CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                text = { Text(if (tab == 1) "New project" else "New task") },
                icon = { Text("+", Modifier.rotate(fabRotation)) },
                onClick = {
                    if (!busy) {
                        tasksViewModel.clearError()
                        projectsViewModel.clearError()
                        if (tab == 1) projectEditor = 0L else {
                            editorDate = if (tab == 2) CalendarDates.encode(LocalDate.ofEpochDay(selectedDay))
                                else if (filter == "Today") CalendarDates.encode(LocalDate.now()) else null
                            taskEditor = 0L
                        }
                    }
                }, expanded = true)
        },
    ) { padding ->
        Crossfade(targetState = tab, animationSpec = tween(250), label = "tabCrossfade") { currentTab ->
        LazyColumn(
            state = listStates[currentTab],
            modifier = Modifier.fillMaxSize().padding(padding).testTag("taskline_content"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(when (currentTab) { 0 -> "Your tasks"; 1 -> "Your projects"; 3 -> "Your stats"; else -> "Your calendar" }, style = MaterialTheme.typography.headlineLarge)
                Text(when (currentTab) { 0 -> LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")); 1 -> "Big ideas, one step at a time."; 3 -> "Your progress, one day at a time."; else -> "Your tasks and your time, together." },
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                entryMessage?.let { message ->
                    Text(message)
                    TextButton(onClick = { entryMessage = null }) { Text("Dismiss") }
                }
                val error = tasks.errorMessage ?: projects.errorMessage
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    if (tasks.loadFailed || projects.loadFailed) {
                        TextButton(onClick = {
                            if (tasks.loadFailed) tasksViewModel.retryLoading()
                            if (projects.loadFailed) projectsViewModel.retryLoading()
                        }) { Text("Retry loading") }
                    } else {
                        TextButton(onClick = { tasksViewModel.clearError(); projectsViewModel.clearError() }) { Text("Dismiss error") }
                    }
                }
            }
            if (tasks.isLoading || projects.isLoading) {
                item { CircularProgressIndicator(Modifier.semantics { contentDescription = "Loading" }) }
            } else if (tasks.loadFailed || projects.loadFailed) {
                item { Text("Your data could not be loaded. Retry to see your tasks and projects.") }
            } else if (currentTab == 0) {
                item { OverviewCard(tasks.tasks) }
                item {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(quickTitle, { quickTitle = it }, singleLine = true,
                            enabled = !busy, label = { Text("Add a task") }, modifier = Modifier.weight(1f))
                        FilledTonalButton(enabled = quickTitle.isNotBlank() && !busy, onClick = {
                            tasksViewModel.saveTask(Task(title = quickTitle.trim(), projectId = projectFilter,
                                dueDateTime = if (filter == "Today") CalendarDates.encode(LocalDate.now()) else null,
                                createdAt = 0, updatedAt = 0)) { quickTitle = "" }
                        }) { Text("Add") }
                    }
                }
                item {
                    TextField(value = search, onValueChange = { search = it },
                        placeholder = { Text("Search tasks") }, singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Today", "Upcoming", "Unscheduled", "Overdue", "Active", "Completed").forEachIndexed { index, option ->
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { delay(index * 60L); visible = true }
                            AnimatedVisibility(visible, enter = slideInHorizontally { it / 2 } + fadeIn()) {
                            AppleFilter(option, filter == option) { filter = option }
                            }
                        }
                    }
                    Choice("Project filter", projects.projects.find { it.id == projectFilter }?.name ?: "All projects",
                        listOf<Long?>(null) + projects.projects.map { it.id },
                        label = { id -> projects.projects.find { it.id == id }?.name ?: "All projects" },
                    ) { projectFilter = it }
                }
                val visible = tasks.tasks.filter {
                    (projectFilter == null || it.projectId == projectFilter) &&
                        (when (filter) {
                            "Completed" -> it.isCompleted
                            "Active" -> !it.isCompleted
                            "Today" -> !it.isCompleted && it.dueDateTime?.toLocalDate() == LocalDate.now()
                            "Upcoming" -> !it.isCompleted && it.plannerDate()?.let { day ->
                                day.isAfter(LocalDate.now()) && !day.isAfter(LocalDate.now().plusDays(7)) } == true
                            "Unscheduled" -> !it.isCompleted && it.plannerDate() == null
                            "Overdue" -> !it.isCompleted && it.dueDateTime?.toLocalDate()?.isBefore(LocalDate.now()) == true
                            else -> true
                        }) &&
                        (search.isBlank() || it.title.contains(search.trim(), ignoreCase = true) ||
                            it.description.contains(search.trim(), ignoreCase = true))
                }
                if (visible.isEmpty()) item {
                    EmptyPanel(if (search.isNotBlank()) "No matching tasks" else if (filter == "Completed") "Your wins will appear here" else "No tasks here yet",
                        "Tap New task to add one, or change your filters.",
                        lottieRes = if (search.isNotBlank()) R.raw.empty_search else if (filter == "Completed") R.raw.empty_done else R.raw.empty_tasks)

                }
                val ordered = visible.sortedWith(compareBy<Task> { it.isCompleted }
                    .thenBy { it.plannerDate() ?: LocalDate.MAX }
                    .thenBy { it.dueTimeMinutes ?: -1 }.thenByDescending { it.priority.ordinal }.thenBy { it.id })
                item { Text("${visible.size} tasks", style = MaterialTheme.typography.labelLarge) }
                items(ordered, key = { it.id }) { task ->
                    TaskCard(task, projects.projects.find { it.id == task.projectId }?.name ?: "No project",
                        task.dateSummary(), busy,
                        onComplete = { completed -> tasksViewModel.setCompleted(task.id, completed) { if (completed) celebration++ } },
                        onDetails = { tasksViewModel.clearError(); detailsId = task.id },
                        onEdit = { tasksViewModel.clearError(); taskEditor = task.id },
                        onDelete = { deleteTaskId = task.id })
                }
            } else if (currentTab == 1) {
                if (projects.projects.isEmpty()) item {
                    EmptyPanel("Make room for a project", "Give your next big idea a place to grow.", 1, R.raw.empty_projects)

                }
                items(projects.projects, key = { it.id }) { project ->
                    GlassCard(Modifier.fillMaxWidth(), tintColor = project.color?.let { Color(it) }) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Box(Modifier.padding(12.dp)) { NavigationGlyph(1) }
                            }
                            Text(project.name, style = MaterialTheme.typography.titleLarge)
                            if (project.description.isNotBlank()) Text(project.description)
                            Text("${tasks.tasks.count { it.projectId == project.id }} tasks")
                            val projectTasks = tasks.tasks.filter { it.projectId == project.id }
                            val completed = projectTasks.count { it.isCompleted }
                            Text("$completed completed", style = MaterialTheme.typography.labelMedium)
                            LinearProgressIndicator(progress = {
                                if (projectTasks.isEmpty()) 0f else completed.toFloat() / projectTasks.size
                            }, modifier = Modifier.fillMaxWidth())
                            FilledTonalButton(onClick = { projectFilter = project.id; filter = "All"; search = ""; tab = 0 }) { Text("View tasks") }
                            Row {
                                TextButton(enabled = !busy, onClick = { projectsViewModel.clearError(); projectEditor = project.id },
                                    modifier = Modifier.semantics { contentDescription = "Edit ${project.name}" }) { Text("Edit") }
                                TextButton(enabled = !busy, onClick = { deleteProjectId = project.id },
                                    modifier = Modifier.semantics { contentDescription = "Delete ${project.name}" }) { Text("Delete") }
                            }
                        }
                    }
                }
            } else if (currentTab == 3) {
                item { StatsScreen(tasks.tasks, projects.projects) }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(calendarMode == "Agenda", { calendarMode = "Agenda" }, label = { Text("Agenda") })
                        FilterChip(calendarMode == "Timeline", { calendarMode = "Timeline" }, label = { Text("Timeline") })
                    }
                }
                if (calendarMode == "Timeline") {
                    item { GanttChart(tasks.tasks, projects.projects) }
                } else {
                    val day = LocalDate.ofEpochDay(selectedDay)
                    item { PlannerCalendar(tasks.tasks, day) { selectedDay = it.toEpochDay() } }
                    val agenda = agendaFor(tasks.tasks, day)
                    item {
                        Text(day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), style = MaterialTheme.typography.titleLarge)
                        Text("${agenda.count { !it.isCompleted }} remaining · ${agenda.count { it.isCompleted }} completed",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (agenda.isEmpty()) item { EmptyPanel("A little room in your day", "Tap New task to plan something for this date.", 3, R.raw.empty_gantt) }
                    items(agenda, key = { it.id }) { task ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(task.dueTimeMinutes?.let { "%02d:%02d".format(it / 60, it % 60) } ?: "All day",
                                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            TaskCard(task, projects.projects.find { it.id == task.projectId }?.name ?: "Inbox",
                                task.dateSummary(), busy,
                                onComplete = { completed -> tasksViewModel.setCompleted(task.id, completed) { if (completed) celebration++ } },
                                onDetails = { tasksViewModel.clearError(); detailsId = task.id },
                                onEdit = { tasksViewModel.clearError(); taskEditor = task.id },
                                onDelete = { deleteTaskId = task.id })
                        }
                    }
                    item { Text("Tasks appear on their due date, or their start date if no due date is set.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
    }
    CompletionBurst(celebration, Modifier.align(androidx.compose.ui.Alignment.Center).size(240.dp))
    }
    if (showFocus) FocusTimerSheet(focusState, onStop = { focusViewModel.stop(); showFocus = false }, onDismiss = { showFocus = false })
    detailsId?.let { id ->
        tasks.tasks.find { it.id == id }?.let { task ->
            key(id) { TaskDetailsDialog(task, tasksViewModel, onStartFocus = { focusViewModel.start(it); detailsId = null; showFocus = true }) { detailsId = null } }
        }
    }
    taskEditor?.let { id ->
        val original = tasks.tasks.find { it.id == id }
        if (id == 0L || original != null) key(id) {
            AddTaskSheet(original, projects.projects, projectFilter, busy, tasks.errorMessage, editorDate,
                onDismiss = { if (!busy) taskEditor = null },
                onSave = { task, subtasks -> tasksViewModel.saveTask(task, subtasks) { taskEditor = null } })
        }
    }
    projectEditor?.let { id ->
        val original = projects.projects.find { it.id == id }
        if (id == 0L || original != null) key(id) {
            ProjectEditor(original, busy, projects.errorMessage,
                onDismiss = { if (!busy) projectEditor = null },
                onSave = { projectsViewModel.saveProject(it) { projectEditor = null } })
        }
    }
    deleteTaskId?.let { id ->
        DeleteConfirmation("Delete task?", "This also deletes its subtasks. This cannot be undone.",
            onDismiss = { deleteTaskId = null }, onConfirm = { tasksViewModel.deleteTask(id); deleteTaskId = null })
    }
    deleteProjectId?.let { id ->
        DeleteConfirmation("Delete project?", "Its tasks will be kept with no project. Other projects and tasks are unaffected.",
            onDismiss = { deleteProjectId = null }, onConfirm = { projectsViewModel.deleteProject(id); deleteProjectId = null })
    }
}

@Composable
private fun ProjectEditor(original: Project?, busy: Boolean, error: String?,
    onDismiss: () -> Unit, onSave: (Project) -> Unit) {
    var selectedColor by rememberSaveable { mutableStateOf(original?.color) }
    var name by rememberSaveable { mutableStateOf(original?.name ?: "") }
    var description by rememberSaveable { mutableStateOf(original?.description ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (original == null) "New project" else "Edit project") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Project name") }, singleLine = true, enabled = !busy)
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, enabled = !busy)
                Text("Project color", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val options = listOf(0xFF0062D6L, 0xFF4CAF50L, 0xFFFF5722L, 0xFF9C27B0L, 0xFFFF9800L, 0xFF00BCD4L, null)
                    val names = listOf("Blue", "Green", "Orange red", "Purple", "Amber", "Cyan", "No color")
                    options.forEachIndexed { index, color ->
                        Box(Modifier.size(48.dp).selectable(selectedColor == color, enabled = !busy, role = Role.RadioButton,
                            onClick = { selectedColor = color }).semantics { contentDescription = names[index] },
                            contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Box(Modifier.size(30.dp).background(color?.let { Color(it) } ?: MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .border(if (selectedColor == color) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                                contentAlignment = androidx.compose.ui.Alignment.Center) { if (color == null) Text("x") }
                        }
                    }
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank() && !busy, onClick = {
            onSave((original ?: Project(name = name, createdAt = 0, updatedAt = 0)).copy(name = name, description = description, color = selectedColor))
        }) { Text(if (busy) "Saving…" else "Save project") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } })
}

@Composable
internal fun <T> Choice(title: String, selected: String, options: List<T>, enabled: Boolean = true,
    label: (T) -> String = { it.toString() }, onSelected: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("$title: $selected")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(label(option)) }, onClick = { onSelected(option); expanded = false })
            }
        }
    }
}

@Composable
private fun DeleteConfirmation(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete permanently") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateField(label: String, value: Long?, enabled: Boolean, onChanged: (Long?) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(
        onClick = { open = true },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("$label: ${value?.formatDate() ?: "Not set"}")
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis =
            value ?: CalendarDates.encode(LocalDate.now()))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    onChanged(state.selectedDateMillis)
                    open = false
                }) { Text("Set date") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) { DatePicker(state = state, title = { Text(label) }) }
    }
    if (value != null) TextButton(enabled = enabled, onClick = { onChanged(null) }) { Text("Clear ${label.lowercase()}") }
}

@Composable
private fun GanttChart(tasks: List<Task>, projects: List<Project>) {
    val scheduled = tasks.mapNotNull { task ->
        val start = task.startDateTime ?: task.dueDateTime
        val end = task.dueDateTime ?: task.startDateTime
        if (start == null || end == null) null else task to Pair(start.toLocalDate(), end.toLocalDate())
    }
    if (scheduled.isEmpty()) {
        EmptyPanel("No scheduled tasks", "Add a start date or due date to see your plan take shape.", 2, R.raw.empty_gantt)

        return
    }
    var windowStart by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    val first = LocalDate.ofEpochDay(windowStart)
    val last = first.plusDays(13)
    val days = 14
    val scroll = rememberScrollState()
    val dayWidth = 76.dp
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${first.format(DateTimeFormatter.ofPattern("d MMM"))} – ${last.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}",
            style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { windowStart -= 14 }) { Text("Previous") }
            FilledTonalButton(onClick = { windowStart = LocalDate.now().toEpochDay() }) { Text("Today") }
            OutlinedButton(onClick = { windowStart += 14 }) { Text("Next") }
        }
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(132.dp))
            Row(Modifier.horizontalScroll(scroll).width(dayWidth * days)) {
                repeat(days) { index ->
                    Text(
                        first.plusDays(index.toLong()).format(DateTimeFormatter.ofPattern("d MMM")),
                        modifier = Modifier.width(dayWidth).padding(6.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        val visible = scheduled.filter { !it.second.second.isBefore(first) && !it.second.first.isAfter(last) }
        if (visible.isEmpty()) Text("No tasks in this fortnight. Browse another period.",
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 24.dp))
        visible.forEach { (task, range) ->
            val clippedStart = maxOf(first, range.first)
            val clippedEnd = minOf(last, range.second)
            val startOffset = ChronoUnit.DAYS.between(first, clippedStart).toInt()
            val span = (ChronoUnit.DAYS.between(clippedStart, clippedEnd).toInt() + 1).coerceAtLeast(1)
            Row(Modifier.height(48.dp)) {
                Text(task.title, modifier = Modifier.width(132.dp).padding(6.dp), style = MaterialTheme.typography.bodySmall)
                Row(Modifier.horizontalScroll(scroll).width(dayWidth * days).fillMaxHeight()) {
                    Box(Modifier.width(dayWidth * startOffset))
                    Box(
                        Modifier.width(dayWidth * span).fillMaxHeight().padding(vertical = 8.dp)
                            .background(if (task.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                MaterialTheme.shapes.small),
                    ) {
                        Text(
                            "${task.progress}%${projects.find { it.id == task.projectId }?.let { " · ${it.name}" } ?: ""}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

private fun Long.toLocalDate(): LocalDate = CalendarDates.decode(this)

private fun Long.formatDate(): String = toLocalDate().format(DateTimeFormatter.ofPattern("d MMM yyyy"))

private fun Task.dateSummary(): String = (when {
    startDateTime != null && dueDateTime != null -> "${startDateTime.formatDate()} → ${dueDateTime.formatDate()}"
    startDateTime != null -> "Starts ${startDateTime.formatDate()}"
    dueDateTime != null -> "Due ${dueDateTime.formatDate()}"
    else -> "No dates"
}) + (dueTimeMinutes?.let { " · %02d:%02d".format(it / 60, it % 60) } ?: "") +
    (if (repeatRule != RepeatRule.NONE) " · Repeats ${repeatRule.name.lowercase()}" else "")

internal fun Enum<*>.display(): String = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

