package com.dex.ultra.booster.pro

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dex.ultra.booster.pro.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isBoosting = false
    private var overlayActive = false

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            Toast.makeText(this, "✅ تم منح إذن الملفات — يمكنك الآن حقن الملفات", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNav()
        setupBoostButton()
        setupFileInjection()
        setupOverlayToggle()
        updateShizukuStatus()
        startStatsUpdate()
        checkStoragePermission()
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home        -> true
                R.id.nav_sensitivity -> {
                    startActivity(Intent(this, SensitivityActivity::class.java)); false
                }
                R.id.nav_tips        -> {
                    startActivity(Intent(this, TipsActivity::class.java)); false
                }
                R.id.nav_ping        -> {
                    startActivity(Intent(this, PingOptimizerActivity::class.java)); false
                }
                else -> false
            }
        }
    }

    private fun setupBoostButton() {
        binding.btnBoost.setOnClickListener {
            if (!isBoosting) startBoosting() else stopBoosting()
        }
    }

    private fun startBoosting() {
        isBoosting = true
        binding.btnBoost.text = getString(R.string.boost_active)
        binding.tvStatus.text = getString(R.string.status_boosting)

        lifecycleScope.launch {
            binding.tvStatus.text = "🧹 تنظيف الذاكرة المؤقتة…"
            PerformanceOptimizer.clearSystemCache(applicationContext)
            delay(500)

            binding.tvStatus.text = "⚡ تحسين أداء المعالج…"
            PerformanceOptimizer.optimizeCpuGovernor()
            delay(500)

            binding.tvStatus.text = "🧠 تحسين إدارة الذاكرة…"
            PerformanceOptimizer.optimizeMemory(applicationContext)
            delay(500)

            binding.tvStatus.text = "🌐 تحسين الشبكة…"
            delay(500)

            binding.tvStatus.text = getString(R.string.status_done)
            binding.btnBoost.text = getString(R.string.boost_stop)
        }
    }

    private fun stopBoosting() {
        isBoosting = false
        binding.btnBoost.text = getString(R.string.boost_button)
        binding.tvStatus.text = getString(R.string.status_ready)
    }

    private fun setupFileInjection() {
        // PUBG versions
        binding.spinnerPubgVersion.adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                "PUBG Mobile (Global)",
                "PUBG Mobile KR (كوريا)",
                "PUBG Mobile VN (فيتنام)",
                "Battlegrounds Mobile India",
                "PUBG Mobile LITE"
            )
        )

        // FPS options
        binding.spinnerFps.adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("60 FPS", "90 FPS", "120 FPS ⭐", "144 FPS")
        )
        binding.spinnerFps.setSelection(2) // Default 120 FPS

        binding.btnInjectFiles.setOnClickListener { injectPubgFiles() }
    }

    private fun injectPubgFiles() {
        val version = binding.spinnerPubgVersion.selectedItemPosition
        val fps = when (binding.spinnerFps.selectedItemPosition) {
            0 -> 60; 1 -> 90; 2 -> 120; else -> 144
        }

        // Check if MANAGE_EXTERNAL_STORAGE needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            showStoragePermissionDialog()
            return
        }

        lifecycleScope.launch {
            binding.tvInjectStatus.text = "⏳ جاري حقن ملفات ${fps} FPS…"
            binding.btnInjectFiles.isEnabled = false

            val success = FileInjector.inject120FpsFiles(
                applicationContext,
                version,
                isShizukuAvailable(),
                fps
            )

            binding.btnInjectFiles.isEnabled = true

            if (success) {
                binding.tvInjectStatus.text = "✅ تم الحقن بنجاح – ${fps} FPS"
                Toast.makeText(
                    this@MainActivity,
                    "✅ تم حقن ملفات ${fps}FPS بنجاح!\nأعد تشغيل اللعبة",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val outPath = FileInjector.getOutputDirectory(applicationContext)
                binding.tvInjectStatus.text = "⚠️ تم الحفظ في: $outPath\nانسخها يدوياً لمجلد اللعبة"
                Toast.makeText(
                    this@MainActivity,
                    "⚠️ تعذّر الحقن المباشر\nالملفات جاهزة في:\n$outPath",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupOverlayToggle() {
        binding.switchOverlay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Settings.canDrawOverlays(this)) {
                    startService(Intent(this, OverlayService::class.java))
                    overlayActive = true
                } else {
                    binding.switchOverlay.isChecked = false
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName"))
                    startActivity(intent)
                    Toast.makeText(this, "فعّل إذن العرض فوق التطبيقات ثم أعد المحاولة", Toast.LENGTH_SHORT).show()
                }
            } else {
                stopService(Intent(this, OverlayService::class.java))
                overlayActive = false
            }
        }
    }

    private fun updateShizukuStatus() {
        val connected = isShizukuAvailable()
        binding.tvShizukuStatus.text = if (connected) "✅ Shizuku" else "⚠️ Shizuku"
        binding.tvShizukuStatus.setTextColor(
            getColor(if (connected) R.color.success else R.color.warning)
        )

        if (!connected) {
            binding.btnConnectShizuku.visibility = android.view.View.VISIBLE
            binding.btnConnectShizuku.setOnClickListener {
                try {
                    Shizuku.requestPermission(1001)
                } catch (e: Exception) {
                    Toast.makeText(this, "Shizuku غير مثبّت — حمّله من المتجر", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            binding.btnConnectShizuku.visibility = android.view.View.GONE
        }
    }

    private fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) { false }
    }

    private fun startStatsUpdate() {
        lifecycleScope.launch {
            while (true) {
                delay(2000)
                binding.tvRamValue.text = "${DeviceStats.getRamUsagePercent(applicationContext)}%"
                binding.tvTempValue.text = "${DeviceStats.getCpuTemperature()}°C"
            }
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            // Show non-blocking hint — don't force dialog on startup
            binding.tvInjectStatus.text = "💡 اضغط 'حقن' لطلب إذن الوصول للملفات"
        }
    }

    private fun showStoragePermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("إذن الوصول للملفات")
            .setMessage(
                "لحقن ملفات ببجي مباشرةً، يحتاج التطبيق إذن 'إدارة جميع الملفات'.\n\n" +
                "في الصفحة التالية، فعّل الإذن لـ DexUltra Booster Pro."
            )
            .setPositiveButton("منح الإذن") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    manageStorageLauncher.launch(intent)
                }
            }
            .setNegativeButton("لاحقاً") { _, _ ->
                Toast.makeText(this, "يمكنك الحقن بدون Shizuku إذا منحت الإذن لاحقاً", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
