package com.example.taskfoundation.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppearanceSheet(onDismiss: () -> Unit) {
    val prefs = LocalContext.current.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    var mode by rememberSaveable { mutableStateOf(prefs.getString("mode", "System") ?: "System") }
    var accent by rememberSaveable { mutableStateOf(prefs.getString("accent", "Indigo") ?: "Indigo") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    TaskLineSheet(title = "Make it yours", busy = busy, onDismiss = onDismiss, content = {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Appearance", style = MaterialTheme.typography.titleLarge)
            Choice("Theme", mode, listOf("System", "Light", "Dark"), !busy) { mode = it }
            Choice("Accent", accent, listOf("Indigo", "Forest", "Rose", "Ocean", "Amber", "Violet"), !busy) { accent = it }
            Text("Widgets follow your device light and dark appearance.", style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, footer = { TaskLinePrimaryButton("Apply appearance", !busy, {
        busy = true
        scope.launch {
            try {
                check(withContext(Dispatchers.IO) { prefs.edit().putString("mode", mode).putString("accent", accent).commit() }) { "Could not save appearance" }
                onDismiss()
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { error = e.message }
            finally { busy = false }
        }
    }, Modifier.fillMaxWidth()) })
}
