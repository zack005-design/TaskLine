package com.example.taskfoundation.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.taskfoundation.data.local.entity.SubtaskEntity
import com.example.taskfoundation.data.local.entity.TagEntity
import com.example.taskfoundation.data.local.entity.TaskEntity
import com.example.taskfoundation.data.local.entity.TaskTagCrossRef
import kotlinx.coroutines.flow.Flow
import com.example.taskfoundation.data.mapper.toDomain
import com.example.taskfoundation.data.mapper.toEntity
import com.example.taskfoundation.domain.time.TaskSchedule

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks")
    suspend fun all(): List<TaskEntity>

    @Query("SELECT * FROM subtasks")
    suspend fun allSubtasks(): List<SubtaskEntity>

    @Query("SELECT * FROM tags")
    suspend fun allTags(): List<TagEntity>

    @Query("SELECT * FROM task_tags")
    suspend fun allLinks(): List<TaskTagCrossRef>

    @Query("SELECT id FROM tasks WHERE importKey = :key LIMIT 1")
    suspend fun importedId(key: String): Long?

    @Query("UPDATE subtasks SET isCompleted = 0, updatedAt = :now WHERE taskId = :id")
    suspend fun resetSubtasks(id: Long, now: Long)

    @Query("UPDATE tasks SET lastNotifiedAt = :at WHERE id = :id")
    suspend fun markNotified(id: Long, at: Long)

    @Query("UPDATE tasks SET snoozedUntil = :until, updatedAt = :now WHERE id = :id AND isCompleted = 0")
    suspend fun snooze(id: Long, until: Long, now: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM subtasks WHERE taskId = :id")
    suspend fun lastSubtaskOrder(id: Long): Int

    @Transaction
    suspend fun saveWithSubtasks(task: TaskEntity, titles: List<String>, now: Long): Long {
        val id = saveScheduled(task, now)
        val firstOrder = lastSubtaskOrder(id) + 1
        titles.forEachIndexed { index, title ->
            require(title.isNotBlank())
            upsertSubtask(SubtaskEntity(taskId = id, title = title, isCompleted = false, sortOrder = firstOrder + index,
                createdAt = now, updatedAt = now))
        }
        return id
    }

    @Transaction
    suspend fun saveScheduled(task: TaskEntity, now: Long): Long {
        val old = if (task.id == 0L) null else checkNotNull(get(task.id)) { "Task no longer exists" }
        val scheduleChanged = old == null || old.dueDateTime != task.dueDateTime ||
            old.dueTimeMinutes != task.dueTimeMinutes || old.reminderMinutes != task.reminderMinutes ||
            old.isCompleted != task.isCompleted
        var current = task.copy(
            repeatAnchor = if (old?.repeatRule != task.repeatRule || old.repeatInterval != task.repeatInterval || old.dueDateTime != task.dueDateTime)
                task.dueDateTime else old.repeatAnchor,
            snoozedUntil = if (scheduleChanged) null else old.snoozedUntil,
            lastNotifiedAt = if (scheduleChanged) null else old.lastNotifiedAt,
        )
        if (current.isCompleted && old?.isCompleted != true && current.repeatRule != "NONE") {
            current = TaskSchedule.next(current.toDomain(), now).toEntity()
            if (old != null) resetSubtasks(old.id, now)
        }
        val id = upsert(current)
        return if (task.id == 0L) id else task.id
    }

    @Transaction
    suspend fun completeScheduled(id: Long, completed: Boolean, now: Long): Boolean {
        val task = get(id) ?: return false
        if (completed && !task.isCompleted && task.repeatRule != "NONE") {
            upsert(TaskSchedule.next(task.toDomain(), now).toEntity())
            resetSubtasks(id, now)
        } else setCompleted(id, completed, now)
        return true
    }
    @Query(
        """
        SELECT * FROM tasks
        ORDER BY isCompleted ASC,
                 CASE WHEN dueDateTime IS NULL THEN 1 ELSE 0 END,
                 dueDateTime ASC,
                 updatedAt DESC
        """,
    )
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeById(taskId: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY isCompleted ASC, updatedAt DESC")
    fun observeByProject(projectId: Long): Flow<List<TaskEntity>>

    @Upsert
    suspend fun upsert(task: TaskEntity): Long

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: Long): Int

    @Query(
        """
        UPDATE tasks
        SET isCompleted = :isCompleted,
            status = CASE WHEN :isCompleted THEN 'DONE' WHEN status = 'DONE' THEN 'TODO' ELSE status END,
            progress = CASE WHEN :isCompleted THEN 100 WHEN progress = 100 THEN 0 ELSE progress END,
            updatedAt = :updatedAt,
            snoozedUntil = NULL,
            lastNotifiedAt = CASE WHEN isCompleted != :isCompleted THEN NULL ELSE lastNotifiedAt END
        WHERE id = :taskId
        """,
    )
    suspend fun setCompleted(taskId: Long, isCompleted: Boolean, updatedAt: Long): Int

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC, id ASC")
    fun observeSubtasks(taskId: Long): Flow<List<SubtaskEntity>>

    @Upsert
    suspend fun upsertSubtask(subtask: SubtaskEntity): Long

    @Query("UPDATE subtasks SET title = :title, isCompleted = :completed, sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id AND taskId = :taskId")
    suspend fun updateSubtask(id: Long, taskId: Long, title: String, completed: Boolean, sortOrder: Int, updatedAt: Long): Int

    @Query("DELETE FROM subtasks WHERE id = :subtaskId")
    suspend fun deleteSubtask(subtaskId: Long): Int

    @Query(
        """
        SELECT tags.* FROM tags
        INNER JOIN task_tags ON tags.id = task_tags.tagId
        WHERE task_tags.taskId = :taskId
        ORDER BY tags.name COLLATE NOCASE ASC
        """,
    )
    fun observeTags(taskId: Long): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findTagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskTag(crossRef: TaskTagCrossRef)

    @Query("DELETE FROM task_tags WHERE taskId = :taskId AND tagId = :tagId")
    suspend fun removeTaskTag(taskId: Long, tagId: Long): Int

    @Transaction
    suspend fun createOrAttachTag(taskId: Long, tag: TagEntity): Long {
        findTagByName(tag.name)?.let { existing ->
            insertTaskTag(TaskTagCrossRef(taskId = taskId, tagId = existing.id))
            return existing.id
        }
        val insertedId = insertTag(tag)
        val tagId = if (insertedId == -1L) {
            checkNotNull(findTagByName(tag.name)) { "Tag conflict without an existing row" }.id
        } else {
            insertedId
        }
        insertTaskTag(TaskTagCrossRef(taskId = taskId, tagId = tagId))
        return tagId
    }
}
