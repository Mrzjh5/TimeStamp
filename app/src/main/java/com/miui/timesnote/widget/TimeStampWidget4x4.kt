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
 * 4×4 尺寸打卡小组件 - 完整版，显示5个事件+统计面板
 */
class TimeStampWidget4x4 : androidx.glance.appwidget.GlanceAppWidget() {

    override val stateDefinition: androidx.glance.state.GlanceStateDefinition<*>?
        get() = null

    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        provideContent {
            val context = LocalContext.current
            
            val events = withContext(Dispatchers.IO) {
                try {
                    val repository = CheckInRepository.getInstance(context)
                    repository.getTopEvents(5).first()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            
            TimeStampWidgetContent4x4(events = events)
        }
    }
}

@Composable
private fun TimeStampWidgetContent4x4(events: List<CheckInEvent>) {
    val context = LocalContext.current
    
    val totalCheckIns = events.sumOf { it.totalCheckIns }
    val bestStreak = events.maxOfOrNull { it.consecutiveDays } ?: 0
    val todayCheckIns = events.count { DateUtil.isToday(it.lastCheckInTime) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = ComposeColor(0xFFF8F9FA),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅 时光印记",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ComposeColor(0xFF333333)
                )
                Text(
                    text = DateUtil.getToday(),
                    fontSize = 11.sp,
                    color = ComposeColor(0xFF999999)
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ComposeColor(0xB3FFFFFF))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(value = "$totalCheckIns", label = "总打卡")
                StatItem(value = "$bestStreak", label = "最佳连续")
                StatItem(value = "${events.size}", label = "习惯数")
                StatItem(value = "$todayCheckIns", label = "今日完成")
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                events.take(5).forEach { event ->
                    CheckInItemCard(
                        event = event,
                        onCheckIn = { /* 刷新 */ }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ComposeColor(0xFF00C3FF).copy(alpha = 0.1f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+",
                    fontSize = 16.sp,
                    color = ComposeColor(0xFF00C3FF),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "添加新习惯",
                    fontSize = 12.sp,
                    color = ComposeColor(0xFF00C3FF)
                )
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ComposeColor(0xFF00C3FF)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = ComposeColor(0xFF999999)
        )
    }
}

@Composable
private fun CheckInItemCard(
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
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MiuiColors.themeColorList.getOrElse(event.colorIndex) { ComposeColor(0xFF00C3FF) })
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = IconTypes.getIconEmoji(event.iconType),
                fontSize = 18.sp
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ComposeColor(0xFF333333),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "连续 ${event.consecutiveDays} 天",
                    fontSize = 10.sp,
                    color = if (event.isBroken) Color.Red else ComposeColor(0xFF999999)
                )
            }
            
            Text(
                text = "${event.totalCheckIns}次",
                fontSize = 10.sp,
                color = ComposeColor(0xFF999999)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
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
                    val strokeWidth = 2.5.dp.toPx()
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
                    text = if (showCheck) "✓" else "●",
                    fontSize = if (showCheck) 14.sp else 10.sp,
                    color = if (showCheck) ComposeColor(0xFF4CAF50) else MiuiColors.themeColorList.getOrElse(event.colorIndex) { ComposeColor(0xFF00C3FF) }
                )
            }
        }
    }
}