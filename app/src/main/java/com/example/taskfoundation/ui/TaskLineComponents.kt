package com.example.taskfoundation.ui

import androidx.compose.animation.core.*
import com.airbnb.lottie.compose.*
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.taskfoundation.domain.model.*

@Composable
fun NavigationGlyph(kind: Int) {
    val ink = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val u = size.width / 24
        when (kind) {
            0 -> { drawRoundRect(ink, Offset(3*u,3*u), Size(18*u,18*u), androidx.compose.ui.geometry.CornerRadius(4*u), style = Stroke(1.8f*u))
                drawLine(ink, Offset(7*u,12*u), Offset(10*u,15*u), 2*u)
                drawLine(ink, Offset(10*u,15*u), Offset(17*u,8*u), 2*u) }
            1 -> { drawRoundRect(ink, Offset(2*u,6*u), Size(20*u,15*u), androidx.compose.ui.geometry.CornerRadius(3*u), style = Stroke(1.8f*u))
                drawLine(ink, Offset(4*u,3*u), Offset(11*u,3*u), 2*u) }
            4 -> {
                for (i in 0..2) drawRoundRect(ink, Offset((4 + i * 6)*u, (14 - i * 5)*u),
                    Size(4*u, (7 + i * 5)*u), androidx.compose.ui.geometry.CornerRadius(u))
            }
            3 -> {
                drawRoundRect(ink, Offset(3*u,5*u), Size(18*u,16*u), androidx.compose.ui.geometry.CornerRadius(2*u), style = Stroke(1.8f*u))
                drawLine(ink, Offset(3*u,10*u), Offset(21*u,10*u), 1.8f*u)
                drawLine(ink, Offset(8*u,2*u), Offset(8*u,7*u), 1.8f*u)
                drawLine(ink, Offset(16*u,2*u), Offset(16*u,7*u), 1.8f*u)
                drawCircle(ink, 1.5f*u, Offset(8*u,15*u))
                drawCircle(ink, 1.5f*u, Offset(15*u,15*u))
            }
            else -> { for (i in 0..2) drawRoundRect(ink, Offset((3+i*3)*u,(4+i*7)*u), Size((15-i*3)*u,3*u), androidx.compose.ui.geometry.CornerRadius(u)) }
        }
    }
}

@Composable
fun TaskCard(task: Task, project: String, dates: String, busy: Boolean,
    onComplete: (Boolean) -> Unit, onDetails: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val accent = priorityColor(task.priority)
    val titleAlpha by animateFloatAsState(if (task.isCompleted) .5f else 1f, tween(350), label = "completedTitle")
    var progressTarget by remember(task.id) { mutableFloatStateOf(0f) }
    LaunchedEffect(task.progress) { progressTarget = task.progress / 100f }
    val animatedProgress by animateFloatAsState(progressTarget, tween(600), label = "taskProgress")
    val currentBusy by rememberUpdatedState(busy)
    val completed by rememberUpdatedState(task.isCompleted)
    val complete by rememberUpdatedState(onComplete)
    val delete by rememberUpdatedState(onDelete)
    val swipeState = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
        if (!currentBusy) when (value) {
            SwipeToDismissBoxValue.StartToEnd -> complete(!completed)
            SwipeToDismissBoxValue.EndToStart -> delete()
            else -> Unit
        }
        false
    })
    SwipeToDismissBox(state = swipeState, modifier = Modifier.testTag("task_card_${task.id}"), gesturesEnabled = !busy, backgroundContent = {
        val completing = swipeState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
        Box(Modifier.fillMaxSize().background(if (completing) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium).padding(20.dp),
            contentAlignment = if (completing) Alignment.CenterStart else Alignment.CenterEnd) {
            Text(if (completing) (if (task.isCompleted) "Reopen" else "Complete") else "Delete",
                color = if (completing) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer)
        }
    }) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Box(Modifier.drawWithContent {
            drawContent()
            val x = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr) 0f else size.width - 4.dp.toPx()
            drawRect(accent, Offset(x, 0f), Size(4.dp.toPx(), size.height))
        }) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val ring = MaterialTheme.colorScheme.primary
                val muted = MaterialTheme.colorScheme.outline
                Box(Modifier.size(48.dp).clip(CircleShape)
                    .toggleable(task.isCompleted, enabled = !busy, role = Role.Checkbox, onValueChange = onComplete)
                    .semantics { contentDescription = "Complete ${task.title}" }, contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(24.dp)) {
                        drawCircle(if (task.isCompleted) ring else muted, style = Stroke(1.5.dp.toPx()))
                        if (task.isCompleted) {
                            drawCircle(ring)
                            drawLine(Color.White, Offset(size.width*.25f, size.height*.5f), Offset(size.width*.43f, size.height*.68f), 2.dp.toPx())
                            drawLine(Color.White, Offset(size.width*.43f, size.height*.68f), Offset(size.width*.76f, size.height*.32f), 2.dp.toPx())
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium, color = LocalContentColor.current.copy(alpha = titleAlpha),
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null)
                    Text("$project · $dates", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (task.description.isNotBlank()) Text(task.description, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 48.dp, top = 4.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (task.priority == TaskPriority.HIGH || task.priority == TaskPriority.URGENT)
                    MetadataPill(task.priority.name.lowercase().replaceFirstChar { it.uppercase() }, accent)
                if (task.status == TaskStatus.BLOCKED || task.status == TaskStatus.IN_PROGRESS)
                    MetadataPill(task.status.name.lowercase().replace('_', ' '), MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.weight(1f))
                Text("${task.progress}%", style = MaterialTheme.typography.labelSmall)
            }
            FlowRow {
                TextButton(onDetails, enabled = !busy, modifier = Modifier.semantics { contentDescription = "Details ${task.title}" }) { Text("Details") }
                TextButton(onEdit, enabled = !busy, modifier = Modifier.semantics { contentDescription = "Edit ${task.title}" }) { Text("Edit") }
                TextButton(onDelete, enabled = !busy, modifier = Modifier.semantics { contentDescription = "Delete ${task.title}" },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            }
        }
    }
    }
    }
}

@Composable
private fun MetadataPill(text: String, color: Color) {
    val dark = isSystemInDarkTheme()
    // Liquid Glass pill: a soft colored rim that reads as a glass edge,
    // more visible in light mode, subtler in dark mode
    Surface(
        color = color.copy(alpha = .09f),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(0.5.dp, color.copy(alpha = if (dark) 0.28f else 0.40f))
    ) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun EmptyPanel(title: String, message: String, kind: Int = 0, lottieRes: Int? = null) {
    val floating = rememberInfiniteTransition(label = "emptyFloat")
    val offset by floating.animateFloat(0f, -8f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "floatY")
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (lottieRes != null) {
            val result = rememberLottieComposition(LottieCompositionSpec.RawRes(lottieRes))
            if (result.value != null) LottieAnimation(result.value, iterations = LottieConstants.IterateForever, modifier = Modifier.size(160.dp))
            else Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.padding(20.dp)) { NavigationGlyph(kind) }
            }
        } else Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.offset { androidx.compose.ui.unit.IntOffset(0, offset.dp.roundToPx()) }) {
            Box(Modifier.padding(20.dp)) { NavigationGlyph(kind) }
        }
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun AppleTab(title: String, kind: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ink = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp))
        .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .10f) else Color.Transparent)
        .selectable(selected, onClick = onClick, role = Role.Tab)
        .heightIn(min = 64.dp).padding(horizontal = 2.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)) {
        CompositionLocalProvider(LocalContentColor provides ink) { NavigationGlyph(kind) }
        Text(title, style = MaterialTheme.typography.labelSmall, color = ink, fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun AppleFilter(title: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Box(Modifier.clip(CircleShape)
        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
        .selectable(selected, enabled = enabled, onClick = onClick, role = Role.RadioButton)
        .heightIn(min = 48.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Filled grouped-form field; preserves Android editing, labels, and accessibility semantics. */
@Composable
fun TaskLineTextField(value: String, onValueChange: (String) -> Unit, label: @Composable () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, singleLine: Boolean = false,
    minLines: Int = 1, maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE) {
    TextField(value, onValueChange, modifier = modifier, enabled = enabled, label = label,
        singleLine = singleLine, minLines = minLines, maxLines = maxLines,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent))
}

@Composable
internal fun priorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.LOW -> Color(0xFF4CAF50)
    TaskPriority.MEDIUM -> MaterialTheme.colorScheme.primary
    TaskPriority.HIGH -> Color(0xFFFF9800)
    TaskPriority.URGENT -> MaterialTheme.colorScheme.error
}
