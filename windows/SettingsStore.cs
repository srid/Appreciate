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

        // SYNC: Default values must match macos/Sources/SettingsStore.swift and android/.../SettingsStore.kt
        public string ReminderText { get; set; } = "Enjoy & appreciate simply being alive\nEnjoy & appreciate being here now";
        public string SelectedPack { get; set; } = "Actualist";
        public float MinIntervalMinutes { get; set; } = 0.1f;
        public float MaxIntervalMinutes { get; set; } = 1.5f;
        public float DisplayDurationSeconds { get; set; } = 6f;
        public bool IsEnabled { get; set; } = true;
        public bool LaunchAtLogin { get; set; } = true;

        // SYNC: Packs must match common/packs.json
        public static readonly Dictionary<string, string> Packs = new()
        {
            ["Actualist"] = "Enjoy & appreciate simply being alive\nEnjoy & appreciate being here now",
            ["Sensory"] = "What can you hear right now?\nFeel the air on your skin\nNotice the colours around you",
            ["Cooking"] = "Don't forget turkey in the oven",
        };
        public static readonly string[] PackNames = Packs.Keys.Append("Custom").ToArray();

        public void SelectPack(string name)
        {
            if (Packs.TryGetValue(name, out var text))
            {
                ReminderText = text;
                SelectedPack = name;
                Save();
            }
        }

        public void CheckCustomPack()
        {
            var trimmed = ReminderText.Trim();
            if (!Packs.Values.Any(v => v.Trim() == trimmed))
            {
                SelectedPack = "Custom";
            }
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
                    return JsonSerializer.Deserialize<SettingsStore>(json) ?? new SettingsStore();
                }
            }
            catch { }
            return new SettingsStore();
        }
    }
}
