# Build and deploy Android app to connected phone (resets app data for fresh packs)
deploy:
    nix develop -c sh -c 'cd android && ./gradlew assembleDebug'
    adb shell pm clear ca.srid.appreciate || true
    adb install -r android/app/build/outputs/apk/debug/app-debug.apk
    @echo "✅ Deployed to phone (app data reset)"

# Build Android APK without deploying
build-android:
    nix develop -c sh -c 'cd android && ./gradlew assembleDebug'

# Build macOS app
build-macos:
    cd macos && ./build.sh

# Show a reminder immediately on the connected phone
show:
    adb shell am startservice -a ca.srid.appreciate.ACTION_SHOW_NOW ca.srid.appreciate/.OverlayService

# Build Windows app
build-windows:
    cd windows && dotnet build
