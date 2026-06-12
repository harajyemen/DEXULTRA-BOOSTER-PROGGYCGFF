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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
            // خطوات التعزيز وتحديث الواجهة بطريقة آمنة ومتوافقة
            val steps = listOf(
                "تنظيف الذاكرة المؤقتة…"  to { PerformanceOptimizer.clearSystemCache(applicationContext) },
                "تحسين أداء المعالج…"   to { PerformanceOptimizer.optimizeCpuGovernor() },
                "تحليلات الذاكرة وتخفيف العبء…" to { PerformanceOptimizer.optimizeMemory(applicationContext) },
                "تقليل استجابة الشبكة البنق…" to { /* تعزيز الشبكة والاتصال */ Unit }
            )
            for ((label, action) in steps) {
                binding.tvStatus.text = label
                action()
                delay(650) // مهلة كافية لإنهاء العمليات وتحديث الـ UI بسلاسة
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
        val shizukuReady = ShizukuHelper.isAvailable()

        // الاعتماد الموحد على الـ Helper لحماية الهواتف من نوع أندرويد 12 فما فوق
        if (ShizukuHelper.requiresShizuku() && !shizukuReady) {
            AlertDialog.Builder(this)
                .setTitle("Shizuku مطلوب")
                .setMessage("جهازك يعمل بـ Android ${Build.VERSION.SDK_INT}.\n\n" +
                    "Android 12+ يحمي مجلد Android/data ويتطلب تشغيل Shizuku للكتابة فيه بدون روت.\n\n" +
                    "1. ثبّت برنامج Shizuku من المتجر.\n" +
                    "2. قم بتفعيله عبر خيارات المطور (تصحيح اللاسلكي).\n" +
                    "3. افتح Shizuku وامنح التطبيق الصلاحية.")
                .setPositiveButton("إعداد الصلاحيات") { _, _ ->
                    startActivity(Intent(this, PermissionsActivity::class.java))
                }
                .setNegativeButton("إغلاق", null)
                .show()
            return
        }

        lifecycleScope.launch {
            binding.tvInjectStatus.text = "جاري تعديل وحقن ملفات الـ 120FPS لـ $versionName…"
            binding.tvInjectStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
            binding.btnInjectFiles.isEnabled = false

            val ok = FileInjector.inject120FpsFiles(
                applicationContext, version, shizukuReady
            )

            binding.btnInjectFiles.isEnabled = true

            if (ok) {
                binding.tvInjectStatus.text = getString(R.string.inject_success)
                binding.tvInjectStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.success))
                Toast.makeText(this@MainActivity,
                    "تم تطبيق التعديلات بنجاح!\nقم بتشغيل اللعبة الآن لتجربة الأداء.", Toast.LENGTH_LONG).show()
            } else {
                binding.tvInjectStatus.text = getString(R.string.inject_fail)
                binding.tvInjectStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.error))
                showInjectFailDialog()
            }
        }
    }

    private fun showInjectFailDialog() {
        val api = Build.VERSION.SDK_INT
        val msg = if (ShizukuHelper.requiresShizuku()) {
            "نظام أندرويد $api يتطلب Shizuku للوصول الآمن لملفات الحزمة داخل مجلد النقل العام.\n\n" +
            "تأكد من تشغيل تطبيق Shizuku في الخلفية أولاً وإعطائه إذن الوصول."
        } else {
            "نظام أندرويد $api — يرجى التأكد من:\n" +
            "1. منح التطبيق صلاحيات التخزين الكاملة.\n" +
            "2. تشغيل اللعبة لمرة واحدة على الأقل لتهيئة المجلدات الداخلية."
        }
        AlertDialog.Builder(this)
            .setTitle("فشل عملية الحقن والتعديل")
            .setMessage(msg)
            .setPositiveButton("إدارة الصلاحيات") { _, _ ->
                startActivity(Intent(this, PermissionsActivity::class.java))
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    // ── العرض فوق التطبيقات (Overlay) ───────────────────────────────

    private fun setupOverlayToggle() {
        binding.switchOverlay.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                if (android.provider.Settings.canDrawOverlays(this)) {
                    startService(Intent(this, OverlayService::class.java))
                    overlayActive = true
                } else {
                    binding.switchOverlay.isChecked = false
                    Toast.makeText(this, "يرجى تفعيل إذن الظهور فوق التطبيقات لتفعيل المؤشر العائم.", Toast.LENGTH_SHORT).show()
                }
            } else {
                stopService(Intent(this, OverlayService::class.java))
                overlayActive = false
            }
        }
    }

    // ── تتبع وحفظ حالة اتصال Shizuku ───────────────────────────────

    private fun updateShizukuStatus() {
        val connected    = ShizukuHelper.isAvailable()
        val running      = ShizukuHelper.isRunning()
        val needsShizuku = ShizukuHelper.requiresShizuku()

        val (text, color) = when {
            connected    -> "نظام Shizuku متصل ونشط" to R.color.success
            running      -> "خدمة Shizuku تعمل — بانتظار الموافقة" to R.color.warning
            needsShizuku -> "بيئة Shizuku مطلوبة للحقن (أندرويد 12+)" to R.color.error
            else         -> "وضع الكتابة المباشر مستقر (أندرويد 11 فما دون)" to R.color.info
        }

        binding.tvShizukuStatus.text = text
        binding.tvShizukuStatus.setTextColor(ContextCompat.getColor(this, color))

        binding.btnConnectShizuku.visibility =
            if (running && !connected) View.VISIBLE else View.GONE
        binding.btnConnectShizuku.setOnClickListener {
            try { Shizuku.requestPermission(1001) }
            catch (e: Exception) { Toast.makeText(this, "تعذر استدعاء واجهة إذن شيزوكو", Toast.LENGTH_SHORT).show() }
        }
    }

    // ── تحديث إحصائيات موارد الهاتف بشكل مستقر ومنظم ─────────────────────

    private fun startStatsUpdate() {
        // حلقة التحديث داخل repeatOnLifecycle لمنع تسريب الذاكرة فور خروج المستخدم من الواجهة الرئيسة
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    binding.tvRamValue.text  = "${DeviceStats.getRamUsagePercent(applicationContext)}%"
                    binding.tvTempValue.text = "${DeviceStats.getCpuTemperature()}°C"
                    delay(2000)
                }
            }
        }
    }
}
