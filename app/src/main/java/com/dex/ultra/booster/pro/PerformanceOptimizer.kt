package com.dex.ultra.booster.pro

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PerformanceOptimizer {

    private const val TAG = "PerfOptimizer"

    /**
     * Clear RAM cache and trim background processes.
     */
    fun clearSystemCache(context: Context) {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            // Request trim memory from all background processes
            activityManager.killBackgroundProcesses(context.packageName)

            // Attempt to clear via Shizuku if available
            if (ShizukuHelper.isAvailable()) {
                ShizukuHelper.runCommand("sync && echo 3 > /proc/sys/vm/drop_caches 2>/dev/null; true")
            }
            Log.i(TAG, "Cache cleared")
        } catch (e: Exception) {
            Log.w(TAG, "clearSystemCache: ${e.message}")
        }
    }

    /**
     * Set CPU governor to performance mode (requires Shizuku/root).
     */
    fun optimizeCpuGovernor() {
        if (!ShizukuHelper.isAvailable()) return
        try {
            // Try setting performance governor on all cores
            val cmd = """
                for f in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
                    [ -w "${'$'}f" ] && echo performance > "${'$'}f"
                done
            """.trimIndent()
            ShizukuHelper.runCommand(cmd)

            // Lock max frequency
            val maxFreqCmd = """
                for f in /sys/devices/system/cpu/cpu*/cpufreq/scaling_max_freq; do
                    max=$(cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq 2>/dev/null)
                    [ -w "${'$'}f" ] && echo "${'$'}max" > "${'$'}f"
                done
            """.trimIndent()
            ShizukuHelper.runCommand(maxFreqCmd)
            Log.i(TAG, "CPU governor set to performance")
        } catch (e: Exception) {
            Log.w(TAG, "optimizeCpuGovernor: ${e.message}")
        }
    }

    /**
     * Optimize virtual memory settings for gaming.
     */
    fun optimizeMemory(context: Context) {
        if (!ShizukuHelper.isAvailable()) return
        try {
            val cmds = listOf(
                "echo 10 > /proc/sys/vm/swappiness 2>/dev/null; true",
                "echo 1 > /proc/sys/vm/overcommit_memory 2>/dev/null; true",
                "echo 100 > /proc/sys/vm/overcommit_ratio 2>/dev/null; true"
            )
            cmds.forEach { ShizukuHelper.runCommand(it) }
            Log.i(TAG, "Memory optimized")
        } catch (e: Exception) {
            Log.w(TAG, "optimizeMemory: ${e.message}")
        }
    }

    /**
     * Optimize network parameters for low latency gaming.
     */
    fun optimizeNetwork() {
        if (!ShizukuHelper.isAvailable()) return
        try {
            val cmds = listOf(
                "echo 1 > /proc/sys/net/ipv4/tcp_low_latency 2>/dev/null; true",
                "echo 0 > /proc/sys/net/ipv4/tcp_slow_start_after_idle 2>/dev/null; true",
                "echo 1 > /proc/sys/net/ipv4/tcp_no_metrics_save 2>/dev/null; true",
                "echo 1 > /proc/sys/net/ipv4/tcp_fastopen 2>/dev/null; true"
            )
            cmds.forEach { ShizukuHelper.runCommand(it) }
            Log.i(TAG, "Network optimized")
        } catch (e: Exception) {
            Log.w(TAG, "optimizeNetwork: ${e.message}")
        }
    }

    /**
     * Disable thermal throttling temporarily for better performance.
     * Use with caution — device may get warm.
     */
    fun disableThermalThrottle() {
        if (!ShizukuHelper.isAvailable()) return
        try {
            ShizukuHelper.runCommand(
                "stop thermald 2>/dev/null; stop thermal-engine 2>/dev/null; true"
            )
            Log.i(TAG, "Thermal throttle disabled")
        } catch (e: Exception) {
            Log.w(TAG, "disableThermalThrottle: ${e.message}")
        }
    }

    /**
     * Re-enable thermal protection.
     */
    fun enableThermalProtection() {
        if (!ShizukuHelper.isAvailable()) return
        try {
            ShizukuHelper.runCommand(
                "start thermald 2>/dev/null; start thermal-engine 2>/dev/null; true"
            )
        } catch (e: Exception) {
            Log.w(TAG, "enableThermalProtection: ${e.message}")
        }
    }
}
