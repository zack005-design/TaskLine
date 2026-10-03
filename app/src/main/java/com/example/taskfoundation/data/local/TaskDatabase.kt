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
import com.example.taskfoundation.data.local.entity.LibraryItem
import com.example.taskfoundation.data.local.dao.LibraryDao

@Database(
    entities = [
        TaskEntity::class,
        SubtaskEntity::class,
        ProjectEntity::class,
        TagEntity::class,
        TaskTagCrossRef::class,
        LibraryItem::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun backupDao(): BackupDao
    abstract fun taskDao(): TaskDao

    abstract fun projectDao(): ProjectDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN durationMinutes INTEGER")
                db.execSQL("ALTER TABLE tasks ADD COLUMN deadline INTEGER")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS library_items (id TEXT NOT NULL PRIMARY KEY, kind TEXT NOT NULL, title TEXT NOT NULL, payload TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
            }
        }
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { instance = it }
            }
    }
}
