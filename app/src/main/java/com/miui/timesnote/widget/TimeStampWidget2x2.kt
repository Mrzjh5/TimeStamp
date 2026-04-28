package com.miui.timesnote.widget

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.miui.timesnote.data.CheckInEvent
import com.miui.timesnote.data.CheckInRepository
import com.miui.timesnote.ui.MainActivity
import com.miui.timesnote.util.DateUtil
import com.miui.timesnote.util.IconTypes
import com.miui.timesnote.util.MiuiColors
import com.miui.timesnote.util.WidgetStateManager
import com.miui.timesnote.util.WallpaperColorUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * 2×2 尺寸打卡小组件 - 极简模式，显示1个高频事件
 */
class TimeStampWidget2x2 : GlanceAppWidget() {

    override val stateDefinition = GlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 获取壁纸颜色
        val wallpaperColor = WallpaperColorUtil.getDominantWallpaperColor(context)
        val backgroundColor = if (wallpaperColor.isDark) {
            Color(0xE61A1A1A)
        } else {
            Color(0xE6F8F9FA)
        }
        
        // 获取最高频事件
        val repository = CheckInRepository.getInstance(context)
        val events = repository.getTopEvents(1).first()
        val topEvent = events.firstOrNull()
        
        provideContent {
            TimeStampWidgetContent2x2(
                event = topEvent,
                backgroundColor = backgroundColor,
                textColor = wallpaperColor.contrastColor,
                isDarkWallpaper = wallpaperColor.isDark
            )
        }
    }
}

@Composable
private fun TimeStampWidgetContent2x2(
    event: CheckInEvent?,
    backgroundColor: Color,
    textColor: Color,
    isDarkWallpaper: Boolean
) {
    var pressProgress by remember { mutableFloatStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }
    var showCheck by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f + (pressProgress * 0.05f) else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    
    LaunchedEffect(isPressed) {
        if (isPressed && event != null) {
            while (pressProgress < 1f) {
                delay(12)
                pressProgress = min(1f, pressProgress + 0.014f)
                if (pressProgress >= 1f) {
                    showCheck = true
                    
                    // 执行打卡
                    scope.launch {
                        val repository = CheckInRepository.getInstance(context)
                        repository.checkIn(event)
                    }
                    
                    delay(1500)
                    showCheck = false
                    pressProgress = 0f
                    isPressed = false
                    
                    // 刷新组件
                    WidgetStateManager.refreshAllWidgets(context)
                }
            }
        } else {
            pressProgress = 0f
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (event == null) {
            EmptyStateContent2x2(textColor)
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = IconTypes.getIconEmoji(event.iconType),
                    fontSize = 32.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = event.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                
                Text(
                    text = "${event.consecutiveDays}天",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuiColors.getColor(event.colorIndex)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MiuiColors.getColor(event.colorIndex).copy(alpha = 0.3f),
                                    MiuiColors.getColor(event.colorIndex).copy(alpha = 0.1f)
                                )
                            )
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    tryAwaitRelease()
                                    isPressed = false
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 4.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val center = Offset(size.width / 2, size.height / 2)
                        
                        drawCircle(
                            color = Color.White.copy(alpha = 0.3f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeWidth)
                        )
                        
                        if (pressProgress > 0) {
                            val sweepAngle = 360 * pressProgress
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        MiuiColors.primaryBlue,
                                        MiuiColors.successGradientStart,
                                        MiuiColors.successGradientEnd
                                    )
                                ),
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                    
                    Text(
                        text = if (showCheck) "✓" else IconTypes.getIconEmoji(event.iconType),
                        fontSize = if (showCheck) 28.sp else 24.sp,
                        color = if (showCheck) MiuiColors.successGradientStart else MiuiColors.getColor(event.colorIndex)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateContent2x2(textColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "📝", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "长按添加习惯",
            fontSize = 12.sp,
            color = textColor.copy(alpha = 0.6f)
        )
    }
}
