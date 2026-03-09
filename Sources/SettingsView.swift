import SwiftUI

/// Settings window for customizing reminder text, intervals, and display duration.
struct SettingsView: View {
    @ObservedObject var settings: SettingsStore
    var onShowNow: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            // Header
            Text("Appreciate Settings")
                .font(.title2.bold())

            // Reminder text
            VStack(alignment: .leading, spacing: 6) {
                Text("Reminder Text")
                    .font(.headline)
                TextEditor(text: $settings.reminderText)
                    .font(.body)
                    .frame(minHeight: 60, maxHeight: 100)
                    .border(Color.secondary.opacity(0.3), width: 1)
            }

            Divider()

            // Interval range
            VStack(alignment: .leading, spacing: 12) {
                Text("Interval Range")
                    .font(.headline)

                HStack {
                    Text("Min:")
                        .frame(width: 35, alignment: .trailing)
                    Slider(value: $settings.minIntervalMinutes, in: 0.1...60, step: 0.5)
                    Text("\(formatInterval(settings.minIntervalMinutes))")
                        .frame(width: 60, alignment: .trailing)
                        .monospacedDigit()
                }

                HStack {
                    Text("Max:")
                        .frame(width: 35, alignment: .trailing)
                    Slider(value: $settings.maxIntervalMinutes, in: 0.5...120, step: 0.5)
                    Text("\(formatInterval(settings.maxIntervalMinutes))")
                        .frame(width: 60, alignment: .trailing)
                        .monospacedDigit()
                }
            }

            // Display duration
            VStack(alignment: .leading, spacing: 6) {
                Text("Display Duration")
                    .font(.headline)
                HStack {
                    Slider(value: $settings.displayDurationSeconds, in: 2...10, step: 0.5)
                    Text("\(settings.displayDurationSeconds, specifier: "%.1f")s")
                        .frame(width: 40, alignment: .trailing)
                        .monospacedDigit()
                }
            }

            Divider()

            // Show Now button
            HStack {
                Spacer()
                Button("✨ Show Now") {
                    onShowNow()
                }
                .controlSize(.large)
                Spacer()
            }
        }
        .padding(24)
        .frame(width: 420)
    }

    private func formatInterval(_ minutes: Double) -> String {
        if minutes < 1 {
            return "\(Int(minutes * 60))s"
        } else if minutes == floor(minutes) {
            return "\(Int(minutes))m"
        } else {
            return String(format: "%.1fm", minutes)
        }
    }
}
