package com.miui.timesnote.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.miui.timesnote.data.ContextProvider

/**
 * 触感反馈工具
 * 已移除打卡成功震动和音效，仅保留成就震动
 */
object HapticUtil {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = ContextProvider.context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ContextProvider.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun init() {
        // 无需初始化
    }

    /**
     * 连续打卡成就震动 - 100天等特殊成就触发
     */
    fun vibrateAchievement() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val timings = longArrayOf(0, 50, 50, 50, 50, 100)
            val amplitudes = intArrayOf(0, 128, 0, 128, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 50, 50, 50, 50, 100), -1)
        }
    }

    /**
     * 7天/30天连续成就震动
     */
    fun vibrateStreakAchievement() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val timings = longArrayOf(0, 30, 30, 60)
            val amplitudes = intArrayOf(0, 100, 0, 200)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 30, 30, 60), -1)
        }
    }

    fun release() {
        // 无需释放
    }
}
