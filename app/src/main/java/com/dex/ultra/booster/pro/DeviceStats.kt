package com.dex.ultra.booster.pro

import android.app.ActivityManager
import android.content.Context
import java.io.FileReader

object DeviceStats {
    fun getRamUsagePercent(context: Context): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val total = memInfo.totalMem
        val available = memInfo.availMem
        val used = total - available
        return ((used.toDouble() / total.toDouble()) * 100).toInt()
    }

    fun getAvailableRamMB(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024 * 1024)
    }

    fun getCpuTemperature(): Float {
        val thermalPaths = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/thermal/thermal_zone4/temp"
        )
        for (path in thermalPaths) {
            try {
                val raw = java.io.File(path).readText().trim().toFloatOrNull() ?: continue
                return if (raw > 1000) raw / 1000f else raw
            } catch (e: Exception) { continue }
        }
        return 0f
    }

    fun getCpuUsagePercent(): Int {
        return try {
            val reader = FileReader("/proc/stat").buffered()
            val line = reader.readLine()
            reader.close()
            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size < 5) return 0
            val user = parts[1].toLong()
            val nice = parts[2].toLong()
            val system = parts[3].toLong()
            val idle = parts[4].toLong()
            val total = user + nice + system + idle
            val work = user + nice + system
            ((work.toDouble() / total.toDouble()) * 100).toInt()
        } catch (e: Exception) { 0 }
    }
}
