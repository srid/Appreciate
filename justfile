# List available targets
default:
    @just --list

# Build and deploy Android app to connected phone (resets app data for fresh packs)
deploy-android:
    nix develop -c sh -c '\
      cd android && ./gradlew assembleDebug && \
      cd .. && \
      adb shell pm clear ca.srid.appreciate || true && \
      adb install -r android/app/build/outputs/apk/debug/app-debug.apk && \
      echo "✅ Deployed to phone (app data reset)"'

# Build Android APK without deploying
build-android:
    nix develop -c sh -c 'cd android && ./gradlew assembleDebug'

# Build macOS app
build-macos:
    cd macos && ./build.sh

# Show a reminder immediately on the connected phone
show-android:
    nix develop -c adb shell am startservice -a ca.srid.appreciate.ACTION_SHOW_NOW ca.srid.appreciate/.OverlayService

# Build Windows app
build-windows:
    cd windows && dotnet build

# Build Linux AppImage
build-linux:
    nix build .#linux-appimage

# List recent GitHub releases
releases:
    gh release list --limit 5

# Create a new GitHub release (triggers CI build for all platforms)
release version:
    gh workflow run release.yml -f version={{version}}
    @echo "🚀 Release {{version}} triggered. Watch: gh run list --workflow=release.yml"
