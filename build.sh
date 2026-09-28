#!/bin/bash

# Build script for UPI Pay Shortcut POC
# Usage: ./build.sh [build|install|release|clean]

set -e

# Set JAVA_HOME if not already set
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/opt/jbr" ]; then
        export JAVA_HOME="/opt/jbr"
    elif [ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
    fi
fi

# Set ANDROID_HOME if not already set
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "/opt/android-sdk" ]; then
        export ANDROID_HOME="/opt/android-sdk"
    elif [ -d "$HOME/AndroidStudioFiles" ]; then
        export ANDROID_HOME="$HOME/AndroidStudioFiles"
    fi
fi

export ANDROID_SDK_ROOT="$ANDROID_HOME"

echo "Using JAVA_HOME: $JAVA_HOME"
echo "Using ANDROID_HOME: $ANDROID_HOME"

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo "Error: gradlew not found. Run this script from the project root."
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

ACTION=${1:-build}

case $ACTION in
    build)
        echo "Building debug APK..."
        ./gradlew assembleDebug
        echo ""
        echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
        ;;
    release)
        echo "Building release APK..."
        ./gradlew assembleRelease
        echo ""
        echo "APK location: app/build/outputs/apk/release/app-release-unsigned.apk"
        ;;
    install)
        echo "Building and installing debug APK..."
        ./gradlew installDebug
        echo ""
        echo "Launching app..."
        adb shell am start -n com.linetra.upishortcut/.MainActivity
        ;;
    test)
        echo "Running JVM unit tests..."
        ./gradlew testDebugUnitTest
        ;;
    androidTest)
        echo "Running instrumented tests..."
        ./gradlew connectedDebugAndroidTest
        ;;
    clean)
        echo "Cleaning build artifacts..."
        ./gradlew clean
        ;;
    bundle)
        echo "Building release bundle (AAB)..."
        ./gradlew bundleRelease
        echo ""
        echo "Bundle location: app/build/outputs/bundle/release/app-release.aab"
        ;;
    *)
        echo "Usage: $0 [build|install|release|clean|bundle|test|androidTest]"
        echo ""
        echo "Commands:"
        echo "  build        - Build debug APK (default)"
        echo "  install      - Build, install to device, and launch"
        echo "  release      - Build release APK"
        echo "  bundle       - Build release AAB bundle"
        echo "  test         - Run JVM unit tests"
        echo "  androidTest  - Run instrumented tests in their own package"
        echo "  clean        - Clean build artifacts"
        exit 1
        ;;
esac

echo ""
echo "Done!"
