package com.example.taskfoundation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import com.example.taskfoundation.domain.stats.*
import com.example.taskfoundation.ui.components.StreakBanner
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.time.CalendarDates
import java.time.LocalDate

@Composable
fun OverviewCard(tasks: List<Task>) {
    val completed = tasks.count { it.isCompleted }
    val today = LocalDate.now()
    val dueToday = tasks.count { !it.isCompleted && it.dueDateTime?.let(CalendarDates::decode) == today }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(GreetingProvider.headline(tasks), style = MaterialTheme.typography.titleMedium)
        Text(GreetingProvider.subtitle(tasks), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        StreakBanner(StreakCalculator.calculate(tasks))
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric("Active", tasks.size - completed, Modifier.weight(1f))
            Metric("Due today", dueToday, Modifier.weight(1f))
            Metric("Done", completed, Modifier.weight(1f))
        }
    }
}

}

@Composable
internal fun Metric(label: String, value: Int, modifier: Modifier) {
    var target by remember { mutableIntStateOf(0) }
    LaunchedEffect(value) { target = value }
    val count by animateIntAsState(target, tween(350), label = "metricCount")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(count.toString(), style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
