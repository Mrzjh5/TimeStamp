package com.miui.timesnote.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 小组件点击事件接收器
 */
class WidgetClickReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CHECK_IN -> {
                val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1)
                if (eventId != -1L) {
                    handleCheckIn(context, eventId)
                }
            }
        }
    }
    
    private fun handleCheckIn(context: Context, eventId: Long) {
        // 处理打卡逻辑后刷新组件
        try {
            TimeStampWidget2x2().updateAll(context)
            TimeStampWidget2x3().updateAll(context)
            TimeStampWidget4x4().updateAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    companion object {
        const val ACTION_CHECK_IN = "com.miui.timesnote.ACTION_CHECK_IN"
        const val EXTRA_EVENT_ID = "event_id"
    }
}

/**
 * 小组件状态定义
 */
object GlanceStateDefinition {
    // 简化版本，不使用复杂状态管理
}