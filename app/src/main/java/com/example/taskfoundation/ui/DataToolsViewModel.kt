package com.example.taskfoundation.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskfoundation.AppContainer

import com.example.taskfoundation.calendar.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneId

data class DataToolsState(val busy: Boolean = false, val message: String? = null, val error: String? = null,
    val calendars: List<DeviceCalendar> = emptyList(), val preview: CalendarPreview? = null,
    val selected: Set<String> = emptySet(), val days: Int = 90)

class DataToolsViewModel(private val context: Context, private val importer: CalendarImport) : ViewModel() {

    private val state = MutableStateFlow(DataToolsState())
    val uiState = state.asStateFlow()
    fun report(message: String) { state.update { it.copy(error = message) } }
    fun setDays(days: Int) { if (!state.value.busy) state.update { it.copy(days = days, preview = null, selected = emptySet()) } }
    fun dismissPreview() { if (!state.value.busy) state.update { it.copy(preview = null, selected = emptySet()) } }
    fun select(key: String, selected: Boolean) { state.update { it.copy(selected = if (selected) it.selected + key else it.selected - key) } }
    fun selectAll(selected: Boolean) { state.update { it.copy(selected = if (selected) it.preview?.events?.map { e -> e.key }?.toSet() ?: emptySet() else emptySet()) } }
    fun loadCalendars() = run {
        val calendars = importer.calendars()
        state.update { it.copy(calendars = calendars, message = if (calendars.isEmpty()) "No visible calendars found. Enable calendar sync for your account in Android settings, then try again." else "Choose a calendar to preview.") }
    }
    private fun range(): Pair<Long, Long> {
        val today = LocalDate.now(); val zone = ZoneId.systemDefault()
        return today.atStartOfDay(zone).toInstant().toEpochMilli() to today.plusDays(state.value.days.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
    }
    private fun dismissOldPreview() { state.update { it.copy(preview = null, selected = emptySet()) } }
    private suspend fun preview(value: CalendarPreview) {
        val prepared = importer.prepare(value)
        state.update { it.copy(preview = prepared, selected = prepared.events.map { e -> e.key }.toSet(), message = null) }
    }
    fun previewDevice(calendar: DeviceCalendar) = run { dismissOldPreview(); val (from, until) = range(); preview(importer.deviceEvents(calendar, from, until)) }
    fun previewFile(uri: Uri) = run { dismissOldPreview(); val (from, until) = range(); preview(IcsReader.read(read(uri), from, until)) }
    fun importSelected(projectId: Long?) = run {
        val current = state.value
        val count = importer.commit(current.preview?.events.orEmpty().filter { it.key in current.selected }, projectId)
        state.update { it.copy(preview = null, selected = emptySet(), message = "$count events imported as tasks. You can find them in Tasks and Gantt.") }
    }
    private fun read(uri: Uri): String = checkNotNull(context.contentResolver.openInputStream(uri)) { "Unable to open file" }.use { input ->
        val output = ByteArrayOutputStream(); val buffer = ByteArray(8192)
        while (true) { val count = input.read(buffer); if (count < 0) break
            require(output.size() + count <= 10_000_000) { "File is too large (maximum 10 MB)" }
            output.write(buffer, 0, count)
        }
        output.toString("UTF-8")
    }
    private fun run(block: suspend () -> Unit) {
        if (state.value.busy) return
        state.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { state.update { it.copy(error = error.message ?: "Operation failed. Please try again.") } }
            finally { state.update { it.copy(busy = false) } }
        }
    }
}

