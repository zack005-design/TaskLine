package com.example.taskfoundation.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.json.JSONObject

@Composable
fun TaskAttachments(taskId: Long, state: LibraryState, vm: LibraryViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var opening by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.attach(taskId, context.applicationContext, uri)
    }
    CollapsibleSection("Attachments", "Saved on this device · up to 2 MB each") {
        state.items.filter { it.kind == "Attachment" && it.id.startsWith("$taskId:") }.forEach { item ->
            val data = remember(item.payload) { JSONObject(item.payload) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(data.getString("name"), style = MaterialTheme.typography.titleSmall)
                Row {
                    TextButton(enabled = !opening, onClick = {
                        opening = true; error = null
                        scope.launch {
                            try {
                                val uri = withContext(Dispatchers.IO) {
                                    val directory = java.io.File(context.cacheDir, "attachment-preview").apply { mkdirs() }
                                    val filename = data.getString("name").replace(Regex("[^a-zA-Z0-9._-]"), "_").takeLast(90).ifBlank { "file" }
                                    val safeId = item.id.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(100)
                                    val file = java.io.File(directory, safeId + "-" + filename)
                                    file.writeBytes(java.util.Base64.getDecoder().decode(data.getString("bytes")))
                                    androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.attachments", file)
                                }
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(uri, data.getString("mime"))
                                    .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                context.startActivity(android.content.Intent.createChooser(intent, "Open attachment"))
                            } catch (e: CancellationException) { throw e }
                            catch (e: Exception) { error = "Unable to open this attachment. Install an app that supports its file type." }
                            finally { opening = false }
                        }
                    }) { Text("Open") }
                    TextButton(enabled = !state.busy, onClick = { vm.delete(item.id) }) { Text("Remove attachment") }
                }
            }
        }
        (error ?: state.error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        TextButton(enabled = !state.busy, onClick = { picker.launch(arrayOf("*/*")) }) { Text("Choose file") }
    }
}
