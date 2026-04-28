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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color as ComposeColor
import com.miui.timesnote.data.CheckInEvent
import com.miui.timesnote.data.CheckInRepository
import com.miui.timesnote.util.DateUtil
import com.miui.timesnote.util.IconTypes
import com.miui.timesnote.util.MiuiColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * 2×3 尺寸打卡小组件 - 平衡模式，显示3个事件
 */
class TimeStampWidget2x3 : androidx.glance.appwidget.GlanceAppWidget() {

    override val stateDefinition: androidx.glance.state.GlanceStateDefinition<*>?
        get() = null

    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        provideContent {
            val context = LocalContext.current
            
            val events = withContext(Dispatchers.IO) {
                try {
                    val repository = CheckInRepository.getInstance(context)
                    repository.getTopEvents(3).first()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            
            TimeStampWidgetContent2x3(events = events)
        }
    }
}

@Composable
private fun TimeStampWidgetContent2x3(events: List<CheckInEvent>) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = ComposeColor(0xFFF8F9FA),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(8.dp)
    ) {
        if (events.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "📝", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "长按这里添加你的第一个习惯",
                    fontSize = 12.sp,
                    color = ComposeColor(0xFF999999),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                events.take(3).forEach { event ->
                    CheckInItemRow(
                        event = event,
                        onCheckIn = { /* 刷新 */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckInItemRow(
    event: CheckInEvent,
    onCheckIn: () -> Unit
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
        if (isPressed) {
            while (pressProgress < 1f) {
                delay(12)
                pressProgress = min(1f, pressProgress + 0.014f)
                if (pressProgress >= 1f) {
                    showCheck = true
                    
                    scope.launch {
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
                    onCheckIn()
                }
            }
        } else {
            pressProgress = 0f
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeColor(0xB3FFFFFF))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = IconTypes.getIconEmoji(event.iconType),
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = event.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ComposeColor(0xFF333333)
                )
                Text(
                    text = "${event.consecutiveDays}天连续",
                    fontSize = 10.sp,
                    color = ComposeColor(0xFF999999)
                )
            }
        }
        
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    if (showCheck) ComposeColor(0xFF4CAF50).copy(alpha = 0.2f)
                    else MiuiColors.themeColorList.getOrElse(event.colorIndex) { ComposeColor(0xFF00C3FF) }.copy(alpha = 0.15f)
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
                val strokeWidth = 3.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)
                
                if (pressProgress > 0) {
                    val sweepAngle = 360 * pressProgress
                    drawArc(
                        brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                            colors = listOf(
                                ComposeColor(0xFF00C3FF),
                                MiuiColors.themeColorList.getOrElse(event.colorIndex) { ComposeColor(0xFF00C3FF) },
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
                fontSize = 16.sp,
                color = if (showCheck) ComposeColor(0xFF4CAF50) else MiuiColors.themeColorList.getOrElse(event.colorIndex) { ComposeColor(0xFF00C3FF) }
            )
        }
    }
}