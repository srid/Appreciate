package ca.srid.appreciate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.view.WindowManager

/**
 * Foreground service that manages the overlay window and timer.
 * Uses a WakeLock while the overlay is visible to prevent the CPU from
 * sleeping mid-animation (which causes the "instant flash on unlock" bug).
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
    private var overlayWakeLock: PowerManager.WakeLock? = null
    private lateinit var ttsManager: TtsManager
    private lateinit var headphoneManager: HeadphoneManager

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        timerManager = TimerManager(this)
        overlayViewFactory = OverlayViewFactory(this)
        ttsManager = TtsManager(this)
        headphoneManager = HeadphoneManager(this)
        headphoneManager.register()
        SettingsStore(this).prefetchRemotePacks()
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundWithNotification()
                try {
                    timerManager.scheduleNext()
                    Log.d(TAG, "Service started, alarm clock scheduled")
                } catch (e: SecurityException) {
                    Log.e(TAG, "Cannot schedule alarm — permission not yet granted: ${e.message}")
                }
            }
            ACTION_STOP -> {
                timerManager.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                Log.d(TAG, "Service stopped")
            }
            TimerManager.ACTION_SHOW_OVERLAY -> {
                Log.d(TAG, "Alarm fired — showing overlay")
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

        Log.d(TAG, "Showing overlay: \"$text\" for ${duration}s")

        // Voice dictation: speak the reminder when headphones are connected
        if (settings.voiceWhenHeadphones && headphoneManager.isHeadphoneConnected) {
            Log.d(TAG, "Headphones connected — speaking reminder via TTS")
            ttsManager.speak(text)
        }

        // Acquire a wake lock for the entire overlay duration + animations
        // This prevents the CPU from sleeping, which would cause the dismiss
        // timer to be deferred and fire instantly on unlock (the "flash" bug).
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val totalMs = ((duration + 3) * 1000).toLong() // duration + entrance + exit + margin
        overlayWakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Appreciate:OverlayWakeLock"
        ).apply {
            acquire(totalMs)
        }
        Log.d(TAG, "WakeLock acquired for ${totalMs}ms")

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
            releaseWakeLock()
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
        releaseWakeLock()
    }

    private fun releaseWakeLock() {
        overlayWakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock released")
            }
        }
        overlayWakeLock = null
    }

    override fun onDestroy() {
        dismissOverlay()
        timerManager.cancel()
        ttsManager.shutdown()
        headphoneManager.unregister()
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }
}
