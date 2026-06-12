package com.dex.ultra.booster.pro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DnsVpnService : VpnService() {

    companion object {
        private const val TAG = "DnsVpnService"
        private const val CHANNEL_ID = "dexultra_vpn_ch"
        private const val NOTIF_ID = 2
        private const val MTU = 1500
        private const val VPN_ADDRESS = "10.111.222.2"

        var DNS_PRIMARY   = "1.1.1.1"
        var DNS_SECONDARY = "8.8.8.8"

        const val ACTION_STOP = "com.dex.ultra.booster.pro.STOP_VPN"
    }

    private var vpnIface: ParcelFileDescriptor? = null
    private var worker: Thread? = null
    @Volatile private var running = false

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            tearDown()
            stopSelf()
            return START_NOT_STICKY
        }

        DNS_PRIMARY   = intent?.getStringExtra("dns_primary")   ?: "1.1.1.1"
        DNS_SECONDARY = intent?.getStringExtra("dns_secondary") ?: "8.8.8.8"

        // ── Call startForeground immediately before any other work ────────────
        // On Android 10+ (API 29) we MUST pass the foreground-service type.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }

        // Tear down any previous VPN before establishing a new one
        tearDown()
        establishVpn()

        return START_STICKY
    }

    override fun onDestroy() {
        tearDown()
        super.onDestroy()
    }

    // ── VPN Setup ────────────────────────────────────────────────────────────

    private fun establishVpn() {
        try {
            val b = Builder()
                .setMtu(MTU)
                .addAddress(VPN_ADDRESS, 32)
                .setSession("DexUltra DNS Optimizer")
                .setBlocking(true)
                // Exclude our own app so it can reach the real DNS servers
                .addDisallowedApplication(packageName)

            // Only route traffic destined for the DNS servers through the tunnel.
            // Everything else goes through the normal network — no internet breakage.
            safeAddRoute(b, DNS_PRIMARY,   32)
            safeAddRoute(b, DNS_SECONDARY, 32)

            // Register the DNS servers that Android will use for this VPN interface
            safeAddDns(b, DNS_PRIMARY)
            safeAddDns(b, DNS_SECONDARY)

            val iface = b.establish()
            if (iface == null) {
                Log.e(TAG, "establish() returned null — VPN permission missing?")
                stopSelf()
                return
            }

            vpnIface = iface
            running  = true
            startTunnel(iface)
            Log.i(TAG, "VPN established. DNS: $DNS_PRIMARY / $DNS_SECONDARY")

        } catch (e: Exception) {
            Log.e(TAG, "VPN setup failed: ${e.message}", e)
            stopSelf()
        }
    }

    private fun safeAddRoute(b: Builder, ip: String, prefix: Int) {
        try { b.addRoute(ip, prefix) }
        catch (e: Exception) { Log.w(TAG, "addRoute($ip/$prefix) skipped: ${e.message}") }
    }

    private fun safeAddDns(b: Builder, ip: String) {
        try { b.addDnsServer(ip) }
        catch (e: Exception) { Log.w(TAG, "addDnsServer($ip) skipped: ${e.message}") }
    }

    // ── DNS Tunnel ───────────────────────────────────────────────────────────

    private fun startTunnel(iface: ParcelFileDescriptor) {
        worker = Thread({
            val buf = ByteArray(MTU)
            val input  = FileInputStream(iface.fileDescriptor)
            val output = FileOutputStream(iface.fileDescriptor)

            while (running) {
                val len = try { input.read(buf) } catch (e: Exception) { break }
                if (len <= 0) continue

                // Only process UDP DNS (port 53) packets; silently discard everything else.
                if (!isDnsPacket(buf, len)) continue

                val response = forwardDns(buf, len)
                if (response != null) {
                    try { output.write(response) } catch (_: Exception) {}
                }
            }

            try { input.close()  } catch (_: Exception) {}
            try { output.close() } catch (_: Exception) {}
            Log.d(TAG, "Tunnel thread exited")
        }, "DexUltra-DNS-Tunnel").also { it.isDaemon = true; it.start() }
    }

    // ── Packet helpers ───────────────────────────────────────────────────────

    /** Returns true if buf[0..len) is an IPv4 UDP packet with destination port 53. */
    private fun isDnsPacket(buf: ByteArray, len: Int): Boolean {
        if (len < 28) return false
        if ((buf[0].toInt() and 0xF0) shr 4 != 4) return false  // must be IPv4
        if (buf[9].toInt() and 0xFF != 17) return false           // must be UDP
        val dstPort = ((buf[22].toInt() and 0xFF) shl 8) or (buf[23].toInt() and 0xFF)
        return dstPort == 53
    }

    /**
     * Forward the DNS payload in ipPacket to the real DNS server via a protected
     * DatagramSocket, then wrap the response into an IPv4/UDP packet and return it.
     */
    private fun forwardDns(ipPacket: ByteArray, len: Int): ByteArray? {
        if (len < 28) return null
        val dnsPayload = ipPacket.copyOfRange(28, len)

        return try {
            val sock = DatagramSocket()
            protect(sock)   // bypass the VPN so we can reach the real internet
            sock.soTimeout = 3_000

            val srv = InetAddress.getByName(DNS_PRIMARY)
            sock.send(DatagramPacket(dnsPayload, dnsPayload.size, srv, 53))

            val rBuf   = ByteArray(MTU)
            val rPkt   = DatagramPacket(rBuf, rBuf.size)
            sock.receive(rPkt)
            sock.close()

            buildIpUdpReply(ipPacket, rBuf, rPkt.length)
        } catch (e: Exception) {
            Log.w(TAG, "DNS forward error: ${e.message}")
            null
        }
    }

    /**
     * Build an IPv4/UDP response packet by swapping src/dst from the request.
     * Includes a valid IPv4 header checksum; UDP checksum is zeroed (valid on Linux TUN).
     */
    private fun buildIpUdpReply(req: ByteArray, dnsResp: ByteArray, dnsLen: Int): ByteArray {
        val totalLen = 20 + 8 + dnsLen
        val pkt = ByteArray(totalLen)

        // ── IPv4 header ──────────────────────────────────────────────────────
        pkt[0] = 0x45.toByte()          // version=4, IHL=5
        pkt[1] = 0                       // DSCP / ECN
        pkt[2] = ((totalLen shr 8) and 0xFF).toByte()
        pkt[3] = (totalLen and 0xFF).toByte()
        pkt[4] = req[4]; pkt[5] = req[5] // identification (copy)
        pkt[6] = 0; pkt[7] = 0           // flags / fragment offset
        pkt[8] = 64                       // TTL
        pkt[9] = 17                       // protocol = UDP
        // bytes 10-11: checksum — computed below
        // swap src/dst IPs from request
        System.arraycopy(req, 16, pkt, 12, 4)  // req dst → reply src
        System.arraycopy(req, 12, pkt, 16, 4)  // req src → reply dst

        // Compute IPv4 header checksum (bytes 10-11, starting at 0)
        pkt[10] = 0; pkt[11] = 0
        val ipCs = ipChecksum(pkt, 0, 20)
        pkt[10] = ((ipCs shr 8) and 0xFF).toByte()
        pkt[11] = (ipCs and 0xFF).toByte()

        // ── UDP header ───────────────────────────────────────────────────────
        val udpLen = 8 + dnsLen
        // swap src/dst ports from request
        pkt[20] = req[22]; pkt[21] = req[23]  // req dst port → reply src port (=53)
        pkt[22] = req[20]; pkt[23] = req[21]  // req src port → reply dst port
        pkt[24] = ((udpLen shr 8) and 0xFF).toByte()
        pkt[25] = (udpLen and 0xFF).toByte()
        pkt[26] = 0; pkt[27] = 0               // checksum = 0 (disabled, valid for IPv4)

        // ── DNS payload ──────────────────────────────────────────────────────
        System.arraycopy(dnsResp, 0, pkt, 28, dnsLen)
        return pkt
    }

    /** RFC 1071 one's-complement checksum over bytes [off, off+len). */
    private fun ipChecksum(buf: ByteArray, off: Int, len: Int): Int {
        var sum = 0
        var i = off
        while (i < off + len - 1) {
            sum += ((buf[i].toInt() and 0xFF) shl 8) or (buf[i + 1].toInt() and 0xFF)
            i += 2
        }
        if ((off + len) % 2 != 0) sum += (buf[off + len - 1].toInt() and 0xFF) shl 8
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv() and 0xFFFF
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, DnsVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("DexUltra – DNS محسّن نشط")
            .setContentText("DNS: $DNS_PRIMARY / $DNS_SECONDARY")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_delete, "إيقاف", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "DNS Optimizer", NotificationManager.IMPORTANCE_LOW)
            ch.description = "يعرض حالة تحسين DNS للألعاب"
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    private fun tearDown() {
        running = false
        worker?.interrupt()
        worker = null
        try { vpnIface?.close() } catch (_: Exception) {}
        vpnIface = null
    }
}
