package com.miui.timesnote.widget

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.style.TextAlign as ComposeTextAlign
import com.miui.timesnote.data.CheckInEvent
import com.miui.timesnote.data.CheckInRepository
import com.miui.timesnote.util.DateUtil
import com.miui.timesnote.util.IconTypes
import com.miui.timesnote.util.MiuiColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * 2×2 尺寸打卡小组件 - 极简模式，显示1个高频事件
 */
class TimeStampWidget2x2 : androidx.glance.appwidget.GlanceAppWidget() {

    override val stateDefinition: androidx.glance.state.GlanceStateDefinition<*>?
        get() = null // 使用简化状态管理

    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        provideContent {
            val context = LocalContext.current
            
            // 获取最高频事件
            val topEvent = withContext(Dispatchers.IO) {
                try {
                    val repository = CheckInRepository.getInstance(context)
                    repository.getTopEvents(1).first().firstOrNull()
                } catch (e: Exception) {
                    null
                }
            }
            
            TimeStampWidgetContent2x2(event = topEvent)
        }
    }
}

@Composable
private fun TimeStampWidgetContent2x2(event: CheckInEvent?) {
    val context = LocalContext.current
    var pressProgress by remember { mutableFloatStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }
    var showCheck by remember { mutableStateOf(false) }
    
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
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                        try {
                            val repository = CheckInRepository.getInstance(context)
                            repository.checkIn(event)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    delay(1500)
                    showCheck = false
                    pressProgress = 0f
                    isPressed = false
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
                color = ComposeColor(0xFFF8F9FA),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (event == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "📝",
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "长按添加习惯",
                    fontSize = 12.sp,
                    color = ComposeColor(0xFF999999)
                )
            }
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
                    color = ComposeColor(0xFF333333)
                )
                
                Text(
                    text = "${event.consecutiveDays}天",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuiColors.themeColorList.getOrElse(event.colorIndex) { ComposeColor(0xFF00C3FF) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    MiuiColors.themeColorList.getOrElse(event.colorIndex) { ComposeColor(0xFF00C3FF) }.copy(alpha = 0.3f),
                                    MiuiColors.themeColorList.getOrElse(event.colorIndex) { ComposeColor(0xFF00C3FF) }.copy(alpha = 0.1f)
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
                                brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                                    colors = listOf(
                                        ComposeColor(0xFF00C3FF),
                                        ComposeColor(0xFF4CAF50),
                                        ComposeColor(0xFF8BC34A)
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
                        color = if (showCheck) ComposeColor(0xFF4CAF50) else MiuiColors.themeColorList.getOrElse(event.colorIndex) { ComposeColor(0xFF00C3FF) }
                    )
                }
            }
        }
    }
}