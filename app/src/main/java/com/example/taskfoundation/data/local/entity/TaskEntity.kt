package com.example.taskfoundation.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("projectId"),
        Index("dueDateTime"),
        Index("status"),
        Index("isCompleted"),
        Index("updatedAt"),
    ],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val projectId: Long?,
    val startDateTime: Long?,
    val dueDateTime: Long?,
    val priority: String,
    val status: String,
    val progress: Int,
    val isCompleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
