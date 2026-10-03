package com.example.taskfoundation.data.backup

import androidx.room.withTransaction
import com.example.taskfoundation.data.local.TaskDatabase
import com.example.taskfoundation.data.local.entity.*
import com.example.taskfoundation.domain.model.TaskPriority
import com.example.taskfoundation.domain.model.TaskStatus
import com.example.taskfoundation.domain.model.RepeatRule
import com.example.taskfoundation.data.mapper.toDomain
import com.example.taskfoundation.domain.time.CalendarDates
import com.example.taskfoundation.domain.time.TaskSchedule
import org.json.JSONArray
import org.json.JSONObject

data class TaskBackup(
    val projects: List<ProjectEntity>,
    val tasks: List<TaskEntity>,
    val subtasks: List<SubtaskEntity>,
    val tags: List<TagEntity>,
    val links: List<TaskTagCrossRef>,
    val library: List<LibraryItem> = emptyList(),
) {
    fun validate() {
        fun valid(condition: Boolean) = require(condition) { "Invalid backup data. Nothing was restored." }
        fun ids(values: List<Long>): Set<Long> {
            valid(values.all { it > 0 } && values.distinct().size == values.size)
            return values.toSet()
        }
        val projectIds = ids(projects.map { it.id })
        val taskIds = ids(tasks.map { it.id })
        val tagIds = ids(tags.map { it.id })
        ids(subtasks.map { it.id })
        valid(projects.map { it.name }.distinct().size == projects.size)
        valid(tags.map { it.name }.distinct().size == tags.size)
        val importKeys = tasks.mapNotNull { it.importKey }
        valid(importKeys.distinct().size == importKeys.size)
        projects.forEach { valid(it.name.isNotBlank()) }
        tags.forEach { valid(it.name.isNotBlank()) }
        tasks.forEach {
            valid(it.title.isNotBlank() && (it.projectId == null || it.projectId in projectIds))
            valid(it.priority in TaskPriority.entries.map { p -> p.name })
            valid(it.status in TaskStatus.entries.map { s -> s.name })
            valid(it.repeatRule in RepeatRule.entries.map { rule -> rule.name })
            valid(it.progress in 0..100)
            valid(it.isCompleted == (it.status == TaskStatus.DONE.name))
            valid(!it.isCompleted || it.progress == 100)
            valid(listOfNotNull(it.startDateTime, it.dueDateTime).all(CalendarDates::isValid))
            valid(it.startDateTime == null || it.dueDateTime == null || it.startDateTime <= it.dueDateTime)
            valid(it.repeatAnchor == null || CalendarDates.isValid(it.repeatAnchor))
            TaskSchedule.validate(it.toDomain())
        }
        subtasks.forEach { valid(it.taskId in taskIds && it.title.isNotBlank() && it.sortOrder >= 0) }
        links.forEach { valid(it.taskId in taskIds && it.tagId in tagIds) }
        valid(links.distinct().size == links.size)
        valid(library.map { it.id }.distinct().size == library.size)
        library.forEach {
            it.validate()
            if (it.kind in listOf("Comment", "Attachment")) {
                val taskId = JSONObject(it.payload).getLong("taskId")
                valid(taskId in taskIds && it.id.startsWith("$taskId:"))
            }
        }
    }
}

class BackupRepository(private val database: TaskDatabase) {
    suspend fun snapshot(): TaskBackup = database.withTransaction {
        val dao = database.backupDao()
        TaskBackup(dao.projects(), dao.tasks(), dao.subtasks(), dao.tags(), dao.links(), database.libraryDao().all())
    }

    suspend fun restore(backup: TaskBackup) {
        backup.validate()
        database.withTransaction {
            val dao = database.backupDao()
            dao.deleteTasks() // Cascades to subtasks and associations.
            dao.deleteProjects()
            dao.deleteTags()
            dao.insertProjects(backup.projects)
            dao.insertTasks(backup.tasks)
            dao.insertSubtasks(backup.subtasks)
            dao.insertTags(backup.tags)
            dao.insertLinks(backup.links)
            database.libraryDao().clear()
            database.libraryDao().insert(backup.library)
        }
    }
}

/** Versioned portable JSON; all fields are required, nullable fields use explicit null. */
object BackupJson {
    const val MAX_BYTES = 20 * 1024 * 1024

    fun encode(backup: TaskBackup): String {
        backup.validate()
        fun row(vararg fields: Pair<String, Any?>) = JSONObject().apply {
            fields.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) }
        }
        return row(
            "format" to "taskline-backup", "version" to 4,
            "dateEncoding" to "utc-midnight-calendar-date",
            "projects" to JSONArray(backup.projects.map {
                row("id" to it.id, "name" to it.name, "description" to it.description,
                    "color" to it.color, "createdAt" to it.createdAt, "updatedAt" to it.updatedAt)
            }),
            "tasks" to JSONArray(backup.tasks.map {
                row("id" to it.id, "title" to it.title, "description" to it.description,
                    "projectId" to it.projectId, "startDateTime" to it.startDateTime,
                    "dueDateTime" to it.dueDateTime, "priority" to it.priority,
                    "status" to it.status, "progress" to it.progress, "isCompleted" to it.isCompleted,
                    "createdAt" to it.createdAt, "updatedAt" to it.updatedAt,
                    "dueTimeMinutes" to it.dueTimeMinutes, "reminderMinutes" to it.reminderMinutes,
                    "repeatRule" to it.repeatRule, "repeatInterval" to it.repeatInterval,
                    "repeatAnchor" to it.repeatAnchor, "snoozedUntil" to it.snoozedUntil,
                    "lastNotifiedAt" to it.lastNotifiedAt, "importKey" to it.importKey,
                    "durationMinutes" to it.durationMinutes, "deadline" to it.deadline)
            }),
            "subtasks" to JSONArray(backup.subtasks.map {
                row("id" to it.id, "taskId" to it.taskId, "title" to it.title,
                    "isCompleted" to it.isCompleted, "sortOrder" to it.sortOrder,
                    "createdAt" to it.createdAt, "updatedAt" to it.updatedAt)
            }),
            "tags" to JSONArray(backup.tags.map {
                row("id" to it.id, "name" to it.name, "color" to it.color, "createdAt" to it.createdAt)
            }),
            "taskTags" to JSONArray(backup.links.map { row("taskId" to it.taskId, "tagId" to it.tagId) }),
            "library" to JSONArray(backup.library.map { row("id" to it.id, "kind" to it.kind,
                "title" to it.title, "payload" to it.payload, "createdAt" to it.createdAt, "updatedAt" to it.updatedAt) }),
        ).toString(2).also { require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup exceeds 20 MB. Remove unnecessary attachments before exporting." } }
    }

    fun decode(json: String): TaskBackup {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup exceeds the 20 MB limit." }
        val root = JSONObject(json)
        val version = root.number("version")
        require(root.string("format") == "taskline-backup" && version in 1L..4L &&
            root.string("dateEncoding") == "utc-midnight-calendar-date") { "Unsupported TaskLine backup format." }
        fun <T> rows(key: String, convert: (JSONObject) -> T): List<T> {
            val array = root.getJSONArray(key)
            return List(array.length()) { convert(array.getJSONObject(it)) }
        }
        return TaskBackup(
            rows("projects") { ProjectEntity(it.number("id"), it.string("name"), it.string("description"),
                it.nullableNumber("color"), it.number("createdAt"), it.number("updatedAt")) },
            rows("tasks") { TaskEntity(it.number("id"), it.string("title"), it.string("description"),
                it.nullableNumber("projectId"), it.nullableNumber("startDateTime"), it.nullableNumber("dueDateTime"),
                it.string("priority"), it.string("status"), it.integer("progress"), it.boolean("isCompleted"),
                it.number("createdAt"), it.number("updatedAt"),
                dueTimeMinutes = if (version >= 2) it.nullableInteger("dueTimeMinutes") else null,
                reminderMinutes = if (version >= 2) it.nullableInteger("reminderMinutes") else null,
                repeatRule = if (version >= 2) it.string("repeatRule") else "NONE",
                repeatInterval = if (version >= 2) it.integer("repeatInterval") else 1,
                repeatAnchor = if (version >= 2) it.nullableNumber("repeatAnchor") else null,
                snoozedUntil = if (version >= 2) it.nullableNumber("snoozedUntil") else null,
                lastNotifiedAt = if (version >= 2) it.nullableNumber("lastNotifiedAt") else null,
                importKey = if (version >= 2) it.nullableString("importKey") else null,
                durationMinutes = if (version >= 4) it.nullableInteger("durationMinutes") else null,
                deadline = if (version >= 4) it.nullableNumber("deadline") else null) },
            rows("subtasks") { SubtaskEntity(it.number("id"), it.number("taskId"), it.string("title"),
                it.boolean("isCompleted"), it.integer("sortOrder"), it.number("createdAt"), it.number("updatedAt")) },
            rows("tags") { TagEntity(it.number("id"), it.string("name"), it.nullableNumber("color"), it.number("createdAt")) },
            rows("taskTags") { TaskTagCrossRef(it.number("taskId"), it.number("tagId")) },
            if (version >= 3) rows("library") { LibraryItem(it.string("id"), it.string("kind"),
                it.string("title"), it.string("payload"), it.number("createdAt"), it.number("updatedAt")) } else emptyList(),
        ).also { it.validate() }
    }

    private fun JSONObject.string(key: String): String = get(key).let {
        require(it is String) { "Invalid text field: $key" }; it
    }
    private fun JSONObject.number(key: String): Long = get(key).let {
        require(it is Long || it is Int) { "Invalid integer field: $key" }; (it as Number).toLong()
    }
    private fun JSONObject.integer(key: String): Int = number(key).let {
        require(it in Int.MIN_VALUE..Int.MAX_VALUE) { "Integer out of range: $key" }; it.toInt()
    }
    private fun JSONObject.nullableNumber(key: String): Long? =
        if (get(key) == JSONObject.NULL) null else number(key)
    private fun JSONObject.nullableInteger(key: String): Int? =
        if (get(key) == JSONObject.NULL) null else integer(key)
    private fun JSONObject.nullableString(key: String): String? =
        if (get(key) == JSONObject.NULL) null else string(key)
    private fun JSONObject.boolean(key: String): Boolean = get(key).let {
        require(it is Boolean) { "Invalid boolean field: $key" }; it
    }
}
