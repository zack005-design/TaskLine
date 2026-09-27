package com.example.taskfoundation.calendar

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import androidx.room.withTransaction
import com.example.taskfoundation.data.local.TaskDatabase
import com.example.taskfoundation.data.mapper.toEntity
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.time.CalendarDates
import java.time.*

data class DeviceCalendar(val id: Long, val name: String, val account: String)
data class CalendarEvent(val key: String, val title: String, val description: String,
    val start: Long, val end: Long, val allDay: Boolean) {
    fun toTask(now: Long, projectId: Long?, zone: ZoneId = ZoneId.systemDefault()): Task {
        val local = Instant.ofEpochMilli(start).atZone(if (allDay) ZoneOffset.UTC else zone)
        val ending = Instant.ofEpochMilli(end).atZone(if (allDay) ZoneOffset.UTC else zone)
        val details = if (allDay) "Calendar: all-day event" else "Calendar: $local – $ending"
        return Task(title = title, description = listOf(description, details).filter { it.isNotBlank() }.joinToString("\n\n"),
            projectId = projectId, dueDateTime = CalendarDates.encode(local.toLocalDate()),
            dueTimeMinutes = if (allDay) null else local.hour * 60 + local.minute,
            importKey = key, createdAt = now, updatedAt = now)
    }
}
data class CalendarPreview(val events: List<CalendarEvent>, val skipped: Int = 0, val existing: Int = 0)

class CalendarImport(private val context: Context, private val db: TaskDatabase) {
    fun calendars(): List<DeviceCalendar> = buildList {
        val columns = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME)
        checkNotNull(context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, columns,
            "${CalendarContract.Calendars.VISIBLE} = 1", null, "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC")) {
            "Unable to read calendars. Try again."
        }.use { cursor -> while (cursor.moveToNext()) add(DeviceCalendar(cursor.getLong(0), cursor.getString(1) ?: "Calendar", cursor.getString(2) ?: "")) }
    }

    fun deviceEvents(calendar: DeviceCalendar, from: Long, until: Long): CalendarPreview {
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uri, from); ContentUris.appendId(uri, until)
        val columns = arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.EVENT_LOCATION)
        val events = mutableListOf<CalendarEvent>()
        checkNotNull(context.contentResolver.query(uri.build(), columns,
            "${CalendarContract.Instances.CALENDAR_ID} = ? AND (${CalendarContract.Instances.STATUS} IS NULL OR ${CalendarContract.Instances.STATUS} != ?)",
            arrayOf(calendar.id.toString(), CalendarContract.Events.STATUS_CANCELED.toString()),
            "${CalendarContract.Instances.BEGIN} ASC")) { "Unable to read calendar events. Try again." }.use { cursor ->
            while (cursor.moveToNext()) {
                require(events.size < 5000) { "Too many events. Choose a shorter import range." }
                val start = cursor.getLong(3)
                events += CalendarEvent("device:${calendar.account}:${calendar.id}:${cursor.getLong(0)}:$start",
                    cursor.getString(1)?.takeIf { it.isNotBlank() } ?: "Untitled event",
                    listOfNotNull(cursor.getString(2), cursor.getString(6)?.takeIf { it.isNotBlank() }?.let { "Location: $it" }).joinToString("\n"),
                    start, cursor.getLong(4), cursor.getInt(5) != 0)
            }
        }
        return CalendarPreview(events.distinctBy { it.key })
    }

    suspend fun prepare(preview: CalendarPreview): CalendarPreview {
        val keys = db.taskDao().all().mapNotNull { it.importKey }.toSet()
        val unique = preview.events.distinctBy { it.key }
        return preview.copy(events = unique.filter { it.key !in keys }, existing = unique.count { it.key in keys })
    }

    suspend fun commit(events: List<CalendarEvent>, projectId: Long?): Int = db.withTransaction {
        require(events.size <= 5000) { "Too many events" }
        if (projectId != null) require(db.projectDao().all().any { it.id == projectId }) { "Project no longer exists" }
        var added = 0
        for (event in events) if (db.taskDao().importedId(event.key) == null) {
            db.taskDao().upsert(event.toTask(System.currentTimeMillis(), projectId).toEntity()); added++
        }
        added
    }
}
