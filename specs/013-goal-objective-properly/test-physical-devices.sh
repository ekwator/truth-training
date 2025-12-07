#!/bin/bash
# Physical Device Testing Helper Script
# Feature: 013-goal-objective-properly
# Tasks: T025, T027, T028

set -e

echo "=== Physical Device Testing Script ==="
echo "Feature: 013-goal-objective-properly"
echo ""

# Check if we're in the right directory
if [ ! -d "truth-android-client" ]; then
    echo "ERROR: truth-android-client directory not found"
    echo "Please run this script from the repository root"
    exit 1
fi

# Check devices
echo "Checking connected devices..."
DEVICES=$(adb devices 2>/dev/null | grep -v "List" | grep "device$" | awk '{print $1}' || echo "")
DEVICE_COUNT=$(echo "$DEVICES" | grep -c . || echo "0")

if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "WARNING: No devices found. Please connect at least 1 Android device with USB debugging enabled."
    echo ""
    echo "To enable USB debugging:"
    echo "1. Settings → About Phone → Tap 'Build Number' 7 times"
    echo "2. Settings → Developer Options → Enable 'USB Debugging'"
    echo ""
    read -p "Press Enter to continue anyway (for build only) or Ctrl+C to exit..."
    DEVICE_COUNT=0
elif [ "$DEVICE_COUNT" -lt 2 ]; then
    echo "WARNING: Only $DEVICE_COUNT device(s) found. SC-001 requires minimum 2 devices."
    echo "Found devices:"
    echo "$DEVICES"
    echo ""
    read -p "Press Enter to continue with available device(s) or Ctrl+C to exit..."
else
    echo "Found $DEVICE_COUNT device(s):"
    echo "$DEVICES"
    echo ""
fi

# Build APK
echo "Building debug APK..."
cd truth-android-client
./gradlew assembleDebug
cd ..

APK_PATH="truth-android-client/app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK_PATH" ]; then
    echo "ERROR: APK not found at $APK_PATH"
    echo "Build may have failed. Check the output above."
    exit 1
fi

APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo "✅ APK built successfully: $APK_PATH ($APK_SIZE)"
echo ""

# Install on devices if available
if [ "$DEVICE_COUNT" -gt 0 ]; then
    echo "Installing APK on devices..."
    for device in $DEVICES; do
        echo "Installing on $device..."
        if adb -s "$device" install -r "$APK_PATH" 2>/dev/null; then
            echo "✅ Installed on $device"
        else
            echo "⚠️  Installation failed on $device (may need to uninstall existing app first)"
            echo "   Try: adb -s $device uninstall com.truth.training.client"
        fi
    done
    echo ""
fi

# Display next steps
echo "=== Next Steps ==="
echo ""
echo "1. Launch the app on each device from the launcher"
echo "2. Follow the test procedures in:"
echo "   specs/013-goal-objective-properly/PHYSICAL_DEVICE_TESTING.md"
echo ""
echo "Test Tasks:"
echo "  - T025: Launch and DashboardScreen display verification"
echo "  - T027: Performance validation (launch time < 2 seconds)"
echo "  - T028: Stability validation (30+ seconds visible)"
echo ""
echo "Quick Test Commands:"
echo "  # Launch app on first device"
echo "  adb shell am start -n com.truth.training.client/.MainActivity"
echo ""
echo "  # Measure launch time"
echo "  adb shell am start -W -n com.truth.training.client/.MainActivity"
echo ""
echo "  # View logs"
echo "  adb logcat | grep -i 'MainActivity\|DashboardScreen\|ViewModelFactory'"
echo ""
echo "For detailed instructions, see:"
echo "  specs/013-goal-objective-properly/PHYSICAL_DEVICE_TESTING.md"
echo ""

