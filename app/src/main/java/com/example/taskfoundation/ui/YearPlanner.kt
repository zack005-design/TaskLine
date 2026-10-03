package com.example.taskfoundation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.time.plannerDate
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun YearPlanner(tasks: List<Task>, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    var year by rememberSaveable { mutableIntStateOf(selected.year) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(enabled = year > 1, onClick = { year-- }) { Text("Previous year") }
            Text(year.toString(), style = MaterialTheme.typography.headlineSmall)
            TextButton(enabled = year < 9999, onClick = { year++ }) { Text("Next year") }
        }
        Month.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { month ->
                    val count = tasks.count { !it.isCompleted && it.plannerDate()?.let { d -> d.year == year && d.month == month } == true }
                    Surface(onClick = { onSelect(LocalDate.of(year, month, 1)) }, modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                        Column(Modifier.padding(12.dp).heightIn(min = 76.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(month.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.titleSmall)
                            Text("$count active", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        Text("Choose a month to plan its days.", style = MaterialTheme.typography.bodySmall)
    }
}
