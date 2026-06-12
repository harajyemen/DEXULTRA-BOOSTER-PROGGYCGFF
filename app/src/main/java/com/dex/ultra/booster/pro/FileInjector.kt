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
+CVars=0A1E573E35382929353C3A1615160B1B0C1F1F1C0B09181A121C1D1F1516180D
+CVars=0B572C0A1C0B280C1815100D002A1C0D0D10171E44495749
+CVars=0B572C0A1C0B2A11181D160E2A0E100D1A1144495749
+CVars=0B572A11181D160E280C1815100D0044495749
+CVars=0B5734161B10151C3A16170D1C170D2A1A18151C3F181A0D160B44485749
+CVars=0B57292C3B3E3D1C0F101A1C3F292A35160E444F495749
+CVars=0B57292C3B3E3D1C0F101A1C3F292A34101D444F495749
+CVars=0B57292C3B3E3D1C0F101A1C3F292A31101E11444F495749
+CVars=0B57292C3B3E3D1C0F101A1C3F292A313D2B444F495749
+CVars=0B5734161B10151C313D2B44495749
+CVars=3B180A1C290B161F10151C3718141C5718091C0109180F1817
+CVars=0B5734161B10151C572A1A1C171C3A1615160B3F160B14180D444B5749
+CVars=0B572C0A1C0B313D2B2A1C0D0D10171E4448
+CVars=0B57383A3C2A2A0D00151C4448
+CVars=0B573B15161614280C1815100D0044495749
+CVars=0B572C0A1C0B342A38382A1C0D0D10171E4449
+CVars=0B573D1C1F180C150D3F1C180D0C0B1C5738170D10381510180A10171E44495749574B
+CVars=0B5734161B10151C342A3838444957495748
+CVars=0B57342A38383A160C170D444957495748
+CVars=0B572C0A1C0B2F0C151218172A1C0D0D10171E44495749
+CVars=0B5734180D1C0B101815280C1815100D00351C0F1C1544495749574B
+CVars=0B572A11181D160E573418013A2A342B1C0A16150C0D101617444D5749574B
+CVars=0B572A11181D160E573A2A345734180134161B10151C3A180A1A181D1C0A444957495748
+CVars=0B572A11181D160E573D100A0D18171A1C2A1A18151C4449574A5749
+CVars=0B5734161B10151C573D00171814101A361B131C1A0D2A11181D160E444957495748
+CVars=0B573D1C090D11361F3F101C151D280C1815100D0044495749574B
+CVars=0B572B1C1F0B181A0D101617280C1815100D00444957495748
+CVars=0B5734161B10151C573C17181B151C29292B
+CVars=0B572A11181D160E573418013A2A342B1C0A16150C0D101617
+CVars=0B5734180D1C0B101815280C1815100D002A0C091C0B31101E11
+CVars=0B572A0D180D101A341C0A1135363D3D100A0D18171A1C2A1A18151C444957415749
+CVars=1F161510181E1C5735363D3D100A0D18171A1C2A1A18151C444857495749
+CVars=1F161510181E1C5734101735363D44495749
+CVars=0B573D1C0D18101534161D1C44495749
+CVars=0B573418013817100A160D0B16090044485749
+CVars=0B572A0D0B1C181410171E57291616152A10031C44484A4C5749574C49
+CVars=0B573C14100D0D1C0B2A09180E172B180D1C2A1A18151C4449574C
+CVars=0B5729180B0D101A151C35363D3B10180A444B5749
+CVars=0B5734161B10151C370C143D00171814101A291610170D35101E110D0A44495749
+CVars=0B57292C3B3E2F1C0B0A101617444C
+CVars=0B5734161B10151C573C180B15002329180A0A444857495748
+CVars=0B5734161B10151C2A101409151C2A11181D1C0B4449
+CVars=0B5736091C173E35572A0D0B10093C010D1C170A1016170A443E35363C2A1A1614090B1C0A0A1C1D3C2D3A482B3E3B410D1C010D0C0B1C
+CVars=0B5736091C173E35572A0D0B10093C010D1C170A1016170A443E353C212D0D1C010D0C0B1C1A1614090B1C0A0A1016170A4A0D1A
+CVars=0B5736091C173E35572A0D0B10093C010D1C170A1016170A443E353C212D0D1C010D0C0B1C1A1614090B1C0A0A1016171D010D48
+CVars=0B5736091C173E35572A0D0B10093C010D1C170A1016170A443E353C212D0D1C010D0C0B1C1A1614090B1C0A0A1016171D010D48
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
