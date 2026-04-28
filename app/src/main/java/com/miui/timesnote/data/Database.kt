package com.miui.timesnote.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 打卡事件实体
 */
@Entity(tableName = "check_in_events")
data class CheckInEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                    // 事件名称（8字以内）
    val iconType: Int,                   // 图标类型 0-7
    val colorIndex: Int,                 // 颜色索引 0-11
    val createdAt: Long = System.currentTimeMillis(),
    val lastCheckInTime: Long = 0,       // 上次打卡时间
    val consecutiveDays: Int = 0,         // 连续打卡天数
    val totalCheckIns: Int = 0,          // 总打卡次数
    val isBroken: Boolean = false,       // 是否已中断
    val sortOrder: Int = 0,              // 排序顺序
    val frequency: Int = 0               // 使用频率
)

/**
 * 打卡记录历史
 */
@Entity(tableName = "check_in_records")
data class CheckInRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventId: Long,
    val checkInTime: Long,
    val date: String,                    // 日期格式 yyyy-MM-dd
    val consecutiveDays: Int             // 打卡时的连续天数
)

/**
 * 事件Dao
 */
@Dao
interface CheckInEventDao {
    @Query("SELECT * FROM check_in_events ORDER BY sortOrder ASC")
    fun getAllEvents(): Flow<List<CheckInEvent>>

    @Query("SELECT * FROM check_in_events ORDER BY frequency DESC LIMIT :limit")
    fun getTopEvents(limit: Int): Flow<List<CheckInEvent>>

    @Query("SELECT * FROM check_in_events WHERE id = :id")
    suspend fun getEventById(id: Long): CheckInEvent?

    @Query("SELECT * FROM check_in_events WHERE isBroken = 0 ORDER BY consecutiveDays DESC LIMIT :limit")
    fun getActiveEvents(limit: Int): Flow<List<CheckInEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CheckInEvent): Long

    @Update
    suspend fun updateEvent(event: CheckInEvent)

    @Delete
    suspend fun deleteEvent(event: CheckInEvent)

    @Query("DELETE FROM check_in_events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: Long)

    @Query("""
        UPDATE check_in_events 
        SET lastCheckInTime = :time, 
            consecutiveDays = :days, 
            totalCheckIns = totalCheckIns + 1, 
            isBroken = 0 
        WHERE id = :eventId
    """)
    suspend fun updateCheckIn(eventId: Long, time: Long, days: Int)

    @Query("UPDATE check_in_events SET isBroken = 1, consecutiveDays = 0 WHERE id = :eventId")
    suspend fun markAsBroken(eventId: Long)

    @Query("UPDATE check_in_events SET frequency = frequency + 1 WHERE id = :eventId")
    suspend fun incrementFrequency(eventId: Long)

    @Query("UPDATE check_in_events SET sortOrder = :order WHERE id = :eventId")
    suspend fun updateSortOrder(eventId: Long, order: Int)

    @Query("UPDATE check_in_events SET sortOrder = :order WHERE id = :eventId")
    suspend fun reorderEvents(eventId: Long, order: Int)

    @Query("SELECT COUNT(*) FROM check_in_events")
    suspend fun getEventCount(): Int
}

/**
 * 记录Dao
 */
@Dao
interface CheckInRecordDao {
    @Query("SELECT * FROM check_in_records WHERE eventId = :eventId ORDER BY checkInTime DESC")
    fun getRecordsByEvent(eventId: Long): Flow<List<CheckInRecord>>

    @Query("SELECT * FROM check_in_records WHERE date = :date")
    fun getRecordsByDate(date: String): Flow<List<CheckInRecord>>

    @Query("SELECT * FROM check_in_records WHERE eventId = :eventId AND date BETWEEN :startDate AND :endDate ORDER BY checkInTime DESC")
    fun getRecordsInRange(eventId: Long, startDate: String, endDate: String): Flow<List<CheckInRecord>>

    @Query("SELECT * FROM check_in_records ORDER BY checkInTime DESC LIMIT 365")
    fun getRecent365Records(): Flow<List<CheckInRecord>>

    @Query("SELECT * FROM check_in_records WHERE eventId = :eventId AND date = :date LIMIT 1")
    suspend fun getRecordByEventAndDate(eventId: Long, date: String): CheckInRecord?

    @Insert
    suspend fun insertRecord(record: CheckInRecord): Long

    @Query("DELETE FROM check_in_records WHERE eventId = :eventId")
    suspend fun deleteRecordsByEvent(eventId: Long)

    @Query("SELECT COUNT(*) FROM check_in_records WHERE eventId = :eventId AND date = :date")
    suspend fun getCheckInCountForDate(eventId: Long, date: String): Int
}

@Database(
    entities = [CheckInEvent::class, CheckInRecord::class],
    version = 1,
    exportSchema = false
)
abstract class TimeNoteDatabase : RoomDatabase() {
    abstract fun checkInEventDao(): CheckInEventDao
    abstract fun checkInRecordDao(): CheckInRecordDao

    companion object {
        @Volatile
        private var INSTANCE: TimeNoteDatabase? = null

        fun getInstance(context: Context): TimeNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TimeNoteDatabase::class.java,
                    "times_note_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
