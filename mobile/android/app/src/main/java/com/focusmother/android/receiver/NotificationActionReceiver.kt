package com.focusmother.android.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.focusmother.android.service.MonitoringService

/**
 * NotificationActionReceiver - Handles actions from intervention notifications
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (intent.action) {
            MonitoringService.ACTION_TAKE_BREAK -> {
                // User agreed to take a break
                notificationManager.cancel(1002) // Cancel intervention notification
                Toast.makeText(context, "Great! Enjoy your break 😊", Toast.LENGTH_LONG).show()

                // Could pause monitoring for 15 minutes here
                saveUserDecision(context, "take_break")
            }

            MonitoringService.ACTION_REQUEST_TIME -> {
                // User requested more time
                notificationManager.cancel(1002)
                Toast.makeText(context, "OK, 5 more minutes. I'll check back soon!", Toast.LENGTH_LONG).show()

                // Could create an agreement/timer here
                saveUserDecision(context, "5_more_minutes")
            }
        }
    }

    private fun saveUserDecision(context: Context, decision: String) {
        val prefs = context.getSharedPreferences("focus_mother_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("last_decision_time", System.currentTimeMillis())
            .putString("last_decision", decision)
            .apply()
    }
}
