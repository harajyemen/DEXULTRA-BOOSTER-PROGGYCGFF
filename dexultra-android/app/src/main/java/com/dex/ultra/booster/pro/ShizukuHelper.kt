package com.dex.ultra.booster.pro

import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.topjohnwu.superuser.Shell
import rikka.shizuku.Shizuku

object ShizukuHelper {

    private const val TAG = "ShizukuHelper"
    const val REQUEST_CODE = 1001

    val PUBG_PACKAGES = mapOf(
        0 to "com.tencent.ig",
        1 to "com.pubg.krmobile",
        2 to "com.vng.pubgmobile",
        3 to "com.pubg.imobile",
        4 to "com.tencent.iglite"
    )

    fun isAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        return try {
            Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) { false }
    }

    fun isRunning(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        return try { Shizuku.pingBinder() } catch (e: Exception) { false }
    }

    fun isRootAvailable(): Boolean {
        return try { Shell.getShell().isRoot } catch (e: Exception) { false }
    }

    fun requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        try {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(REQUEST_CODE)
            }
        } catch (e: Exception) {
            Log.w(TAG, "requestPermission error: ${e.message}")
        }
    }

    /**
     * Run a shell command — tries Shizuku (ADB-level) first, then libsu (root).
     * Returns stdout string on success, null on failure.
     */
    fun runCommand(command: String): String? {
        if (isAvailable()) {
            val result = runViaShizuku(command)
            if (result != null) return result
        }
        return try {
            val r = Shell.cmd(command).exec()
            if (r.isSuccess) r.out.joinToString("\n").trim() else null
        } catch (e: Exception) {
            Log.w(TAG, "libsu failed: ${e.message}")
            null
        }
    }

    fun writeFileViaTmp(destPath: String, content: String): Boolean {
        val tmpPath = "/data/local/tmp/dux_${System.currentTimeMillis()}.tmp"

        // Write to tmp using printf (handles most special chars)
        val escapedContent = content.replace("\\", "\\\\").replace("'", "'\\''")
        val writeCmd = "printf '%s' '$escapedContent' > '$tmpPath'"
        val cpCmd   = "cp -f '$tmpPath' '$destPath' && chmod 644 '$destPath' && rm -f '$tmpPath'"

        if (runCommand(writeCmd) != null && runCommand(cpCmd) != null) return true

        // Fallback: line-by-line echo
        val sb = StringBuilder()
        content.lines().forEachIndexed { i, line ->
            val esc = line.replace("\\", "\\\\").replace("\"", "\\\"").replace("`", "\\`").replace("$", "\\$")
            if (i == 0) sb.append("printf '%s\\n' \"$esc\" > '$tmpPath'")
            else sb.append(" && printf '%s\\n' \"$esc\" >> '$tmpPath'")
        }
        if (runCommand(sb.toString()) != null && runCommand(cpCmd) != null) return true

        return false
    }

    fun pathExists(path: String): Boolean =
        runCommand("[ -e \"$path\" ] && echo yes || echo no") == "yes"

    fun getPubgDataPath(packageName: String) = "/data/data/$packageName"

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Use Shizuku.newProcess() DIRECTLY — no reflection needed.
     * It's a public static API method in rikka.shizuku:api:13.x
     */
    private fun runViaShizuku(command: String): String? {
        return try {
            val process = Shizuku.newProcess(
                arrayOf("sh", "-c", command),
                null,
                null
            )
            val stdout   = process.inputStream.bufferedReader().readText().trim()
            val stderr   = process.errorStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            if (stderr.isNotEmpty()) Log.d(TAG, "Shizuku stderr: $stderr")
            Log.d(TAG, "Shizuku[$exitCode]: $command")
            if (exitCode == 0) stdout.ifEmpty { "" } else null
        } catch (e: SecurityException) {
            Log.w(TAG, "Shizuku permission denied: ${e.message}")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku error: ${e.message}")
            null
        }
    }
}
