import Foundation
import Combine
import ServiceManagement

/// Persists user preferences via UserDefaults.
final class SettingsStore: ObservableObject {
    static let shared = SettingsStore()

    private let defaults = UserDefaults.standard

    private enum Keys {
        static let packs = "packs"
        static let selectedPack = "selectedPack"
        static let minIntervalMinutes = "minIntervalMinutes"
        static let maxIntervalMinutes = "maxIntervalMinutes"
        static let displayDurationSeconds = "displayDurationSeconds"
        static let isEnabled = "isEnabled"
        static let launchAtLogin = "launchAtLogin"
    }

    // SYNC: Built-in packs must match common/packs.json
    static let defaultPacks: [String: String] = [
        "Actualism Method": "Enjoy & appreciate simply being alive\nEnjoy & appreciate being here now",
        "Sensory": "Notice the play of light and shadow around you\nListen to the layers of sound in this moment\nBreathe in — what scents are in the air?\nNotice any lingering taste in your mouth\nFeel the texture of what your hands are touching\nSense the weight of your body in the chair\nNotice the position of your arms without looking\nFeel your feet planted on the ground\nNotice the temperature where skin meets air\nSense the gentle rise and fall of your breathing\nFeel the subtle pull of gravity on your limbs\nNotice where tension sits in your body right now",
        "Cooking": "Don't forget turkey in the oven",
    ]

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
        set {
            packs[selectedPack] = newValue
        }
    }

    /// Sorted pack names for display.
    var packNames: [String] {
        packs.keys.sorted()
    }

    private init() {
        defaults.register(defaults: [
            Keys.selectedPack: "Actualism Method",
            Keys.minIntervalMinutes: 0.1,
            Keys.maxIntervalMinutes: 1.5,
            Keys.displayDurationSeconds: 6.0,
            Keys.isEnabled: true,
            Keys.launchAtLogin: true,
        ])

        // Load packs from UserDefaults or use defaults
        if let data = defaults.data(forKey: Keys.packs),
           let saved = try? JSONDecoder().decode([String: String].self, from: data) {
            self.packs = saved
        } else {
            self.packs = SettingsStore.defaultPacks
        }

        self.selectedPack = defaults.string(forKey: Keys.selectedPack) ?? "Actualism Method"
        self.minIntervalMinutes = defaults.double(forKey: Keys.minIntervalMinutes)
        self.maxIntervalMinutes = defaults.double(forKey: Keys.maxIntervalMinutes)
        self.displayDurationSeconds = defaults.double(forKey: Keys.displayDurationSeconds)
        self.isEnabled = defaults.bool(forKey: Keys.isEnabled)
        self.launchAtLogin = defaults.bool(forKey: Keys.launchAtLogin)

        // Ensure selected pack exists
        if packs[selectedPack] == nil {
            selectedPack = packs.keys.sorted().first ?? "Actualism Method"
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
            selectedPack = packs.keys.sorted().first ?? "Actualism Method"
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
        let lines = reminderText.components(separatedBy: "\n").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
        return lines.randomElement() ?? reminderText
    }
}
