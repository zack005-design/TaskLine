package com.example.taskfoundation.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.taskfoundation.data.local.dao.BackupDao
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.taskfoundation.domain.time.CalendarDates
import java.time.ZoneId
import com.example.taskfoundation.data.local.dao.ProjectDao
import com.example.taskfoundation.data.local.dao.TaskDao
import com.example.taskfoundation.data.local.entity.ProjectEntity
import com.example.taskfoundation.data.local.entity.SubtaskEntity
import com.example.taskfoundation.data.local.entity.TagEntity
import com.example.taskfoundation.data.local.entity.TaskEntity
import com.example.taskfoundation.data.local.entity.TaskTagCrossRef

@Database(
    entities = [
        TaskEntity::class,
        SubtaskEntity::class,
        ProjectEntity::class,
        TagEntity::class,
        TaskTagCrossRef::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun backupDao(): BackupDao
    abstract fun taskDao(): TaskDao

    abstract fun projectDao(): ProjectDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("dueTimeMinutes", "reminderMinutes", "repeatAnchor", "snoozedUntil", "lastNotifiedAt").forEach {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN $it INTEGER")
                }
                db.execSQL("ALTER TABLE tasks ADD COLUMN repeatRule TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE tasks ADD COLUMN repeatInterval INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE tasks ADD COLUMN importKey TEXT")
                db.execSQL("CREATE UNIQUE INDEX index_tasks_importKey ON tasks(importKey)")
            }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val zone = ZoneId.systemDefault()
                db.query("SELECT id, startDateTime, dueDateTime FROM tasks").use { cursor ->
                    while (cursor.moveToNext()) {
                        val start = if (cursor.isNull(1)) null else CalendarDates.migrateLegacy(cursor.getLong(1), zone)
                        val due = if (cursor.isNull(2)) null else CalendarDates.migrateLegacy(cursor.getLong(2), zone)
                        db.execSQL("UPDATE tasks SET startDateTime = ?, dueDateTime = ? WHERE id = ?",
                            arrayOf(start, due, cursor.getLong(0)))
                    }
                }
            }
        }

        @Volatile
        private var instance: TaskDatabase? = null

        fun getInstance(context: Context): TaskDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "task-foundation.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}
