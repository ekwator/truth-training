# Android Discovery Architecture

**Date**: 2025-01-XX  
**Feature**: Unified Node Discovery & Address Exchange - Android Implementation  
**Status**: ✅ **COMPLETE**

## Overview

Android implementation of the cross-platform node discovery system, fully compatible with Desktop (Tauri) and CLI (truthctl) implementations.

## Architecture Components

### 1. Database Layer (Room)

**Location**: `truth-android-client/app/src/main/java/com/truth/training/client/data/database/`

#### NodeEntity
- **File**: `entities/NodeEntity.kt`
- **Purpose**: Room entity matching canonical SQLite schema
- **Schema**: Identical to Desktop/CLI (`core/src/storage.rs`)
- **Indices**: `address`, `last_seen`, `type`, `reachable`

#### NodeDao
- **File**: `daos/NodeDao.kt`
- **Purpose**: Data Access Object for node operations
- **Methods**:
  - `upsertNode()` - Insert or replace by address
  - `getNode()`, `getNodeByAddress()` - Query by ID/address
  - `listNodes()`, `listNodesSync()` - List with filters (Flow and sync)
  - `updateReachability()`, `updateTtl()`, `updateLastSeen()` - Updates
  - `deleteNode()` - Delete by ID
  - `pruneStaleNodes()` - TTL-based cleanup
  - `countNodes()` - Count with filters

#### Database Migration
- **File**: `TruthDatabaseMigrations.kt`
- **Migration**: `MIGRATION_2_3` creates `nodes` table
- **Version**: Database version 3

### 2. Repository Layer

**Location**: `truth-android-client/app/src/main/java/com/truth/training/client/data/repository/`

#### DiscoveryRepository
- **File**: `DiscoveryRepository.kt`
- **Purpose**: High-level discovery operations
- **Features**:
  - Node upsert with deduplication (by address or node_id)
  - Global registry polling (HTTP GET)
  - HTTP reachability checks
  - TTL-based cleanup
  - LAN announcement processing

**Key Methods**:
- `upsertNode()` - Canonical deduplication logic
- `pollGlobalRegistries()` - Fetch from HTTP registries
- `runReachabilityChecks()` - Health check all nodes
- `pruneStaleNodes()` - Remove expired nodes
- `processLanAnnouncement()` - Handle UDP multicast announcements

### 3. Network Layer

**Location**: `truth-android-client/app/src/main/java/com/truth/training/client/network/`

#### LanDiscoveryClient
- **File**: `LanDiscoveryClient.kt`
- **Purpose**: UDP Multicast listener for LAN discovery
- **Protocol**: 
  - Address: `239.255.0.1:52525`
  - Format: JSON `LanAnnouncement`
  - Signature: ed25519 verification

**Features**:
- Background coroutine listener
- Signature verification using `Ed25519CryptoManager`
- Self-announcement filtering
- Automatic storage via `DiscoveryRepository`

### 4. Worker Layer

**Location**: `truth-android-client/app/src/main/java/com/truth/training/client/worker/`

#### NodeSyncWorker
- **File**: `NodeSyncWorker.kt`
- **Purpose**: WorkManager worker for periodic sync
- **Operations**:
  1. Poll global registries (if configured)
  2. Run HTTP reachability checks
  3. Prune stale nodes (TTL expired)

**Scheduling**:
- Periodic: Every 15 minutes (WorkManager minimum)
- One-time: Manual trigger via `createOneTimeWorkRequest()`
- Constraints: Requires network connection

### 5. UI Layer

**Location**: `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/nodes/`

#### NodesScreen
- **File**: `NodesScreen.kt`
- **Purpose**: Compose UI for node list
- **Features**:
  - Node list with filters (type, reachability)
  - Manual refresh/discover/cleanup/health check
  - TTL countdown display
  - Reachability status badges

#### NodesViewModel
- **File**: `NodesViewModel.kt`
- **Purpose**: ViewModel for NodesScreen
- **State Management**:
  - Node list (Flow)
  - Loading/error states
  - Filter state (type, reachability)
  - Last updated timestamp

### 6. Models

**Location**: `truth-android-client/app/src/main/java/com/truth/training/client/data/models/`

#### NodeType
- **File**: `NodeType.kt`
- **Purpose**: Enum matching Rust `NodeType`
- **Values**: `LAN`, `WIFI`, `GLOBAL`, `RELAY`, `CLIENT`
- **Parsing**: `fromString()` matches Rust `from_str()`

#### NodeSource
- **File**: `NodeSource.kt`
- **Purpose**: Enum matching Rust `NodeSource`
- **Values**: `LOCAL_BROADCAST`, `WIFI_SCAN`, `GLOBAL_REGISTRY`, `MANUAL`, `PEER_SYNC`
- **Parsing**: `fromString()` matches Rust `from_str()`

## Integration Points

### Application Startup
- **File**: `TruthTrainingApplication.kt`
- **Method**: `startNodeDiscovery()`
- **Action**: Enqueues `NodeSyncWorker.createPeriodicWorkRequest()`

### Database Initialization
- **File**: `TruthDatabase.kt`
- **Entities**: `NodeEntity` registered
- **Migrations**: `MIGRATION_2_3` included

## Cross-Platform Compatibility

### ✅ Schema Parity
- Identical SQLite DDL across Desktop/CLI/Android
- Same indices and constraints

### ✅ Format Compatibility
- JSON payloads match Rust serialization
- NodeType/NodeSource string values match
- TTL defaults match Rust constants

### ✅ Protocol Compatibility
- UDP multicast: `239.255.0.1:52525`
- JSON format: `LanAnnouncement`
- Signature verification: ed25519

### ✅ API Compatibility
- Global registry polling: HTTP GET
- Reachability checks: HTTP GET `/health`
- Error handling: Matches Rust patterns

## Testing

### Unit Tests
- **Location**: `app/src/test/java/.../repository/`
- **Coverage**: `DiscoveryRepository` methods

### Instrumentation Tests
- **Location**: `app/src/androidTest/java/.../`
- **Files**:
  - `NodeDaoTest.kt` - DAO CRUD operations
  - `NodeDiscoveryTest.kt` - End-to-end discovery
  - `NodeSyncWorkerTest.kt` - Worker lifecycle

## Configuration

### Permissions
- **File**: `AndroidManifest.xml`
- **Required**:
  - `INTERNET` - HTTP requests
  - `CHANGE_WIFI_MULTICAST_STATE` - UDP multicast

### Settings
- Registry URLs: Loaded from SharedPreferences (TODO: UI)
- Discovery interval: 15 minutes (WorkManager minimum)
- Reachability timeout: 5 seconds (default)
- Reachability retries: 2 (default)

## Navigation Integration

### MainNavigation Integration

The `NodesScreen` is integrated into the main navigation via `MainNavigation.kt`:

```kotlin
composable("nodes") {
    val context = LocalContext.current
    val viewModel: NodesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NodesViewModel(context.applicationContext as Application) as T
            }
        }
    )
    NodesScreen(viewModel = viewModel)
}
```

**Navigation**: Use `navController.navigate("nodes")` to open the nodes screen.

### Adding Navigation Entry

To add a menu entry or bottom navigation item:

```kotlin
// In your navigation menu/bar
NavigationBarItem(
    icon = { Icon(Icons.Default.DeviceHub) },
    label = { Text("Nodes") },
    selected = currentRoute == "nodes",
    onClick = { navController.navigate("nodes") }
)
```

## Running Tests on Physical Devices

### Prerequisites

1. **Android Device**:
   - Android 8.0+ (API 26+)
   - Wi-Fi enabled
   - USB debugging enabled
   - Same Wi-Fi network as test machine

2. **Development Machine**:
   - Android SDK installed
   - ADB configured
   - Device connected via USB: `adb devices`

### Running Instrumentation Tests

```bash
cd truth-android-client

# Run all instrumentation tests
./gradlew connectedDebugAndroidTest

# Run specific test class
./gradlew connectedDebugAndroidTest --tests "com.truth.training.client.NodeDiscoveryTest"

# Run with verbose output
./gradlew connectedDebugAndroidTest --info

# Run with coverage
./gradlew connectedDebugAndroidTest jacocoTestReport
```

### Network Requirements

For cross-device discovery tests:
- Desktop/CLI and Android device on same Wi-Fi network
- UDP multicast enabled (port 52525)
- Firewall allows UDP multicast traffic
- No VPN or network isolation

### Troubleshooting

**Issue**: Tests fail with "No devices found"
- **Fix**: Run `adb devices` and verify device is listed
- **Fix**: Enable USB debugging on device

**Issue**: UDP multicast not working
- **Fix**: Check Wi-Fi multicast lock is acquired
- **Fix**: Verify firewall allows port 52525
- **Fix**: Test on same network segment (no router isolation)

## References

- **Cross-Platform Compatibility**: `[docs/cross_platform_discovery_compatibility.md](docs/cross_platform_discovery_compatibility.md)`
- **Post-Integration Hardening**: `[docs/post_integration_hardening.md](docs/post_integration_hardening.md)`
- **Rust Implementation**: `src/p2p/node.rs`
- **Desktop Implementation**: `ui/desktop/src-tauri/src/discovery.rs`
- **CLI Implementation**: `app/src/bin/truthctl.rs`

_Version: v1.0.0_
