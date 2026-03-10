using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;

namespace Appreciate
{
    /// <summary>
    /// Persists user preferences to %APPDATA%/Appreciate/settings.json.
    /// </summary>
    public class SettingsStore
    {
        private static readonly string SettingsDir =
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Appreciate");
        private static readonly string SettingsFile = Path.Combine(SettingsDir, "settings.json");

        private static SettingsStore? _instance;
        public static SettingsStore Instance => _instance ??= Load();

        // SYNC: Built-in packs must match common/packs.json
        public static readonly Dictionary<string, string> DefaultPacks = new()
        {
            ["Actualism Method"] = "Enjoy & appreciate simply being alive\nEnjoy & appreciate being here now",
            ["Sensory"] = "Notice the play of light and shadow around you\nListen to the layers of sound in this moment\nBreathe in — what scents are in the air?\nNotice any lingering taste in your mouth\nFeel the texture of what your hands are touching\nSense the weight of your body in the chair\nNotice the position of your arms without looking\nFeel your feet planted on the ground\nNotice the temperature where skin meets air\nSense the gentle rise and fall of your breathing\nFeel the subtle pull of gravity on your limbs\nNotice where tension sits in your body right now",
            ["Cooking"] = "Don't forget turkey in the oven",
        };

        /// <summary>User's packs (editable dictionary).</summary>
        public Dictionary<string, string> Packs { get; set; } = new(DefaultPacks);
        public string SelectedPack { get; set; } = "Actualism Method";
        public float MinIntervalMinutes { get; set; } = 0.1f;
        public float MaxIntervalMinutes { get; set; } = 1.5f;
        public float DisplayDurationSeconds { get; set; } = 6f;
        public bool IsEnabled { get; set; } = true;
        public bool LaunchAtLogin { get; set; } = true;

        /// <summary>The reminder text of the current pack.</summary>
        public string ReminderText
        {
            get => Packs.TryGetValue(SelectedPack, out var text) ? text : "";
            set { Packs[SelectedPack] = value; }
        }

        /// <summary>Sorted pack names for display.</summary>
        public string[] PackNames => Packs.Keys.OrderBy(k => k).ToArray();

        /// <summary>Add a new pack. Returns false if name already exists.</summary>
        public bool AddPack(string name)
        {
            if (string.IsNullOrWhiteSpace(name) || Packs.ContainsKey(name)) return false;
            Packs[name] = "";
            SelectedPack = name;
            Save();
            return true;
        }

        /// <summary>Delete a pack. Cannot delete last remaining pack.</summary>
        public bool DeletePack(string name)
        {
            if (Packs.Count <= 1 || !Packs.ContainsKey(name)) return false;
            Packs.Remove(name);
            if (SelectedPack == name)
                SelectedPack = Packs.Keys.OrderBy(k => k).First();
            Save();
            return true;
        }

        /// <summary>Returns a random line from ReminderText.</summary>
        public string RandomLine
        {
            get
            {
                var lines = ReminderText.Split('\n', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
                if (lines.Length == 0) return ReminderText;
                return lines[Random.Shared.Next(lines.Length)];
            }
        }

        public void Save()
        {
            Directory.CreateDirectory(SettingsDir);
            var json = JsonSerializer.Serialize(this, new JsonSerializerOptions { WriteIndented = true });
            File.WriteAllText(SettingsFile, json);
        }

        private static SettingsStore Load()
        {
            try
            {
                if (File.Exists(SettingsFile))
                {
                    var json = File.ReadAllText(SettingsFile);
                    var store = JsonSerializer.Deserialize<SettingsStore>(json) ?? new SettingsStore();
                    // Ensure selected pack exists
                    if (!store.Packs.ContainsKey(store.SelectedPack))
                        store.SelectedPack = store.Packs.Keys.OrderBy(k => k).FirstOrDefault() ?? "Actualism Method";
                    return store;
                }
            }
            catch { }
            return new SettingsStore();
        }
    }
}
