package com.miui.timesnote.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.timesnote.data.CheckInEvent
import com.miui.timesnote.data.CheckInRepository
import com.miui.timesnote.util.DateUtil
import com.miui.timesnote.util.IconTypes
import com.miui.timesnote.util.MiuiColors
import com.miui.timesnote.util.WallpaperColorUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.min

class MainActivity : ComponentActivity() {
    
    private lateinit var repository: CheckInRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = CheckInRepository.getInstance(this)
        
        setContent {
            MaterialTheme {
                MainScreen(repository = repository)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(repository: CheckInRepository) {
    var events by remember { mutableStateOf<List<CheckInEvent>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isDarkWallpaper by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 加载壁纸颜色
    LaunchedEffect(Unit) {
        scope.launch {
            val wallpaperColor = WallpaperColorUtil.getDominantWallpaperColor(context)
            isDarkWallpaper = wallpaperColor.isDark
        }
    }
    
    // 观察数据库变化
    LaunchedEffect(Unit) {
        repository.allEvents.collectLatest { eventList ->
            events = eventList
        }
    }
    
    val backgroundColor = if (isDarkWallpaper) MiuiColors.backgroundDark else MiuiColors.backgroundLight
    val textColor = if (isDarkWallpaper) MiuiColors.textPrimaryDark else MiuiColors.textPrimary
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "📅 时光印记",
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                },
                actions = {
                    IconButton(onClick = { /* 导出功能 */ }) {
                        Text("📤", fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MiuiColors.primaryBlue,
                shape = CircleShape
            ) {
                Text("+", fontSize = 24.sp, color = androidx.compose.ui.graphics.Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = androidx.compose.ui.graphics.Color.Transparent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("我的习惯") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("数据统计") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("设置") }
                )
            }
            
            when (selectedTab) {
                0 -> EventListTab(
                    events = events,
                    repository = repository,
                    isDarkWallpaper = isDarkWallpaper
                )
                1 -> StatisticsTab(events = events, repository = repository)
                2 -> SettingsTab(repository = repository)
            }
        }
    }
    
    if (showAddDialog) {
        AddEventDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, iconType, colorIndex ->
                scope.launch {
                    val newEvent = CheckInEvent(
                        name = name,
                        iconType = iconType,
                        colorIndex = colorIndex,
                        sortOrder = events.size
                    )
                    repository.insertEvent(newEvent)
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
fun EventListTab(
    events: List<CheckInEvent>,
    repository: CheckInRepository,
    isDarkWallpaper: Boolean
) {
    val scope = rememberCoroutineScope()
    
    if (events.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "📝", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "还没有习惯，开始添加吧",
                    fontSize = 16.sp,
                    color = MiuiColors.textHint
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(events, key = { it.id }) { event ->
                EventCard(
                    event = event,
                    repository = repository,
                    isDarkWallpaper = isDarkWallpaper,
                    onDelete = {
                        scope.launch {
                            repository.deleteEvent(event)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun EventCard(
    event: CheckInEvent,
    repository: CheckInRepository,
    isDarkWallpaper: Boolean,
    onDelete: () -> Unit
) {
    var pressProgress by remember { mutableFloatStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }
    var showCheckAnimation by remember { mutableStateOf(false) }
    var hasCheckedInToday by remember { mutableStateOf(DateUtil.isToday(event.lastCheckInTime)) }
    
    val scope = rememberCoroutineScope()
    
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.95f + (pressProgress * 0.05f) else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    LaunchedEffect(isPressed) {
        if (isPressed && !hasCheckedInToday) {
            while (pressProgress < 1f) {
                delay(12)
                pressProgress = min(1f, pressProgress + 0.014f)
                if (pressProgress >= 1f) {
                    showCheckAnimation = true
                    
                    // 执行数据库打卡
                    scope.launch {
                        repository.checkIn(event)
                    }
                    
                    delay(1500)
                    showCheckAnimation = false
                    pressProgress = 0f
                    isPressed = false
                    hasCheckedInToday = true
                }
            }
        } else {
            pressProgress = 0f
        }
    }
    
    val cardBackground = if (isDarkWallpaper) {
        MiuiColors.cardDark
    } else {
        MiuiColors.cardLight
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 颜色指示条
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MiuiColors.getColor(event.colorIndex))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 图标
            Text(text = IconTypes.getIconEmoji(event.iconType), fontSize = 32.sp)
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "连续 ${event.consecutiveDays} 天 · 共 ${event.totalCheckIns} 次打卡",
                    fontSize = 12.sp,
                    color = MiuiColors.textHint
                )
                if (event.lastCheckInTime > 0) {
                    Text(
                        text = "上次打卡: ${DateUtil.formatDateTime(event.lastCheckInTime)}",
                        fontSize = 10.sp,
                        color = MiuiColors.textHint
                    )
                }
            }
            
            // 打卡按钮
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = when {
                                showCheckAnimation -> listOf(
                                    MiuiColors.successGradientStart.copy(alpha = 0.3f),
                                    MiuiColors.successGradientEnd.copy(alpha = 0.1f)
                                )
                                hasCheckedInToday -> listOf(
                                    MiuiColors.successGradientStart.copy(alpha = 0.15f),
                                    MiuiColors.successGradientStart.copy(alpha = 0.05f)
                                )
                                else -> listOf(
                                    MiuiColors.getColor(event.colorIndex).copy(alpha = 0.2f),
                                    MiuiColors.getColor(event.colorIndex).copy(alpha = 0.05f)
                                )
                            }
                        )
                    )
                    .clickable(enabled = !hasCheckedInToday) {
                        isPressed = true
                    }
            ) {
                // 进度环
                if (!hasCheckedInToday) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val strokeWidth = 4.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                        
                        // 背景环
                        drawCircle(
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f),
                            radius = radius,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )
                        
                        // 进度环
                        if (pressProgress > 0) {
                            val sweepAngle = 360 * pressProgress
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        MiuiColors.primaryBlue,
                                        MiuiColors.getColor(event.colorIndex),
                                        MiuiColors.successGradientEnd
                                    )
                                ),
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeWidth,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            )
                        }
                    }
                }
                
                Text(
                    text = when {
                        showCheckAnimation -> "✓"
                        hasCheckedInToday -> "✓"
                        else -> IconTypes.getIconEmoji(event.iconType)
                    },
                    fontSize = if (showCheckAnimation || hasCheckedInToday) 28.sp else 20.sp,
                    color = if (showCheckAnimation || hasCheckedInToday) {
                        MiuiColors.successGradientStart
                    } else {
                        MiuiColors.getColor(event.colorIndex)
                    }
                )
            }
        }
    }
}

@Composable
fun StatisticsTab(events: List<CheckInEvent>, repository: CheckInRepository) {
    val totalCheckIns = events.sumOf { it.totalCheckIns }
    val totalConsecutive = events.sumOf { it.consecutiveDays }
    val bestStreak = events.maxOfOrNull { it.consecutiveDays } ?: 0
    val todayCount = events.count { DateUtil.isToday(it.lastCheckInTime) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 数据概览",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCard(value = "$totalCheckIns", label = "总打卡次数", color = MiuiColors.primaryBlue)
                        StatCard(value = "$totalConsecutive", label = "累计连续天数", color = MiuiColors.successGradientStart)
                        StatCard(value = "$bestStreak", label = "最佳连续", color = MiuiColors.themeColorList[4])
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📅 今日进度", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${todayCount}/${events.size}", fontSize = 14.sp, color = MiuiColors.primaryBlue)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (events.isEmpty()) 0f else todayCount.toFloat() / events.size },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MiuiColors.primaryBlue,
                        trackColor = MiuiColors.primaryBlue.copy(alpha = 0.1f)
                    )
                }
            }
        }
        
        items(events) { event ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = IconTypes.getIconEmoji(event.iconType), fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = event.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "连续 ${event.consecutiveDays} 天",
                            fontSize = 12.sp,
                            color = MiuiColors.getColor(event.colorIndex)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "${event.totalCheckIns}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = "总打卡", fontSize = 10.sp, color = MiuiColors.textHint)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 11.sp, color = MiuiColors.textHint)
    }
}

@Composable
fun SettingsTab(repository: CheckInRepository) {
    var wallpaperAdaptive by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "外观",
                fontSize = 14.sp,
                color = MiuiColors.textHint,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        item {
            SettingsSwitchItem(
                title = "壁纸色彩感知",
                subtitle = "根据壁纸自动调整界面颜色",
                checked = wallpaperAdaptive,
                onCheckedChange = {
                    wallpaperAdaptive = it
                    scope.launch {
                        com.miui.timesnote.data.SettingsData.setWallpaperAdaptive(it)
                    }
                }
            )
        }
        
        item {
            SettingsSwitchItem(
                title = "深色模式",
                subtitle = "跟随系统设置",
                checked = darkMode,
                onCheckedChange = {
                    darkMode = it
                    scope.launch {
                        com.miui.timesnote.data.SettingsData.setDarkMode(it)
                    }
                }
            )
        }
        
        item {
            Text(
                text = "数据",
                fontSize = 14.sp,
                color = MiuiColors.textHint,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        item {
            SettingsClickItem(title = "导出数据", subtitle = "导出为CSV格式")
        }
        
        item {
            SettingsClickItem(title = "清空数据", subtitle = "删除所有打卡记录")
        }
        
        item {
            Text(
                text = "关于",
                fontSize = 14.sp,
                color = MiuiColors.textHint,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        item {
            SettingsClickItem(title = "版本", subtitle = "3.0.301.0")
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp)
                Text(text = subtitle, fontSize = 12.sp, color = MiuiColors.textHint)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun SettingsClickItem(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, fontSize = 14.sp)
                Text(text = subtitle, fontSize = 12.sp, color = MiuiColors.textHint)
            }
            Text(text = ">", fontSize = 16.sp, color = MiuiColors.textHint)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableIntStateOf(0) }
    var selectedColor by remember { mutableIntStateOf(0) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加新习惯", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 8) name = it },
                    label = { Text("习惯名称（8字以内）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "选择图标", fontSize = 14.sp, color = MiuiColors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconTypes.allIcons.forEach { iconType ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedIcon == iconType) MiuiColors.primaryBlue.copy(alpha = 0.2f)
                                    else androidx.compose.ui.graphics.Color.Transparent
                                )
                                .clickable { selectedIcon = iconType },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = IconTypes.getIconEmoji(iconType), fontSize = 20.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "选择颜色", fontSize = 14.sp, color = MiuiColors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MiuiColors.themeColorList.take(6).forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = index },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == index) {
                                Text("✓", color = androidx.compose.ui.graphics.Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MiuiColors.themeColorList.drop(6).forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = index + 6 },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == index + 6) {
                                Text("✓", color = androidx.compose.ui.graphics.Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selectedIcon, selectedColor) },
                enabled = name.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
