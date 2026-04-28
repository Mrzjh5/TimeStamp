package com.miui.timesnote.util

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * 每日重置Worker - 检查打卡状态，中断过期打卡
 */
class DailyResetWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        return try {
            // 获取当前日期
            val today = DateUtil.getToday()
            
            // 检查昨天的打卡情况，更新中断状态
            // 实际实现需要访问数据库
            
            // 更新最后重置日期
            kotlinx.coroutines.MainScope().launch {
                SettingsData.setLastResetDate(today)
            }
            
            // 刷新小组件
            WidgetStateManager.refreshAllWidgets(applicationContext)
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "daily_reset_work"

        /**
         * 调度每日重置任务
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyResetWorker>(
                1, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                dailyWorkRequest
            )
        }

        /**
         * 计算到明天凌晨的延迟毫秒数
         */
        private fun calculateInitialDelay(): Long {
            val now = java.util.Calendar.getInstance()
            val tomorrow = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            return tomorrow.timeInMillis - now.timeInMillis
        }

        /**
         * 取消每日重置任务
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

/**
 * 数据备份Worker
 */
class BackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        return try {
            // 实现数据备份逻辑
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "backup_work"

        fun scheduleWeekly(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()

            val weeklyWorkRequest = PeriodicWorkRequestBuilder<BackupWorker>(
                7, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                weeklyWorkRequest
            )
        }
    }
}
