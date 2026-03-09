import Foundation
import Combine

/// Persists user preferences via UserDefaults.
final class SettingsStore: ObservableObject {
    static let shared = SettingsStore()

    private let defaults = UserDefaults.standard

    private enum Keys {
        static let reminderText = "reminderText"
        static let minIntervalMinutes = "minIntervalMinutes"
        static let maxIntervalMinutes = "maxIntervalMinutes"
        static let displayDurationSeconds = "displayDurationSeconds"
        static let isEnabled = "isEnabled"
    }

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

    @Published var isEnabled: Bool {
        didSet { defaults.set(isEnabled, forKey: Keys.isEnabled) }
    }

    private init() {
        // Register defaults
        defaults.register(defaults: [
            Keys.reminderText: "Enjoy & appreciate simply being alive\nEnjoy & appreciate being here now\nEnjoy & appreciate sensuously",
            Keys.minIntervalMinutes: 1.0,
            Keys.maxIntervalMinutes: 5.0,
            Keys.displayDurationSeconds: 4.0,
            Keys.isEnabled: true,
        ])

        self.reminderText = defaults.string(forKey: Keys.reminderText) ?? "Enjoy & appreciate simply being alive\nEnjoy & appreciate being here now\nEnjoy & appreciate sensuously"
        self.minIntervalMinutes = defaults.double(forKey: Keys.minIntervalMinutes)
        self.maxIntervalMinutes = defaults.double(forKey: Keys.maxIntervalMinutes)
        self.displayDurationSeconds = defaults.double(forKey: Keys.displayDurationSeconds)
        self.isEnabled = defaults.bool(forKey: Keys.isEnabled)
    }

    /// Returns a random line from reminderText. If only one line, returns it directly.
    var randomLine: String {
        let lines = reminderText.components(separatedBy: "\n").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
        return lines.randomElement() ?? reminderText
    }
}
