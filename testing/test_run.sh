#!/bin/bash

# Configuration
export PATH="$PATH:/Users/nicolasraoul/Library/Android/sdk/platform-tools"
PACKAGE_NAME="com.example.jxlcamera"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
TEST_DIR="testing"

# Ensure output directory exists
mkdir -p "$TEST_DIR"

echo "Checking for connected devices..."
adb devices

# Install APK
if [ -f "$APK_PATH" ]; then
    echo "Installing APK..."
    adb install "$APK_PATH"
else
    echo "APK not found at $APK_PATH. Please build first."
    exit 1
fi

# Grant permissions
echo "Granting Camera permission..."
adb shell pm grant "$PACKAGE_NAME" android.permission.CAMERA

# Run app
echo "Launching app..."
adb shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1

# Clear logs
adb logcat -c

# Wait for launch and initialization
sleep 30

# Capture screenshot on launch
echo "Capturing launch screenshot..."
adb shell screencap -p /sdcard/screenshot_launch.png
adb pull /sdcard/screenshot_launch.png "$TEST_DIR/"

# Record screencast (run in background for a bit)
echo "Starting screen recording..."
adb shell screenrecord /sdcard/video_run.mp4 &
RECORD_PID=$!

# Simulate tap on capture button
echo "Simulating image capture tap..."
adb shell input tap 540 2094

sleep 5

# Stop recording
echo "Stopping screen recording..."
kill "$RECORD_PID"
adb pull /sdcard/video_run.mp4 "$TEST_DIR/"

# Pull JXL files from app specific directory
echo "Pulling JXL files..."
APP_DIR="/sdcard/Download/JxlCamera"
adb pull "$APP_DIR" "$TEST_DIR/"

# Dump logs
echo "Dumping logs..."
adb logcat -d > "$TEST_DIR/logs.txt"

echo "Testing completed. Results in $TEST_DIR/"
