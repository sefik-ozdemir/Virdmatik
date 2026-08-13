package com.virdmatik.data.repository

import androidx.room.withTransaction
import com.virdmatik.data.local.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VirdRepository @Inject constructor(
    private val db: VirdDatabase,
    private val zikirDao: ZikirDao,
    private val dailyDao: DailyRecordDao
) {
    suspend fun initialize() {
        db.withTransaction {
            if (zikirDao.countAll() == 0) {
                zikirDao.insertAll(listOf(
                    ZikirEntity(name = "Estağfirullah", target = 100, sortOrder = 0),
                    ZikirEntity(name = "Bismillahirrahmanirrahim", target = 300, sortOrder = 1),
                    ZikirEntity(name = "Elhamdülillah", target = 300, sortOrder = 2),
                    ZikirEntity(name = "Salavat", target = 300, sortOrder = 3),
                    ZikirEntity(name = "La ilahe illallah", target = 300, sortOrder = 4)
                ))
            }
        }
        val today = LocalDate.now()
        val max = dailyDao.maxDate()?.let(LocalDate::parse)
        if (max == null) ensureDay(today.toString())
        else {
            var d = max.plusDays(1)
            while (!d.isAfter(today)) {
                ensureDay(d.toString())
                d = d.plusDays(1)
            }
            ensureDay(today.toString())
        }
    }

    suspend fun ensureDay(date: String) = db.withTransaction {
        val zikirs = zikirDao.getActive()
        dailyDao.insertAll(zikirs.map {
            DailyRecordEntity(date = date, zikirId = it.id, targetSnapshot = it.target)
        })
    }

    fun observeDay(date: String): Flow<List<ZikirProgressRow>> = dailyDao.observeDay(date)
    fun observeRecord(date: String, id: Long) = dailyDao.observeRecord(date, id)
    fun observeHistory(from: String, to: String) = dailyDao.observeHistory(from, to)
    fun observeActive() = zikirDao.observeActive()
    suspend fun getZikir(id: Long) = zikirDao.getById(id)
    suspend fun increment(date: String, id: Long) = dailyDao.increment(date, id)
    suspend fun decrement(date: String, id: Long) = dailyDao.decrement(date, id)

    suspend fun addZikir(name: String, target: Int) {
        val id = zikirDao.insert(ZikirEntity(name = name.trim(), target = target, sortOrder = Int.MAX_VALUE))
        ensureDay(LocalDate.now().toString())
    }

    suspend fun updateZikir(id: Long, name: String, target: Int) {
        zikirDao.update(id, name.trim(), target)
        dailyDao.updateTodayTarget(LocalDate.now().toString(), id, target)
    }

    suspend fun archiveZikir(id: Long) = zikirDao.archive(id)
}
