package com.miui.timesnote.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * 打卡数据仓库 - 统一管理数据库操作
 */
class CheckInRepository private constructor(context: Context) {

    private val database = TimeNoteDatabase.getInstance(context)
    private val eventDao = database.checkInEventDao()
    private val recordDao = database.checkInRecordDao()

    // ==================== 事件操作 ====================

    val allEvents: Flow<List<CheckInEvent>> = eventDao.getAllEvents()

    fun getTopEvents(limit: Int = 5): Flow<List<CheckInEvent>> = eventDao.getTopEvents(limit)

    fun getActiveEvents(limit: Int = 5): Flow<List<CheckInEvent>> = eventDao.getActiveEvents(limit)

    suspend fun getEventById(id: Long): CheckInEvent? = eventDao.getEventById(id)

    suspend fun insertEvent(event: CheckInEvent): Long = eventDao.insertEvent(event)

    suspend fun updateEvent(event: CheckInEvent) = eventDao.updateEvent(event)

    suspend fun deleteEvent(event: CheckInEvent) {
        // 同时删除关联的记录
        recordDao.deleteRecordsByEvent(event.id)
        eventDao.deleteEvent(event)
    }

    suspend fun deleteEventById(eventId: Long) {
        recordDao.deleteRecordsByEvent(eventId)
        eventDao.deleteEventById(eventId)
    }

    // ==================== 打卡操作 ====================

    /**
     * 执行打卡
     */
    suspend fun checkIn(event: CheckInEvent): CheckInResult {
        val now = System.currentTimeMillis()
        val today = DateUtil.getToday()

        // 检查今天是否已打卡
        val existingRecord = recordDao.getRecordByEventAndDate(event.id, today)
        if (existingRecord != null) {
            return CheckInResult.AlreadyCheckedIn
        }

        // 计算新的连续天数
        val newConsecutiveDays = calculateNewConsecutiveDays(event, now)

        // 更新事件
        eventDao.updateCheckIn(event.id, now, newConsecutiveDays)
        eventDao.incrementFrequency(event.id)

        // 插入打卡记录
        val record = CheckInRecord(
            eventId = event.id,
            checkInTime = now,
            date = today,
            consecutiveDays = newConsecutiveDays
        )
        recordDao.insertRecord(record)

        // 检查成就
        val achievement = checkAchievement(newConsecutiveDays)

        return CheckInResult.Success(
            consecutiveDays = newConsecutiveDays,
            achievement = achievement
        )
    }

    /**
     * 计算新的连续天数
     */
    private fun calculateNewConsecutiveDays(event: CheckInEvent, currentTime: Long): Int {
        if (event.lastCheckInTime == 0L) {
            return 1
        }

        return when {
            DateUtil.isToday(event.lastCheckInTime) -> event.consecutiveDays
            DateUtil.isYesterday(event.lastCheckInTime) -> event.consecutiveDays + 1
            else -> 1  // 中断了，重新开始
        }
    }

    /**
     * 检查成就
     */
    private fun checkAchievement(consecutiveDays: Int): Achievement? {
        return when (consecutiveDays) {
            7 -> Achievement.STREAK_7
            30 -> Achievement.STREAK_30
            100 -> Achievement.STREAK_100
            else -> null
        }
    }

    /**
     * 批量打卡（用于补卡等场景）
     */
    suspend fun batchCheckIn(events: List<CheckInEvent>) {
        events.forEach { event ->
            checkIn(event)
        }
    }

    // ==================== 记录查询 ====================

    fun getRecordsByEvent(eventId: Long): Flow<List<CheckInRecord>> =
        recordDao.getRecordsByEvent(eventId)

    fun getRecordsByDate(date: String): Flow<List<CheckInRecord>> =
        recordDao.getRecordsByDate(date)

    fun getRecentRecords(): Flow<List<CheckInRecord>> =
        recordDao.getRecent365Records()

    fun getRecordsInRange(eventId: Long, startDate: String, endDate: String): Flow<List<CheckInRecord>> =
        recordDao.getRecordsInRange(eventId, startDate, endDate)

    suspend fun hasCheckedInToday(eventId: Long): Boolean {
        val today = DateUtil.getToday()
        return recordDao.getCheckInCountForDate(eventId, today) > 0
    }

    // ==================== 统计 ====================

    suspend fun getTotalCheckIns(): Int {
        var total = 0
        allEvents.collect { events ->
            total = events.sumOf { it.totalCheckIns }
        }
        return total
    }

    suspend fun getBestStreak(): Int {
        var best = 0
        allEvents.collect { events ->
            best = events.maxOfOrNull { it.consecutiveDays } ?: 0
        }
        return best
    }

    suspend fun getTodayCheckInCount(): Int {
        val today = DateUtil.getToday()
        var count = 0
        allEvents.collect { events ->
            count = events.count { DateUtil.isToday(it.lastCheckInTime) }
        }
        return count
    }

    // ==================== 每日重置 ====================

    /**
     * 检查并更新中断状态
     */
    suspend fun checkAndUpdateBrokenStatus() {
        val today = DateUtil.getToday()
        allEvents.collect { events ->
            events.forEach { event ->
                if (!event.isBroken && event.lastCheckInTime > 0) {
                    // 检查是否昨天打卡了
                    if (!DateUtil.isToday(event.lastCheckInTime) && !DateUtil.isYesterday(event.lastCheckInTime)) {
                        eventDao.markAsBroken(event.id)
                    }
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: CheckInRepository? = null

        fun getInstance(context: Context): CheckInRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = CheckInRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * 打卡结果
 */
sealed class CheckInResult {
    data class Success(
        val consecutiveDays: Int,
        val achievement: Achievement? = null
    ) : CheckInResult()
    
    data object AlreadyCheckedIn : CheckInResult()
    
    data object Error : CheckInResult()
}

/**
 * 成就类型
 */
enum class Achievement {
    STREAK_7,    // 坚持者
    STREAK_30,   // 习惯大师
    STREAK_100   // 传奇坚持者
}
