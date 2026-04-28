package com.miui.timesnote.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * 壁纸色彩感知工具
 * 分析壁纸主色调，自动调整小组件背景透明度和文字对比度
 */
object WallpaperColorUtil {

    private const val ADAPTIVE_ALPHA = 0.85f  // 明亮壁纸时降低透明度
    private const val DARK_ALPHA = 0.95f       // 暗色壁纸时增加透明度
    private const val CONTRAST_THRESHOLD = 0.5 // 对比度阈值

    /**
     * 获取壁纸主色调
     */
    suspend fun getDominantWallpaperColor(context: Context): WallpaperColorResult {
        return withContext(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                
                val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.getBitmap()
                } else {
                    @Suppress("DEPRECATION")
                    wallpaperManager.drawable?.let { drawable ->
                        if (drawable is android.graphics.drawable.BitmapDrawable) {
                            drawable.bitmap
                        } else null
                    }
                }

                if (bitmap != null) {
                    analyzeBitmap(bitmap)
                } else {
                    WallpaperColorResult.Default
                }
            } catch (e: Exception) {
                e.printStackTrace()
                WallpaperColorResult.Default
            }
        }
    }

    private fun analyzeBitmap(bitmap: Bitmap): WallpaperColorResult {
        // 采样策略：从壁纸不同位置采样，分析主色调
        val samplePoints = listOf(
            bitmap.width / 4 to bitmap.height / 4,
            bitmap.width * 3 / 4 to bitmap.height / 4,
            bitmap.width / 2 to bitmap.height / 2,
            bitmap.width / 4 to bitmap.height * 3 / 4,
            bitmap.width * 3 / 4 to bitmap.height * 3 / 4
        )

        val colors = samplePoints.mapNotNull { (x, y) ->
            try {
                if (x < bitmap.width && y < bitmap.height) {
                    bitmap.getPixel(x, y)
                } else null
            } catch (e: Exception) {
                null
            }
        }

        if (colors.isEmpty()) {
            return WallpaperColorResult.Default
        }

        // 计算平均颜色
        var r = 0L
        var g = 0L
        var b = 0L
        colors.forEach { color ->
            r += Color.red(color)
            g += Color.green(color)
            b += Color.blue(color)
        }
        r /= colors.size
        g /= colors.size
        b /= colors.size

        // 计算亮度
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        
        // 判断是否需要深色/浅色主题
        val isDark = luminance < CONTRAST_THRESHOLD

        // 计算对比色（用于文字/图标）
        val contrastColor = if (isDark) Color.White else Color(0xFF1A1A1A)

        // 计算自适应透明度
        val adaptiveAlpha = if (isDark) DARK_ALPHA else ADAPTIVE_ALPHA

        // 判断冷暖色调
        val isWarmTone = (r > g && r > b) || (abs(r - g) < 30 && r > b)

        return WallpaperColorResult(
            primaryColor = Color(r.toInt(), g.toInt(), b.toInt()),
            isDark = isDark,
            adaptiveAlpha = adaptiveAlpha,
            contrastColor = contrastColor,
            isWarmTone = isWarmTone
        )
    }

    /**
     * 快速检测壁纸亮度（用于轻量级检测）
     */
    @ColorInt
    fun quickDetectBrightness(context: Context): Int {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.getBitmap()
            } else {
                null
            }

            if (bitmap != null) {
                // 只采样中心点
                val centerX = bitmap.width / 2
                val centerY = bitmap.height / 2
                if (centerX < bitmap.width && centerY < bitmap.height) {
                    bitmap.getPixel(centerX, centerY)
                } else {
                    Color.WHITE
                }
            } else {
                Color.WHITE
            }
        } catch (e: Exception) {
            Color.WHITE
        }
    }

    /**
     * 获取适合的背景色（基于壁纸自适应）
     */
    suspend fun getAdaptiveBackgroundColor(context: Context): Color {
        val wallpaperColor = getDominantWallpaperColor(context)
        return if (wallpaperColor.isDark) {
            Color(0xE61A1A1A) // 深色背景，高透明度
        } else {
            Color(0xE6F8F9FA) // 浅色背景
        }
    }

    /**
     * 获取适合的文字颜色
     */
    suspend fun getAdaptiveTextColor(context: Context): Color {
        val wallpaperColor = getDominantWallpaperColor(context)
        return wallpaperColor.contrastColor
    }
}

/**
 * 壁纸颜色分析结果
 */
data class WallpaperColorResult(
    val primaryColor: Color,
    val isDark: Boolean,
    val adaptiveAlpha: Float,
    val contrastColor: Color,
    val isWarmTone: Boolean
) {
    companion object {
        val Default = WallpaperColorResult(
            primaryColor = Color(0xFF00C3FF),
            isDark = false,
            adaptiveAlpha = ADAPTIVE_ALPHA,
            contrastColor = Color(0xFF1A1A1A),
            isWarmTone = false
        )
        
        private const val ADAPTIVE_ALPHA = 0.85f
    }
}
