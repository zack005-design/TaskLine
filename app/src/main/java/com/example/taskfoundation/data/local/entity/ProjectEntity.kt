package com.example.taskfoundation.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    indices = [Index(value = ["name"], unique = true), Index("updatedAt")],
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val color: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)
