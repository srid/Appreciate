package ca.srid.appreciate

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log

/**
 * Schedules overlay reminders at random intervals using AlarmManager.
 * Uses exact alarms for reliable timing even during doze mode.
 */
class TimerManager(private val context: Context) {

    companion object {
        private const val TAG = "TimerManager"
        const val ACTION_SHOW_OVERLAY = "ca.srid.appreciate.ACTION_SHOW_OVERLAY"
        private const val REQUEST_CODE = 1001
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNext() {
        val settings = SettingsStore(context)
        if (!settings.isEnabled) {
            Log.d(TAG, "Reminders disabled, not scheduling")
            return
        }

        val minMs = (settings.minIntervalMinutes * 60 * 1000).toLong()
        val maxMs = (settings.maxIntervalMinutes * 60 * 1000).toLong()
        val intervalMs = if (maxMs > minMs) {
            minMs + (Math.random() * (maxMs - minMs)).toLong()
        } else {
            minMs
        }

        val intent = Intent(context, OverlayService::class.java).apply {
            action = ACTION_SHOW_OVERLAY
        }
        val pendingIntent = PendingIntent.getService(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = SystemClock.elapsedRealtime() + intervalMs

        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }

        Log.d(TAG, "Next reminder in ${intervalMs / 1000}s (${intervalMs / 60000f}min)")
    }

    fun cancel() {
        val intent = Intent(context, OverlayService::class.java).apply {
            action = ACTION_SHOW_OVERLAY
        }
        val pendingIntent = PendingIntent.getService(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled scheduled reminders")
    }
}
