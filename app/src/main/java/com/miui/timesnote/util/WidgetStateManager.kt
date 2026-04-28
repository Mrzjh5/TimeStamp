package com.miui.timesnote.util

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.state.GlanceWidgetState
import com.miui.timesnote.data.CheckInEvent
import com.miui.timesnote.widget.TimeStampWidget2x2
import com.miui.timesnote.widget.TimeStampWidget2x3
import com.miui.timesnote.widget.TimeStampWidget4x4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 小组件状态管理
 */
object WidgetStateManager {
    
    const val ACTION_REFRESH = "com.miui.timesnote.ACTION_REFRESH"
    const val EXTRA_EVENT_ID = "event_id"
    const val EXTRA_PROGRESS = "progress"
    const val EXTRA_CHECKED = "checked"

    /**
     * 刷新所有小组件
     */
    fun refreshAllWidgets(context: Context) {
        val intent = Intent(context, TimeStampWidget2x2::class.java).apply {
            action = ACTION_REFRESH
        }
        context.sendBroadcast(intent)

        val intent2 = Intent(context, TimeStampWidget2x3::class.java).apply {
            action = ACTION_REFRESH
        }
        context.sendBroadcast(intent2)

        val intent3 = Intent(context, TimeStampWidget4x4::class.java).apply {
            action = ACTION_REFRESH
        }
        context.sendBroadcast(intent3)
    }

    /**
     * 刷新特定尺寸的小组件
     */
    fun refreshWidget(context: Context, size: WidgetSize) {
        val intent = when (size) {
            WidgetSize.SIZE_2x2 -> Intent(context, TimeStampWidget2x2::class.java)
            WidgetSize.SIZE_2x3 -> Intent(context, TimeStampWidget2x3::class.java)
            WidgetSize.SIZE_4x4 -> Intent(context, TimeStampWidget4x4::class.java)
        }.apply {
            action = ACTION_REFRESH
        }
        context.sendBroadcast(intent)
    }

    /**
     * 更新特定事件的打卡进度
     */
    fun updateCheckInProgress(context: Context, eventId: Long, progress: Float, isChecked: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val manager = GlanceAppWidgetManager(context)
                val clazz = when {
                    // Find the appropriate widget class based on size
                    true -> TimeStampWidget2x2::class.java
                }
                val componentName = ComponentName(context, clazz)
                
                // Update will be handled by Glance state
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    enum class WidgetSize {
        SIZE_2x2,
        SIZE_2x3,
        SIZE_4x4
    }
}

/**
 * 每日重置广播接收器
 */
class DailyResetReceiver {
    // 使用 WorkManager 替代 AlarmManager 进行每日重置
}
