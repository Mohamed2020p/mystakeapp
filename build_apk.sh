#!/usr/bin/env bash
# Local build helper (needs JDK 17 + Android SDK + Gradle 8.7).
# On Colab, use colab_build.ipynb / COLAB.md instead.
set -e
cd "$(dirname "$0")"

: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK path first. Example: export ANDROID_HOME=\$HOME/Android/Sdk}"

gradle assembleDebug --no-daemon
echo ""
echo "Done! APK is at: app/build/outputs/apk/debug/app-debug.apk"
