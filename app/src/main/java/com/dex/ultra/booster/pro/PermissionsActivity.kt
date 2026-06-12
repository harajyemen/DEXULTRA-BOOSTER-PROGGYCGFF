package com.dex.ultra.booster.pro

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.dex.ultra.booster.pro.databinding.ActivityPermissionsBinding
import rikka.shizuku.Shizuku

class PermissionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionsBinding

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { updateUi() }

    private val storageLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { updateUi() }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { updateUi() }

    /** هل الجهاز يحتاج Shizuku؟ (Android 12+) */
    private val needsShizuku: Boolean get() = ShizukuHelper.requiresShizuku()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupVersionSpecificUi()

        binding.btnGrantStorage.setOnClickListener { requestStorage() }
        binding.btnGrantOverlay.setOnClickListener { requestOverlay() }
        binding.btnGrantShizuku.setOnClickListener { openShizuku() }
        binding.btnContinue.setOnClickListener { goToMain() }

        updateUi()
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    // ── واجهة حسب إصدار الأندرويد ──────────────────────────────────

    private fun setupVersionSpecificUi() {
        if (needsShizuku) {
            // Android 12+ → Shizuku مطلوب
            binding.tvPermSubtitle.text =
                "Android ${Build.VERSION.SDK_INT} — يحتاج Shizuku لكتابة ملفات اللعبة"
            binding.cardShizuku.visibility = View.VISIBLE
            binding.tvShizukuBadge.text = "مطلوب"
            binding.tvShizukuDesc.text =
                "Android 12+ يحمي مجلد Android/data — Shizuku يتيح الكتابة بدون روت"
        } else {
            // Android 11 وما دون → صلاحية الملفات فقط
            binding.tvPermSubtitle.text =
                "Android ${Build.VERSION.SDK_INT} — يكفي منح صلاحية الملفات"
            binding.cardShizuku.visibility = View.VISIBLE
            binding.tvShizukuBadge.text = "اختياري"
            binding.tvShizukuDesc.text =
                "Android 11 وما دون يسمح بالكتابة المباشرة — Shizuku اختياري فقط"
        }
    }

    // ── تحديث حالة الواجهة ─────────────────────────────────────────

    private fun updateUi() {
        val storageOk  = hasStoragePermission()
        val overlayOk  = Settings.canDrawOverlays(this)
        val shizukuOk  = isShizukuGranted()

        // Storage
        binding.ivStorageStatus.setImageResource(
            if (storageOk) R.drawable.ic_check else R.drawable.ic_close)
        binding.tvStorageStatus.text =
            if (storageOk) getString(R.string.perm_granted) else getString(R.string.perm_denied)

        // Overlay
        binding.ivOverlayStatus.setImageResource(
            if (overlayOk) R.drawable.ic_check else R.drawable.ic_close)
        binding.tvOverlayStatus.text =
            if (overlayOk) getString(R.string.perm_granted) else getString(R.string.perm_denied)

        // Shizuku
        binding.ivShizukuStatus.setImageResource(
            if (shizukuOk) R.drawable.ic_check else R.drawable.ic_close)
        val shizukuStatusText = when {
            shizukuOk              -> getString(R.string.perm_granted)
            ShizukuHelper.isRunning() -> "يعمل — بانتظار الإذن"
            else                   -> if (needsShizuku) getString(R.string.perm_denied) else "غير متصل (اختياري)"
        }
        binding.tvShizukuStatus.text = shizukuStatusText

        // زر التابعة: Shizuku مطلوب للأندرويد 12+ فإن لم يكن متاحاً لا تُتح المتابعة
        val canContinue = storageOk && overlayOk && (!needsShizuku || shizukuOk)
        binding.btnContinue.isEnabled = canContinue
        binding.btnContinue.alpha = if (canContinue) 1f else 0.5f

        // تلميح
        if (needsShizuku && !shizukuOk) {
            binding.tvContinueHint.visibility = View.VISIBLE
            binding.tvContinueHint.text = "يجب ربط Shizuku أولاً على Android 12+"
        } else if (!storageOk) {
            binding.tvContinueHint.visibility = View.VISIBLE
            binding.tvContinueHint.text = "يجب منح صلاحية الملفات أولاً"
        } else {
            binding.tvContinueHint.visibility = View.GONE
        }
    }

    // ── فحص الصلاحيات ─────────────────────────────────────────────

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PermissionChecker.PERMISSION_GRANTED
        }
    }

    private fun isShizukuGranted(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PermissionChecker.PERMISSION_GRANTED
        } catch (e: Exception) { false }
    }

    // ── طلب الصلاحيات ─────────────────────────────────────────────

    private fun requestStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                manageStorageLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        .setData(Uri.parse("package:$packageName"))
                )
            } catch (e: Exception) {
                manageStorageLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                )
            }
        } else {
            storageLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun requestOverlay() {
        overlayLauncher.launch(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
        )
    }

    private fun openShizuku() {
        try {
            Shizuku.requestPermission(1001)
        } catch (e: Exception) {
            try {
                val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                if (intent != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this,
                        "ثبّت Shizuku من Google Play أولاً", Toast.LENGTH_LONG).show()
                    startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")))
                }
            } catch (ex: Exception) {
                Toast.makeText(this, "Shizuku غير متوفر", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        finish()
    }
}
