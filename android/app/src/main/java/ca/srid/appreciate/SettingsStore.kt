package ca.srid.appreciate

import android.content.SharedPreferences
import android.content.Context

/**
 * Persists user preferences via SharedPreferences.
 * Mirror of the macOS SettingsStore.
 */
class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("appreciate_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_REMINDER_TEXT = "reminder_text"
        private const val KEY_MIN_INTERVAL = "min_interval_minutes"
        private const val KEY_MAX_INTERVAL = "max_interval_minutes"
        private const val KEY_DISPLAY_DURATION = "display_duration_seconds"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_LAUNCH_AT_BOOT = "launch_at_boot"

        // SYNC: Default values must match macos/Sources/SettingsStore.swift defaults
        private const val DEFAULT_REMINDER_TEXT =
            "Enjoy & appreciate simply being alive\nEnjoy & appreciate being here now\nEnjoy & appreciate sensuously"
        private const val DEFAULT_MIN_INTERVAL = 1f
        private const val DEFAULT_MAX_INTERVAL = 5f
        private const val DEFAULT_DISPLAY_DURATION = 4f
        private const val DEFAULT_ENABLED = true
        private const val DEFAULT_LAUNCH_AT_BOOT = true
    }

    var reminderText: String
        get() = prefs.getString(KEY_REMINDER_TEXT, DEFAULT_REMINDER_TEXT) ?: DEFAULT_REMINDER_TEXT
        set(value) = prefs.edit().putString(KEY_REMINDER_TEXT, value).apply()

    var minIntervalMinutes: Float
        get() = prefs.getFloat(KEY_MIN_INTERVAL, DEFAULT_MIN_INTERVAL)
        set(value) = prefs.edit().putFloat(KEY_MIN_INTERVAL, value).apply()

    var maxIntervalMinutes: Float
        get() = prefs.getFloat(KEY_MAX_INTERVAL, DEFAULT_MAX_INTERVAL)
        set(value) = prefs.edit().putFloat(KEY_MAX_INTERVAL, value).apply()

    var displayDurationSeconds: Float
        get() = prefs.getFloat(KEY_DISPLAY_DURATION, DEFAULT_DISPLAY_DURATION)
        set(value) = prefs.edit().putFloat(KEY_DISPLAY_DURATION, value).apply()

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, DEFAULT_ENABLED)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var launchAtBoot: Boolean
        get() = prefs.getBoolean(KEY_LAUNCH_AT_BOOT, DEFAULT_LAUNCH_AT_BOOT)
        set(value) = prefs.edit().putBoolean(KEY_LAUNCH_AT_BOOT, value).apply()

    /** Returns a random line from reminderText. */
    val randomLine: String
        get() {
            val lines = reminderText.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            return lines.randomOrNull() ?: reminderText
        }
}
