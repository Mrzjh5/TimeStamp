package com.miui.timesnote.util

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.miui.timesnote.data.CheckInEvent
import com.miui.timesnote.data.CheckInRecord
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 数据导出工具
 */
object ExportUtil {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val fileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * 导出CSV格式
     */
    fun exportToCsv(
        context: Context,
        events: List<CheckInEvent>,
        records: List<CheckInRecord>,
        outputStream: OutputStream
    ): Boolean {
        return try {
            val writer = outputStream.bufferedWriter()
            
            // CSV Header
            writer.write("日期,事件名称,打卡时间,连续天数,总打卡次数,事件颜色索引\n")
            
            // Sort records by time
            val sortedRecords = records.sortedByDescending { it.checkInTime }
            
            for (record in sortedRecords) {
                val event = events.find { it.id == record.eventId }
                if (event != null) {
                    val line = buildString {
                        append(record.date)
                        append(",")
                        append("\"${event.name}\"")
                        append(",")
                        append(DateUtil.formatDateTime(record.checkInTime))
                        append(",")
                        append(record.consecutiveDays)
                        append(",")
                        append(event.totalCheckIns)
                        append(",")
                        append(event.colorIndex)
                        append("\n")
                    }
                    writer.write(line)
                }
            }
            
            writer.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 生成分享文件URI
     */
    fun createShareUri(context: Context, fileName: String): Uri? {
        return try {
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            }
            context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 生成文件名
     */
    fun generateFileName(): String {
        return "times_note_export_${fileNameFormat.format(Date())}.csv"
    }

    /**
     * 生成热力图数据 (7天/30天)
     */
    fun generateHeatmapData(
        records: List<CheckInRecord>,
        days: Int
    ): Map<String, Int> {
        val heatmap = mutableMapOf<String, Int>()
        val calendar = Calendar.getInstance()
        
        // Initialize all days with 0
        for (i in 0 until days) {
            val date = dateFormat.format(calendar.time)
            heatmap[date] = 0
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        // Count check-ins per day
        for (record in records) {
            val count = heatmap[record.date] ?: 0
            heatmap[record.date] = count + 1
        }
        
        return heatmap
    }
}
