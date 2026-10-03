package com.example.taskfoundation.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.taskfoundation.domain.model.*
import com.example.taskfoundation.domain.stats.*
import com.example.taskfoundation.ui.components.StreakBanner
import java.time.format.DateTimeFormatter

@Composable
fun StatsScreen(tasks: List<Task>, projects: List<Project>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val completed = tasks.count { it.isCompleted }
        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(20.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                ProgressRing(if (tasks.isEmpty()) 0f else completed.toFloat() / tasks.size,
                    "$completed of ${tasks.size} tasks complete", Modifier.size(76.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Small steps. Real progress.", style = MaterialTheme.typography.titleLarge)
                    Text("$completed tasks completed", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("Active", TaskStats.totalActive(tasks), Modifier.weight(1f))
                Metric("Overdue", TaskStats.overdueCount(tasks), Modifier.weight(1f))
                Metric("Done", TaskStats.totalCompleted(tasks), Modifier.weight(1f))
            }
        }
        val streak = StreakCalculator.calculate(tasks)
        StreakBanner(streak)
        if (streak < 2) Text(if (streak == 1) "One day strong. Keep going tomorrow." else "Complete a task to start your streak.")
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Last 7 days", style = MaterialTheme.typography.titleLarge)
                WeekBarChart(TaskStats.dailyCompletions(tasks))
                Text("Activity is estimated from completed tasks' last update. Edits can move a completion; reopened and repeating tasks are excluded.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TaskLineSection("Project progress")
        if (projects.isEmpty()) Text("Create a project to track its progress here.")
        projects.forEach { project ->
            val items = tasks.filter { it.projectId == project.id }
            val done = items.count { it.isCompleted }
            val progress by animateFloatAsState(if (items.isEmpty()) 0f else done.toFloat() / items.size, label = "projectProgress")
            GlassCard(Modifier.fillMaxWidth(), tintColor = project.color?.let { Color(it) }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text("$done / ${items.size} tasks complete")
                }
            }
        }
    }
}

@Composable
fun WeekBarChart(data: List<DailyCount>) {
    if (data.isEmpty()) return
    val max = data.maxOf { it.completed }.coerceAtLeast(1)
    val color = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainer
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val amount by animateFloatAsState(if (entered) 1f else 0f, tween(600), label = "weekBars")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        data.forEach { day ->
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {
                contentDescription = "${day.date}: ${day.completed} completed tasks"
            }, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text(day.completed.toString(), style = MaterialTheme.typography.labelSmall)
                Canvas(Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    drawRoundRect(track, cornerRadius = CornerRadius(8.dp.toPx()))
                    val h = size.height * day.completed.toFloat() / max * amount
                    if (h > 0) drawRoundRect(color, Offset(0f, size.height - h), Size(size.width, h), CornerRadius(8.dp.toPx()))
                }
                Text(day.date.format(DateTimeFormatter.ofPattern("EEEEE")), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
