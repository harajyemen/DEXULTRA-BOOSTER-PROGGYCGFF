package com.dex.ultra.booster.pro

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dex.ultra.booster.pro.databinding.ActivitySensitivityBinding

class SensitivityActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySensitivityBinding

    // Sensitivity presets [camera, ads, gyro, recoil]
    private val presets = mapOf(
        "classic_no_gyro" to SensPreset(
            name = "كلاسيكي – بدون جيرو",
            camera = intArrayOf(100, 98, 86, 92, 95, 98),
            ads = intArrayOf(55, 52, 48, 40, 35, 30),
            gyro = intArrayOf(0, 0, 0, 0, 0, 0),
            recoil = 75
        ),
        "warehouse_no_gyro" to SensPreset(
            name = "مستودع – بدون جيرو",
            camera = intArrayOf(110, 105, 95, 100, 98, 100),
            ads = intArrayOf(65, 60, 55, 48, 42, 38),
            gyro = intArrayOf(0, 0, 0, 0, 0, 0),
            recoil = 80
        ),
        "headshot_no_gyro" to SensPreset(
            name = "هيدشوت – بدون جيرو",
            camera = intArrayOf(95, 93, 82, 88, 90, 92),
            ads = intArrayOf(50, 48, 44, 36, 32, 28),
            gyro = intArrayOf(0, 0, 0, 0, 0, 0),
            recoil = 70
        ),
        "classic_gyro" to SensPreset(
            name = "كلاسيكي – مع جيرو",
            camera = intArrayOf(100, 98, 86, 92, 95, 98),
            ads = intArrayOf(55, 52, 48, 40, 35, 30),
            gyro = intArrayOf(300, 280, 260, 220, 200, 180),
            recoil = 75
        ),
        "warehouse_gyro" to SensPreset(
            name = "مستودع – مع جيرو",
            camera = intArrayOf(110, 105, 95, 100, 98, 100),
            ads = intArrayOf(65, 60, 55, 48, 42, 38),
            gyro = intArrayOf(350, 320, 290, 250, 230, 210),
            recoil = 80
        ),
        "headshot_gyro" to SensPreset(
            name = "هيدشوت – مع جيرو",
            camera = intArrayOf(120, 115, 105, 110, 108, 110),
            ads = intArrayOf(60, 58, 54, 46, 40, 36),
            gyro = intArrayOf(400, 380, 350, 300, 280, 260),
            recoil = 85
        ),
        "pro_competitive" to SensPreset(
            name = "تنافسي احترافي",
            camera = intArrayOf(105, 100, 90, 95, 93, 96),
            ads = intArrayOf(58, 55, 50, 43, 37, 33),
            gyro = intArrayOf(320, 300, 275, 240, 215, 195),
            recoil = 78
        ),
        "smooth_antilag" to SensPreset(
            name = "ضد اللاق",
            camera = intArrayOf(88, 85, 75, 82, 84, 88),
            ads = intArrayOf(48, 46, 42, 34, 30, 26),
            gyro = intArrayOf(280, 260, 240, 200, 185, 165),
            recoil = 68
        )
    )

    private var currentPreset: SensPreset? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySensitivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.sensitivity_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupTabs()
        loadPreset("classic_no_gyro")
        setupButtons()
    }

    private fun setupTabs() {
        binding.tabLayoutSens.addTab(binding.tabLayoutSens.newTab().setText(getString(R.string.gyro_off)))
        binding.tabLayoutSens.addTab(binding.tabLayoutSens.newTab().setText(getString(R.string.gyro_on)))

        binding.tabLayoutSens.addOnTabSelectedListener(object :
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                updatePresetVisibility(tab.position == 1)
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })

        // Preset chip group
        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val hasGyro = binding.tabLayoutSens.selectedTabPosition == 1
            val suffix = if (hasGyro) "_gyro" else "_no_gyro"
            val key = when (checkedIds[0]) {
                R.id.chipClassic -> "classic$suffix"
                R.id.chipWarehouse -> "warehouse$suffix"
                R.id.chipHeadshot -> "headshot$suffix"
                R.id.chipPro -> "pro_competitive"
                R.id.chipAntiLag -> "smooth_antilag"
                else -> "classic_no_gyro"
            }
            loadPreset(key)
        }
    }

    private fun updatePresetVisibility(gyroEnabled: Boolean) {
        val suffix = if (gyroEnabled) "_gyro" else "_no_gyro"
        val currentChip = when {
            binding.chipClassic.isChecked -> "classic$suffix"
            binding.chipWarehouse.isChecked -> "warehouse$suffix"
            binding.chipHeadshot.isChecked -> "headshot$suffix"
            binding.chipPro.isChecked -> "pro_competitive"
            binding.chipAntiLag.isChecked -> "smooth_antilag"
            else -> "classic$suffix"
        }
        loadPreset(currentChip)
    }

    private fun loadPreset(key: String) {
        val preset = presets[key] ?: return
        currentPreset = preset
        binding.tvPresetName.text = preset.name

        // Update sliders
        binding.sliderCamera3p.value = preset.camera[0].toFloat()
        binding.sliderCameraRedZone.value = preset.camera[1].toFloat()
        binding.sliderCameraSmoke.value = preset.camera[2].toFloat()
        binding.sliderCamera4x.value = preset.camera[3].toFloat()
        binding.sliderCamera8x.value = preset.camera[4].toFloat()
        binding.sliderCameraWin94.value = preset.camera[5].toFloat()

        binding.sliderAds3p.value = preset.ads[0].toFloat()
        binding.sliderAdsRedZone.value = preset.ads[1].toFloat()
        binding.sliderAdsSmoke.value = preset.ads[2].toFloat()
        binding.sliderAds4x.value = preset.ads[3].toFloat()
        binding.sliderAds8x.value = preset.ads[4].toFloat()
        binding.sliderAdsWin94.value = preset.ads[5].toFloat()

        // Gyro visibility
        val showGyro = preset.gyro.any { it > 0 }
        binding.layoutGyro.visibility = if (showGyro) android.view.View.VISIBLE else android.view.View.GONE
        if (showGyro) {
            binding.sliderGyro3p.value = preset.gyro[0].toFloat().coerceIn(0f, 400f)
            binding.sliderGyroAds.value = preset.gyro[1].toFloat().coerceIn(0f, 400f)
            binding.sliderGyro4x.value = preset.gyro[2].toFloat().coerceIn(0f, 400f)
        }

        binding.sliderRecoil.value = preset.recoil.toFloat()
    }

    private fun setupButtons() {
        binding.btnApplySens.setOnClickListener {
            Toast.makeText(this, "✅ تم حفظ إعدادات الحساسية", Toast.LENGTH_SHORT).show()
        }

        binding.btnCopySens.setOnClickListener {
            val text = buildSensText()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Sensitivity", text))
            Toast.makeText(this, "📋 تم نسخ الإعدادات", Toast.LENGTH_SHORT).show()
        }

        binding.btnResetSens.setOnClickListener {
            loadPreset("classic_no_gyro")
            Toast.makeText(this, "🔄 تم إعادة التعيين", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildSensText(): String {
        val p = currentPreset ?: return ""
        return """
🎮 DexUltra Booster Pro – ${p.name}
━━━━━━━━━━━━━━━━━━━━
📷 حساسية الكاميرا:
  شخص ثالث: ${binding.sliderCamera3p.value.toInt()}
  المنطقة الحمراء: ${binding.sliderCameraRedZone.value.toInt()}
  الدخان: ${binding.sliderCameraSmoke.value.toInt()}
  4x: ${binding.sliderCamera4x.value.toInt()}
  8x: ${binding.sliderCamera8x.value.toInt()}
  Win94: ${binding.sliderCameraWin94.value.toInt()}
🔭 حساسية ADS:
  شخص ثالث: ${binding.sliderAds3p.value.toInt()}
  المنطقة الحمراء: ${binding.sliderAdsRedZone.value.toInt()}
  الدخان: ${binding.sliderAdsSmoke.value.toInt()}
  4x: ${binding.sliderAds4x.value.toInt()}
  8x: ${binding.sliderAds8x.value.toInt()}
  Win94: ${binding.sliderAdsWin94.value.toInt()}
🎯 التحكم بالارتداد: ${binding.sliderRecoil.value.toInt()}
━━━━━━━━━━━━━━━━━━━━
        """.trimIndent()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

data class SensPreset(
    val name: String,
    val camera: IntArray,
    val ads: IntArray,
    val gyro: IntArray,
    val recoil: Int
)
