package com.dex.ultra.booster.pro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

/**
 * DNS-only VPN service — يُحسّن البنق عبر توجيه طلبات DNS إلى خوادم أسرع.
 * يعمل كـ Foreground Service مع إشعار دائم لمنع Android من إيقافه في الخلفية.
 */
class DnsVpnService : VpnService() {

    companion object {
        private const val TAG              = "DnsVpnService"
        const val CHANNEL_ID               = "dexultra_dns_channel"
        const val NOTIF_ID                 = 42
        private const val ACTION_STOP_DNS  = "com.dex.ultra.ACTION_STOP_DNS"
        private const val VPN_MTU          = 1500
        private const val VPN_ADDRESS      = "10.0.0.2"
        private const val VPN_ROUTE        = "0.0.0.0"

        var DNS_PRIMARY   = "1.1.1.1"
        var DNS_SECONDARY = "8.8.8.8"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunnelThread: Thread? = null
    private var isRunning = false

    // ── دورة الحياة ─────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // زر الإيقاف من الإشعار
        if (intent?.action == ACTION_STOP_DNS) {
            stopSelf()
            return START_NOT_STICKY
        }

        DNS_PRIMARY   = intent?.getStringExtra("dns_primary")   ?: "1.1.1.1"
        DNS_SECONDARY = intent?.getStringExtra("dns_secondary") ?: "8.8.8.8"

        // ابدأ كـ Foreground Service فوراً — يمنع Android من قتل الخدمة
        startForeground(NOTIF_ID, buildNotification("جاري تحسين البنق…"))
        establishVpn()
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        tunnelThread?.interrupt()
        try { vpnInterface?.close() } catch (_: Exception) {}
        super.onDestroy()
    }

    // ── بناء VPN ────────────────────────────────────────────────────

    private fun establishVpn() {
        try {
            val builder = Builder()
                .setMtu(VPN_MTU)
                .addAddress(VPN_ADDRESS, 32)
                .addRoute(VPN_ROUTE, 0)
                .addDnsServer(DNS_PRIMARY)
                .addDnsServer(DNS_SECONDARY)
                .setBlocking(true)
                .setSession("DexUltra DNS")
                .addDisallowedApplication(packageName)

            vpnInterface = builder.establish()
            if (vpnInterface == null) { stopSelf(); return }

            isRunning = true
            // تحديث الإشعار بعد الاتصال
            val nm = getSystemService(NotificationManager::class.java)
            nm?.notify(NOTIF_ID, buildNotification("DNS نشط: $DNS_PRIMARY"))

            startTunnel()
            Log.i(TAG, "DNS VPN started — $DNS_PRIMARY / $DNS_SECONDARY")

        } catch (e: Exception) {
            Log.e(TAG, "VPN failed: ${e.message}")
            stopSelf()
        }
    }

    private fun startTunnel() {
        tunnelThread = Thread({
            val buf = ByteBuffer.allocate(VPN_MTU)
            val fd  = vpnInterface?.fileDescriptor ?: return@Thread
            try {
                val inStream  = java.io.FileInputStream(fd)
                val outStream = java.io.FileOutputStream(fd)
                while (isRunning) {
                    buf.clear()
                    val len = inStream.read(buf.array())
                    if (len <= 0) continue
                    buf.limit(len)
                    if (isDnsPacket(buf)) {
                        forwardDnsQuery(buf, len)?.let { outStream.write(it) }
                    }
                }
            } catch (e: Exception) {
                if (isRunning) Log.e(TAG, "Tunnel error: ${e.message}")
            }
        }, "DexUltra-DNS")
        tunnelThread?.start()
    }

    // ── معالجة الحزم ────────────────────────────────────────────────

    private fun isDnsPacket(buf: ByteBuffer): Boolean {
        if (buf.limit() < 28) return false
        if ((buf.get(9).toInt() and 0xFF) != 17) return false // UDP فقط
        val destPort = ((buf.get(22).toInt() and 0xFF) shl 8) or (buf.get(23).toInt() and 0xFF)
        return destPort == 53
    }

    private fun forwardDnsQuery(buf: ByteBuffer, len: Int): ByteArray? {
        return try {
            val payload = buf.array().copyOfRange(28, len)
            val ch = DatagramChannel.open()
            ch.connect(java.net.InetSocketAddress(InetAddress.getByName(DNS_PRIMARY), 53))
            protect(ch.socket())
            ch.write(ByteBuffer.wrap(payload))
            val resp = ByteBuffer.allocate(VPN_MTU)
            ch.read(resp)
            ch.close()
            rebuildIpPacket(buf.array().copyOfRange(0, 28),
                            resp.array().copyOfRange(0, resp.position()))
        } catch (e: Exception) {
            Log.w(TAG, "DNS forward failed: ${e.message}"); null
        }
    }

    private fun rebuildIpPacket(hdr: ByteArray, dns: ByteArray): ByteArray {
        val total = 20 + 8 + dns.size
        val pkt = ByteArray(total)
        System.arraycopy(hdr, 0, pkt, 0, 20)
        System.arraycopy(hdr, 16, pkt, 12, 4)
        System.arraycopy(hdr, 12, pkt, 16, 4)
        pkt[2] = ((total shr 8) and 0xFF).toZero()
        pkt[3] = (total and 0xFF).toByte()
        System.arraycopy(hdr, 22, pkt, 20, 2)
        System.arraycopy(hdr, 20, pkt, 22, 2)
        val udpLen = 8 + dns.size
        pkt[24] = ((udpLen shr 8) and 0xFF).toByte()
        pkt[25] = (udpLen and 0xFF).toByte()
        pkt[26] = 0; pkt[27] = 0
        System.arraycopy(dns, 0, pkt, 28, dns.size)
        return pkt
    }

    private fun Int.toZero(): Byte = ((this shr 8) and 0xFF).toByte()

    // ── الإشعار الدائم ──────────────────────────────────────────────

    private fun buildNotification(statusText: String): Notification {
        val openIntent = Intent(this, PingOptimizerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val stopIntent = Intent(this, DnsVpnService::class.java).apply {
            action = ACTION_STOP_DNS
        }
        val stopPi = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // استخدام الـ drawable الرئيسي لتجنب أخطاء الميبماب في التجميع
            .setContentTitle("DexUltra — DNS نشط")
            .setContentText(statusText)
            .setSubText("اضغط لفتح التطبيق")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openPi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف DNS", stopPi)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "DexUltra DNS Optimizer",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "يُظهر حالة تحسين البنق عند التشغيل في الخلفية"
                setShowBadge(true)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
    }
}
