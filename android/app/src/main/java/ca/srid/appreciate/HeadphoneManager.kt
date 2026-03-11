package ca.srid.appreciate

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log

/**
 * Detects whether Bluetooth audio headphones (A2DP) or wired headphones
 * are currently connected. Listens for connect/disconnect events.
 */
class HeadphoneManager(private val context: Context) {

    companion object {
        private const val TAG = "HeadphoneManager"
    }

    /**
     * True when any headphone (Bluetooth A2DP or wired) is connected.
     * Always queries the current device state — never stale.
     */
    val isHeadphoneConnected: Boolean
        get() = checkDevices()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(
                        BluetoothProfile.EXTRA_STATE,
                        BluetoothProfile.STATE_DISCONNECTED
                    )
                    val connected = state == BluetoothProfile.STATE_CONNECTED
                    Log.d(TAG, "Bluetooth A2DP state changed: connected=$connected")
                    updateState()
                }
                AudioManager.ACTION_HEADSET_PLUG -> {
                    val plugged = intent.getIntExtra("state", 0) == 1
                    Log.d(TAG, "Wired headset plug: connected=$plugged")
                    updateState()
                }
            }
        }
    }

    /** Start listening for headphone connect/disconnect. */
    fun register() {
        lastKnownState = checkDevices()
        val filter = IntentFilter().apply {
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
        }
        context.registerReceiver(receiver, filter)
        Log.d(TAG, "Registered headphone receiver, initial state: connected=$isHeadphoneConnected")
    }

    /** Stop listening. Call in Service.onDestroy(). */
    fun unregister() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.w(TAG, "Receiver already unregistered: ${e.message}")
        }
    }

    /** Optional callback invoked when headphone connection state changes. */
    var onConnectionChanged: ((Boolean) -> Unit)? = null

    private var lastKnownState: Boolean = false

    /** Queries current audio output devices to determine headphone state. */
    private fun checkDevices(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.any { device ->
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                device.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        }
    }

    /** Called by the BroadcastReceiver to notify listeners of state changes. */
    private fun updateState() {
        val connected = checkDevices()
        Log.d(TAG, "updateState: isHeadphoneConnected=$connected")
        if (connected != lastKnownState) {
            lastKnownState = connected
            onConnectionChanged?.invoke(connected)
        }
    }
}
