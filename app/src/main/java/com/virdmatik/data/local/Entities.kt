package com.virdmatik.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "zikirs")
data class ZikirEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Int,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "daily_records",
    primaryKeys = ["date", "zikirId"],
    indices = [Index("date"), Index("zikirId")]
)
data class DailyRecordEntity(
    val date: String,
    val zikirId: Long,
    val count: Int = 0,
    val targetSnapshot: Int,
    val isCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

data class DayProgressRow(
    val date: String,
    val totalCount: Int,
    val totalTarget: Int,
    val completedCount: Int,
    val itemCount: Int
)

data class ZikirProgressRow(
    val id: Long,
    val name: String,
    val count: Int,
    val target: Int,
    val completed: Boolean
)
