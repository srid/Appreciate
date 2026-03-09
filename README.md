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
3. Since the app is **unsigned**, macOS will block it on first launch:
   - Open **System Settings → Privacy & Security**
   - Scroll down to the security section — you'll see *"Appreciate" was blocked*
   - Click **Open Anyway**, then confirm
4. The ✨ sparkle icon appears in your menubar — you're good to go

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

Releases are automated via [release-please](https://github.com/googleapis/release-please). Just push commits to `main` using [Conventional Commits](https://www.conventionalcommits.org/) format:

```
feat: add custom font picker
fix: overlay not appearing on secondary display
```

release-please will automatically open a Release PR. When you merge it, a GitHub Release is created and the DMG is built and attached.


## License

MIT
