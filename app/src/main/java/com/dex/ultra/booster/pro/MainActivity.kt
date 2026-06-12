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
    private var isBoosting = false
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

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_sensitivity -> {
                    startActivity(Intent(this, SensitivityActivity::class.java))
                    false
                }
                R.id.nav_tips -> {
                    startActivity(Intent(this, TipsActivity::class.java))
                    false
                }
                R.id.nav_ping -> {
                    startActivity(Intent(this, PingOptimizerActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun showAndroidVersionBadge() {
        val api = Build.VERSION.SDK_INT
        val needsShizuku = ShizukuHelper.requiresShizuku()
        val method = if (needsShizuku) "Shizuku required" else "Direct write"
        binding.tvAndroidBadge.text = "Android $api — $method"
        binding.tvAndroidBadge.visibility = View.VISIBLE
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
            binding.tvStatus.text = "Cleaning memory..."
            PerformanceOptimizer.clearSystemCache(applicationContext)
            delay(650)

            binding.tvStatus.text = "Optimizing CPU..."
            PerformanceOptimizer.optimizeCpuGovernor()
            delay(650)

            binding.tvStatus.text = "Checking memory..."
            PerformanceOptimizer.optimizeMemory(applicationContext)
            delay(650)

            binding.tvStatus.text = "Optimizing network..."
            PerformanceOptimizer.optimizeNetwork() // داخل coroutine
            delay(650)

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
            .setTitle("File Paths")
            .setMessage(info)
            .setPositiveButton("Copy") { _, _ ->
                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("PUBG Paths", info))
                Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun injectPubgFiles() {
        val version = binding.spinnerPubgVersion.selectedItemPosition
        val versionName = ShizukuHelper.PUBG_VERSION_NAMES[version] ?: ""
        val shizukuReady = ShizukuHelper.isAvailable()

        if (ShizukuHelper.requiresShizuku() && !shizukuReady) {
            AlertDialog.Builder(this)
                .setTitle("Shizuku Required")
                .setMessage("Your device runs Android ${Build.VERSION.SDK_INT}.\n\n" +
                    "Android 12+ protects Android/data folder. You need to enable Shizuku.\n\n" +
                    "1. Install Shizuku from Play Store.\n" +
                    "2. Enable it via Developer Options (Wireless debugging).\n" +
                    "3. Open Shizuku and grant permission to this app.")
                .setPositiveButton("Grant Permissions") { _, _ ->
                    startActivity(Intent(this, PermissionsActivity::class.java))
                }
                .setNegativeButton("Close", null)
                .show()
            return
        }

        lifecycleScope.launch {
            binding.tvInjectStatus.text = "Injecting 120FPS files for $versionName..."
            binding.tvInjectStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
            binding.btnInjectFiles.isEnabled = false

            val ok = FileInjector.inject120FpsFiles(
                applicationContext, version, shizukuReady
            )

            binding.btnInjectFiles.isEnabled = true

            if (ok) {
                binding.tvInjectStatus.text = getString(R.string.inject_success)
                binding.tvInjectStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.success))
                Toast.makeText(this@MainActivity, "Injected successfully! Launch PUBG now.", Toast.LENGTH_LONG).show()
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
            "Android $api requires Shizuku to access protected folders.\n\nMake sure Shizuku is running and permission granted."
        } else {
            "Android $api — Please check:\n1. Storage permission granted.\n2. Game launched at least once."
        }
        AlertDialog.Builder(this)
            .setTitle("Injection Failed")
            .setMessage(msg)
            .setPositiveButton("Manage Permissions") { _, _ ->
                startActivity(Intent(this, PermissionsActivity::class.java))
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun setupOverlayToggle() {
        binding.switchOverlay.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                if (android.provider.Settings.canDrawOverlays(this)) {
                    startService(Intent(this, OverlayService::class.java))
                    overlayActive = true
                } else {
                    binding.switchOverlay.isChecked = false
                    Toast.makeText(this, "Please grant overlay permission first.", Toast.LENGTH_SHORT).show()
                }
            } else {
                stopService(Intent(this, OverlayService::class.java))
                overlayActive = false
            }
        }
    }

    private fun updateShizukuStatus() {
        val connected = ShizukuHelper.isAvailable()
        val running = ShizukuHelper.isRunning()
        val needsShizuku = ShizukuHelper.requiresShizuku()

        val (text, color) = when {
            connected -> "Shizuku active" to R.color.success
            running -> "Shizuku service running — waiting for permission" to R.color.warning
            needsShizuku -> "Shizuku required (Android 12+)" to R.color.error
            else -> "Direct write mode (Android 11-)" to R.color.info
        }

        binding.tvShizukuStatus.text = text
        binding.tvShizukuStatus.setTextColor(ContextCompat.getColor(this, color))

        binding.btnConnectShizuku.visibility = if (running && !connected) View.VISIBLE else View.GONE
        binding.btnConnectShizuku.setOnClickListener {
            try {
                Shizuku.requestPermission(1001)
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot request Shizuku permission", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startStatsUpdate() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    binding.tvRamValue.text = "${DeviceStats.getRamUsagePercent(applicationContext)}%"
                    binding.tvTempValue.text = "${DeviceStats.getCpuTemperature()}°C"
                    delay(2000)
                }
            }
        }
    }
}
