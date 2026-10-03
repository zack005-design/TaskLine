package com.example.taskfoundation.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskfoundation.data.local.entity.LibraryItem
import com.example.taskfoundation.domain.model.*
import org.json.JSONObject
import org.json.JSONArray
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Composable
fun LibraryScreen(vm: LibraryViewModel, projects: List<Project>, onFilter: (LibraryItem) -> Unit,
    onDismiss: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var kind by rememberSaveable { mutableStateOf("Habit") }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    var template by rememberSaveable { mutableStateOf<String?>(null) }
    var project by rememberSaveable { mutableStateOf<Long?>(null) }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    TaskLineSheet(title = "Your library", onDismiss = onDismiss, busy = state.busy,
        footer = { if (kind == "Activity") TextButton(onClick = onDismiss) { Text("Close") }
            else TaskLinePrimaryButton("New ${kind.lowercase()}", !state.busy,
            { editing = "new" }, Modifier.fillMaxWidth()) }, content = {
            Column {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LibraryItem.KINDS.forEach { option -> AppleFilter(option, kind == option) { kind = option } }
                }
                if (state.loading) CircularProgressIndicator()
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = vm::retry) { Text("Retry") } }
                notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                    if (state.items.none { it.kind == kind }) item {
                        Text(when (kind) { "Habit" -> "Small actions, every day. Set a daily target and build a streak."
                            "Note" -> "A quiet space for your ideas."
                            "Template" -> "Save a reusable checklist for your next routine."
                            "Filter" -> "Save a focused view by text, priority and status."
                            "Activity" -> "Your task activity will appear here from this version onward."
                            else -> "Keep your next milestone in sight." }, Modifier.padding(vertical = 24.dp))
                    }
                    items(state.items.filter { it.kind == kind }.let { if (kind == "Activity") it.sortedByDescending { row -> row.createdAt } else it }, key = { it.id }) { item ->
                        val data = remember(item.payload) { JSONObject(item.payload) }
                        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                when (kind) {
                                    "Activity" -> Text("${data.getString("action")} · ${java.time.Instant.ofEpochMilli(item.createdAt).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("d MMM, HH:mm"))}")
                                    "Habit" -> {
                                        val today = LocalDate.now()
                                        val days = data.getJSONObject("days")
                                        val goal = data.getInt("goal")
                                        val count = days.optInt(today.toString())
                                        var cursor = if (count >= goal) today else today.minusDays(1)
                                        var streak = 0
                                        while (days.optInt(cursor.toString()) >= goal) { streak++; cursor = cursor.minusDays(1) }
                                        Text("$count / $goal today · $streak day streak", color = MaterialTheme.colorScheme.primary)
                                        LinearProgressIndicator(progress = { count.toFloat() / goal }, modifier = Modifier.fillMaxWidth())
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            (6L downTo 0L).forEach { offset ->
                                                val day = today.minusDays(offset)
                                                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                                    Text(day.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()).take(2), style = MaterialTheme.typography.labelSmall)
                                                    Text(if (days.optInt(day.toString()) >= goal) "●" else "○", color = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                        Row {
                                            TextButton(enabled = !state.busy && count < goal, onClick = { vm.checkIn(item.id, today, 1) }) { Text("Check in") }
                                            TextButton(enabled = !state.busy && count > 0, onClick = { vm.checkIn(item.id, today, -1) }) { Text("Undo") }
                                        }
                                    }
                                    "Countdown" -> {
                                        val days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(data.getString("date")))
                                        Text(when { days > 0 -> "$days days to go"; days == 0L -> "Today is the day"; else -> "${-days} days ago" }, style = MaterialTheme.typography.headlineMedium)
                                        Text(data.getString("date"))
                                    }
                                    "Note" -> Text(data.getString("text"))
                                    "Template" -> { Text("${data.getJSONArray("tasks").length()} tasks"); TextButton(enabled = !state.busy,
                                        onClick = { template = item.id; project = null }) { Text("Use template") } }
                                    "Filter" -> { Text("${data.getString("query").ifBlank { "All text" }} · ${data.getString("priority")} · ${data.getString("status")}")
                                        TextButton(onClick = { onFilter(item); onDismiss() }) { Text("Open filter") } }
                                }
                                Row { if (kind != "Activity") TextButton(enabled = !state.busy, onClick = { editing = item.id }) { Text("Edit") }
                                    TextButton(enabled = !state.busy, onClick = { deleting = item.id }) { Text("Delete") } }
                            }
                        }
                    }
                }
            }
        })
    editing?.let { id ->
        val original = state.items.find { it.id == id }
        if (id == "new" || original != null) key(id) { LibraryEditor(kind, original, state.busy, state.error,
            onDismiss = { editing = null }, onSave = { vm.save(it) { editing = null } }) }
    }
    deleting?.let { id -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete this ${kind.lowercase()}?") },
        text = { Text("This removes its saved data and cannot be undone.") },
        confirmButton = { TextButton(onClick = { vm.delete(id); deleting = null }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }) }
    template?.let { id -> AlertDialog(onDismissRequest = { if (!state.busy) template = null }, title = { Text("Create tasks from template") },
        text = { Column { Choice("Project", projects.find { it.id == project }?.name ?: "Inbox", listOf<Long?>(null) + projects.map { it.id },
            !state.busy, label = { p -> projects.find { it.id == p }?.name ?: "Inbox" }) { project = it }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
        confirmButton = { TextButton(enabled = !state.busy, onClick = { vm.useTemplate(id, project) { template = null; notice = "Tasks added" } }) { Text("Create tasks") } },
        dismissButton = { TextButton(enabled = !state.busy, onClick = { template = null }) { Text("Cancel") } }) }
}

@Composable
private fun LibraryEditor(kind: String, original: LibraryItem?, busy: Boolean, error: String?,
    onDismiss: () -> Unit, onSave: (LibraryItem) -> Unit) {
    val data = remember(original) { original?.let { JSONObject(it.payload) } ?: JSONObject() }
    var title by rememberSaveable { mutableStateOf(original?.title ?: "") }
    var body by rememberSaveable { mutableStateOf(when (kind) {
        "Template" -> data.optJSONArray("tasks")?.let { a -> (0 until a.length()).joinToString("\n") { a.getString(it) } } ?: ""
        "Habit" -> data.optInt("goal", 1).toString()
        "Countdown" -> data.optString("date", LocalDate.now().plusDays(7).toString())
        "Filter" -> data.optString("query")
        else -> data.optString("text") }) }
    var priority by rememberSaveable { mutableStateOf(data.optString("priority", "Any")) }
    var status by rememberSaveable { mutableStateOf(data.optString("status", "Any")) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    TaskLineSheet(title = if (original == null) "New ${kind.lowercase()}" else "Edit ${kind.lowercase()}", busy = busy, onDismiss = onDismiss,
        content = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TaskLineTextField(title, { title = it }, enabled = !busy, label = { Text("Name") })
            TaskLineTextField(body, { body = it }, enabled = !busy, label = { Text(when (kind) {
                "Habit" -> "Daily target"; "Countdown" -> "Date (YYYY-MM-DD)"; "Template" -> "One task per line"; "Filter" -> "Search text"; else -> "Write your note" }) })
            if (kind == "Filter") {
                Choice("Priority", priority, listOf("Any") + TaskPriority.entries.map { it.name }, !busy) { priority = it }
                Choice("Status", status, listOf("Any") + TaskStatus.entries.map { it.name }, !busy) { status = it }
            }
            (localError ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } }, footer = { TaskLinePrimaryButton("Save ${kind.lowercase()}", !busy && title.isNotBlank(), {
            try {
                val payload = JSONObject(data.toString())
                when (kind) {
                    "Habit" -> { val goal = body.toIntOrNull() ?: error("Enter a whole-number target")
                        require(goal >= (data.optJSONObject("days")?.let { days -> days.keys().asSequence().map { days.getInt(it) }.maxOrNull() } ?: 0)) { "Target cannot be below an existing check-in count" }
                        payload.put("goal", goal).put("days", data.optJSONObject("days") ?: JSONObject()) }
                    "Countdown" -> payload.put("date", LocalDate.parse(body.trim()).toString())
                    "Template" -> payload.put("tasks", JSONArray(body.lines().map { it.trim() }.filter { it.isNotBlank() }))
                    "Filter" -> payload.put("query", body.trim()).put("priority", priority).put("status", status)
                    else -> payload.put("text", body)
                }
                val now = System.currentTimeMillis()
                val item = LibraryItem(original?.id ?: UUID.randomUUID().toString(), kind, title.trim(), payload.toString(), original?.createdAt ?: now, now)
                item.validate(); onSave(item)
            } catch (e: Exception) { localError = e.message ?: "Check the values and try again" }
        }, Modifier.fillMaxWidth()) })
}
