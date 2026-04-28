package com.miui.timesnote.util

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color

/**
 * 小米主题色
 */
object MiuiColors {
    // 12种小米主题色
    val themeColors = listOf(
        AndroidColor.parseColor("#00C3FF"),  // 活力蓝
        AndroidColor.parseColor("#FF6B6B"),  // 珊瑚红
        AndroidColor.parseColor("#4CAF50"),  // 自然绿
        AndroidColor.parseColor("#FF9800"),  // 暖阳橙
        AndroidColor.parseColor("#9C27B0"),  // 浪漫紫
        AndroidColor.parseColor("#00BCD4"),  // 清新青
        AndroidColor.parseColor("#E91E63"),  // 时尚粉
        AndroidColor.parseColor("#3F51B5"),  // 智慧蓝
        AndroidColor.parseColor("#795548"),  // 大地棕
        AndroidColor.parseColor("#607D8B"),  // 静谧灰
        AndroidColor.parseColor("#FFEB3B"),  // 阳光黄
        AndroidColor.parseColor("#8BC34A")   // 翠绿
    )

    // Compose Color 版本
    val themeColorList = listOf(
        Color(0xFF00C3FF),
        Color(0xFFFF6B6B),
        Color(0xFF4CAF50),
        Color(0xFFFF9800),
        Color(0xFF9C27B0),
        Color(0xFF00BCD4),
        Color(0xFFE91E63),
        Color(0xFF3F51B5),
        Color(0xFF795548),
        Color(0xFF607D8B),
        Color(0xFFFFEB3B),
        Color(0xFF8BC34A)
    )

    // 主色
    val primaryBlue = Color(0xFF00C3FF)
    
    // 成功渐变色
    val successGradientStart = Color(0xFF4CAF50)
    val successGradientEnd = Color(0xFF8BC34A)

    // 背景色
    val backgroundLight = Color(0xFFF8F9FA)
    val backgroundDark = Color(0xFF1A1A1A)

    // 卡片色
    val cardLight = Color(0xB3FFFFFF)  // 70%不透明度白色
    val cardDark = Color(0xB31A1A1A)   // 70%不透明度深色

    // 文字色
    val textPrimary = Color(0xFF333333)
    val textSecondary = Color(0xFF666666)
    val textHint = Color(0xFF999999)
    val textPrimaryDark = Color(0xFFFFFFFF)
    val textSecondaryDark = Color(0xFFB3B3B3)

    fun getColor(index: Int): Color = themeColorList.getOrElse(index) { primaryBlue }
    fun getColorInt(index: Int): Int = themeColors.getOrElse(index) { themeColors[0] }
}
