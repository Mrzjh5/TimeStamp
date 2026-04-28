# 时光印记 - HyperOS 3.0.301.0 打卡小组件

## 项目概述

一款深度融合小米设计语言的习惯打卡小组件，支持2×2、2×3、4×4三种尺寸，采用Jetpack Glance构建。

## 核心功能

### 1. 打卡事件管理
- 自定义名称（8字以内）
- 8种预设图标：读书、运动、早起、喝水、早睡、学习、冥想、健康
- 12种小米主题色选择
- 智能排序（频率/时间/自定义）

### 2. 长按打卡交互
- 环形进度条可视化（蓝色→绿色渐变）
- 1.2秒最小长按阈值防误触
- 多阶段动画反馈
- 打卡成功后图标变绿显示✓

### 3. 数据库持久化
- Room数据库存储所有事件和打卡记录
- CheckInRepository统一数据访问层
- 支持Flow响应式数据更新

### 4. 壁纸色彩感知
- 分析壁纸主色调
- 自动调整背景透明度（明亮壁纸85%，暗色壁纸95%）
- 自动调整文字颜色对比度
- 深色/浅色主题智能切换

## 技术架构

```
com.miui.timesnote/
├── TimesNoteApp.kt              # Application + WorkManager初始化
├── data/
│   ├── Database.kt            # Room数据库(CheckInEvent/CheckInRecord)
│   ├── Preferences.kt          # DataStore设置管理
│   └── CheckInRepository.kt    # 数据仓库模式
├── ui/
│   └── MainActivity.kt         # 主界面(管理App+三个Tab)
├── widget/
│   ├── TimeStampWidget2x2.kt   # 2×2组件
│   ├── TimeStampWidget2x3.kt   # 2×3组件
│   ├── TimeStampWidget4x4.kt   # 4×4组件
│   └── WidgetClickReceiver.kt
└── util/
    ├── HapticUtil.kt           # 触感反馈(仅保留成就震动)
    ├── WallpaperColorUtil.kt    # 壁纸色彩感知
    ├── DateUtil.kt             # 日期工具
    ├── MiuiColors.kt           # 颜色规范
    ├── IconTypes.kt            # 图标定义
    ├── ExportUtil.kt           # 导出工具
    ├── WidgetStateManager.kt   # 组件状态管理
    └── Workers.kt              # 后台任务
```

## 数据库设计

### CheckInEvent 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String | 习惯名称 |
| iconType | Int | 图标类型(0-7) |
| colorIndex | Int | 颜色索引(0-11) |
| consecutiveDays | Int | 连续打卡天数 |
| totalCheckIns | Int | 总打卡次数 |
| lastCheckInTime | Long | 上次打卡时间 |
| isBroken | Boolean | 是否已中断 |
| sortOrder | Int | 排序顺序 |
| frequency | Int | 使用频率 |

### CheckInRecord 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| eventId | Long | 关联事件ID |
| checkInTime | Long | 打卡时间戳 |
| date | String | 日期(yyyy-MM-dd) |
| consecutiveDays | Int | 打卡时连续天数 |

## 壁纸感知算法

```
1. 获取壁纸Bitmap (Android 6.0+)
2. 采样5个点计算平均颜色
3. 计算亮度 L = 0.299*R + 0.587*G + 0.114*B
4. 若 L < 0.5 则为暗色壁纸，调整卡片透明度至95%
5. 根据壁纸色调选择文字颜色(白/深灰)
```

## 界面预览

### 2×2 极简模式
- 显示1个高频事件
- 中心环形打卡按钮
- 简洁的连续天数展示

### 2×3 平衡模式
- 显示3个事件列表
- 横向打卡按钮
- 适合桌面紧凑布局

### 4×4 完整版
- 5个事件+统计面板
- 顶部数据概览
- 底部添加按钮

## 动画设计

| 阶段 | 进度 | 效果 |
|------|------|------|
| 初始 | 0% | 按钮轻微放大95%→100% |
| 中段 | 30-70% | 中心图标跳动，进度环加速 |
| 完成 | 70-100% | 边缘发光，进度环完成 |
| 成功 | 100% | ✓图标显示，1.5秒后淡出 |

## 性能指标

- 冷启动: <300ms
- 内存占用: <15MB（空闲）
- 电池消耗: <0.5%/日
- 流畅度: 95%时间维持55fps+

## 构建要求

- Android SDK: 35+
- Min SDK: 26
- Kotlin: 1.9.20
- AGP: 8.2.0
- Jetpack Glance: 1.0.0

## 构建方式

```bash
cd E:\ZJHJAVA\TimeStamp
./gradlew assembleDebug   # Debug构建
./gradlew assembleRelease # Release构建
```

## 更新日志

### v3.0.301.0
- ✅ 集成Room数据库实际读写
- ✅ 添加壁纸色彩感知功能
- ✅ 移除打卡成功震动和音效
- ✅ 保留成就系统震动反馈
- ✅ CheckInRepository数据仓库模式
