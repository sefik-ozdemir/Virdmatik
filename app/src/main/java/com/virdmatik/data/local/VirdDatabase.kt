package com.virdmatik.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ZikirEntity::class, DailyRecordEntity::class],
    version = 1,
    exportSchema = true
)
abstract class VirdDatabase : RoomDatabase() {
    abstract fun zikirDao(): ZikirDao
    abstract fun dailyRecordDao(): DailyRecordDao
}
