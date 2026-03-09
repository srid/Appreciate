#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/build"
APP_DIR="$BUILD_DIR/Appreciate.app"
CONTENTS_DIR="$APP_DIR/Contents"
MACOS_DIR="$CONTENTS_DIR/MacOS"
RESOURCES_DIR="$CONTENTS_DIR/Resources"
SOURCES_DIR="$SCRIPT_DIR/Sources"

echo "🔨 Building Appreciate..."

# Kill existing instance (skip in CI)
if [ -z "${CI:-}" ]; then
    pkill -x Appreciate 2>/dev/null && echo "   Killed existing instance" && sleep 0.5 || true
fi

# Clean
rm -rf "$APP_DIR"

# Create bundle structure
mkdir -p "$MACOS_DIR" "$RESOURCES_DIR"

# Copy Info.plist and icon
cp "$SCRIPT_DIR/Info.plist" "$CONTENTS_DIR/Info.plist"
cp "$SCRIPT_DIR/AppIcon.icns" "$RESOURCES_DIR/AppIcon.icns"

# Compile
swiftc \
    -o "$MACOS_DIR/Appreciate" \
    -target arm64-apple-macosx14.0 \
    -sdk "$(xcrun --show-sdk-path)" \
    -framework Cocoa \
    -framework SwiftUI \
    "$SOURCES_DIR/main.swift" \
    "$SOURCES_DIR/SettingsStore.swift" \
    "$SOURCES_DIR/OverlayContentView.swift" \
    "$SOURCES_DIR/OverlayManager.swift" \
    "$SOURCES_DIR/TimerManager.swift" \
    "$SOURCES_DIR/SettingsView.swift" \
    "$SOURCES_DIR/AppDelegate.swift"

echo "✅ Built: $APP_DIR"

# Auto-launch (skip in CI)
if [ -z "${CI:-}" ]; then
    open "$APP_DIR"
    echo "🚀 Launched Appreciate"
fi
