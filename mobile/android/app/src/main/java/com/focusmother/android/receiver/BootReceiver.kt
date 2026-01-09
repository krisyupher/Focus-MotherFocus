package com.focusmother.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.focusmother.android.service.MonitoringService

/**
 * BootReceiver - Starts monitoring service on device boot
 * Only starts if user had monitoring enabled before reboot
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Check if monitoring was enabled (from SharedPreferences)
            val prefs = context.getSharedPreferences("focus_mother_prefs", Context.MODE_PRIVATE)
            val wasMonitoring = prefs.getBoolean("monitoring_enabled", false)

            if (wasMonitoring) {
                startMonitoringService(context)
            }
        }
    }

    private fun startMonitoringService(context: Context) {
        val serviceIntent = Intent(context, MonitoringService::class.java).apply {
            action = MonitoringService.ACTION_START_MONITORING
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
