package com.dex.ultra.booster.pro

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        if (result.resultCode == RESULT_OK) startDnsVpn()
        else Toast.makeText(this, "لم يتم منح إذن VPN", Toast.LENGTH_SHORT).show()
    }

    private val dnsServers = listOf(
        DnsServer("Cloudflare Gaming", "1.1.1.1", "1.0.0.1"),
        DnsServer("Google DNS", "8.8.8.8", "8.8.4.4"),
        DnsServer("OpenDNS", "208.67.222.222", "208.67.220.220"),
        DnsServer("Quad9 (آمن)", "9.9.9.9", "149.112.112.112"),
        DnsServer("AdGuard Gaming", "94.140.14.14", "94.140.15.15"),
        DnsServer("PUBG Optimized", "1.1.1.1", "8.8.8.8")
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

        val dnsNames = dnsServers.map { "${it.name} (${it.primary})" }
        binding.spinnerDns.adapter = android.widget.ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, dnsNames
        )
    }

    private fun setupButtons() {
        binding.btnOptimizePing.setOnClickListener {
            if (!isOptimizing) optimizePing() else stopOptimizing()
        }
        binding.btnDnsBoost.setOnClickListener {
            if (!isDnsActive) requestVpnPermission() else stopDnsVpn()
        }
    }

    private fun optimizePing() {
        isOptimizing = true
        binding.btnOptimizePing.text = "⏹ إيقاف التحسين"

        lifecycleScope.launch {
            binding.tvPingStatus.text = "🔧 ضبط مخازن الشبكة…"
            withContext(Dispatchers.IO) { PerformanceOptimizer.optimizeNetwork() }
            delay(600)

            binding.tvPingStatus.text = "🌐 اختبار خوادم DNS…"
            val bestDns = withContext(Dispatchers.IO) { findBestDns() }
            delay(600)

            if (ShizukuHelper.isAvailable()) {
                binding.tvPingStatus.text = "⚡ تطبيق DNS: ${bestDns.primary}…"
                withContext(Dispatchers.IO) { applyDnsViaShizuku(bestDns) }
            }

            delay(400)
            binding.tvPingStatus.text = "✅ ${getString(R.string.vpn_connected)}"
            Toast.makeText(
                this@PingOptimizerActivity,
                "✅ تم تحسين البنق!\nDNS: ${bestDns.primary}",
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
        try {
            val intent = VpnService.prepare(this)
            if (intent != null) {
                vpnPermissionLauncher.launch(intent)
            } else {
                startDnsVpn()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "تعذّر تحضير VPN: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startDnsVpn() {
        isDnsActive = true
        binding.btnDnsBoost.text = "🔴 إيقاف DNS"
        binding.tvDnsStatus.text = getString(R.string.dns_status_on)

        val selected = dnsServers.getOrElse(binding.spinnerDns.selectedItemPosition) { dnsServers[0] }

        val serviceIntent = Intent(this, DnsVpnService::class.java).apply {
            putExtra("dns_primary", selected.primary)
            putExtra("dns_secondary", selected.secondary)
        }

        // Use startForegroundService on API 26+ to avoid IllegalStateException
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        Toast.makeText(this, "✅ DNS نشط: ${selected.name}", Toast.LENGTH_SHORT).show()
    }

    private fun stopDnsVpn() {
        isDnsActive = false
        binding.btnDnsBoost.text = getString(R.string.btn_dns_boost)
        binding.tvDnsStatus.text = getString(R.string.dns_status_off)
        stopService(Intent(this, DnsVpnService::class.java))
    }

    private fun findBestDns(): DnsServer {
        var best = dnsServers[0]
        var bestPing = Long.MAX_VALUE
        for (server in dnsServers) {
            val ping = measureDnsPing(server.primary)
            if (ping < bestPing) {
                bestPing = ping
                best = server
            }
        }
        return best
    }

    private fun measureDnsPing(ip: String): Long {
        return try {
            val t = System.currentTimeMillis()
            InetAddress.getByName(ip)
            System.currentTimeMillis() - t
        } catch (_: Exception) { Long.MAX_VALUE }
    }

    private fun applyDnsViaShizuku(dns: DnsServer) {
        ShizukuHelper.runCommand("settings put global dns_server \"${dns.primary}\" 2>/dev/null; true")
        ShizukuHelper.runCommand("ndc resolver setnetdns 100 \"\" \"${dns.primary}\" \"${dns.secondary}\" 2>/dev/null; true")
    }

    private fun startPingMonitor() {
        lifecycleScope.launch {
            while (true) {
                val ping = withContext(Dispatchers.IO) { measurePubgPing() }
                binding.tvPingValue.text = if (ping < Long.MAX_VALUE) "${ping}ms" else "--"
                val color = when {
                    ping < 40 -> getColor(R.color.success)
                    ping < 80 -> getColor(R.color.warning)
                    else      -> getColor(R.color.error)
                }
                binding.tvPingValue.setTextColor(color)
                delay(3000)
            }
        }
    }

    private fun measurePubgPing(): Long {
        return try {
            val t = System.currentTimeMillis()
            InetAddress.getByName("kr-pubg-gt.pubg.com").isReachable(2000)
            System.currentTimeMillis() - t
        } catch (_: Exception) {
            try {
                val t = System.currentTimeMillis()
                InetAddress.getByName("8.8.8.8")
                System.currentTimeMillis() - t
            } catch (_: Exception) { Long.MAX_VALUE }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}

data class DnsServer(val name: String, val primary: String, val secondary: String)
