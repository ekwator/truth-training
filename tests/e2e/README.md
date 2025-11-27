# Cross-Device E2E Tests

**Task**: T062 - Add cross-device E2E tests (Linux Desktop ↔ Android device, CLI ↔ Android ↔ Desktop)

## Overview

These tests verify that node discovery and synchronization work correctly across different platforms:
- **Linux Desktop ↔ Android device**: LAN discovery and sync
- **CLI ↔ Android ↔ Desktop**: Database compatibility and sync

## Prerequisites

### Required Setup

1. **Android Device/Emulator**:
   ```bash
   # Check if device is connected
   adb devices
   
   # If using emulator, start it first
   emulator -avd <avd_name>
   ```

2. **Desktop Server**:
   ```bash
   # Start server for sync tests
   cargo run --bin truth_core_server -- --port 8080
   ```

3. **Network Configuration**:
   - All devices must be on the same network
   - Firewall must allow UDP multicast (239.255.0.1:52525)
   - HTTP ports (8080, 8081) must be accessible

### Android App Setup

1. **Install APK**:
   ```bash
   cd truth-android-client
   ./gradlew installDebug
   ```

2. **Grant Permissions** (if needed):
   ```bash
   adb shell pm grant com.truth.training.client android.permission.INTERNET
   adb shell pm grant com.truth.training.client android.permission.ACCESS_WIFI_STATE
   ```

## Running Tests

### All E2E Tests

```bash
# Run all cross-device E2E tests
cargo test --test test_cross_device_e2e

# Run with output
cargo test --test test_cross_device_e2e -- --nocapture
```

### Specific Tests

```bash
# Test CLI reading Android database
cargo test --test test_cross_device_e2e test_cli_reads_android_database -- --ignored

# Test CLI-Desktop sync
cargo test --test test_cross_device_e2e test_cli_desktop_sync

# Test schema compatibility
cargo test --test test_cross_device_e2e test_cross_platform_schema_compatibility
```

### Manual E2E Scenarios

Some tests require manual execution due to device dependencies:

#### Scenario 1: Desktop ↔ Android LAN Discovery

1. **Start Desktop Server**:
   ```bash
   cargo run --bin truth_core_server -- --port 8080 --bind 0.0.0.0
   ```

2. **Open Android App**:
   - Navigate to "Nodes" screen
   - Ensure app is on same Wi-Fi network

3. **Verify Discovery**:
   - Desktop should broadcast UDP multicast announcements
   - Android should receive and display desktop server in node list
   - Check Android logs: `adb logcat | grep -i discovery`

#### Scenario 2: CLI ↔ Android Database Exchange

1. **Create Database on Android**:
   - Open Android app
   - Add a test node via UI
   - Note the database path: `/data/data/com.truth.training.client/databases/truth_database`

2. **Pull Database to CLI**:
   ```bash
   adb pull /data/data/com.truth.training.client/databases/truth_database test_android.db
   ```

3. **Verify with CLI**:
   ```bash
   cargo run --bin truthctl -- --db test_android.db nodes list
   ```

4. **Add Node via CLI**:
   ```bash
   cargo run --bin truthctl -- --db test_android.db nodes add \
     --address "http://192.168.1.50:8080/api/v1" \
     --type WIFI \
     --ttl 300
   ```

5. **Push Back to Android**:
   ```bash
   adb push test_android.db /data/data/com.truth.training.client/databases/truth_database
   ```

6. **Verify in Android App**:
   - Restart app
   - Check that CLI-added node appears in node list

#### Scenario 3: CLI ↔ Desktop Sync

1. **Start Desktop Server**:
   ```bash
   cargo run --bin truth_core_server -- --db desktop.db --port 8080
   ```

2. **Add Nodes via CLI**:
   ```bash
   cargo run --bin truthctl -- --db cli.db nodes add \
     --address "http://192.168.1.100:8080/api/v1" \
     --type LAN \
     --ttl 120
   ```

3. **Sync CLI to Desktop**:
   ```bash
   cargo run --bin truthctl -- --db cli.db nodes sync \
     --server "http://localhost:8080/api/v1"
   ```

4. **Verify on Desktop**:
   ```bash
   curl http://localhost:8080/api/v1/nodes | jq '.'
   ```

## Test Structure

### Automated Tests (CI-Compatible)

- `test_cross_platform_schema_compatibility`: Verifies schema matches across platforms
- `test_cross_platform_merge_priority`: Verifies merge rules are consistent
- `test_cli_desktop_sync`: Tests CLI ↔ Desktop sync (requires server)

### Manual Tests (Require Devices)

- `test_cli_reads_android_database`: Requires Android device with database
- `test_android_reads_cli_database`: Requires Android device (may need root)
- `test_desktop_android_lan_discovery`: Requires both Desktop server and Android device
- `test_cli_android_sync_via_server`: Requires Android device and server

## Troubleshooting

### Android Device Not Detected

```bash
# Check ADB connection
adb devices

# Restart ADB server
adb kill-server
adb start-server

# Check USB debugging is enabled on device
```

### Database Access Issues

```bash
# Android database requires root for push/pull
adb root
adb remount

# Or use run-as for non-root access
adb shell run-as com.truth.training.client
```

### Network Issues

```bash
# Check if devices are on same network
# Desktop: ip addr show
# Android: adb shell ip addr show wlan0

# Check UDP multicast
# Desktop: tcpdump -i any -n udp port 52525
# Android: adb logcat | grep -i multicast
```

### Server Not Running

```bash
# Check if port is in use
lsof -i :8080

# Start server with verbose logging
RUST_LOG=debug cargo run --bin truth_core_server -- --port 8080
```

## CI Integration

For CI environments without physical devices, tests are marked with `#[ignore]`:

```bash
# Run only non-ignored tests (CI-friendly)
cargo test --test test_cross_device_e2e

# Run all tests including ignored ones (requires devices)
cargo test --test test_cross_device_e2e -- --ignored
```

## Expected Results

All tests should verify:
- ✅ Schema compatibility across platforms
- ✅ Database read/write compatibility
- ✅ Sync operations work correctly
- ✅ Merge priority rules are consistent
- ✅ LAN discovery works (when devices available)

## Next Steps

1. **T062 Completion**: Mark as complete after manual verification
2. **CI Enhancement**: Add Android emulator to CI pipeline
3. **Documentation**: Update with actual test results from physical devices

_Version: v1.0.0_
