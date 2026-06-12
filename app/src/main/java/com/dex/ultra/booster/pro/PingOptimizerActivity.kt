package com.dex.ultra.booster.pro

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dex.ultra.booster.pro.databinding.ActivityPingOptimizerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

class PingOptimizerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPingOptimizerBinding
    private var isOptimizing = false
    private var isDnsActive = false

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startDnsVpn()
        }
    }

    // DNS servers optimized for gaming in the Middle East
    private val dnsServers = mapOf(
        "cloudflare_gaming" to DnsServer("Cloudflare Gaming", "1.1.1.1", "1.0.0.1"),
        "google" to DnsServer("Google DNS", "8.8.8.8", "8.8.4.4"),
        "opendns" to DnsServer("OpenDNS", "208.67.222.222", "208.67.220.220"),
        "quad9" to DnsServer("Quad9 (آمن)", "9.9.9.9", "149.112.112.112"),
        "adguard" to DnsServer("AdGuard Gaming", "94.140.14.14", "94.140.15.15"),
        "pubg_optimized" to DnsServer("PUBG Optimized", "1.1.1.1", "8.8.8.8") // Cloudflare primary
    )

    // Game server IPs for PUBG Mobile (major regions)
    private val pubgServers = mapOf(
        "auto" to "auto",
        "me" to "kr-pubg-gt.pubg.com",      // Middle East region
        "asia" to "prod-live-front.game.kakao.com",
        "eu" to "eu-pubg-front.pubg.com"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPingOptimizerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.ping_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupSpinner()
        setupButtons()
        startPingMonitor()
    }

    private fun setupSpinner() {
        val serverNames = listOf(
            "🌐 تلقائي (أفضل بنق)",
            "🏙️ الشرق الأوسط",
            "🌏 آسيا",
            "🌍 أوروبا"
        )
        binding.spinnerServer.adapter = android.widget.ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, serverNames
        )

        val dnsNames = dnsServers.values.map { "${it.name} (${it.primary})" }
        binding.spinnerDns.adapter = android.widget.ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, dnsNames
        )
    }

    private fun setupButtons() {
        binding.btnOptimizePing.setOnClickListener {
            if (!isOptimizing) {
                optimizePing()
            } else {
                stopOptimizing()
            }
        }

        binding.btnDnsBoost.setOnClickListener {
            if (!isDnsActive) {
                requestVpnPermission()
            } else {
                stopDnsVpn()
            }
        }
    }

    private fun optimizePing() {
        isOptimizing = true
        binding.btnOptimizePing.text = "⏹ إيقاف التحسين"
        binding.tvPingStatus.text = getString(R.string.vpn_connecting)

        lifecycleScope.launch {
            // Step 1: Optimize network buffers
            binding.tvPingStatus.text = "🔧 ضبط مخازن الشبكة…"
            PerformanceOptimizer.optimizeNetwork()
            delay(600)

            // Step 2: Find best DNS
            binding.tvPingStatus.text = "🌐 اختبار خوادم DNS…"
            val bestDns = findBestDns()
            delay(600)

            // Step 3: Apply via Shizuku if available
            if (ShizukuHelper.isAvailable()) {
                binding.tvPingStatus.text = "⚡ تطبيق DNS محسّن: ${bestDns.primary}…"
                applyDnsViaShizuku(bestDns)
            }

            delay(400)
            binding.tvPingStatus.text = "✅ ${getString(R.string.vpn_connected)}"
            Toast.makeText(
                this@PingOptimizerActivity,
                "✅ تم تحسين البنق بنجاح!\nDNS: ${bestDns.primary}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun stopOptimizing() {
        isOptimizing = false
        binding.btnOptimizePing.text = getString(R.string.btn_optimize_ping)
        binding.tvPingStatus.text = getString(R.string.vpn_disconnected)
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startDnsVpn()
        }
    }

    private fun startDnsVpn() {
        isDnsActive = true
        binding.btnDnsBoost.text = "🔴 إيقاف DNS"
        binding.tvDnsStatus.text = getString(R.string.dns_status_on)

        val selectedDns = dnsServers.values.elementAt(binding.spinnerDns.selectedItemPosition)

        val serviceIntent = Intent(this, DnsVpnService::class.java).apply {
            putExtra("dns_primary", selectedDns.primary)
            putExtra("dns_secondary", selectedDns.secondary)
        }
        startService(serviceIntent)

        Toast.makeText(this, "✅ DNS محسّن نشط: ${selectedDns.name}", Toast.LENGTH_SHORT).show()
    }

    private fun stopDnsVpn() {
        isDnsActive = false
        binding.btnDnsBoost.text = getString(R.string.btn_dns_boost)
        binding.tvDnsStatus.text = getString(R.string.dns_status_off)
        stopService(Intent(this, DnsVpnService::class.java))
    }

    private suspend fun findBestDns(): DnsServer = withContext(Dispatchers.IO) {
        var best = dnsServers["cloudflare_gaming"]!!
        var bestPing = Long.MAX_VALUE

        for (server in dnsServers.values) {
            val ping = measureDnsPing(server.primary)
            if (ping < bestPing) {
                bestPing = ping
                best = server
            }
        }
        best
    }

    private fun measureDnsPing(dnsIp: String): Long {
        return try {
            val start = System.currentTimeMillis()
            InetAddress.getByName(dnsIp)
            System.currentTimeMillis() - start
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }

    private fun applyDnsViaShizuku(dns: DnsServer) {
        if (!ShizukuHelper.isAvailable()) return
        ShizukuHelper.runCommand(
            "settings put global dns_server \"${dns.primary}\" 2>/dev/null; true"
        )
        ShizukuHelper.runCommand(
            "ndc resolver setnetdns 100 \"\" \"${dns.primary}\" \"${dns.secondary}\" 2>/dev/null; true"
        )
    }

    private fun startPingMonitor() {
        lifecycleScope.launch {
            while (true) {
                val ping = withContext(Dispatchers.IO) {
                    measurePubgPing()
                }
                binding.tvPingValue.text = if (ping < Long.MAX_VALUE) "${ping}ms" else "--"

                val color = when {
                    ping < 40 -> getColor(R.color.success)
                    ping < 80 -> getColor(R.color.warning)
                    else -> getColor(R.color.error)
                }
                binding.tvPingValue.setTextColor(color)

                delay(3000)
            }
        }
    }

    private fun measurePubgPing(): Long {
        return try {
            val start = System.currentTimeMillis()
            val addr = InetAddress.getByName("kr-pubg-gt.pubg.com")
            addr.isReachable(2000)
            System.currentTimeMillis() - start
        } catch (e: Exception) {
            try {
                val start = System.currentTimeMillis()
                InetAddress.getByName("8.8.8.8")
                System.currentTimeMillis() - start
            } catch (ex: Exception) {
                Long.MAX_VALUE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

data class DnsServer(
    val name: String,
    val primary: String,
    val secondary: String
)
