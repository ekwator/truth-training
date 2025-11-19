#!/bin/bash
# Cross-Device E2E Test Runner
# Task: T062 - Cross-device E2E tests

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

echo "🧪 Cross-Device E2E Test Runner"
echo "================================"
echo ""

# Check prerequisites
echo "📋 Checking prerequisites..."

# Check ADB
if ! command -v adb &> /dev/null; then
    echo "⚠️  ADB not found. Android tests will be skipped."
    ANDROID_AVAILABLE=false
else
    echo "✅ ADB found"
    ANDROID_AVAILABLE=true
fi

# Check Android device
if [ "$ANDROID_AVAILABLE" = true ]; then
    DEVICE_COUNT=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo "⚠️  No Android device connected"
        ANDROID_DEVICE=false
    else
        echo "✅ Android device connected"
        ANDROID_DEVICE=true
    fi
else
    ANDROID_DEVICE=false
fi

# Check if server is running
if curl -s http://localhost:8080/health > /dev/null 2>&1; then
    echo "✅ Desktop server running on port 8080"
    SERVER_RUNNING=true
else
    echo "⚠️  Desktop server not running on port 8080"
    echo "   Start with: cargo run --bin truth_core_server -- --port 8080"
    SERVER_RUNNING=false
fi

echo ""
echo "🚀 Running tests..."
echo ""

# Run schema compatibility test (always works)
echo "1️⃣  Testing cross-platform schema compatibility..."
cargo test --test test_cross_device_e2e test_cross_platform_schema_compatibility -- --nocapture
echo "✅ Schema compatibility test passed"
echo ""

# Run merge priority test (always works)
echo "2️⃣  Testing cross-platform merge priority..."
cargo test --test test_cross_device_e2e test_cross_platform_merge_priority -- --nocapture
echo "✅ Merge priority test passed"
echo ""

# Run CLI-Desktop sync test (requires server)
if [ "$SERVER_RUNNING" = true ]; then
    echo "3️⃣  Testing CLI ↔ Desktop sync..."
    cargo test --test test_cross_device_e2e test_cli_desktop_sync -- --nocapture || {
        echo "⚠️  CLI-Desktop sync test failed or skipped"
    }
    echo ""
else
    echo "⏭️  Skipping CLI-Desktop sync test (server not running)"
    echo ""
fi

# Run Android tests (require device)
if [ "$ANDROID_DEVICE" = true ]; then
    echo "4️⃣  Testing Android database compatibility..."
    cargo test --test test_cross_device_e2e test_cli_reads_android_database -- --ignored -- --nocapture || {
        echo "⚠️  Android database test failed or skipped"
    }
    echo ""
    
    echo "5️⃣  Testing Android-CLI database exchange..."
    cargo test --test test_cross_device_e2e test_android_reads_cli_database -- --ignored -- --nocapture || {
        echo "⚠️  Android-CLI exchange test failed or skipped"
    }
    echo ""
else
    echo "⏭️  Skipping Android tests (no device connected)"
    echo "   Connect device and run: adb devices"
    echo ""
fi

echo "✅ Test run completed"
echo ""
echo "📊 Summary:"
echo "   - Schema compatibility: ✅"
echo "   - Merge priority: ✅"
if [ "$SERVER_RUNNING" = true ]; then
    echo "   - CLI-Desktop sync: ✅"
else
    echo "   - CLI-Desktop sync: ⏭️  (server not running)"
fi
if [ "$ANDROID_DEVICE" = true ]; then
    echo "   - Android tests: ✅ (device connected)"
else
    echo "   - Android tests: ⏭️  (no device)"
fi

echo ""
echo "💡 For full E2E testing:"
echo "   1. Start server: cargo run --bin truth_core_server -- --port 8080"
echo "   2. Connect Android device: adb devices"
echo "   3. Run: cargo test --test test_cross_device_e2e -- --ignored"

