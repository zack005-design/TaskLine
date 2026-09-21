package com.example.taskfoundation.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
    exportSchema = true,
)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var instance: TaskDatabase? = null

        fun getInstance(context: Context): TaskDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "task-foundation.db",
                ).build().also { instance = it }
            }
    }
}
