<!-- Archived from [docs/cross_device_e2e_tests.md](docs/cross_device_e2e_tests.md) -->

# Cross-Device E2E Tests Documentation

**Task**: T062 - Add cross-device E2E tests (Linux Desktop ↔ Android device, CLI ↔ Android ↔ Desktop)

## Overview

Cross-device E2E tests verify that node discovery and synchronization work correctly across different platforms. These tests ensure that:

- **Linux Desktop ↔ Android device**: LAN discovery and database compatibility
- **CLI ↔ Android ↔ Desktop**: Sync operations and database exchange

## Test Structure

### Location

- **Test File**: `tests/test_cross_device_e2e.rs`
- **Documentation**: [tests/e2e/README.md](tests/e2e/README.md)
- **Test Runner Script**: `scripts/run_cross_device_tests.sh`

### Test Categories

#### 1. Automated Tests (CI-Compatible)

These tests can run in CI without physical devices:

- **`test_cross_platform_schema_compatibility`**: Verifies database schema matches across all platforms
- **`test_cross_platform_merge_priority`**: Verifies merge priority rules (Local > Global) are consistent

#### 2. Device-Dependent Tests (Require Physical Devices)

These tests require Android devices/emulators and are marked with `#[ignore]`:

- **`test_cli_reads_android_database`**: CLI reads database created by Android
- **`test_android_reads_cli_database`**: Android reads database modified by CLI
- **`test_desktop_android_lan_discovery`**: Desktop and Android discover each other via LAN
- **`test_cli_android_sync_via_server`**: CLI and Android sync via server

#### 3. Server-Dependent Tests

These tests require a running server:

- **`test_cli_desktop_sync`**: CLI syncs with Desktop server

## Running Tests

### Quick Start

```bash
# Run all automated tests (CI-compatible)
cargo test --test test_cross_device_e2e

# Run all tests including device-dependent ones
cargo test --test test_cross_device_e2e -- --ignored

# Run specific test
cargo test --test test_cross_device_e2e test_cross_platform_schema_compatibility
```

### Using Test Runner Script

```bash
# Run automated test suite
./scripts/run_cross_device_tests.sh
```

The script will:
1. Check prerequisites (ADB, Android device, server)
2. Run automated tests
3. Run device-dependent tests if devices are available
4. Provide summary of results

## Prerequisites

### For Automated Tests

- Rust toolchain (1.75+)
- SQLite 3.x
- Cargo workspace

### For Device-Dependent Tests

- **Android Device/Emulator**:
  ```bash
  # Check connection
  adb devices
  
  # Start emulator (if using)
  emulator -avd <avd_name>
  ```

- **Desktop Server** (for sync tests):
  ```bash
  cargo run --bin truth_core_server -- --port 8080
  ```

- **Network Configuration**:
  - All devices on same network
  - UDP multicast enabled (239.255.0.1:52525)
  - HTTP ports accessible (8080, 8081)

### Android App Setup

1. **Install APK**:
   ```bash
   cd truth-android-client
   ./gradlew installDebug
   ```

2. **Grant Permissions**:
   ```bash
   adb shell pm grant com.truth.training.client android.permission.INTERNET
   adb shell pm grant com.truth.training.client android.permission.ACCESS_WIFI_STATE
   ```

## Test Scenarios

### Scenario 1: CLI Reads Android Database

**Goal**: Verify CLI can read database created by Android app.

**Steps**:
1. Create database on Android (via app UI)
2. Pull database: `adb pull /data/data/com.truth.training.client/databases/truth_database test.db`
3. Read with CLI: `cargo run --bin truthctl -- --db test.db nodes list`

**Expected**: CLI successfully reads all nodes from Android database.

### Scenario 2: Android Reads CLI Database

**Goal**: Verify Android can read database modified by CLI.

**Steps**:
1. Create database with CLI and add nodes
2. Push to Android: `adb push test.db /data/data/com.truth.training.client/databases/truth_database`
3. Verify in Android app (restart app, check Nodes screen)

**Expected**: Android app displays nodes added by CLI.

### Scenario 3: Desktop ↔ Android LAN Discovery

**Goal**: Verify nodes discover each other on local network.

**Steps**:
1. Start Desktop server: `cargo run --bin truth_core_server -- --port 8080`
2. Open Android app on same network
3. Navigate to Nodes screen
4. Verify Desktop server appears in Android node list

**Expected**: Both platforms discover each other via UDP multicast.

### Scenario 4: CLI ↔ Desktop Sync

**Goal**: Verify CLI can sync with Desktop server.

**Steps**:
1. Start server: `cargo run --bin truth_core_server -- --port 8080`
2. Add nodes via CLI
3. Sync: `cargo run --bin truthctl -- nodes sync --server http://localhost:8080/api/v1`
4. Verify merged nodes on server

**Expected**: CLI and server have consistent node lists after sync.

### Scenario 5: CLI ↔ Android Sync via Server

**Goal**: Verify CLI and Android sync via server and get consistent results.

**Steps**:
1. Start server
2. Android app syncs (automatic or manual)
3. CLI syncs with server
4. Verify both have same merged node list

**Expected**: Both platforms converge to same node list via server sync.

## Test Results

### Automated Tests Status

- ✅ **Schema Compatibility**: PASS - All platforms use identical schema
- ✅ **Merge Priority**: PASS - Local > Global rules consistent

### Device-Dependent Tests Status

- ⚠️ **Requires Manual Execution**: Full E2E tests require physical devices
- ✅ **Components Verified**: All discovery and sync components implemented
- ✅ **Format Compatibility**: UDP multicast packet format verified

## CI Integration

For CI environments, tests are structured to:

1. **Run Automated Tests**: Always run schema and merge priority tests
2. **Skip Device Tests**: Device-dependent tests are marked `#[ignore]`
3. **Provide Instructions**: Test output includes setup instructions

### CI Configuration

```yaml
# Example CI step
- name: Run cross-device E2E tests
  run: |
    cargo test --test test_cross_device_e2e
    # Device-dependent tests require manual execution
```

## Troubleshooting

### Common Issues

1. **ADB Device Not Found**:
   ```bash
   adb kill-server
   adb start-server
   adb devices
   ```

2. **Database Access Denied**:
   ```bash
   # May require root access
   adb root
   adb remount
   ```

3. **Server Not Running**:
   ```bash
   # Check if port is in use
   lsof -i :8080
   
   # Start server
   cargo run --bin truth_core_server -- --port 8080
   ```

4. **Network Issues**:
   - Verify all devices on same network
   - Check firewall allows UDP multicast
   - Verify HTTP ports accessible

## Next Steps

1. **Manual Verification**: Execute full E2E scenarios with physical devices
2. **CI Enhancement**: Add Android emulator to CI pipeline for automated device tests
3. **Documentation**: Update with actual test results from device testing

## Related Documentation

- [tests/e2e/README.md](tests/e2e/README.md) - Detailed test execution guide
- [specs/008-specify-md/quickstart.md](specs/008-specify-md/quickstart.md) - Quickstart scenarios
- [docs/node_discovery_test_results.md](docs/node_discovery_test_results.md) - Quickstart test results
- [docs/final_validation.md](docs/final_validation.md) - CLI validation results

_Version: v1.0.0_

