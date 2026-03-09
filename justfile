# Build and deploy Android app to connected phone
deploy:
    cd android && ./gradlew assembleDebug
    adb install -r android/app/build/outputs/apk/debug/app-debug.apk
    @echo "✅ Deployed to phone"

# Build Android APK without deploying
build-android:
    cd android && ./gradlew assembleDebug

# Build macOS app
build-macos:
    cd macos && ./build.sh

# Show a reminder immediately on the connected phone
show:
    adb shell am startservice -a ca.srid.appreciate.ACTION_SHOW_NOW ca.srid.appreciate/.OverlayService
