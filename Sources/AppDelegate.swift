import Cocoa
import SwiftUI

final class AppDelegate: NSObject, NSApplicationDelegate {
    private var statusItem: NSStatusItem!
    private var timerManager: TimerManager!
    private let overlayManager = OverlayManager()
    private let settings = SettingsStore.shared
    private var settingsWindow: NSWindow?
    private var enabledMenuItem: NSMenuItem!

    func applicationDidFinishLaunching(_ notification: Notification) {
        // Activate as accessory (no dock icon)
        NSApp.setActivationPolicy(.accessory)

        setupMenuBar()
        setupTimer()

        if settings.isEnabled {
            timerManager.start()
        }
    }

    // MARK: - Menu Bar

    private func setupMenuBar() {
        statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)

        if let button = statusItem.button {
            button.image = NSImage(systemSymbolName: "sparkles", accessibilityDescription: "Appreciate")
            button.image?.size = NSSize(width: 18, height: 18)
        }

        let menu = NSMenu()

        let showNowItem = NSMenuItem(title: "✨ Show Now", action: #selector(showNow), keyEquivalent: "s")
        showNowItem.target = self
        menu.addItem(showNowItem)

        menu.addItem(.separator())

        enabledMenuItem = NSMenuItem(title: "Enabled", action: #selector(toggleEnabled), keyEquivalent: "e")
        enabledMenuItem.target = self
        enabledMenuItem.state = settings.isEnabled ? .on : .off
        menu.addItem(enabledMenuItem)

        let settingsItem = NSMenuItem(title: "Settings…", action: #selector(openSettings), keyEquivalent: ",")
        settingsItem.target = self
        menu.addItem(settingsItem)

        menu.addItem(.separator())

        let quitItem = NSMenuItem(title: "Quit Appreciate", action: #selector(quit), keyEquivalent: "q")
        quitItem.target = self
        menu.addItem(quitItem)

        statusItem.menu = menu
    }

    // MARK: - Timer

    private func setupTimer() {
        timerManager = TimerManager(settings: settings) { [weak self] in
            self?.showReminder()
        }
    }

    private func showReminder() {
        guard settings.isEnabled else { return }
        overlayManager.showOverlay(
            text: settings.randomLine,
            displayDuration: settings.displayDurationSeconds
        )
    }

    // MARK: - Actions

    @objc private func showNow() {
        showReminder()
    }

    @objc private func toggleEnabled() {
        settings.isEnabled.toggle()
        enabledMenuItem.state = settings.isEnabled ? .on : .off

        if settings.isEnabled {
            timerManager.start()
        } else {
            timerManager.stop()
        }
    }

    @objc private func openSettings() {
        if let existingWindow = settingsWindow {
            existingWindow.makeKeyAndOrderFront(nil)
            NSApp.activate(ignoringOtherApps: true)
            return
        }

        let settingsView = SettingsView(settings: settings) { [weak self] in
            self?.showReminder()
        }

        let window = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 420, height: 500),
            styleMask: [.titled, .closable],
            backing: .buffered,
            defer: false
        )
        window.title = "Appreciate Settings"
        window.contentView = NSHostingView(rootView: settingsView)
        window.center()
        window.isReleasedWhenClosed = false
        window.delegate = self
        window.makeKeyAndOrderFront(nil)
        NSApp.activate(ignoringOtherApps: true)

        settingsWindow = window
    }

    @objc private func quit() {
        NSApp.terminate(nil)
    }
}

// MARK: - NSWindowDelegate

extension AppDelegate: NSWindowDelegate {
    func windowWillClose(_ notification: Notification) {
        if (notification.object as? NSWindow) === settingsWindow {
            settingsWindow = nil
            // Restart timer in case intervals changed
            if settings.isEnabled {
                timerManager.restart()
            }
        }
    }
}
