package ca.srid.appreciate

import android.content.SharedPreferences
import android.content.Context
import org.json.JSONObject

/**
 * Persists user preferences via SharedPreferences.
 * Default packs are loaded from assets/packs.json (common/packs.json).
 */
class SettingsStore(private val context: Context) {
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
        private const val KEY_VOICE_WHEN_HEADPHONES = "voice_when_headphones"

        private const val DEFAULT_MIN_INTERVAL = 0.1f
        private const val DEFAULT_MAX_INTERVAL = 1.5f
        private const val DEFAULT_DISPLAY_DURATION = 6f
        private const val DEFAULT_ENABLED = true
        private const val DEFAULT_LAUNCH_AT_BOOT = true
        private const val DEFAULT_VOICE_WHEN_HEADPHONES = true
    }

    /** Reads default packs from assets/packs.json. */
    private fun loadBundledPacks(): Pair<LinkedHashMap<String, String>, String> {
        try {
            val json = context.assets.open("packs.json").bufferedReader().readText()
            val obj = JSONObject(json)
            val packsObj = obj.getJSONObject("packs")
            val defaultPack = obj.optString("default_pack", "")
            val map = linkedMapOf<String, String>()
            for (key in packsObj.keys()) {
                val arr = packsObj.getJSONArray(key)
                val lines = (0 until arr.length()).joinToString("\n") { arr.getString(it) }
                map[key] = lines
            }
            return Pair(map, defaultPack)
        } catch (_: Exception) {
            return Pair(linkedMapOf(), "")
        }
    }

    private val bundled = loadBundledPacks()
    private val bundledPacks get() = bundled.first
    private val bundledDefaultPack get() = bundled.second

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
            return LinkedHashMap(bundledPacks)
        }
        set(value) {
            val obj = JSONObject()
            for ((k, v) in value) obj.put(k, v)
            prefs.edit().putString(KEY_PACKS, obj.toString()).apply()
        }

    var selectedPack: String
        get() = prefs.getString(KEY_SELECTED_PACK, bundledDefaultPack) ?: bundledDefaultPack
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

    var voiceWhenHeadphones: Boolean
        get() = prefs.getBoolean(KEY_VOICE_WHEN_HEADPHONES, DEFAULT_VOICE_WHEN_HEADPHONES)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_WHEN_HEADPHONES, value).apply()

    /** Returns a random line from the current pack's reminder text. */
    val randomLine: String
        get() {
            val lines = reminderText.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            return lines.randomOrNull() ?: reminderText
        }
}
