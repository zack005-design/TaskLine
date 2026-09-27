package com.example.taskfoundation.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.taskfoundation.focus.FocusState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerSheet(state: FocusState, onStop: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var permission by remember { mutableStateOf(context.getSystemService(android.app.NotificationManager::class.java).areNotificationsEnabled()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (state.isActive) "Time to focus" else "Focus session", style = MaterialTheme.typography.headlineSmall)
            Text(state.taskTitle, style = MaterialTheme.typography.titleMedium)
            val primary = MaterialTheme.colorScheme.primary
            val track = MaterialTheme.colorScheme.surfaceVariant
            Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                    drawArc(track, -90f, 360f, false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(primary, -90f, 360f * state.remainingSeconds / state.totalSeconds.coerceAtLeast(1), false,
                        style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
                }
                Text("%02d:%02d".format(state.remainingSeconds / 60, state.remainingSeconds % 60), style = MaterialTheme.typography.displayMedium)
            }
            if (!state.isActive && state.taskId != null) Text("Well done. Take a short break.")
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!permission && Build.VERSION.SDK_INT >= 33) TextButton(onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                Text("Enable completion notifications")
            }
            Text("You can close this sheet. Background alerts may arrive later when Android limits background work.", style = MaterialTheme.typography.bodySmall)
            Button(onStop, Modifier.fillMaxWidth()) { Text(if (state.isActive) "Stop focus" else "Done") }
            TextButton(onDismiss) { Text("Close") }
        }
    }
}
