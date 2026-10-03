package com.example.taskfoundation.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONObject
import java.time.LocalDate

/** Portable local records. Payloads are validated on writes and before restore. */
@Entity(tableName = "library_items")
data class LibraryItem(
    @PrimaryKey val id: String,
    val kind: String,
    val title: String,
    val payload: String,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun validate() {
        require(id.isNotBlank() && title.isNotBlank()) { "A name is required" }
        require(kind in KINDS || kind in listOf("Comment", "Attachment")) { "Unknown library item" }
        val data = JSONObject(payload)
        when (kind) {
            "Habit" -> {
                val goal = data.getInt("goal")
                require(goal in 1..1000) { "Daily target must be 1–1000" }
                val days = data.getJSONObject("days")
                days.keys().forEach { day ->
                    LocalDate.parse(day)
                    require(days.getInt(day) in 0..goal) { "Invalid habit count" }
                }
            }
            "Countdown" -> LocalDate.parse(data.getString("date"))
            "Note" -> data.getString("text")
            "Comment" -> { require(data.getLong("taskId") > 0); require(data.getString("text").isNotBlank()) }
            "Attachment" -> {
                require(data.getLong("taskId") > 0)
                require(data.getString("name").isNotBlank())
                require(data.getString("mime").contains('/'))
                require(java.util.Base64.getDecoder().decode(data.getString("bytes")).size <= 2 * 1024 * 1024) { "Attachment exceeds 2 MB" }
            }
            "Activity" -> { require(data.getLong("taskId") > 0); require(data.getString("action") in listOf("Created", "Updated", "Completed", "Reopened", "Deleted")) }
            "Template" -> {
                val tasks = data.getJSONArray("tasks")
                require(tasks.length() > 0) { "Add at least one task" }
                repeat(tasks.length()) { require(tasks.getString(it).isNotBlank()) }
            }
            "Filter" -> {
                data.getString("query")
                require(data.getString("priority") in listOf("Any", "LOW", "MEDIUM", "HIGH", "URGENT"))
                require(data.getString("status") in listOf("Any", "TODO", "IN_PROGRESS", "BLOCKED", "DONE"))
            }
        }
    }

    companion object { val KINDS = listOf("Habit", "Note", "Countdown", "Template", "Filter", "Activity") }
}
