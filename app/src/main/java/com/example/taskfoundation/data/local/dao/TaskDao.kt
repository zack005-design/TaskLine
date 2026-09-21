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

@Dao
interface TaskDao {
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
            updatedAt = :updatedAt
        WHERE id = :taskId
        """,
    )
    suspend fun setCompleted(taskId: Long, isCompleted: Boolean, updatedAt: Long): Int

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC, id ASC")
    fun observeSubtasks(taskId: Long): Flow<List<SubtaskEntity>>

    @Upsert
    suspend fun upsertSubtask(subtask: SubtaskEntity): Long

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
