package com.example.taskfoundation.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskfoundation.domain.model.Project
import com.example.taskfoundation.domain.model.Task

/** TaskLine's compact, accessible segmented control, shared by calendar views. */
@Composable
internal fun TaskLineSegments(options: List<String>, selected: String, onSelect: (String) -> Unit,
    modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
        .padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        options.forEach { option ->
            val color by animateColorAsState(if (selected == option) MaterialTheme.colorScheme.surfaceContainerLowest
                else Color.Transparent, label = "segment")
            Box(Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(color)
                .selectable(selected == option, role = Role.Tab, onClick = { onSelect(option) })
                .heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(option, style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected == option) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected == option) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun TaskLineSection(title: String, detail: String? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        detail?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** Circular geometry is shared by task progress, project progress and the focus clock. */
@Composable
internal fun ProgressRing(fraction: Float, label: String, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainer
    Box(modifier.semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(4.dp)) {
            drawArc(track, -90f, 360f, false, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
            drawArc(accent, -90f, 360f * fraction.coerceIn(0f, 1f), false,
                style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
        }
        Text("${(fraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun ProjectCollectionCard(project: Project, tasks: List<Task>, busy: Boolean,
    onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val done = tasks.count { it.isCompleted }
    val accent = project.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    var menu by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = accent.copy(alpha = .12f), contentColor = accent) {
                    Box(Modifier.padding(12.dp)) { NavigationGlyph(1) }
                }
                Spacer(Modifier.weight(1f))
                Text("${tasks.size - done} remaining", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box {
                    IconButton(onClick = { menu = true }, enabled = !busy,
                        modifier = Modifier.semantics { contentDescription = "Actions ${project.name}" }) { NavigationGlyph(5) }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit() },
                            modifier = Modifier.semantics { contentDescription = "Edit ${project.name}" })
                        DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onDelete() },
                            modifier = Modifier.semantics { contentDescription = "Delete ${project.name}" })
                    }
                }
            }
            Text(project.name, style = MaterialTheme.typography.titleLarge)
            if (project.description.isNotBlank()) Text(project.description, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(progress = { if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size },
                modifier = Modifier.fillMaxWidth().height(4.dp), color = accent, trackColor = accent.copy(alpha = .10f),
                drawStopIndicator = {})
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$done of ${tasks.size} complete", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onOpen) { Text("View tasks"); Spacer(Modifier.width(8.dp)); Text("›") }
            }
        }
    }
}

/** Branded primary action with a rectangular iOS-style footprint. */
@Composable
internal fun TaskLinePrimaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit,
    modifier: Modifier = Modifier) {
    Surface(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(16.dp),
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.heightIn(min = 52.dp)) {
        Box(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Shared sheet anatomy with a persistent title and footer, and caller-owned scrolling. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskLineSheet(title: String, onDismiss: () -> Unit, busy: Boolean = false,
    footer: @Composable () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val currentBusy by rememberUpdatedState(busy)
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { !currentBusy }),
        sheetGesturesEnabled = !busy, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).imePadding().padding(horizontal = 20.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 20.dp))
            Column(Modifier.weight(1f), content = content)
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp)) { footer() }
        }
    }
}
