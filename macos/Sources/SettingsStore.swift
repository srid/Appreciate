import Foundation
import Combine
import ServiceManagement

/// Persists user preferences via UserDefaults.
final class SettingsStore: ObservableObject {
    static let shared = SettingsStore()

    private let defaults = UserDefaults.standard

    private enum Keys {
        static let reminderText = "reminderText"
        static let selectedPack = "selectedPack"
        static let minIntervalMinutes = "minIntervalMinutes"
        static let maxIntervalMinutes = "maxIntervalMinutes"
        static let displayDurationSeconds = "displayDurationSeconds"
        static let isEnabled = "isEnabled"
        static let launchAtLogin = "launchAtLogin"
    }

    // SYNC: Packs must match common/packs.json
    static let packs: [(name: String, text: String)] = [
        ("Actualist", "Enjoy & appreciate simply being alive\nEnjoy & appreciate being here now"),
        ("Sensory", "What can you hear right now?\nFeel the air on your skin\nNotice the colours around you"),
        ("Cooking", "Don't forget turkey in the oven"),
    ]
    static let packNames: [String] = packs.map { $0.name } + ["Custom"]

    @Published var reminderText: String {
        didSet { defaults.set(reminderText, forKey: Keys.reminderText) }
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

    @Published var selectedPack: String {
        didSet { defaults.set(selectedPack, forKey: Keys.selectedPack) }
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

    private init() {
        // SYNC: Default values must match android/.../SettingsStore.kt defaults
        defaults.register(defaults: [
            Keys.reminderText: "Enjoy & appreciate simply being alive\nEnjoy & appreciate being here now",
            Keys.selectedPack: "Actualist",
            Keys.minIntervalMinutes: 0.1,
            Keys.maxIntervalMinutes: 1.5,
            Keys.displayDurationSeconds: 6.0,
            Keys.isEnabled: true,
            Keys.launchAtLogin: true,
        ])

        self.reminderText = defaults.string(forKey: Keys.reminderText) ?? "Enjoy & appreciate simply being alive\nEnjoy & appreciate being here now"
        self.selectedPack = defaults.string(forKey: Keys.selectedPack) ?? "Actualist"
        self.minIntervalMinutes = defaults.double(forKey: Keys.minIntervalMinutes)
        self.maxIntervalMinutes = defaults.double(forKey: Keys.maxIntervalMinutes)
        self.displayDurationSeconds = defaults.double(forKey: Keys.displayDurationSeconds)
        self.isEnabled = defaults.bool(forKey: Keys.isEnabled)
        self.launchAtLogin = defaults.bool(forKey: Keys.launchAtLogin)
        updateLoginItem()
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

    /// Select a pack by name, updating reminderText.
    func selectPack(_ name: String) {
        if let pack = SettingsStore.packs.first(where: { $0.name == name }) {
            reminderText = pack.text
            selectedPack = name
        }
    }

    /// Called when user edits text — auto-switch to Custom if it no longer matches a pack.
    func checkCustomPack() {
        let trimmed = reminderText.trimmingCharacters(in: .whitespacesAndNewlines)
        if !SettingsStore.packs.contains(where: { $0.text.trimmingCharacters(in: .whitespacesAndNewlines) == trimmed }) {
            selectedPack = "Custom"
        }
    }

    /// Returns a random line from reminderText. If only one line, returns it directly.
    var randomLine: String {
        let lines = reminderText.components(separatedBy: "\n").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
        return lines.randomElement() ?? reminderText
    }
}
