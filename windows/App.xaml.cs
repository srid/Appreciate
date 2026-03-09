using System;
using System.Collections.Generic;
using System.Windows;

namespace Appreciate
{
    public partial class App : Application
    {
        private TrayIcon? _trayIcon;
        private TimerManager? _timerManager;
        private SettingsWindow? _settingsWindow;
        private readonly List<OverlayWindow> _activeOverlays = new();

        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);

            // Setup tray icon
            _trayIcon = new TrayIcon();
            _trayIcon.ShowNowRequested += ShowOverlay;
            _trayIcon.SettingsRequested += ShowSettings;
            _trayIcon.QuitRequested += () =>
            {
                _timerManager?.Stop();
                _trayIcon?.Dispose();
                Shutdown();
            };

            // Setup timer
            _timerManager = new TimerManager();
            _timerManager.OnTick += () => Dispatcher.Invoke(ShowOverlay);

            // Auto-start timer if enabled
            if (SettingsStore.Instance.IsEnabled)
                _timerManager.Start();

            // Setup auto-start registry
            TrayIcon.UpdateAutoStart(SettingsStore.Instance.LaunchAtLogin);
        }

        private void ShowOverlay()
        {
            var settings = SettingsStore.Instance;
            var text = settings.RandomLine;
            var duration = settings.DisplayDurationSeconds;

            // Show on all monitors
            foreach (var screen in System.Windows.Forms.Screen.AllScreens)
            {
                var overlay = new OverlayWindow(
                    text, duration,
                    screen.Bounds.Left, screen.Bounds.Top,
                    screen.Bounds.Width, screen.Bounds.Height
                );
                _activeOverlays.Add(overlay);
                overlay.Closed += (_, _) => _activeOverlays.Remove(overlay);
                overlay.Show();
            }
        }

        private void ShowSettings()
        {
            if (_settingsWindow == null)
            {
                _settingsWindow = new SettingsWindow();
                _settingsWindow.ShowNowRequested += ShowOverlay;
                _settingsWindow.Closed += (_, _) =>
                {
                    _settingsWindow = null;
                    // Restart timer with new settings
                    _timerManager?.Stop();
                    if (SettingsStore.Instance.IsEnabled)
                        _timerManager?.Start();
                };
            }
            _settingsWindow.Show();
            _settingsWindow.Activate();
        }
    }
}
