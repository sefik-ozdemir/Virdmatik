package com.virdmatik.di

import android.content.Context
import androidx.room.Room
import com.virdmatik.data.local.DailyRecordDao
import com.virdmatik.data.local.VirdDatabase
import com.virdmatik.data.local.ZikirDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): VirdDatabase =
        Room.databaseBuilder(context, VirdDatabase::class.java, "virdmatik.db").build()

    @Provides fun zikirDao(db: VirdDatabase): ZikirDao = db.zikirDao()
    @Provides fun dailyDao(db: VirdDatabase): DailyRecordDao = db.dailyRecordDao()
}
