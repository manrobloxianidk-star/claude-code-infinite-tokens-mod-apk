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

exec ./gradlew --no-daemon :app:connectedDebugAndroidTest
