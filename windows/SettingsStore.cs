using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;

namespace Appreciate
{
    /// <summary>
    /// Persists user preferences to %APPDATA%/Appreciate/settings.json.
    /// Default packs are loaded from common/packs.json bundled alongside the exe.
    /// </summary>
    public class SettingsStore
    {
        private static readonly string SettingsDir =
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Appreciate");
        private static readonly string SettingsFile = Path.Combine(SettingsDir, "settings.json");

        private static SettingsStore? _instance;
        public static SettingsStore Instance => _instance ??= Load();

        /// <summary>Loaded from packs.json at startup.</summary>
        private static readonly (Dictionary<string, string> Packs, string DefaultPack) Bundled = LoadBundledPacks();

        private static (Dictionary<string, string>, string) LoadBundledPacks()
        {
            try
            {
                // Look for packs.json next to the executable
                var exeDir = AppDomain.CurrentDomain.BaseDirectory;
                var path = Path.Combine(exeDir, "packs.json");
                if (!File.Exists(path))
                    path = Path.Combine(exeDir, "..", "common", "packs.json");
                if (!File.Exists(path))
                    path = Path.Combine(exeDir, "..", "..", "..", "common", "packs.json");

                if (File.Exists(path))
                {
                    var json = File.ReadAllText(path);
                    using var doc = JsonDocument.Parse(json);
                    var root = doc.RootElement;
                    var defaultPack = root.TryGetProperty("default_pack", out var dp) ? dp.GetString() ?? "" : "";
                    var packs = new Dictionary<string, string>();
                    if (root.TryGetProperty("packs", out var packsEl))
                    {
                        foreach (var prop in packsEl.EnumerateObject())
                        {
                            var lines = new List<string>();
                            foreach (var line in prop.Value.EnumerateArray())
                                lines.Add(line.GetString() ?? "");
                            packs[prop.Name] = string.Join("\n", lines);
                        }
                    }
                    return (packs, defaultPack);
                }
            }
            catch { }
            return (new Dictionary<string, string>(), "");
        }

        /// <summary>User's packs (editable dictionary).</summary>
        public Dictionary<string, string> Packs { get; set; } = new(Bundled.Packs);
        public string SelectedPack { get; set; } = Bundled.DefaultPack;
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
                        store.SelectedPack = store.Packs.Keys.OrderBy(k => k).FirstOrDefault() ?? "";
                    return store;
                }
            }
            catch { }
            return new SettingsStore();
        }
    }
}
