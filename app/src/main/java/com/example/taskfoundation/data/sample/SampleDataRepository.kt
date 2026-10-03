package com.example.taskfoundation.data.sample

import androidx.room.withTransaction
import com.example.taskfoundation.data.local.TaskDatabase
import com.example.taskfoundation.data.local.entity.*
import com.example.taskfoundation.domain.time.CalendarDates
import com.example.taskfoundation.domain.time.Clock
import com.example.taskfoundation.domain.time.SystemClock
import java.time.Instant
import java.time.ZoneId
import java.util.Base64
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Additive fixtures, committed together and identified independently of editable titles. */
class SampleDataRepository(
    private val database: TaskDatabase,
    private val clock: Clock = SystemClock,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    suspend fun add(): Int = database.withTransaction {
        val dao = database.taskDao()
        // Keep edited samples intact, including renamed projects/tags. Delete all sample
        // tasks before adding a fresh set; the keys also survive backup and restore.
        if (dao.all().any { it.importKey?.startsWith(KEY_PREFIX) == true }) return@withTransaction 0
        val now = clock.nowMillis()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        fun date(offset: Long) = CalendarDates.encode(today.plusDays(offset))
        val names = database.projectDao().all().map { it.name }.toMutableSet()
        val projects = listOf("Work", "Personal", "Learning").mapIndexed { index, label ->
            val base = "Sample · $label"
            var name = base
            var suffix = 2
            while (names.any { it.equals(name, ignoreCase = true) }) name = "$base (${suffix++})"
            names.add(name)
            database.projectDao().upsert(ProjectEntity(name = name,
                description = "Test data you can edit or delete.",
                color = listOf(0xFF3478F6L, 0xFF34C759L, 0xFFFF9500L)[index],
                createdAt = now, updatedAt = now))
        }
        data class Example(val title: String, val due: Long?, val status: String = "TODO",
            val progress: Int = 0, val repeat: String = "NONE")
        val examples = listOf(
            Example("Plan today's priorities", 0),
            Example("Build the project prototype", 3, "IN_PROGRESS", 45),
            Example("Waiting for design feedback", -2, "BLOCKED", 20),
            Example("Capture an unscheduled idea", null),
            Example("Review next week's plan", 7),
            Example("Plan next month's milestone", 28),
            Example("Daily reading", 1, repeat = "DAILY"),
            Example("Weekly review", 2, repeat = "WEEKLY"),
            Example("Monthly budget review", 4, repeat = "MONTHLY"),
            Example("Annual planning", 6, repeat = "YEARLY"),
        ) + (0L..6L).map { Example("Completed milestone ${it + 1}", -it, "DONE", 100) }
        val taskIds = mutableListOf<Long>()
        examples.forEachIndexed { index, example ->
            val done = example.status == "DONE"
            val due = example.due?.let(::date)
            val updated = if (done) today.plusDays(example.due!!).atStartOfDay(zone).toInstant().toEpochMilli() else now
            val id = dao.upsert(TaskEntity(
                title = "Sample · ${example.title}",
                description = "Sample data for exploring tasks, calendar, stats and focus. Edit or delete freely. Reminders start off; enable one in Schedule to test notifications.",
                projectId = if (index == 3) null else projects[index % projects.size],
                startDateTime = if (due != null) date(minOf(-3L, example.due)) else null,
                dueDateTime = due, priority = listOf("LOW", "MEDIUM", "HIGH", "URGENT")[index % 4],
                status = example.status, progress = example.progress, isCompleted = done,
                createdAt = date(-14), updatedAt = updated,
                dueTimeMinutes = if (due != null && !done) 9 * 60 + index * 15 else null,
                repeatRule = example.repeat, repeatAnchor = if (example.repeat != "NONE") due else null,
                importKey = "$KEY_PREFIX$index",
                durationMinutes = if (due != null && !done) 30 + (index % 3) * 15 else null,
                deadline = if (index in 1..2) date(example.due!! + 1) else null,
            ))
            taskIds.add(id)
            listOf("Review the details", "Finish and check the result").forEachIndexed { order, title ->
                dao.upsertSubtask(SubtaskEntity(taskId = id, title = title,
                    isCompleted = done || (example.progress > 0 && order == 0), sortOrder = order,
                    createdAt = date(-14), updatedAt = updated))
            }
            dao.createOrAttachTag(id, TagEntity(name = "Sample", color = 0xFF3478F6L, createdAt = now))
            dao.createOrAttachTag(id, TagEntity(name = if (index % 2 == 0) "Sample · Quick win" else "Sample · Deep work",
                color = null, createdAt = now))
        }
        suspend fun library(kind: String, title: String, data: JSONObject, taskId: Long? = null) {
            val id = "${taskId?.let { "$it:" } ?: KEY_PREFIX}${UUID.randomUUID()}"
            val item = LibraryItem(id, kind, "Sample · $title", data.toString(), now, now)
            item.validate()
            database.libraryDao().save(item)
        }
        val history = JSONObject().apply {
            (0L..6L).forEach { put(today.minusDays(it).toString(), if (it == 2L) 1 else 2) }
        }
        library("Habit", "Drink water", JSONObject().put("goal", 2).put("days", history))
        library("Habit", "Read every day", JSONObject().put("goal", 1).put("days", JSONObject()))
        library("Note", "Project ideas", JSONObject().put("text", "Try the board and priority matrix, review the calendar, then start a focus session. This editable note is sample data."))
        library("Countdown", "Project launch", JSONObject().put("date", today.plusDays(21).toString()))
        library("Template", "Weekly planning", JSONObject().put("tasks", JSONArray(listOf("Review last week", "Choose three priorities", "Schedule focus time"))))
        library("Filter", "Urgent samples", JSONObject().put("query", "Sample").put("priority", "URGENT").put("status", "Any"))
        library("Filter", "Active samples", JSONObject().put("query", "Sample").put("priority", "Any").put("status", "IN_PROGRESS"))
        val firstTask = taskIds.first()
        library("Comment", "Planning context", JSONObject().put("taskId", firstTask).put("text", "Sample comment: add notes here as you work."), firstTask)
        library("Attachment", "Example checklist", JSONObject().put("taskId", firstTask).put("name", "sample-checklist.txt")
            .put("mime", "text/plain").put("bytes", Base64.getEncoder().encodeToString("Sample checklist\n1. Review the task\n2. Try Focus\n3. Complete a subtask\n".toByteArray(Charsets.UTF_8))), firstTask)
        library("Activity", "Sample task created", JSONObject().put("taskId", firstTask).put("action", "Created"), firstTask)
        examples.size
    }

    companion object { const val KEY_PREFIX = "taskline:sample:v1:" }
}
