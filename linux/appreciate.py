#!/usr/bin/env python3
"""
Appreciate — Linux edition.
Periodically flashes a reminder overlay on your screen.

Usage: python appreciate.py
"""

import random
import os
import subprocess
import sys
import gi
gi.require_version("Gtk", "4.0")
gi.require_version("Gdk", "4.0")
from gi.repository import Gtk, Gdk, GLib, Gio, Pango

from settings import SettingsStore
from overlay import show_overlay


class AppreciateApp(Gtk.Application):
    """Main application — manages tray icon, timer, and settings window."""

    def __init__(self):
        super().__init__(application_id="ca.srid.appreciate",
                         flags=Gio.ApplicationFlags.FLAGS_NONE)
        self.settings = SettingsStore()
        self._timer_id = None
        self._settings_window = None
        self._tray_process = None

        # Register D-Bus actions for tray communication
        show_action = Gio.SimpleAction.new("show-now", None)
        show_action.connect("activate", self._show_now)
        self.add_action(show_action)

        quit_action = Gio.SimpleAction.new("quit", None)
        quit_action.connect("activate", lambda *_: self.quit())
        self.add_action(quit_action)

    def do_activate(self):
        if self._settings_window is not None:
            self._settings_window.present()
            return

        # Keep app alive even when settings window is hidden
        self.hold()

        self._create_settings_window()
        self._setup_tray()
        self._schedule_next()

    def do_shutdown(self):
        """Clean up tray subprocess on exit."""
        if self._tray_process:
            self._tray_process.terminate()
        Gtk.Application.do_shutdown(self)

    def _setup_tray(self):
        """Spawn the tray icon as a separate GTK3 process."""
        tray_script = os.path.join(os.path.dirname(os.path.abspath(__file__)), "tray.py")
        if os.path.exists(tray_script):
            try:
                self._tray_process = subprocess.Popen(
                    [sys.executable, tray_script],
                    stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                )
                print("✨ Appreciate is running with tray icon.")
            except Exception as e:
                print(f"Tray icon unavailable: {e}")
                print("   App runs in background. Re-run command to open settings.")
        else:
            print("✨ Appreciate is running in the background.")
            print("   Re-run the command to open settings.")

    def _schedule_next(self):
        """Schedule the next overlay at a random interval."""
        if self._timer_id:
            GLib.source_remove(self._timer_id)
            self._timer_id = None

        if not self.settings.enabled:
            return

        min_s = self.settings.min_interval_minutes * 60
        max_s = self.settings.max_interval_minutes * 60
        interval = random.uniform(min_s, max_s)
        self._timer_id = GLib.timeout_add(int(interval * 1000), self._on_timer)

    def _on_timer(self):
        """Timer fired — show overlay and schedule next."""
        self._timer_id = None
        text = self.settings.random_line
        duration = self.settings.display_duration_seconds
        show_overlay(text, duration)
        self._schedule_next()
        return False  # one-shot

    def _show_now(self, *_args):
        """Immediately show an overlay."""
        text = self.settings.random_line
        duration = self.settings.display_duration_seconds
        show_overlay(text, duration)

    def _create_settings_window(self):
        """Build the settings UI."""
        win = Gtk.ApplicationWindow(application=self, title="Appreciate Settings")
        win.set_default_size(420, 500)
        win.set_resizable(False)

        # Apply dark theme
        display = Gdk.Display.get_default()
        css = Gtk.CssProvider()
        css.load_from_data(b"""
        window {
            background: #1a1a2e;
            color: #f0f0f5;
        }
        .title-label {
            font-size: 20px;
            font-weight: bold;
            color: #f0f0f5;
        }
        .section-label {
            font-size: 14px;
            font-weight: bold;
            color: #f0f0f5;
        }
        .dim-label {
            color: #8a8a9a;
        }
        textview, textview text {
            background: #2a2a3e;
            color: #f0f0f5;
            border-radius: 8px;
            padding: 8px;
        }
        button.show-now {
            background: linear-gradient(135deg, #f27059, #f5a623);
            color: white;
            font-weight: bold;
            border-radius: 24px;
            padding: 10px 32px;
        }
        """)
        Gtk.StyleContext.add_provider_for_display(display, css, Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION)

        box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=16)
        box.set_margin_top(24)
        box.set_margin_bottom(24)
        box.set_margin_start(24)
        box.set_margin_end(24)

        # Title
        title = Gtk.Label(label="Appreciate Settings")
        title.add_css_class("title-label")
        box.append(title)

        # Reminder text
        text_label = Gtk.Label(label="Reminder Text (one per line)")
        text_label.add_css_class("section-label")
        text_label.set_halign(Gtk.Align.START)
        box.append(text_label)

        text_scroll = Gtk.ScrolledWindow()
        text_scroll.set_min_content_height(80)
        text_view = Gtk.TextView()
        text_view.set_wrap_mode(Gtk.WrapMode.WORD)
        text_view.get_buffer().set_text(self.settings.reminder_text)
        text_view.get_buffer().connect("changed", self._on_text_changed)
        text_scroll.set_child(text_view)
        box.append(text_scroll)

        # Enabled toggle
        enable_box = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=12)
        enable_label = Gtk.Label(label="Enabled")
        enable_label.set_hexpand(True)
        enable_label.set_halign(Gtk.Align.START)
        enable_switch = Gtk.Switch()
        enable_switch.set_active(self.settings.enabled)
        enable_switch.connect("state-set", self._on_enabled_changed)
        enable_box.append(enable_label)
        enable_box.append(enable_switch)
        box.append(enable_box)

        # Interval range
        interval_label = Gtk.Label(label="Interval Range")
        interval_label.add_css_class("section-label")
        interval_label.set_halign(Gtk.Align.START)
        box.append(interval_label)

        # Min interval
        min_box = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=8)
        min_text = Gtk.Label(label="Min:")
        min_text.set_size_request(40, -1)
        self._min_value_label = Gtk.Label()
        self._min_value_label.set_size_request(50, -1)
        self._min_value_label.set_halign(Gtk.Align.END)
        min_scale = Gtk.Scale.new_with_range(Gtk.Orientation.HORIZONTAL, 0.1, 10.0, 0.1)
        min_scale.set_value(self.settings.min_interval_minutes)
        min_scale.set_draw_value(False)
        min_scale.set_hexpand(True)
        min_scale.connect("value-changed", self._on_min_changed)
        self._update_interval_label(self._min_value_label, self.settings.min_interval_minutes)
        min_box.append(min_text)
        min_box.append(min_scale)
        min_box.append(self._min_value_label)
        box.append(min_box)

        # Max interval
        max_box = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=8)
        max_text = Gtk.Label(label="Max:")
        max_text.set_size_request(40, -1)
        self._max_value_label = Gtk.Label()
        self._max_value_label.set_size_request(50, -1)
        self._max_value_label.set_halign(Gtk.Align.END)
        max_scale = Gtk.Scale.new_with_range(Gtk.Orientation.HORIZONTAL, 0.1, 30.0, 0.1)
        max_scale.set_value(self.settings.max_interval_minutes)
        max_scale.set_draw_value(False)
        max_scale.set_hexpand(True)
        max_scale.connect("value-changed", self._on_max_changed)
        self._update_interval_label(self._max_value_label, self.settings.max_interval_minutes)
        max_box.append(max_text)
        max_box.append(max_scale)
        max_box.append(self._max_value_label)
        box.append(max_box)

        # Display duration
        dur_label = Gtk.Label(label="Display Duration")
        dur_label.add_css_class("section-label")
        dur_label.set_halign(Gtk.Align.START)
        box.append(dur_label)

        dur_box = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=8)
        self._dur_value_label = Gtk.Label(label=f"{self.settings.display_duration_seconds:.1f}s")
        self._dur_value_label.set_size_request(50, -1)
        self._dur_value_label.set_halign(Gtk.Align.END)
        dur_scale = Gtk.Scale.new_with_range(Gtk.Orientation.HORIZONTAL, 1.0, 15.0, 0.5)
        dur_scale.set_value(self.settings.display_duration_seconds)
        dur_scale.set_draw_value(False)
        dur_scale.set_hexpand(True)
        dur_scale.connect("value-changed", self._on_duration_changed)
        dur_box.append(dur_scale)
        dur_box.append(self._dur_value_label)
        box.append(dur_box)

        # Show Now button
        show_btn = Gtk.Button(label="✨ Show Now")
        show_btn.add_css_class("show-now")
        show_btn.connect("clicked", self._show_now)
        box.append(show_btn)

        win.set_child(box)
        win.connect("close-request", self._on_close_request)
        self._settings_window = win
        win.present()

    def _on_close_request(self, *_args):
        """Hide window instead of quitting — keep running in background."""
        self._settings_window.set_visible(False)
        return True  # prevent destroy

    def _on_text_changed(self, buffer):
        start, end = buffer.get_bounds()
        self.settings.reminder_text = buffer.get_text(start, end, False)

    def _on_enabled_changed(self, switch, state):
        self.settings.enabled = state
        if state:
            self._schedule_next()
        elif self._timer_id:
            GLib.source_remove(self._timer_id)
            self._timer_id = None

    def _on_min_changed(self, scale):
        val = round(scale.get_value(), 1)
        self.settings.min_interval_minutes = val
        self._update_interval_label(self._min_value_label, val)
        self._schedule_next()

    def _on_max_changed(self, scale):
        val = round(scale.get_value(), 1)
        self.settings.max_interval_minutes = val
        self._update_interval_label(self._max_value_label, val)
        self._schedule_next()

    def _on_duration_changed(self, scale):
        val = round(scale.get_value(), 1)
        self.settings.display_duration_seconds = val
        self._dur_value_label.set_text(f"{val:.1f}s")

    @staticmethod
    def _update_interval_label(label, minutes):
        if minutes < 1:
            label.set_text(f"{int(minutes * 60)}s")
        else:
            label.set_text(f"{minutes:.1f}m")


def main():
    app = AppreciateApp()
    app.run(sys.argv)


if __name__ == "__main__":
    main()
