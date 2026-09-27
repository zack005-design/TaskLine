package com.example.taskfoundation.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.taskfoundation.domain.model.RepeatRule
import com.example.taskfoundation.reminders.TaskReminders
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleFields(dueDate: Long?, time: Int?, reminder: Int?, repeat: RepeatRule, interval: String, enabled: Boolean,
    onTime: (Int?) -> Unit, onReminder: (Int?) -> Unit, onRepeat: (RepeatRule) -> Unit, onInterval: (String) -> Unit) {
    var pickTime by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val manager = context.getSystemService(NotificationManager::class.java)
    fun notificationsEnabled() = manager.areNotificationsEnabled() &&
        manager.getNotificationChannel(TaskReminders.CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    var notificationsEnabled by remember { mutableStateOf(notificationsEnabled()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) notificationsEnabled = notificationsEnabled()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsEnabled = notificationsEnabled()
    }
    if (dueDate == null) {
        Text("Add a due date to set a time, reminder, or repeat.", style = MaterialTheme.typography.bodySmall)
        return
    }
    OutlinedButton(enabled = enabled, onClick = { pickTime = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Due time: ${time?.let { LocalTime.of(it / 60, it % 60).format(DateTimeFormatter.ofPattern("HH:mm")) } ?: "Not set"}")
    }
    if (time != null) TextButton(enabled = enabled, onClick = { onTime(null); onReminder(null) }) { Text("Clear due time") }
    if (pickTime) {
        val state = rememberTimePickerState(initialHour = (time ?: 540) / 60, initialMinute = (time ?: 540) % 60, is24Hour = true)
        AlertDialog(onDismissRequest = { pickTime = false }, title = { Text("Due time") },
            text = { TimeInput(state) }, confirmButton = { TextButton(onClick = { onTime(state.hour * 60 + state.minute); pickTime = false }) { Text("Set time") } },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Cancel") } })
    }
    fun reminderLabel(value: Int?) = when (value) { null -> "Off"; 0 -> "At due time"; 1440 -> "1 day before"; else -> "$value minutes before" }
    Choice("Reminder", reminderLabel(reminder), listOf(null, 0, 5, 15, 30, 60, 1440), enabled && time != null,
        label = ::reminderLabel, onSelected = onReminder)
    if (reminder != null) {
        Text("Reminders follow the device time zone. Android may delay delivery to save battery.", style = MaterialTheme.typography.bodySmall)
        if (!notificationsEnabled) {
            Text("Notifications are off. Your reminder is saved but cannot notify you yet.", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = {
                if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }) { Text("Enable notifications") }
        }
    }
    Choice("Repeat", repeat.name.lowercase().replaceFirstChar { it.uppercase() }, RepeatRule.entries, enabled,
        label = { it.name.lowercase().replaceFirstChar { first -> first.uppercase() } }, onSelected = onRepeat)
    if (repeat != RepeatRule.NONE) {
        OutlinedTextField(interval, onInterval, label = { Text("Repeat interval") }, singleLine = true, enabled = enabled,
            isError = interval.toIntOrNull() !in 1..365, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Text("Every $interval ${when (repeat) { RepeatRule.DAILY -> "days"; RepeatRule.WEEKLY -> "weeks"; RepeatRule.MONTHLY -> "months"; else -> "years" }}. Completing advances to the next date and resets subtasks; missed dates are skipped.",
            style = MaterialTheme.typography.bodySmall)
    }
}
