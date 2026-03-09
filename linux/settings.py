"""
Settings store for Appreciate (Linux).
Persists user preferences to ~/.config/appreciate/settings.json.

SYNC: Default values must match macos/Sources/SettingsStore.swift,
      android/.../SettingsStore.kt, and windows/SettingsStore.cs.
"""

import json
import os
import random


# SYNC: Default values — keep in sync across all platforms
DEFAULTS = {
    "reminder_text": "Enjoy & appreciate simply being alive\nEnjoy & appreciate being here now",
    "min_interval_minutes": 0.1,
    "max_interval_minutes": 1.5,
    "display_duration_seconds": 6.0,
    "enabled": True,
    "launch_at_login": True,
}

CONFIG_DIR = os.path.join(os.environ.get("XDG_CONFIG_HOME", os.path.expanduser("~/.config")), "appreciate")
CONFIG_FILE = os.path.join(CONFIG_DIR, "settings.json")


class SettingsStore:
    """Persists user preferences to JSON."""

    def __init__(self):
        self._data = dict(DEFAULTS)
        self._load()

    def _load(self):
        try:
            if os.path.exists(CONFIG_FILE):
                with open(CONFIG_FILE, "r") as f:
                    saved = json.load(f)
                    self._data.update(saved)
        except Exception:
            pass

    def save(self):
        os.makedirs(CONFIG_DIR, exist_ok=True)
        with open(CONFIG_FILE, "w") as f:
            json.dump(self._data, f, indent=2)

    @property
    def reminder_text(self):
        return self._data["reminder_text"]

    @reminder_text.setter
    def reminder_text(self, value):
        self._data["reminder_text"] = value
        self.save()

    @property
    def min_interval_minutes(self):
        return self._data["min_interval_minutes"]

    @min_interval_minutes.setter
    def min_interval_minutes(self, value):
        self._data["min_interval_minutes"] = value
        self.save()

    @property
    def max_interval_minutes(self):
        return self._data["max_interval_minutes"]

    @max_interval_minutes.setter
    def max_interval_minutes(self, value):
        self._data["max_interval_minutes"] = value
        self.save()

    @property
    def display_duration_seconds(self):
        return self._data["display_duration_seconds"]

    @display_duration_seconds.setter
    def display_duration_seconds(self, value):
        self._data["display_duration_seconds"] = value
        self.save()

    @property
    def enabled(self):
        return self._data["enabled"]

    @enabled.setter
    def enabled(self, value):
        self._data["enabled"] = value
        self.save()

    @property
    def launch_at_login(self):
        return self._data["launch_at_login"]

    @launch_at_login.setter
    def launch_at_login(self, value):
        self._data["launch_at_login"] = value
        self.save()

    @property
    def random_line(self):
        """Returns a random line from reminder_text."""
        lines = [l.strip() for l in self.reminder_text.split("\n") if l.strip()]
        return random.choice(lines) if lines else self.reminder_text
