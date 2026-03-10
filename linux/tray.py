#!/usr/bin/env python3
"""
System tray icon for Appreciate (Linux).
Runs as a separate subprocess using GTK3 + AyatanaAppIndicator3.
Communicates with the main GTK4 app via GApplication D-Bus activation.
"""

import os
import signal
import subprocess
import sys

import gi
gi.require_version("Gtk", "3.0")

try:
    gi.require_version("AyatanaAppIndicator3", "0.1")
    from gi.repository import AyatanaAppIndicator3 as AppIndicator3
except (ValueError, ImportError):
    gi.require_version("AppIndicator3", "0.1")
    from gi.repository import AppIndicator3

from gi.repository import Gtk, GLib


def activate_main_app():
    """Activate the main GTK4 app via D-Bus (re-opens settings window)."""
    subprocess.Popen([
        "gdbus", "call", "--session",
        "--dest", "ca.srid.appreciate",
        "--object-path", "/ca/srid/appreciate",
        "--method", "org.gtk.Application.Activate",
        "[]",
    ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def show_overlay():
    """Tell the main app to show an overlay via D-Bus action."""
    subprocess.Popen([
        "gdbus", "call", "--session",
        "--dest", "ca.srid.appreciate",
        "--object-path", "/ca/srid/appreciate",
        "--method", "org.gtk.Actions.Activate",
        "show-now", "[]", "{}",
    ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def quit_app():
    """Quit both the tray and the main app."""
    subprocess.Popen([
        "gdbus", "call", "--session",
        "--dest", "ca.srid.appreciate",
        "--object-path", "/ca/srid/appreciate",
        "--method", "org.gtk.Actions.Activate",
        "quit", "[]", "{}",
    ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    Gtk.main_quit()


def main():
    # Ignore SIGINT so Ctrl+C in main app doesn't kill tray separately
    signal.signal(signal.SIGINT, signal.SIG_DFL)

    indicator = AppIndicator3.Indicator.new(
        "appreciate",
        "dialog-information",
        AppIndicator3.IndicatorCategory.APPLICATION_STATUS,
    )
    indicator.set_status(AppIndicator3.IndicatorStatus.ACTIVE)
    indicator.set_title("Appreciate")

    menu = Gtk.Menu()

    item_settings = Gtk.MenuItem(label="Settings")
    item_settings.connect("activate", lambda _: activate_main_app())
    menu.append(item_settings)

    item_show = Gtk.MenuItem(label="Show Now")
    item_show.connect("activate", lambda _: show_overlay())
    menu.append(item_show)

    menu.append(Gtk.SeparatorMenuItem())

    item_quit = Gtk.MenuItem(label="Quit")
    item_quit.connect("activate", lambda _: quit_app())
    menu.append(item_quit)

    menu.show_all()
    indicator.set_menu(menu)

    Gtk.main()


if __name__ == "__main__":
    main()
