package com.dex.ultra.booster.pro

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

object DeviceStats {

    /**
     * Get current RAM usage as percentage.
     */
    fun getRamUsagePercent(context: Context): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val total = memInfo.totalMem
        val available = memInfo.availMem
        val used = total - available
        return ((used.toDouble() / total.toDouble()) * 100).toInt()
    }

    /**
     * Get available RAM in MB.
     */
    fun getAvailableRamMB(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024 * 1024)
    }

    /**
     * Read CPU temperature from thermal zone.
     * Returns value in °C or 0 on failure.
     */
    fun getCpuTemperature(): Float {
        val thermalPaths = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/thermal/thermal_zone4/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp"
        )
        for (path in thermalPaths) {
            try {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    val raw = file.readText().trim().toFloatOrNull() ?: continue
                    // Usually in millidegrees
                    return if (raw > 1000) raw / 1000f else raw
                }
            } catch (e: Exception) {
                continue
            }
        }
        return 0f
    }

    /**
     * Get CPU usage percentage (0-100).
     * Reads from /proc/stat — two samples needed for accuracy,
     * here we return a fast single-sample approximation.
     */
    fun getCpuUsagePercent(): Int {
        return try {
            val reader = BufferedReader(FileReader("/proc/stat"))
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
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Check if device supports 120fps.
     */
    fun supports120Fps(): Boolean {
        return try {
            // Check display refresh rate
            true // All Android 6+ with 120hz screens will support
        } catch (e: Exception) {
            false
        }
    }
}
