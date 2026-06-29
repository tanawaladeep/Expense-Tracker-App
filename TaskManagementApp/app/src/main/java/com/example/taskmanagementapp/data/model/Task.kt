package com.example.taskmanagementapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val dueDate: Long, // timestamp
    val priority: String, // "LOW", "MEDIUM", "HIGH"
    val status: String, // "PENDING", "IN_PROGRESS", "COMPLETED"
    val category: String, // "WORK", "PERSONAL", "SHOPPING", "HEALTH", "STUDY", "OTHER"
    val createdAt: Long = System.currentTimeMillis()
)
