package ca.srid.appreciate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowManager

/**
 * Foreground service that manages the overlay window and timer.
 * Runs persistently to ensure reminders fire reliably.
 */
class OverlayService : Service() {

    companion object {
        private const val TAG = "OverlayService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "appreciate_service"

        const val ACTION_START = "ca.srid.appreciate.ACTION_START"
        const val ACTION_STOP = "ca.srid.appreciate.ACTION_STOP"
        const val ACTION_SHOW_NOW = "ca.srid.appreciate.ACTION_SHOW_NOW"
    }

    private var windowManager: WindowManager? = null
    private var currentOverlay: View? = null
    private lateinit var timerManager: TimerManager
    private lateinit var overlayViewFactory: OverlayViewFactory

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        timerManager = TimerManager(this)
        overlayViewFactory = OverlayViewFactory(this)
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundWithNotification()
                timerManager.scheduleNext()
                Log.d(TAG, "Service started, timer scheduled")
            }
            ACTION_STOP -> {
                timerManager.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                Log.d(TAG, "Service stopped")
            }
            TimerManager.ACTION_SHOW_OVERLAY -> {
                showOverlay()
                timerManager.scheduleNext()
            }
            ACTION_SHOW_NOW -> {
                showOverlay()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundWithNotification() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Appreciate Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Keeps the reminder service running"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Appreciate")
            .setContentText("Reminders active")
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun showOverlay() {
        // Remove existing overlay if any
        dismissOverlay()

        val settings = SettingsStore(this)
        val text = settings.randomLine
        val duration = settings.displayDurationSeconds

        Log.d(TAG, "Showing overlay: \"$text\"")

        val overlayView = overlayViewFactory.createOverlayView(text, duration) {
            dismissOverlay()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        try {
            windowManager?.addView(overlayView, params)
            currentOverlay = overlayView
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay: ${e.message}")
        }
    }

    private fun dismissOverlay() {
        currentOverlay?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove overlay: ${e.message}")
            }
            currentOverlay = null
        }
    }

    override fun onDestroy() {
        dismissOverlay()
        timerManager.cancel()
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }
}
