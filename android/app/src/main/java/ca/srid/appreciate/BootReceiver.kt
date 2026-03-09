package ca.srid.appreciate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Starts the overlay service on device boot if launch-at-boot is enabled.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val settings = SettingsStore(context)
            if (settings.launchAtBoot && settings.isEnabled) {
                Log.d(TAG, "Boot completed — starting Appreciate service")
                val serviceIntent = Intent(context, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_START
                }
                context.startForegroundService(serviceIntent)
            } else {
                Log.d(TAG, "Boot completed — service not enabled, skipping")
            }
        }
    }
}
