package com.example.taskfoundation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.time.monthGrid
import com.example.taskfoundation.domain.time.plannerDate
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
internal fun PlannerCalendar(tasks: List<Task>, selectedDay: LocalDate, onSelect: (LocalDate) -> Unit) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(selectedDay).toString()) }
    var weekOnly by rememberSaveable { mutableStateOf(false) }
    val month = YearMonth.parse(monthText)
    val counts = tasks.filterNot { it.isCompleted }.mapNotNull { it.plannerDate() }.groupingBy { it }.eachCount()
    val weekStart = selectedDay.minusDays((selectedDay.dayOfWeek.value - 1).toLong())
    val days = if (weekOnly) List(7) { weekStart.plusDays(it.toLong()) } else monthGrid(month)
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(start = 4.dp))
                IconButton(onClick = {
                    if (weekOnly) {
                        val day = selectedDay.minusWeeks(1); onSelect(day); monthText = YearMonth.from(day).toString()
                    } else monthText = month.minusMonths(1).toString()
                }, modifier = Modifier.semantics { contentDescription = "Previous calendar period" }) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
                IconButton(onClick = {
                    if (weekOnly) {
                        val day = selectedDay.plusWeeks(1); onSelect(day); monthText = YearMonth.from(day).toString()
                    } else monthText = month.plusMonths(1).toString()
                }, modifier = Modifier.semantics { contentDescription = "Next calendar period" }) { Text("›", style = MaterialTheme.typography.headlineMedium) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TaskLineSegments(listOf("Month", "Week"), if (weekOnly) "Week" else "Month", {
                    weekOnly = it == "Week"; monthText = YearMonth.from(selectedDay).toString()
                }, Modifier.weight(1f))
                TextButton(onClick = { onSelect(LocalDate.now()); monthText = YearMonth.now().toString() }) { Text("Today") }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            days.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        val chosen = date == selectedDay
                        val count = counts[date] ?: 0
                        Column(Modifier.weight(1f).heightIn(min = 52.dp)
                            .semantics {
                                contentDescription = "${date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))}, $count active tasks"
                                selected = chosen
                            }
                            .clickable { onSelect(date); monthText = YearMonth.from(date).toString() }
                            .padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(32.dp).background(
                                if (chosen) MaterialTheme.colorScheme.primary
                                else if (date == LocalDate.now()) MaterialTheme.colorScheme.primaryContainer
                                else androidx.compose.ui.graphics.Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
                                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium,
                                    color = if (chosen) MaterialTheme.colorScheme.onPrimary
                                    else if (YearMonth.from(date) != month) MaterialTheme.colorScheme.outline
                                    else MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(Modifier.height(3.dp))
                            Box(Modifier.size(4.dp).background(if (count > 0) MaterialTheme.colorScheme.primary
                                else androidx.compose.ui.graphics.Color.Transparent, CircleShape))
                        }
                    }
                }
            }
        }
    }
}
