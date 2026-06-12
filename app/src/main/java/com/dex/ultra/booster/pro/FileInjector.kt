package com.dex.ultra.booster.pro

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FileInjector {

    private const val TAG = "FileInjector"

    data class PubgPaths(
        val userCustomIni: String,
        val activeSav: String,
        val packageName: String
    )

    fun getPubgPaths(packageName: String): PubgPaths {
        val saved = "/sdcard/Android/data/$packageName/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved"
        return PubgPaths(
            userCustomIni = "$saved/Config/Android/UserCustom.ini",
            activeSav     = "$saved/SaveGames/Active.sav",
            packageName   = packageName
        )
    }

    suspend fun inject120FpsFiles(
        context: Context,
        versionIndex: Int,
        useShizuku: Boolean
    ): Boolean = withContext(Dispatchers.IO) {

        val pkg   = ShizukuHelper.PUBG_PACKAGES[versionIndex] ?: return@withContext false
        val paths = getPubgPaths(pkg)

        Log.i(TAG, "Injecting for $pkg (API ${Build.VERSION.SDK_INT})")

        val iniContent = getUserCustomIniContent()
        val iniBytes = iniContent.toByteArray(Charsets.UTF_8)
        val savBytes = loadActiveSavFromAssets(context) ?: generateActiveSavFallback()

        var iniOk = false
        var savOk = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!ShizukuHelper.isAvailable() && useShizuku) {
                Log.e(TAG, "Android 12+ requires Shizuku")
                return@withContext false
            }

            val configParent = paths.userCustomIni.substringBeforeLast("/")
            val saveParent = paths.activeSav.substringBeforeLast("/")
            ShizukuHelper.runCommand("mkdir -p \"$configParent\"")
            ShizukuHelper.runCommand("mkdir -p \"$saveParent\"")

            val tmpIniFile = File(context.cacheDir, "tmp_UserCustom.ini")
            val tmpSavFile = File(context.cacheDir, "tmp_Active.sav")
            tmpIniFile.writeBytes(iniBytes)
            tmpSavFile.writeBytes(savBytes)

            val iniResult = ShizukuHelper.runCommand("cp -f \"${tmpIniFile.absolutePath}\" \"${paths.userCustomIni}\" && chmod 660 \"${paths.userCustomIni}\"")
            iniOk = iniResult != null
            val savResult = ShizukuHelper.runCommand("cp -f \"${tmpSavFile.absolutePath}\" \"${paths.activeSav}\" && chmod 660 \"${paths.activeSav}\"")
            savOk = savResult != null

            tmpIniFile.delete()
            tmpSavFile.delete()
        } else {
            iniOk = writeExternal(paths.userCustomIni, iniBytes)
            savOk = writeExternal(paths.activeSav, savBytes)
        }

        Log.i(TAG, "Result: ini=$iniOk sav=$savOk")
        return@withContext iniOk && savOk
    }

    private fun writeExternal(destPath: String, data: ByteArray): Boolean {
        return try {
            val file = File(destPath)
            file.parentFile?.mkdirs()
            file.writeBytes(data)
            true
        } catch (e: Exception) {
            Log.e(TAG, "writeExternal failed: ${e.message}")
            false
        }
    }

    private fun loadActiveSavFromAssets(context: Context): ByteArray? {
        return try {
            context.assets.open("Active.sav").use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    fun getUserCustomIniContent(): String = """
[UserCustom DeviceProfile]
+CVars=0A1E573E35382929353C3A1510093D100A0D18171A1C
... (المحتوى الطويل الذي لديك) ...
    """.trimIndent()

    fun generateActiveSavFallback(): ByteArray {
        val rawText = """
[Device]
DeviceModel=Samsung Galaxy S24 Ultra
Manufacturer=Samsung
DeviceID=SM-S928B
SupportFrameRate120=1
SupportFrameRate90=1
SupportFrameRate60=1
MaxSupportedFrameRate=120
DevicePerformanceLevel=5
GPU=Adreno 750
CPU=Snapdragon 8 Gen 3
RAM=12288
ScreenWidth=3088
ScreenHeight=1440
DPI=505
[Settings]
FrameRate=120
GraphicsLevel=Ultra
HDR=1
AntiAliasing=0
Shadows=0
Effects=1
FoliageOff=1
AutoFPS=0
        """.trimIndent()
        return rawText.toByteArray(Charsets.UTF_8)
    }

    fun getPathsInfo(versionIndex: Int): String {
        val pkg = ShizukuHelper.PUBG_PACKAGES[versionIndex] ?: return ""
        val p   = getPubgPaths(pkg)
        val api = Build.VERSION.SDK_INT
        val method = if (api >= Build.VERSION_CODES.S) "Shizuku (Android 12+)" else "Direct write"
        return "Method: $method\n\nUserCustom.ini:\n${p.userCustomIni}\n\nActive.sav:\n${p.activeSav}"
    }
}
