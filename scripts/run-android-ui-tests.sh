#!/bin/sh
set -eu

android_api_level=""
attempt=1
while [ "$attempt" -le 24 ]; do
    android_api_level="$(adb shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r' || true)"
    case "$android_api_level" in
        ''|*[!0-9]*)
            echo "Waiting for Android shell ($attempt/24)"
            sleep 5
            ;;
        *)
            echo "Android shell ready; API $android_api_level"
            break
            ;;
    esac
    attempt=$((attempt + 1))
done

case "$android_api_level" in
    ''|*[!0-9]*)
        adb devices -l
        exit 1
        ;;
esac

adb shell settings put secure show_ime_with_hard_keyboard 1
adb logcat -c
set +e
./gradlew --no-daemon :app:connectedDebugAndroidTest
android_test_status=$?
adb logcat -d -b crash > app/build/android-crash.log
adb logcat -d -s AndroidRuntime Room SQLiteLog ChatFlow > app/build/android-runtime.log
for android_screenshot in welcome-screen.png chat-screen.png failure-screen.png; do
    adb pull "/sdcard/Download/$android_screenshot" "app/build/$android_screenshot" || true
done
cat app/build/android-crash.log
exit "$android_test_status"
