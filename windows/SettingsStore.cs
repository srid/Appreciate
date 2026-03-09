using System;
using System.IO;
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
        public string ReminderText { get; set; } = "Enjoy & appreciate simply being alive\nEnjoy & appreciate being here now\nEnjoy & appreciate sensuously";
        public float MinIntervalMinutes { get; set; } = 1f;
        public float MaxIntervalMinutes { get; set; } = 5f;
        public float DisplayDurationSeconds { get; set; } = 4f;
        public bool IsEnabled { get; set; } = true;
        public bool LaunchAtLogin { get; set; } = true;

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
