import Foundation

/// Manages the random-interval timer that triggers overlay displays.
final class TimerManager {
    private var timer: Timer?
    private let settings: SettingsStore
    private let onFire: () -> Void

    init(settings: SettingsStore, onFire: @escaping () -> Void) {
        self.settings = settings
        self.onFire = onFire
    }

    func start() {
        scheduleNext()
    }

    func stop() {
        timer?.invalidate()
        timer = nil
    }

    func restart() {
        stop()
        start()
    }

    private func scheduleNext() {
        timer?.invalidate()

        let minSeconds = settings.minIntervalMinutes * 60.0
        let maxSeconds = settings.maxIntervalMinutes * 60.0
        let interval = Double.random(in: minSeconds...max(minSeconds, maxSeconds))

        NSLog("[Appreciate] Next reminder in %.0f seconds (%.1f min)", interval, interval / 60.0)

        let t = Timer(timeInterval: interval, repeats: false) { [weak self] _ in
            NSLog("[Appreciate] Timer fired — showing reminder")
            self?.onFire()
            self?.scheduleNext()
        }
        // Add to .common mode so it fires even during menu tracking or other modal sessions
        RunLoop.main.add(t, forMode: .common)
        self.timer = t
    }
}
