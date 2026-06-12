package com.dex.ultra.booster.pro

import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.topjohnwu.superuser.Shell
import rikka.shizuku.Shizuku

object ShizukuHelper {

    private const val TAG = "ShizukuHelper"
    const val REQUEST_CODE = 1001

    /**
     * هل الجهاز يحتاج Shizuku؟ — Android 12 (API 31) وفوق
     */
    fun requiresShizuku(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /**
     * جميع نسخ PUBG Mobile وأسماء حزمها
     * 0 = Global   | 1 = KR    | 2 = VN    | 3 = BGMI
     * 4 = Lite     | 5 = TW    | 6 = JP    | 7 = TH
     */
    val PUBG_PACKAGES = mapOf(
        0 to "com.tencent.ig",         // PUBG Mobile Global — النسخة العالمية
        1 to "com.pubg.krmobile",      // PUBG Mobile KR — كوريا
        2 to "com.vng.pubgmobile",     // PUBG Mobile VN — فيتنام
        3 to "com.pubg.imobile",       // Battlegrounds Mobile India (BGMI)
        4 to "com.tencent.iglite",     // PUBG Mobile LITE — لايت
        5 to "com.rekoo.pubgm",        // PUBG Mobile TW — تايوان
        6 to "com.tencent.igJapan",    // PUBG Mobile JP — اليابان
        7 to "com.tencent.igthailand"  // PUBG Mobile TH — تايلاند
    )

    val PUBG_VERSION_NAMES = mapOf(
        0 to "PUBG Mobile Global (النسخة العالمية)",
        1 to "PUBG Mobile KR (كوريا)",
        2 to "PUBG Mobile VN (فيتنام)",
        3 to "Battlegrounds Mobile India (BGMI)",
        4 to "PUBG Mobile LITE (لايت)",
        5 to "PUBG Mobile TW (تايوان)",
        6 to "PUBG Mobile JP (اليابان)",
        7 to "PUBG Mobile TH (تايلاند)"
    )

    /** هل Shizuku متاح ومُصرَّح له؟ */
    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku not available: ${e.message}")
            false
        }
    }

    /** هل خدمة Shizuku تعمل (حتى بدون إذن)؟ */
    fun isRunning(): Boolean {
        return try { Shizuku.pingBinder() } catch (e: Exception) { false }
    }

    /** طلب إذن Shizuku */
    fun requestPermission() {
        try {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                Shizuku.requestPermission(REQUEST_CODE)
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku requestPermission error: ${e.message}")
        }
    }

    /** تشغيل أمر shell بصلاحيات مرتفعة */
    fun runCommand(command: String): String? {
        return try {
            val result = Shell.cmd(command).exec()
            if (result.isSuccess) result.out.joinToString("\n").trim()
            else { Log.e(TAG, "Cmd failed: ${result.err.joinToString("\n")}"); null }
        } catch (e: Exception) {
            Log.e(TAG, "Shell error: ${e.message}"); null
        }
    }

    /** كتابة نص إلى مسار محمي عبر ملف مؤقت */
    fun writeFileViaTmp(destPath: String, content: String): Boolean {
        val tmp = "/data/local/tmp/dexultra_${System.currentTimeMillis()}"
        return try {
            java.io.File(tmp).writeText(content)
            val parentDir = destPath.substringBeforeLast("/")
            runCommand(
                "mkdir -p \"$parentDir\" && " +
                "cp -f \"$tmp\" \"$destPath\" && " +
                "chmod 660 \"$destPath\" && rm -f \"$tmp\""
            ) != null
        } catch (e: Exception) {
            Log.e(TAG, "writeFileViaTmp failed: ${e.message}"); false
        }
    }

    /** كتابة بيانات ثنائية إلى مسار محمي */
    fun writeBinaryViaTmp(destPath: String, data: ByteArray): Boolean {
        val tmp = "/data/local/tmp/dexultra_${System.currentTimeMillis()}.bin"
        return try {
            java.io.File(tmp).writeBytes(data)
            val parentDir = destPath.substringBeforeLast("/")
            runCommand(
                "mkdir -p \"$parentDir\" && " +
                "cp -f \"$tmp\" \"$destPath\" && " +
                "chmod 660 \"$destPath\" && rm -f \"$tmp\""
            ) != null
        } catch (e: Exception) {
            Log.e(TAG, "writeBinaryViaTmp failed: ${e.message}"); false
        }
    }

    /** هل المسار موجود؟ */
    fun pathExists(path: String): Boolean =
        runCommand("[ -e \"$path\" ] && echo 1 || echo 0") == "1"

    /** المسار الخارجي لملفات اللعبة */
    fun getExternalSavedPath(pkg: String) =
        "/storage/emulated/0/Android/data/$pkg" +
        "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved"
}
