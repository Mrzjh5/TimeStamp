package com.miui.timesnote.util

/**
 * 图标类型定义 - 8种预设图标
 */
object IconTypes {
    const val READING = 0      // 读书
    const val SPORTS = 1       // 运动
    const val WAKE_UP = 2      // 早起
    const val WATER = 3        // 喝水
    const val SLEEP = 4        // 早睡
    const val STUDY = 5        // 学习
    const val MEDITATION = 6   // 冥想
    const val HEALTH = 7       // 健康

    val allIcons = listOf(
        READING, SPORTS, WAKE_UP, WATER, SLEEP, STUDY, MEDITATION, HEALTH
    )

    fun getIconName(type: Int): String = when (type) {
        READING -> "读书"
        SPORTS -> "运动"
        WAKE_UP -> "早起"
        WATER -> "喝水"
        SLEEP -> "早睡"
        STUDY -> "学习"
        MEDITATION -> "冥想"
        HEALTH -> "健康"
        else -> "习惯"
    }

    fun getIconEmoji(type: Int): String = when (type) {
        READING -> "📖"
        SPORTS -> "🏃"
        WAKE_UP -> "🌅"
        WATER -> "💧"
        SLEEP -> "🌙"
        STUDY -> "📚"
        MEDITATION -> "🧘"
        HEALTH -> "❤️"
        else -> "✓"
    }
}
