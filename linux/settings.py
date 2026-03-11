"""
Settings store for Appreciate (Linux).
Persists user preferences to ~/.config/appreciate/settings.json.
Default packs are loaded from packs.json bundled alongside the .py files.
"""

import json
import os
import random
import time
import urllib.request


def _load_bundled_packs():
    """Read default packs from packs.json (common/packs.json) bundled with the app."""
    # Look for packs.json next to this script, then in ../common/
    script_dir = os.path.dirname(os.path.abspath(__file__))
    candidates = [
        os.path.join(script_dir, "packs.json"),
        os.path.join(script_dir, "..", "common", "packs.json"),
    ]
    for path in candidates:
        if os.path.exists(path):
            try:
                with open(path, "r") as f:
                    data = json.load(f)
                packs_raw = data.get("packs", {})
                default_pack = data.get("default_pack", "")
                packs = {name: "\n".join(lines) for name, lines in packs_raw.items()}
                return packs, default_pack
            except Exception:
                pass
    return {}, ""


_BUNDLED_PACKS, _BUNDLED_DEFAULT_PACK = _load_bundled_packs()

# Default values — only non-pack settings are hardcoded here
DEFAULTS = {
    "packs": dict(_BUNDLED_PACKS),
    "selected_pack": _BUNDLED_DEFAULT_PACK,
    "min_interval_minutes": 0.1,
    "max_interval_minutes": 1.5,
    "display_duration_seconds": 6.0,
    "enabled": True,
    "launch_at_login": True,
}

CONFIG_DIR = os.path.join(
    os.environ.get("XDG_CONFIG_HOME", os.path.expanduser("~/.config")), "appreciate"
)
CONFIG_FILE = os.path.join(CONFIG_DIR, "settings.json")

# In-memory cache for remote packs
_remote_cache: dict[str, list[str]] = {}
_remote_fetch_time: dict[str, float] = {}
_CACHE_DURATION = 3600  # 1 hour


def _is_remote_pack(text: str) -> bool:
    trimmed = text.strip()
    return trimmed.startswith("https://") and "\n" not in trimmed


def _fetch_remote_pack(url: str) -> list[str]:
    now = time.time()

    # Check cache
    if url in _remote_cache:
        fetch_time = _remote_fetch_time.get(url, 0)
        if now - fetch_time < _CACHE_DURATION:
            return _remote_cache[url]

    # Fetch
    try:
        with urllib.request.urlopen(url, timeout=10) as response:
            text = response.read().decode("utf-8")
            lines = [l.strip() for l in text.split("\n") if l.strip()]
            _remote_cache[url] = lines
            _remote_fetch_time[url] = now
            return lines
    except Exception:
        pass

    return _remote_cache.get(url, [])


class SettingsStore:
    """Persists user preferences to JSON."""

    def __init__(self):
        self._data = {
            k: (dict(v) if isinstance(v, dict) else v) for k, v in DEFAULTS.items()
        }
        self._load()
        # Ensure selected pack exists
        if self.selected_pack not in self.packs:
            self.selected_pack = sorted(self.packs.keys())[0] if self.packs else ""

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
        text = self.reminder_text
        if _is_remote_pack(text):
            lines = _fetch_remote_pack(text.strip())
            return random.choice(lines) if lines else ""
        lines = [l.strip() for l in text.split("\n") if l.strip()]
        return random.choice(lines) if lines else text
