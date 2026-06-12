package com.dex.ultra.booster.pro

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dex.ultra.booster.pro.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isBoosting  = false
    private var overlayActive = false

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
        showAndroidVersionBadge()
    }

    override fun onResume() {
        super.onResume()
        updateShizukuStatus()
    }

    // ── شريط التنقل ─────────────────────────────────────────────────

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home        -> true
                R.id.nav_sensitivity -> { startActivity(Intent(this, SensitivityActivity::class.java)); false }
                R.id.nav_tips        -> { startActivity(Intent(this, TipsActivity::class.java)); false }
                R.id.nav_ping        -> { startActivity(Intent(this, PingOptimizerActivity::class.java)); false }
                else -> false
            }
        }
    }

    // ── شارة إصدار Android ─────────────────────────────────────────

    private fun showAndroidVersionBadge() {
        val api = Build.VERSION.SDK_INT
        val needsShizuku = ShizukuHelper.requiresShizuku()
        val method = if (needsShizuku) "Shizuku مطلوب" else "كتابة مباشرة"
        binding.tvAndroidBadge?.text = "Android $api — $method"
        binding.tvAndroidBadge?.visibility = View.VISIBLE
    }

    // ── زر التعزيز ──────────────────────────────────────────────────

    private fun setupBoostButton() {
        binding.btnBoost.setOnClickListener {
            if (!isBoosting) startBoosting() else stopBoosting()
        }
    }

    private fun startBoosting() {
        isBoosting = true
        binding.btnBoost.text = getString(R.string.boost_active)
        binding.tvStatus.text  = getString(R.string.status_boosting)

        lifecycleScope.launch {
            val steps = listOf(
                "تنظيف الذاكرة المؤقتة…"  to { PerformanceOptimizer.clearSystemCache(applicationContext) },
                "تحسين المعالج…"           to { PerformanceOptimizer.optimizeCpuGovernor() },
                "تحسين الذاكرة…"           to { PerformanceOptimizer.optimizeMemory(applicationContext) },
                "تحسين الشبكة…"            to { /* network */ Unit }
            )
            for ((label, action) in steps) {
                binding.tvStatus.text = label
                action()
                delay(550)
            }
            binding.tvStatus.text = getString(R.string.status_done)
            binding.btnBoost.text = getString(R.string.boost_stop)
        }
    }

    private fun stopBoosting() {
        isBoosting = false
        binding.btnBoost.text = getString(R.string.boost_button)
        binding.tvStatus.text  = getString(R.string.status_ready)
    }

    // ── حقن الملفات ─────────────────────────────────────────────────

    private fun setupFileInjection() {
        binding.spinnerPubgVersion.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            ShizukuHelper.PUBG_VERSION_NAMES.values.toList()
        )

        binding.spinnerPubgVersion.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    updatePathsDisplay(pos)
                }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
            }

        binding.btnInjectFiles.setOnClickListener { injectPubgFiles() }
        binding.btnShowPaths?.setOnClickListener {
            showPathsDialog(binding.spinnerPubgVersion.selectedItemPosition)
        }
    }

    private fun updatePathsDisplay(idx: Int) {
        binding.tvPathsInfo?.text = FileInjector.getPathsInfo(idx)
        binding.tvInjectStatus.text = ""
    }

    private fun showPathsDialog(idx: Int) {
        val info = FileInjector.getPathsInfo(idx)
        AlertDialog.Builder(this)
            .setTitle("مسارات الملفات")
            .setMessage(info)
            .setPositiveButton("نسخ") { _, _ ->
                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("PUBG Paths", info))
                Toast.makeText(this, "تم نسخ المسارات", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun injectPubgFiles() {
        val version     = binding.spinnerPubgVersion.selectedItemPosition
        val versionName = ShizukuHelper.PUBG_VERSION_NAMES[version] ?: ""

        // Android 12+ يحتاج Shizuku
        if (ShizukuHelper.requiresShizuku() && !isShizukuAvailable()) {
            AlertDialog.Builder(this)
                .setTitle("Shizuku مطلوب")
                .setMessage("جهازك يعمل بـ Android ${Build.VERSION.SDK_INT}.\n\n" +
                    "Android 12+ يحمي مجلد Android/data ويتطلب Shizuku للكتابة فيه بدون روت.\n\n" +
                    "1. ثبّت Shizuku من Google Play\n" +
                    "2. فعّله عبر الإعدادات → المطور → تصحيح USB اللاسلكي\n" +
                    "3. افتح Shizuku وابدأ الخدمة")
                .setPositiveButton("فتح الإعدادات") { _, _ ->
                    startActivity(Intent(this, PermissionsActivity::class.java))
                }
                .setNegativeButton("إغلاق", null)
                .show()
            return
        }

        lifecycleScope.launch {
            binding.tvInjectStatus.text = "جاري حقن ملفات $versionName…"
            binding.tvInjectStatus.setTextColor(getColor(R.color.text_secondary))
            binding.btnInjectFiles.isEnabled = false

            val ok = FileInjector.inject120FpsFiles(
                applicationContext, version, isShizukuAvailable()
            )

            binding.btnInjectFiles.isEnabled = true

            if (ok) {
                binding.tvInjectStatus.text = getString(R.string.inject_success)
                binding.tvInjectStatus.setTextColor(getColor(R.color.success))
                Toast.makeText(this@MainActivity,
                    "تم حقن ملفات 120FPS بنجاح!\nأعد تشغيل اللعبة", Toast.LENGTH_LONG).show()
            } else {
                binding.tvInjectStatus.text = getString(R.string.inject_fail)
                binding.tvInjectStatus.setTextColor(getColor(R.color.error))
                showInjectFailDialog()
            }
        }
    }

    private fun showInjectFailDialog() {
        val api = Build.VERSION.SDK_INT
        val msg = if (ShizukuHelper.requiresShizuku()) {
            "Android $api يحتاج Shizuku للكتابة في Android/data\n\n" +
            "1. تأكد من تثبيت Shizuku\n2. تأكد من تشغيل الخدمة\n3. امنح إذن Shizuku"
        } else {
            "Android $api — تأكد من:\n" +
            "1. منح صلاحية الوصول الكامل للملفات\n" +
            "2. تشغيل اللعبة مرة واحدة لإنشاء المجلدات\n" +
            "3. تثبيت اللعبة على الجهاز وليس SD"
        }
        AlertDialog.Builder(this)
            .setTitle("فشل الحقن")
            .setMessage(msg)
            .setPositiveButton("الإعدادات") { _, _ ->
                startActivity(Intent(this, PermissionsActivity::class.java))
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    // ── Overlay ─────────────────────────────────────────────────────

    private fun setupOverlayToggle() {
        binding.switchOverlay.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                if (android.provider.Settings.canDrawOverlays(this)) {
                    startService(Intent(this, OverlayService::class.java))
                    overlayActive = true
                } else {
                    binding.switchOverlay.isChecked = false
                    Toast.makeText(this, "يجب منح إذن العرض فوق التطبيقات", Toast.LENGTH_SHORT).show()
                }
            } else {
                stopService(Intent(this, OverlayService::class.java))
                overlayActive = false
            }
        }
    }

    // ── حالة Shizuku ────────────────────────────────────────────────

    private fun updateShizukuStatus() {
        val connected    = isShizukuAvailable()
        val running      = ShizukuHelper.isRunning()
        val needsShizuku = ShizukuHelper.requiresShizuku()

        val (text, color) = when {
            connected    -> "Shizuku متصل" to R.color.success
            running      -> "Shizuku يعمل — بانتظار الإذن" to R.color.warning
            needsShizuku -> "Shizuku مطلوب للحقن (Android 12+)" to R.color.error
            else         -> "بدون Shizuku (Android 11-)" to R.color.info
        }

        binding.tvShizukuStatus.text = text
        binding.tvShizukuStatus.setTextColor(getColor(color))

        binding.btnConnectShizuku.visibility =
            if (running && !connected) View.VISIBLE else View.GONE
        binding.btnConnectShizuku.setOnClickListener {
            try { Shizuku.requestPermission(1001) }
            catch (e: Exception) { Toast.makeText(this, "خطأ في Shizuku", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) { false }
    }

    // ── إحصائيات الجهاز ─────────────────────────────────────────────

    private fun startStatsUpdate() {
        lifecycleScope.launch {
            while (true) {
                delay(2000)
                binding.tvRamValue.text  = "${DeviceStats.getRamUsagePercent(applicationContext)}%"
                binding.tvTempValue.text = "${DeviceStats.getCpuTemperature()}°C"
            }
        }
    }
}
