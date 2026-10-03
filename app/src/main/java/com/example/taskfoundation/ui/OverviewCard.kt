package com.example.taskfoundation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.time.CalendarDates
import java.time.LocalDate

@Composable
fun OverviewCard(tasks: List<Task>, onSelect: (String) -> Unit = {}) {
    val today = LocalDate.now()
    val counts = listOf(tasks.count { !it.isCompleted && it.dueDateTime?.let(CalendarDates::decode) == today },
        tasks.count { !it.isCompleted }, tasks.count { it.isCompleted })
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("Today", "Active", "Completed").forEachIndexed { index, label ->
            Surface(onClick = { onSelect(label) }, modifier = Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(18.dp),
                color = if (index == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(counts[index].toString(), style = MaterialTheme.typography.headlineMedium,
                        color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    Text(if (index == 2) "Done" else if (index == 0) "Due today" else "Active tasks",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun Metric(label: String, value: Int, modifier: Modifier) {
    val count by animateIntAsState(value, tween(350), label = "metricCount")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(count.toString(), style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
