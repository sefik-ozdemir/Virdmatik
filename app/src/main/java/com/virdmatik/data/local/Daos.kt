package com.virdmatik.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ZikirDao {
    @Query("SELECT * FROM zikirs WHERE isActive = 1 ORDER BY sortOrder, id")
    fun observeActive(): Flow<List<ZikirEntity>>

    @Query("SELECT * FROM zikirs WHERE isActive = 1 ORDER BY sortOrder, id")
    suspend fun getActive(): List<ZikirEntity>

    @Query("SELECT * FROM zikirs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ZikirEntity?

    @Query("SELECT COUNT(*) FROM zikirs")
    suspend fun countAll(): Int

    @Insert
    suspend fun insert(item: ZikirEntity): Long

    @Insert
    suspend fun insertAll(items: List<ZikirEntity>)

    @Query("UPDATE zikirs SET name = :name, target = :target WHERE id = :id")
    suspend fun update(id: Long, name: String, target: Int)

    @Query("UPDATE zikirs SET isActive = 0 WHERE id = :id")
    suspend fun archive(id: Long)
}

@Dao
interface DailyRecordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<DailyRecordEntity>)

    @Query("SELECT MAX(date) FROM daily_records")
    suspend fun maxDate(): String?

    @Query("""
        SELECT z.id AS id, z.name AS name, d.count AS count,
               d.targetSnapshot AS target, d.isCompleted AS completed
        FROM daily_records d
        JOIN zikirs z ON z.id = d.zikirId
        WHERE d.date = :date
        ORDER BY z.sortOrder, z.id
    """)
    fun observeDay(date: String): Flow<List<ZikirProgressRow>>

    @Query("SELECT * FROM daily_records WHERE date = :date AND zikirId = :zikirId LIMIT 1")
    fun observeRecord(date: String, zikirId: Long): Flow<DailyRecordEntity?>

    @Query("""
        UPDATE daily_records SET
        count = CASE WHEN count < targetSnapshot THEN count + 1 ELSE count END,
        isCompleted = CASE WHEN count + 1 >= targetSnapshot THEN 1 ELSE isCompleted END,
        updatedAt = :now
        WHERE date = :date AND zikirId = :zikirId
    """)
    suspend fun increment(date: String, zikirId: Long, now: Long = System.currentTimeMillis())

    @Query("""
        UPDATE daily_records SET
        count = CASE WHEN count > 0 THEN count - 1 ELSE 0 END,
        isCompleted = 0,
        updatedAt = :now
        WHERE date = :date AND zikirId = :zikirId
    """)
    suspend fun decrement(date: String, zikirId: Long, now: Long = System.currentTimeMillis())

    @Query("""
        SELECT date,
               SUM(count) AS totalCount,
               SUM(targetSnapshot) AS totalTarget,
               SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) AS completedCount,
               COUNT(*) AS itemCount
        FROM daily_records
        WHERE date BETWEEN :fromDate AND :toDate
        GROUP BY date
        ORDER BY date DESC
    """)
    fun observeHistory(fromDate: String, toDate: String): Flow<List<DayProgressRow>>

    @Query("UPDATE daily_records SET targetSnapshot = :target, isCompleted = CASE WHEN count >= :target THEN 1 ELSE 0 END WHERE date = :date AND zikirId = :zikirId")
    suspend fun updateTodayTarget(date: String, zikirId: Long, target: Int)
}
