package com.example.taskfoundation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.taskfoundation.data.local.TaskDatabase
import com.example.taskfoundation.data.local.entity.LibraryItem
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.repository.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate

data class LibraryState(val items: List<LibraryItem> = emptyList(), val loading: Boolean = true,
    val busy: Boolean = false, val error: String? = null)

class LibraryViewModel(private val db: TaskDatabase, private val tasks: TaskRepository) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val retry = MutableStateFlow(0)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state = retry.flatMapLatest { db.libraryDao().observe().map { LibraryState(it, loading = false) }
        .catch { emit(LibraryState(loading = false, error = "Unable to load your library. Retry.")) } }
        .combine(busy) { s, b -> s.copy(busy = b) }.combine(error) { s, e -> s.copy(error = e ?: s.error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryState())

    fun retry() { error.value = null; retry.value++ }
    fun save(item: LibraryItem, done: () -> Unit) = mutate {
        item.validate(); db.libraryDao().save(item); done()
    }
    fun delete(id: String) = mutate { check(db.libraryDao().delete(id) == 1) { "Item no longer exists" } }
    fun comment(taskId: Long, text: String, done: () -> Unit) = mutate {
        require(text.isNotBlank()) { "Write a comment first" }
        db.withTransaction {
            val task = checkNotNull(db.taskDao().get(taskId)) { "Task no longer exists" }
            val now = System.currentTimeMillis()
            db.libraryDao().save(LibraryItem("$taskId:${java.util.UUID.randomUUID()}", "Comment", task.title,
                JSONObject().put("taskId", taskId).put("text", text.trim()).toString(), now, now))
        }
        done()
    }
    fun attach(taskId: Long, context: android.content.Context, uri: android.net.Uri) = mutate {
        val data = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val resolver = context.contentResolver
            var name = "Attachment"
            resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) name = it.getString(0) ?: name
            }
            val output = java.io.ByteArrayOutputStream()
            checkNotNull(resolver.openInputStream(uri)) { "Unable to open this file" }.use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 2 * 1024 * 1024) { "Choose a file smaller than 2 MB" }
                    output.write(buffer, 0, count)
                }
            }
            JSONObject().put("taskId", taskId).put("name", name).put("mime", resolver.getType(uri) ?: "application/octet-stream")
                .put("bytes", java.util.Base64.getEncoder().encodeToString(output.toByteArray()))
        }
        db.withTransaction {
            val task = checkNotNull(db.taskDao().get(taskId)) { "Task no longer exists" }
            val now = System.currentTimeMillis()
            val item = LibraryItem("$taskId:${java.util.UUID.randomUUID()}", "Attachment", task.title, data.toString(), now, now)
            item.validate(); db.libraryDao().save(item)
        }
    }
    fun checkIn(id: String, day: LocalDate, delta: Int) = mutate {
        db.withTransaction {
            val item = checkNotNull(db.libraryDao().get(id)) { "Habit no longer exists" }
            val data = JSONObject(item.payload)
            val days = data.getJSONObject("days")
            val count = (days.optInt(day.toString()) + delta).coerceIn(0, data.getInt("goal"))
            if (count == 0) days.remove(day.toString()) else days.put(day.toString(), count)
            db.libraryDao().save(item.copy(payload = data.toString(), updatedAt = System.currentTimeMillis()))
        }
    }
    fun useTemplate(id: String, projectId: Long?, done: () -> Unit) = mutate {
        db.withTransaction {
            val item = checkNotNull(db.libraryDao().get(id)) { "Template no longer exists" }
            val rows = JSONObject(item.payload).getJSONArray("tasks")
            repeat(rows.length()) { tasks.saveTask(Task(title = rows.getString(it), projectId = projectId, createdAt = 0, updatedAt = 0)) }
        }
        done()
    }
    private fun mutate(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            error.value = null
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Unable to save. Please retry." }
            finally { busy.value = false }
        }
    }
}
