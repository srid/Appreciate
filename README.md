<p align="center">
  <img src="AppIcon.png" width="128" height="128" alt="Appreciate icon">
</p>

# Appreciate ✨

A tiny macOS menubar app that periodically flashes a reminder across your screen — **"Enjoy and appreciate being alive"** — to nudge you back into the present moment.

The text is rendered as a transparent overlay directly on your desktop (not a system notification), then fades away. Interval, position, font size, color, and style are all **randomized** to prevent habituation.

## Features

- 🎯 **Menubar-only** — no Dock icon, stays out of your way
- 🖥️ **Screen overlay** — text is painted on screen above all windows, click-through
- 🎲 **Anti-habituation** — randomized interval, position, font size, weight, design, color, and rotation
- ⚙️ **Configurable** — edit the reminder text, interval range, and display duration in Settings
- 📦 **Zero dependencies** — pure Swift, single `build.sh`

## Install

### From GitHub Releases (pre-built DMG)

1. Download the latest `.dmg` from [Releases](../../releases)
2. Open the DMG and drag **Appreciate** to **Applications**
3. Before opening, **remove the quarantine flag** (required for unsigned apps):
   ```bash
   xattr -cr /Applications/Appreciate.app
   ```
4. Open **Appreciate** — the ✨ sparkle icon appears in your menubar

### Build from source

```bash
git clone https://github.com/srid/Appreciate.git
cd Appreciate
./build.sh
# App launches automatically after build
```

Requires Xcode Command Line Tools (`xcode-select --install`).

## Usage

| Menu Item | Action |
|---|---|
| **✨ Show Now** | Trigger a reminder immediately |
| **Enabled** | Toggle auto-reminders on/off |
| **Settings…** | Customize text, intervals, duration |
| **Quit** | Exit the app |

## Releasing


1. Go to [Actions → Release](../../actions/workflows/release.yml)
2. Click **Run workflow**
3. Enter a version tag (e.g. `v1.0.0`)
4. The workflow builds a DMG and creates a GitHub Release with auto-generated release notes from commits

## License

MIT
