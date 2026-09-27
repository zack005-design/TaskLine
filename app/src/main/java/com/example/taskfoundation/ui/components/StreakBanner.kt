package com.example.taskfoundation.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskfoundation.ui.GlassCard

@Composable
fun StreakBanner(streak: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(streak) { visible = streak >= 2 }
    AnimatedVisibility(visible, enter = slideInVertically { -it } + fadeIn()) {
        GlassCard(Modifier.fillMaxWidth(), tintColor = MaterialTheme.colorScheme.primary) {
            Column(Modifier.padding(16.dp)) {
                Text("$streak-day streak!", style = MaterialTheme.typography.titleMedium)
                Text("Small steps, every day. Keep going.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
