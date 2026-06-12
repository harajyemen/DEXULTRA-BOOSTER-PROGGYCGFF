package com.dex.ultra.booster.pro

import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.topjohnwu.superuser.Shell
import rikka.shizuku.Shizuku

object ShizukuHelper {

    private const val TAG = "ShizukuHelper"
    const val REQUEST_CODE = 1001

    private var shellInitialized = false

    fun requiresShizuku(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val PUBG_PACKAGES = mapOf(
        0 to "com.tencent.ig",
        1 to "com.pubg.krmobile",
        2 to "com.vng.pubgmobile",
        3 to "com.pubg.imobile",
        4 to "com.tencent.iglite",
        5 to "com.rekoo.pubgm",
        6 to "com.tencent.igJapan",
        7 to "com.tencent.igthailand"
    )

    val PUBG_VERSION_NAMES = mapOf(
        0 to "PUBG Mobile Global",
        1 to "PUBG Mobile KR (كوريا)",
        2 to "PUBG Mobile VN (فيتنام)",
        3 to "Battlegrounds Mobile India (BGMI)",
        4 to "PUBG Mobile LITE",
        5 to "PUBG Mobile TW (تايوان)",
        6 to "PUBG Mobile JP (اليابان)",
        7 to "PUBG Mobile TH (تايلاند)"
    )

    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    fun isRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun requestPermission() {
        try {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                Shizuku.requestPermission(REQUEST_CODE)
        } catch (e: Exception) {
            Log.w(TAG, "requestPermission error: ${e.message}")
        }
    }

    fun runCommand(command: String): String? {
        if (!isAvailable()) {
            Log.e(TAG, "Shizuku not available")
            return null
        }
        return try {
            if (!shellInitialized) {
                Shell.enableVerboseLogging = BuildConfig.DEBUG
                Shell.startShizuku()
                shellInitialized = true
            }
            val result = Shell.cmd(command).exec()
            if (result.isSuccess) {
                result.out.joinToString("\n")
            } else {
                Log.e(TAG, "Command failed: ${result.err.joinToString("\n")}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "runCommand error: ${e.message}")
            null
        }
    }

    fun writeFileViaTmp(destPath: String, content: String): Boolean {
        return try {
            val tmpFile = java.io.File("/data/local/tmp", "tmp_${System.currentTimeMillis()}.txt")
            tmpFile.writeText(content)
            val result = runCommand("mkdir -p ${destPath.substringBeforeLast("/")} && cp -f ${tmpFile.absolutePath} \"$destPath\" && chmod 660 \"$destPath\" && rm -f ${tmpFile.absolutePath}")
            tmpFile.delete()
            result != null
        } catch (e: Exception) {
            false
        }
    }

    fun writeBinaryViaTmp(destPath: String, data: ByteArray): Boolean {
        return try {
            val tmpFile = java.io.File("/data/local/tmp", "tmp_${System.currentTimeMillis()}.bin")
            tmpFile.writeBytes(data)
            val result = runCommand("mkdir -p ${destPath.substringBeforeLast("/")} && cp -f ${tmpFile.absolutePath} \"$destPath\" && chmod 660 \"$destPath\" && rm -f ${tmpFile.absolutePath}")
            tmpFile.delete()
            result != null
        } catch (e: Exception) {
            false
        }
    }

    fun pathExists(path: String): Boolean =
        runCommand("[ -e \"$path\" ] && echo 1 || echo 0") == "1"
}
