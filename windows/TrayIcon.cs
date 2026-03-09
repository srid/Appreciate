using System;
using System.Drawing;
using System.Windows.Forms;
using Microsoft.Win32;

namespace Appreciate
{
    /// <summary>
    /// System tray icon with context menu.
    /// Manages auto-start via the Windows Registry Run key.
    /// </summary>
    public class TrayIcon : IDisposable
    {
        private readonly NotifyIcon _notifyIcon;
        private readonly ToolStripMenuItem _enabledItem;
        private const string AppName = "Appreciate";
        private const string RunKey = @"SOFTWARE\Microsoft\Windows\CurrentVersion\Run";

        public event Action? ShowNowRequested;
        public event Action? SettingsRequested;
        public event Action? QuitRequested;

        public TrayIcon()
        {
            _notifyIcon = new NotifyIcon
            {
                Text = "Appreciate ✨",
                Visible = true
            };

            // Use a simple star icon from system resources
            _notifyIcon.Icon = SystemIcons.Application;

            _enabledItem = new ToolStripMenuItem("Enabled")
            {
                Checked = SettingsStore.Instance.IsEnabled,
                CheckOnClick = true
            };
            _enabledItem.CheckedChanged += (_, _) =>
            {
                SettingsStore.Instance.IsEnabled = _enabledItem.Checked;
                SettingsStore.Instance.Save();
            };

            var menu = new ContextMenuStrip();
            menu.Items.Add("✨ Show Now", null, (_, _) => ShowNowRequested?.Invoke());
            menu.Items.Add(new ToolStripSeparator());
            menu.Items.Add(_enabledItem);
            menu.Items.Add("Settings…", null, (_, _) => SettingsRequested?.Invoke());
            menu.Items.Add(new ToolStripSeparator());
            menu.Items.Add("Quit", null, (_, _) => QuitRequested?.Invoke());

            _notifyIcon.ContextMenuStrip = menu;
            _notifyIcon.DoubleClick += (_, _) => SettingsRequested?.Invoke();
        }

        public static void UpdateAutoStart(bool enabled)
        {
            try
            {
                using var key = Registry.CurrentUser.OpenSubKey(RunKey, true);
                if (key == null) return;

                if (enabled)
                {
                    var exePath = Environment.ProcessPath;
                    if (exePath != null)
                        key.SetValue(AppName, $"\"{exePath}\"");
                }
                else
                {
                    key.DeleteValue(AppName, false);
                }
            }
            catch { }
        }

        public void Dispose()
        {
            _notifyIcon.Visible = false;
            _notifyIcon.Dispose();
        }
    }
}
