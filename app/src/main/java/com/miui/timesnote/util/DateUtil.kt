package com.miui.timesnote.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期工具类
 */
object DateUtil {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun getToday(): String = dateFormat.format(Date())

    fun getCurrentTime(): Long = System.currentTimeMillis()

    fun formatDate(time: Long): String = dateFormat.format(Date(time))

    fun formatTime(time: Long): String = timeFormat.format(Date(time))

    fun formatDateTime(time: Long): String = dateTimeFormat.format(Date(time))

    fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isToday(time: Long): Boolean = isSameDay(time, System.currentTimeMillis())

    fun isYesterday(time: Long): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return isSameDay(time, yesterday.timeInMillis)
    }

    /**
     * 计算连续打卡天数
     */
    fun calculateConsecutiveDays(lastCheckInTime: Long, currentTime: Long): Int {
        if (lastCheckInTime == 0L) return 1

        return when {
            isToday(lastCheckInTime) -> 1  // 今天已打卡
            isYesterday(lastCheckInTime) -> 1  // 昨天打卡了，今天是连续第一天
            else -> 1  // 中断了，重新开始
        }
    }

    /**
     * 获取本周开始日期
     */
    fun getWeekStartDate(): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }
        return dateFormat.format(calendar.time)
    }

    /**
     * 获取本月开始日期
     */
    fun getMonthStartDate(): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return dateFormat.format(calendar.time)
    }

    /**
     * 获取指定天数前的日期
     */
    fun getDateBeforeDays(days: Int): String {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
        }
        return dateFormat.format(calendar.time)
    }
}
