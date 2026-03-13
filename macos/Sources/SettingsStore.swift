import Foundation
import Combine
import ServiceManagement

/// Persists user preferences via UserDefaults.
final class SettingsStore: ObservableObject {
    static let shared = SettingsStore()

    private let defaults = UserDefaults.standard

    // In-memory cache for remote packs
    private static var remoteCache: [String: [String]] = [:]
    private static var remoteFetchTime: [String: Date] = [:]
    private static let cacheDuration: TimeInterval = 3600 // 1 hour

    private enum Keys {
        static let packs = "packs"
        static let selectedPack = "selectedPack"
        static let minIntervalMinutes = "minIntervalMinutes"
        static let maxIntervalMinutes = "maxIntervalMinutes"
        static let displayDurationSeconds = "displayDurationSeconds"
        static let isEnabled = "isEnabled"
        static let launchAtLogin = "launchAtLogin"
    }

    /// Reads default packs from the bundled packs.json (common/packs.json).
    private static func loadBundledPacks() -> (packs: [String: String], defaultPack: String) {
        guard let url = Bundle.main.url(forResource: "packs", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let packsDict = json["packs"] as? [String: [String]] else {
            return (packs: [:], defaultPack: "")
        }
        let defaultPack = json["default_pack"] as? String ?? ""
        var result: [String: String] = [:]
        for (name, lines) in packsDict {
            result[name] = lines.joined(separator: "\n")
        }
        return (packs: result, defaultPack: defaultPack)
    }

    /// The built-in packs loaded from packs.json.
    static let bundled = loadBundledPacks()

    /// User's packs (editable dictionary). Keys = pack names, values = newline-separated reminder text.
    @Published var packs: [String: String] {
        didSet { savePacks() }
    }

    @Published var selectedPack: String {
        didSet {
            defaults.set(selectedPack, forKey: Keys.selectedPack)
            objectWillChange.send()
        }
    }

    @Published var minIntervalMinutes: Double {
        didSet { defaults.set(minIntervalMinutes, forKey: Keys.minIntervalMinutes) }
    }

    @Published var maxIntervalMinutes: Double {
        didSet { defaults.set(maxIntervalMinutes, forKey: Keys.maxIntervalMinutes) }
    }

    @Published var displayDurationSeconds: Double {
        didSet { defaults.set(displayDurationSeconds, forKey: Keys.displayDurationSeconds) }
    }

    @Published var isEnabled: Bool {
        didSet { defaults.set(isEnabled, forKey: Keys.isEnabled) }
    }

    @Published var launchAtLogin: Bool {
        didSet {
            defaults.set(launchAtLogin, forKey: Keys.launchAtLogin)
            updateLoginItem()
        }
    }

    /// The reminder text of the currently selected pack.
    var reminderText: String {
        get { packs[selectedPack] ?? "" }
        set { packs[selectedPack] = newValue }
    }

    /// Sorted pack names for display.
    var packNames: [String] {
        packs.keys.sorted()
    }

    private init() {
        let bundled = SettingsStore.bundled

        defaults.register(defaults: [
            Keys.selectedPack: bundled.defaultPack,
            Keys.minIntervalMinutes: 0.1,
            Keys.maxIntervalMinutes: 1.5,
            Keys.displayDurationSeconds: 6.0,
            Keys.isEnabled: true,
            Keys.launchAtLogin: true,
        ])

        // Load packs from UserDefaults or use bundled defaults
        if let data = defaults.data(forKey: Keys.packs),
           let saved = try? JSONDecoder().decode([String: String].self, from: data) {
            self.packs = saved
        } else {
            self.packs = bundled.packs
        }

        self.selectedPack = defaults.string(forKey: Keys.selectedPack) ?? bundled.defaultPack
        self.minIntervalMinutes = defaults.double(forKey: Keys.minIntervalMinutes)
        self.maxIntervalMinutes = defaults.double(forKey: Keys.maxIntervalMinutes)
        self.displayDurationSeconds = defaults.double(forKey: Keys.displayDurationSeconds)
        self.isEnabled = defaults.bool(forKey: Keys.isEnabled)
        self.launchAtLogin = defaults.bool(forKey: Keys.launchAtLogin)

        // Ensure selected pack exists
        if packs[selectedPack] == nil {
            selectedPack = packs.keys.sorted().first ?? ""
        }

        updateLoginItem()
    }

    private func savePacks() {
        if let data = try? JSONEncoder().encode(packs) {
            defaults.set(data, forKey: Keys.packs)
        }
    }

    /// Add a new pack with the given name. Returns false if name already exists.
    func addPack(name: String) -> Bool {
        guard !name.isEmpty, packs[name] == nil else { return false }
        packs[name] = ""
        selectedPack = name
        return true
    }

    /// Delete a pack. Cannot delete the last remaining pack.
    func deletePack(name: String) -> Bool {
        guard packs.count > 1, packs[name] != nil else { return false }
        packs.removeValue(forKey: name)
        if selectedPack == name {
            selectedPack = packs.keys.sorted().first ?? ""
        }
        return true
    }

    private func updateLoginItem() {
        let service = SMAppService.mainApp
        do {
            if launchAtLogin {
                try service.register()
            } else {
                try service.unregister()
            }
        } catch {
            NSLog("[Appreciate] Login item update failed: %@", error.localizedDescription)
        }
    }

    /// Returns a random line from the current pack's reminder text.
    var randomLine: String {
        let text = reminderText
        if isRemotePack(text) {
            let lines = fetchRemotePack(url: text.trimmingCharacters(in: .whitespacesAndNewlines))
            return lines.randomElement() ?? ""
        }
        let lines = text.components(separatedBy: "\n").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
        return lines.randomElement() ?? text
    }

    private func isRemotePack(_ text: String) -> Bool {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.hasPrefix("https://") && !trimmed.contains("\n")
    }

    private func fetchRemotePack(url: String) -> [String] {
        // Check cache
        if let cached = Self.remoteCache[url],
           let fetchTime = Self.remoteFetchTime[url],
           Date().timeIntervalSince(fetchTime) < Self.cacheDuration {
            return cached
        }

        // Fetch synchronously
        guard let urlObj = URL(string: url) else { return Self.remoteCache[url] ?? [] }
        var request = URLRequest(url: urlObj)
        request.timeoutInterval = 10
        request.httpMethod = "GET"

        do {
            let data = try Data(contentsOf: urlObj, options: .alwaysMapped)
            if let text = String(data: data, encoding: .utf8) {
                let lines = text.components(separatedBy: "\n")
                    .map { $0.trimmingCharacters(in: .whitespaces) }
                    .filter { !$0.isEmpty }
                Self.remoteCache[url] = lines
                Self.remoteFetchTime[url] = Date()
                return lines
            }
        } catch {
            NSLog("[Appreciate] Failed to fetch remote pack: %@", error.localizedDescription)
        }

        return Self.remoteCache[url] ?? []
    }
}
