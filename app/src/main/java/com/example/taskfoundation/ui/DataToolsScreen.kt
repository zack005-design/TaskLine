package com.example.taskfoundation.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskfoundation.domain.model.Project
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
fun DataToolsScreen(viewModel: DataToolsViewModel, projects: List<Project>, onClose: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var projectId by rememberSaveable { mutableStateOf<Long?>(null) }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.loadCalendars() else viewModel.report("Calendar access was not granted. You can import an .ics file instead, or allow Calendar access in Android app settings.")
    }
    val openIcs = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(viewModel::previewFile) }
    Dialog(onDismissRequest = { if (!state.busy) onClose() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.safeDrawingPadding().padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Calendar import", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    TextButton(enabled = !state.busy, onClick = onClose) { Text("Close") }
                }
                LazyColumn(Modifier.weight(1f).testTag("calendar_content"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = "Working" }) }
                    state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                    state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
                    item {
                        TaskLineSection("Bring your plans together")
                        Text("Bring Google Calendar or another synced Android calendar into TaskLine as tasks. This is a one-time copy; later calendar changes do not sync automatically.")
                    }
                    item {
                        Choice("Import upcoming", "${state.days} days", listOf(30, 90, 365), !state.busy,
                            label = { "$it days" }, onSelected = viewModel::setDays)
                        Choice("Destination", projects.find { it.id == projectId }?.name ?: "No project",
                            listOf<Long?>(null) + projects.map { it.id }, !state.busy,
                            label = { id -> projects.find { it.id == id }?.name ?: "No project" }) { projectId = it }
                    }
                    item {
                        Button(enabled = !state.busy, onClick = { calendarPermission.launch(Manifest.permission.READ_CALENDAR) }, modifier = Modifier.fillMaxWidth()) { Text("Choose phone / Google calendar") }
                        OutlinedButton(enabled = !state.busy, onClick = { openIcs.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Upload calendar file (.ics)") }
                        Text("For a Google export: Calendar on the web → Settings → Import & export → Export. Extract the ZIP, then choose an .ics file. Files stay on your device.", style = MaterialTheme.typography.bodySmall)
                    }
                    items(state.calendars, key = { "calendar-${it.id}" }) { calendar ->
                        OutlinedButton(enabled = !state.busy, onClick = { viewModel.previewDevice(calendar) }, modifier = Modifier.fillMaxWidth()) {
                            Text("${calendar.name}\n${calendar.account}")
                        }
                    }
                    state.preview?.let { preview ->
                        item {
                            HorizontalDivider()
                            Text("Preview: ${preview.events.size} new events", style = MaterialTheme.typography.titleMedium)
                            Text("${preview.existing} already imported. Re-importing the same source skips existing events.")
                            if (preview.skipped > 0) Text("${preview.skipped} events or series could not be read. Complex recurrence rules or edited occurrences are supported through phone calendar import instead.", color = MaterialTheme.colorScheme.error)
                            Text("Imported tasks are due at the event's start. Reminders are off until you enable them.", style = MaterialTheme.typography.bodySmall)
                            if (preview.events.isEmpty()) Text("No new events in this date range. Try a longer range or another calendar.")
                            Row {
                                TextButton(enabled = !state.busy, onClick = { viewModel.selectAll(true) }) { Text("Select all") }
                                TextButton(enabled = !state.busy, onClick = { viewModel.selectAll(false) }) { Text("Clear selection") }
                            }
                        }
                        items(preview.events, key = { it.key }) { event ->
                            Row(Modifier.fillMaxWidth()) {
                                Checkbox(event.key in state.selected, enabled = !state.busy,
                                    onCheckedChange = { viewModel.select(event.key, it) },
                                    modifier = Modifier.semantics { contentDescription = "Import ${event.title}" })
                                Column(Modifier.weight(1f).padding(top = 8.dp)) {
                                    Text(event.title)
                                    val time = Instant.ofEpochMilli(event.start).atZone(if (event.allDay) ZoneOffset.UTC else ZoneId.systemDefault())
                                    Text(time.format(DateTimeFormatter.ofPattern(if (event.allDay) "d MMM yyyy '· All day'" else "d MMM yyyy · HH:mm")), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        item {
                            Button(enabled = !state.busy && state.selected.isNotEmpty(), onClick = { viewModel.importSelected(projectId) }, modifier = Modifier.fillMaxWidth()) { Text("Import ${state.selected.size} events") }
                            TextButton(enabled = !state.busy, onClick = viewModel::dismissPreview) { Text("Cancel import") }
                        }
                    }
                    item {
                        HorizontalDivider()
                        Text("Notifications", style = MaterialTheme.typography.titleMedium)
                        Text("Set a due date, time, and reminder when editing a task. Android may delay reminders to save battery.")
                        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("Open notification settings") }
                    }
                }
            }
        }
    }
}

