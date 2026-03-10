package ca.srid.appreciate

import android.content.SharedPreferences
import android.content.Context
import org.json.JSONObject

/**
 * Persists user preferences via SharedPreferences.
 * Mirror of the macOS SettingsStore.
 */
class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("appreciate_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PACKS = "packs_json"
        private const val KEY_SELECTED_PACK = "selected_pack"
        private const val KEY_MIN_INTERVAL = "min_interval_minutes"
        private const val KEY_MAX_INTERVAL = "max_interval_minutes"
        private const val KEY_DISPLAY_DURATION = "display_duration_seconds"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_LAUNCH_AT_BOOT = "launch_at_boot"

        // SYNC: Default values must match macos/Sources/SettingsStore.swift defaults
        private const val DEFAULT_MIN_INTERVAL = 0.1f
        private const val DEFAULT_MAX_INTERVAL = 1.5f
        private const val DEFAULT_DISPLAY_DURATION = 6f
        private const val DEFAULT_ENABLED = true
        private const val DEFAULT_LAUNCH_AT_BOOT = true

        // SYNC: Built-in packs must match common/packs.json
        val DEFAULT_PACKS = linkedMapOf(
            "Actualism Method" to "Enjoy & appreciate simply being alive\nEnjoy & appreciate being here now",
            "Sensory" to "Notice the play of light and shadow around you\nListen to the layers of sound in this moment\nBreathe in \u2014 what scents are in the air?\nNotice any lingering taste in your mouth\nFeel the texture of what your hands are touching\nSense the weight of your body in the chair\nNotice the position of your arms without looking\nFeel your feet planted on the ground\nNotice the temperature where skin meets air\nSense the gentle rise and fall of your breathing\nFeel the subtle pull of gravity on your limbs\nNotice where tension sits in your body right now",
            "Cooking" to "Don't forget turkey in the oven",
        )
    }

    /** User's packs as a mutable map. */
    var packs: LinkedHashMap<String, String>
        get() {
            val json = prefs.getString(KEY_PACKS, null)
            if (json != null) {
                try {
                    val obj = JSONObject(json)
                    val map = linkedMapOf<String, String>()
                    for (key in obj.keys()) {
                        map[key] = obj.getString(key)
                    }
                    return map
                } catch (_: Exception) {}
            }
            return LinkedHashMap(DEFAULT_PACKS)
        }
        set(value) {
            val obj = JSONObject()
            for ((k, v) in value) obj.put(k, v)
            prefs.edit().putString(KEY_PACKS, obj.toString()).apply()
        }

    var selectedPack: String
        get() = prefs.getString(KEY_SELECTED_PACK, "Sensory") ?: "Sensory"
        set(value) = prefs.edit().putString(KEY_SELECTED_PACK, value).apply()

    /** The reminder text of the currently selected pack. */
    var reminderText: String
        get() = packs[selectedPack] ?: ""
        set(value) {
            val p = packs
            p[selectedPack] = value
            packs = p
        }

    /** Sorted pack names for display. */
    val packNames: List<String>
        get() = packs.keys.sorted()

    /** Add a new pack. Returns false if name already exists or is empty. */
    fun addPack(name: String): Boolean {
        if (name.isBlank() || packs.containsKey(name)) return false
        val p = packs
        p[name] = ""
        packs = p
        selectedPack = name
        return true
    }

    /** Delete a pack. Cannot delete the last remaining pack. */
    fun deletePack(name: String): Boolean {
        val p = packs
        if (p.size <= 1 || !p.containsKey(name)) return false
        p.remove(name)
        packs = p
        if (selectedPack == name) {
            selectedPack = p.keys.sorted().first()
        }
        return true
    }

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

    /** Returns a random line from the current pack's reminder text. */
    val randomLine: String
        get() {
            val lines = reminderText.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            return lines.randomOrNull() ?: reminderText
        }
}
