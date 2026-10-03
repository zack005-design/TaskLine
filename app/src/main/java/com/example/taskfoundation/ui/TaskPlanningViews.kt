package com.example.taskfoundation.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskfoundation.domain.model.*
import java.time.LocalDate

@Composable
fun TaskPlanningViews(mode: String, tasks: List<Task>, busy: Boolean,
    onOpen: (Task) -> Unit, onStatus: (Task, TaskStatus) -> Unit) {
    val groups = if (mode == "Board") TaskStatus.entries.map { status ->
        Triple(status.display(), "${tasks.count { it.status == status }} tasks", tasks.filter { it.status == status })
    } else MatrixQuadrant.entries.map { quadrant ->
        Triple(quadrant.title, quadrant.hint, tasks.filter { !it.isCompleted && it.quadrant(LocalDate.now()) == quadrant })
    }
    Text(if (mode == "Board") "Move work forward" else "Make space for what matters", style = MaterialTheme.typography.titleLarge)
    if (mode == "Matrix") Text("Importance follows priority. Urgency follows due dates and deadlines. Completed tasks are hidden.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        groups.forEach { (title, hint, rows) ->
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.width(280.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (rows.isEmpty()) Text("Nothing here yet", Modifier.padding(vertical = 20.dp))
                    LazyColumn(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rows, key = { it.id }) { task ->
                        Surface(onClick = { onOpen(task) }, enabled = !busy, shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(task.title, style = MaterialTheme.typography.titleSmall)
                                Text(task.priority.display(), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                                Choice("Move to", task.status.display(), TaskStatus.entries, !busy,
                                    label = { it.display() }) { onStatus(task, it) }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}
