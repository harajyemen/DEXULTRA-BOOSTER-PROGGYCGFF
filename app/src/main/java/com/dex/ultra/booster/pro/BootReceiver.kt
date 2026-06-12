package com.dex.ultra.booster.pro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            Log.i("BootReceiver", "Boot completed — DexUltra ready")
            // Auto-start optimization on boot if user enabled it
            val prefs = context.getSharedPreferences("dexultra_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("auto_boost_on_boot", false)) {
                PerformanceOptimizer.clearSystemCache(context)
                PerformanceOptimizer.optimizeNetwork()
            }
        }
    }
}
