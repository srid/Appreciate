"""
Settings store for Appreciate (Linux).
Persists user preferences to ~/.config/appreciate/settings.json.

SYNC: Default values must match macos/Sources/SettingsStore.swift,
      android/.../SettingsStore.kt, and windows/SettingsStore.cs.
"""

import json
import os
import random


# SYNC: Built-in packs must match common/packs.json (updated after final sync)
DEFAULT_PACKS = {
    "Actualism Method": "Enjoy & appreciate simply being alive\nEnjoy & appreciate being here now",
    "Sensory": "Notice the play of light and shadow around you\nListen to the layers of sound in this moment\nBreathe in \u2014 what scents are in the air?\nNotice any lingering taste in your mouth\nFeel the texture of what your hands are touching\nSense the weight of your body in the chair\nNotice the position of your arms without looking\nFeel your feet planted on the ground\nNotice the temperature where skin meets air\nSense the gentle rise and fall of your breathing\nFeel the subtle pull of gravity on your limbs\nNotice where tension sits in your body right now",
    "Cooking": "Don't forget turkey in the oven",
}

# SYNC: Default values — keep in sync across all platforms
DEFAULTS = {
    "packs": dict(DEFAULT_PACKS),
    "selected_pack": "Sensory",
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
        self._data = {k: (dict(v) if isinstance(v, dict) else v) for k, v in DEFAULTS.items()}
        self._load()
        # Ensure selected pack exists
        if self.selected_pack not in self.packs:
            self.selected_pack = sorted(self.packs.keys())[0] if self.packs else "Sensory"

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

    # --- Packs ---

    @property
    def packs(self):
        return self._data["packs"]

    @packs.setter
    def packs(self, value):
        self._data["packs"] = value
        self.save()

    @property
    def selected_pack(self):
        return self._data["selected_pack"]

    @selected_pack.setter
    def selected_pack(self, value):
        self._data["selected_pack"] = value
        self.save()

    @property
    def reminder_text(self):
        return self.packs.get(self.selected_pack, "")

    @reminder_text.setter
    def reminder_text(self, value):
        self.packs[self.selected_pack] = value
        self.save()

    @property
    def pack_names(self):
        return sorted(self.packs.keys())

    def add_pack(self, name):
        """Add a new pack. Returns False if name already exists."""
        if not name or name in self.packs:
            return False
        self.packs[name] = ""
        self.selected_pack = name
        self.save()
        return True

    def delete_pack(self, name):
        """Delete a pack. Cannot delete the last remaining pack."""
        if len(self.packs) <= 1 or name not in self.packs:
            return False
        del self.packs[name]
        if self.selected_pack == name:
            self.selected_pack = sorted(self.packs.keys())[0]
        self.save()
        return True

    # --- Other settings ---

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
        """Returns a random line from the current pack's reminder text."""
        lines = [l.strip() for l in self.reminder_text.split("\n") if l.strip()]
        return random.choice(lines) if lines else self.reminder_text
