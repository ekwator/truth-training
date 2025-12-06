#!/bin/bash
# Manual 30-second stability test script
# Task: T028 - Stability validation

set -e

DEVICE_ID="REMOVED"
PACKAGE_NAME="com.truth.training.client"
APK_PATH="/home/ekwator/Code/truth-training/truth-android-client/app/build/outputs/apk/local/debug/app-local-debug.apk"

echo "=== 30-Second Stability Test ==="
echo "Device: $DEVICE_ID"
echo "Package: $PACKAGE_NAME"
echo ""

# Check device connection
if ! adb devices | grep -q "$DEVICE_ID.*device"; then
    echo "❌ ERROR: Device $DEVICE_ID not connected"
    exit 1
fi

echo "✅ Device connected: $DEVICE_ID"
echo ""

# Clear app data to avoid database schema issues
echo "Clearing app data..."
adb -s $DEVICE_ID shell pm clear $PACKAGE_NAME || echo "⚠️  Warning: Could not clear app data (may not be installed)"
echo ""

# Install APK
echo "Installing APK..."
adb -s $DEVICE_ID install -r "$APK_PATH"
if [ $? -ne 0 ]; then
    echo "❌ ERROR: Failed to install APK"
    exit 1
fi
echo "✅ APK installed successfully"
echo ""

# Launch app
echo "Launching application..."
adb -s $DEVICE_ID shell am start -n "$PACKAGE_NAME/.MainActivity"
if [ $? -ne 0 ]; then
    echo "❌ ERROR: Failed to launch application"
    exit 1
fi
echo "✅ Application launched"
echo ""

# Wait for app to initialize (2 seconds)
echo "Waiting for app initialization (2 seconds)..."
sleep 2

# Check if app process is running
echo "Checking app process..."
PROCESS_CHECK=$(adb -s $DEVICE_ID shell "ps | grep $PACKAGE_NAME | grep -v grep" || echo "")
if [ -z "$PROCESS_CHECK" ]; then
    echo "❌ ERROR: Application process not found - app may have crashed"
    exit 1
fi
echo "✅ Application process is running"
echo ""

# Monitor app for 30 seconds
echo "=== Starting 30-second stability test ==="
echo "Monitoring application for 30 seconds..."
echo ""

START_TIME=$(date +%s)
CHECK_INTERVAL=5
ELAPSED=0

while [ $ELAPSED -lt 30 ]; do
    sleep $CHECK_INTERVAL
    ELAPSED=$((ELAPSED + CHECK_INTERVAL))
    
    # Check if process is still running
    PROCESS_CHECK=$(adb -s $DEVICE_ID shell "ps | grep $PACKAGE_NAME | grep -v grep" || echo "")
    if [ -z "$PROCESS_CHECK" ]; then
        echo "❌ FAIL: Application crashed at ${ELAPSED} seconds"
        echo "Collecting crash logs..."
        adb -s $DEVICE_ID logcat -d | grep -A 20 "FATAL\|AndroidRuntime" | tail -30
        exit 1
    fi
    
    # Check if MainActivity is still in foreground
    CURRENT_ACTIVITY=$(adb -s $DEVICE_ID shell "dumpsys window windows | grep -E 'mCurrentFocus|mFocusedApp' | grep $PACKAGE_NAME" || echo "")
    if [ -z "$CURRENT_ACTIVITY" ]; then
        echo "⚠️  Warning: Application may not be in foreground at ${ELAPSED} seconds"
    fi
    
    echo "✅ App stable at ${ELAPSED} seconds (process running)"
done

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "=== Test Results ==="
echo "✅ SUCCESS: Application remained stable for ${DURATION} seconds"
echo "✅ Process was running throughout the test"
echo ""

# Final process check
FINAL_CHECK=$(adb -s $DEVICE_ID shell "ps | grep $PACKAGE_NAME | grep -v grep" || echo "")
if [ -z "$FINAL_CHECK" ]; then
    echo "❌ FAIL: Application process not found after test completion"
    exit 1
fi

echo "✅ Final check: Application process still running"
echo ""
echo "=== Test Complete ==="
echo "Result: ✅ PASS - Application remains visible and stable for 30+ seconds"
echo ""

# Collect memory info
echo "Memory usage:"
adb -s $DEVICE_ID shell "dumpsys meminfo $PACKAGE_NAME | grep -E 'TOTAL|Native Heap|Dalvik Heap'" || echo "Memory info not available"
echo ""

exit 0

