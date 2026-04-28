package com.miui.timesnote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager
import com.miui.timesnote.data.ContextProvider
import com.miui.timesnote.util.DailyResetWorker

class TimesNoteApp : Application(), Configuration.Provider {

    companion object {
        const val CHANNEL_REMINDER = "reminder_channel"
        lateinit var instance: TimesNoteApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 初始化全局Context
        ContextProvider.context = this.applicationContext
        
        // 初始化触感反馈
        com.miui.timesnote.util.HapticUtil.init()
        
        // 创建通知渠道
        createNotificationChannels()
        
        // 调度每日重置任务
        DailyResetWorker.schedule(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_REMINDER,
                "打卡提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "习惯打卡提醒通知"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
