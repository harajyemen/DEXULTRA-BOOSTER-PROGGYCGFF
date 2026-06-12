package com.dex.ultra.booster.pro

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FileInjector {

    private const val TAG = "FileInjector"

    /**
     * Inject UserCustom.ini + Active.sav into the selected PUBG version.
     * Strategy order:
     *   1. Direct write to /sdcard/Android/data/<pkg>/files/ (works on most devices)
     *   2. MANAGE_EXTERNAL_STORAGE path (Android 11+)
     *   3. Shizuku (ADB-level, no root required)
     *   4. libsu root fallback
     *
     * @param versionIndex  0=Global,1=KR,2=VN,3=BGMI,4=Lite
     * @param targetFps     60 | 90 | 120 | 144
     */
    suspend fun inject120FpsFiles(
        context: Context,
        versionIndex: Int,
        useShizuku: Boolean,
        targetFps: Int = 120
    ): Boolean = withContext(Dispatchers.IO) {

        val pkg = ShizukuHelper.PUBG_PACKAGES[versionIndex] ?: return@withContext false
        val iniContent = generateUserCustomIni(targetFps)
        val savContent = generateActiveSav(targetFps)

        // ── Strategy 1: write directly to sdcard Android/data path ─────────
        val sdcard = Environment.getExternalStorageDirectory().absolutePath
        val sdcardDir = "$sdcard/Android/data/$pkg/files"
        val sdcardResult = writeSdcard(sdcardDir, iniContent, savContent)
        if (sdcardResult) {
            Log.i(TAG, "✅ Injected via sdcard: $sdcardDir")
            return@withContext true
        }

        // ── Strategy 2: Shizuku (ADB-level) ──────────────────────────────
        if (useShizuku && ShizukuHelper.isAvailable()) {
            val dataDir = "/data/data/$pkg/files"
            // Create target directory first (ADB-level has permission)
            ShizukuHelper.runCommand("mkdir -p '$dataDir' 2>/dev/null; true")
            ShizukuHelper.runCommand("chmod 771 '$dataDir' 2>/dev/null; true")
            val r1 = ShizukuHelper.writeFileViaTmp("$dataDir/UserCustom.ini", iniContent)
            val r2 = ShizukuHelper.writeFileViaTmp("$dataDir/Active.sav", savContent)
            if (r1 && r2) {
                ShizukuHelper.runCommand("chown $pkg:$pkg '$dataDir/UserCustom.ini' '$dataDir/Active.sav' 2>/dev/null; true")
                Log.i(TAG, "✅ Injected via Shizuku to $dataDir")
                return@withContext true
            }
            // Also try PUBG config path (UE4 style)
            val cfgDir = "/sdcard/Android/data/$pkg/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android"
            ShizukuHelper.runCommand("mkdir -p '$cfgDir' 2>/dev/null; true")
            val c1 = ShizukuHelper.writeFileViaTmp("$cfgDir/UserCustom.ini", iniContent)
            if (c1) {
                Log.i(TAG, "✅ Injected via Shizuku (UE4 path) to $cfgDir")
                return@withContext true
            }
        }

        // ── Strategy 3: write to app external files (manual copy fallback) ─
        val fallbackDir = context.getExternalFilesDir("DexUltra_Output")
        if (fallbackDir != null) {
            fallbackDir.mkdirs()
            try {
                File(fallbackDir, "UserCustom.ini").writeText(iniContent)
                File(fallbackDir, "Active.sav").writeText(savContent)
                Log.i(TAG, "⚠️ Files saved to ${fallbackDir.absolutePath} — copy manually if needed")
            } catch (e: Exception) {
                Log.w(TAG, "Fallback write failed: ${e.message}")
            }
        }

        return@withContext false
    }

    private fun writeSdcard(dir: String, iniContent: String, savContent: String): Boolean {
        return try {
            // Check if already accessible (pre-Android 11, or MANAGE_EXTERNAL_STORAGE granted)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) return false
            }
            val d = File(dir)
            if (!d.exists()) d.mkdirs()
            if (!d.canWrite()) return false
            File(d, "UserCustom.ini").writeText(iniContent)
            File(d, "Active.sav").writeText(savContent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "sdcard write failed: ${e.message}")
            false
        }
    }

    fun getOutputDirectory(context: Context): String =
        context.getExternalFilesDir("DexUltra_Output")?.absolutePath ?: ""

    // ── Content generators ────────────────────────────────────────────────

    fun generateUserCustomIni(fps: Int = 120): String {
        val fpsValue = fps.coerceIn(30, 144)
        val frameSetting = when {
            fpsValue >= 120 -> 4
            fpsValue >= 90  -> 3
            fpsValue >= 60  -> 2
            else            -> 1
        }
        return """
[UserCustom]
IsHighFrameRateVersion=${fpsValue >= 90}
FrameRateSetting=$frameSetting
FrameRateSettingEx=$frameSetting
DisplayRefreshRate=$fpsValue
TargetFrameRate=$fpsValue
CustomFrameRate=$fpsValue
bIsHighFrameRate=${fpsValue >= 90}
bIsHighFrameRateAllow=True
IsForceUseCustomSettings=True
IsAutoQualityOpen=False
IsGraphicStyleOpen=False
ShadowQuality=0
AntiAliasingQuality=0
TextureQuality=3
EffectsQuality=1
FoliageQuality=0
PostProcessQuality=0
ViewDistanceQuality=3
GraphicStyle=0
ColorTemperature=0
IsHighPrecisionShadow=False
IsRealTimeShadow=False
IsVehicleReflection=False
IsUseHDRForRenderingTarget=False
IsUseEnvCapture=False
IsAllowHiResScreenshot=False
IsHdrScreenshot=False
IsAntiAliasing=False
IsBloom=False
IsMotionBlur=False
IsLensFlare=False
RagdollQuality=0
FurQuality=0
GrassQuality=0
FoliageShadows=0
DynamicAO=0
ScreenParticleQuality=0
ExplosionQuality=0
IsScreenSpaceReflection=False
GFXLevel=Ultra
GraphicsPreset=5
        """.trimIndent()
    }

    private fun generateActiveSav(fps: Int = 120): String {
        return """
[Device]
DeviceModel=Samsung Galaxy S24 Ultra
Manufacturer=Samsung
DeviceID=SM-S928B
SupportFrameRate120=${if (fps >= 120) 1 else 0}
SupportFrameRate90=${if (fps >= 90) 1 else 0}
SupportFrameRate60=1
MaxSupportedFrameRate=$fps
DevicePerformanceLevel=4
GPU=Adreno 750
CPU=Snapdragon 8 Gen 3
RAM=12288
ScreenWidth=3088
ScreenHeight=1440
DPI=500
[Settings]
FrameRate=$fps
GraphicsLevel=Ultra
HDR=1
AntiAliasing=0
Shadows=0
Effects=1
FoliageOff=1
AutoFPS=0
        """.trimIndent()
    }
}
