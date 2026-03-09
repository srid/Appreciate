package ca.srid.appreciate

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Schedules overlay reminders using AlarmManager.setAlarmClock() — the most
 * aggressive alarm type on Android. Unlike setExact/setAndAllowWhileIdle,
 * setAlarmClock() is NEVER deferred by doze mode, battery saver, or any
 * OEM optimization. It is designed for alarm clock apps and always fires
 * exactly on time.
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

        val triggerAt = System.currentTimeMillis() + intervalMs

        // setAlarmClock is the NUCLEAR option:
        // - Ignores doze mode completely
        // - Ignores battery saver
        // - Ignores OEM battery optimizations (Samsung, Xiaomi, etc.)
        // - Always fires exactly on time
        // - No special permissions needed (unlike setExact which needs SCHEDULE_EXACT_ALARM)
        val alarmInfo = AlarmManager.AlarmClockInfo(triggerAt, pendingIntent)
        alarmManager.setAlarmClock(alarmInfo, pendingIntent)

        Log.d(TAG, "ALARM CLOCK set: next reminder in ${intervalMs / 1000}s (${intervalMs / 60000f}min)")
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
