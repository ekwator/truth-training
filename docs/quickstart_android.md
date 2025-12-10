# Quickstart: Android Mobile App Installation and Usage

This document provides step-by-step instructions for installing, using, and uninstalling the Truth Training Android mobile application.

## Prerequisites

- Android device running Android 8.0 (API level 26) or higher
- Network access for synchronization (optional, works offline)
- Google Play Store access (for Play Store distribution) OR ability to install APK files

## Localization Status

**Android app supports RU/EN language switching.** The app can be switched between English and Russian via the Settings screen. Language preference persists across app restarts.

**Note**: Both Desktop and Android support RU/EN language switching. For current localization status across platforms, see `spec/09-ux-guidelines.md` and `docs/UI_Desktop.md`.

**Implementation Details**: For comprehensive documentation on the localization implementation, including architecture, algorithms, and technical details, see `specs/014-android-localization/LOCALIZATION_IMPLEMENTATION.md`.

## Installation

### From Google Play Store

#### Step 1: Search for App
1. Open Google Play Store
2. Search for "Truth Training"
3. Tap on the app

#### Step 2: Install
1. Tap "Install" button
2. Wait for download and installation
3. Tap "Open" when installation completes

### From APK File (Side Loading)

#### Step 1: Enable Unknown Sources
1. Go to Settings → Security
2. Enable "Unknown sources" or "Install unknown apps"
3. Select the app you'll use to install (e.g., Files, Chrome)

#### Step 2: Download APK
```bash
# Download APK file (Debug build for testing)
wget https://github.com/ekwator/truth-training/releases/download/v1.0.0/app-debug.apk

# Or download AAB file (Release build for Google Play)
wget https://github.com/ekwator/truth-training/releases/download/v1.0.0/app-release.aab
```

#### Step 3: Install APK
1. Open file manager
2. Navigate to downloaded APK file
3. Tap on APK file
4. Tap "Install"
5. Wait for installation
6. Tap "Open" when done

#### Step 4: Verify Installation
1. Look for "Truth Training" app icon in app drawer
2. Tap to launch
3. App should open to Dashboard screen

### From ADB (Android Debug Bridge)

This method is recommended for developers and testers who have Android Debug Bridge (ADB) installed and a device connected via USB or wireless debugging.

#### Prerequisites

1. **Install ADB:**
   ```bash
   # On Linux (Debian/Ubuntu)
   sudo apt install android-tools-adb
   
   # On macOS
   brew install android-platform-tools
   
   # On Windows
   # Download from: https://developer.android.com/studio/releases/platform-tools
   ```

2. **Enable USB Debugging on Device:**
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times to enable Developer Options
   - Go to Settings → Developer Options
   - Enable "USB Debugging"

3. **Connect Device:**
   ```bash
   # Check if device is connected
   adb devices
   # Should show your device ID
   ```

#### Build Variants Overview

The Android app has **3 product flavors** and **2 build types**, resulting in **6 build variants**:

| Flavor | Description | Base URL | Use Case |
|--------|-------------|----------|----------|
| **local** | Local development | `http://10.0.2.2:8080` | Development with local server (Android Emulator) |
| **mock** | Mock/Testing | `http://mock` | Testing without real server, uses mock data |
| **remote** | Production/Remote | `https://truth-core.example.com` | Production deployment with remote server |

| Build Type | Description | Use Case |
|------------|-------------|----------|
| **debug** | Debug build | Development, testing, includes debug symbols |
| **release** | Release build | Production, optimized, no debug symbols |

#### Building APK Files

Navigate to the project root and build the desired variant:

```bash
cd truth-android-client

# Local variants (for development with local server)
./gradlew assembleLocalDebug      # Debug build for local development
./gradlew assembleLocalRelease    # Release build for local testing

# Mock variants (for testing without server)
./gradlew assembleMockDebug       # Debug build with mock data
./gradlew assembleMockRelease      # Release build with mock data

# Remote variants (for production/remote server)
./gradlew assembleRemoteDebug     # Debug build for remote server
./gradlew assembleRemoteRelease   # Release build for production
```

**APK File Locations:**

After building, APK files are located at:
```
truth-android-client/app/build/outputs/apk/{flavor}/{buildType}/app-{flavor}-{buildType}.apk
```

**Specific file paths:**
- **Local Debug:** `app/build/outputs/apk/local/debug/app-local-debug.apk`
- **Local Release:** `app/build/outputs/apk/local/release/app-local-release-unsigned.apk` (unsigned, requires signing for production)
- **Mock Debug:** `app/build/outputs/apk/mock/debug/app-mock-debug.apk`
- **Mock Release:** `app/build/outputs/apk/mock/release/app-mock-release-unsigned.apk` (unsigned, requires signing for production)
- **Remote Debug:** `app/build/outputs/apk/remote/debug/app-remote-debug.apk`
- **Remote Release:** `app/build/outputs/apk/remote/release/app-remote-release-unsigned.apk` (unsigned, requires signing for production)

**Note:** Release APK files are unsigned by default. For production deployment, you need to sign them using `jarsigner` or Android Studio's signing configuration.

#### Building AAB Files (Android App Bundle)

AAB files are used for Google Play Store distribution:

```bash
cd truth-android-client

# Build AAB files
./gradlew bundleLocalDebug        # Local debug bundle
./gradlew bundleLocalRelease      # Local release bundle
./gradlew bundleMockDebug         # Mock debug bundle
./gradlew bundleMockRelease       # Mock release bundle
./gradlew bundleRemoteDebug       # Remote debug bundle
./gradlew bundleRemoteRelease     # Remote release bundle
```

**AAB File Locations:**

AAB files are located at:
```
truth-android-client/app/build/outputs/bundle/{flavor}{BuildType}/app-{flavor}-{buildType}.aab
```

**Specific file paths:**
- **Local Debug:** `app/build/outputs/bundle/localDebug/app-local-debug.aab`
- **Local Release:** `app/build/outputs/bundle/localRelease/app-local-release.aab`
- **Mock Debug:** `app/build/outputs/bundle/mockDebug/app-mock-debug.aab`
- **Mock Release:** `app/build/outputs/bundle/mockRelease/app-mock-release.aab`
- **Remote Debug:** `app/build/outputs/bundle/remoteDebug/app-remote-debug.aab`
- **Remote Release:** `app/build/outputs/bundle/remoteRelease/app-remote-release.aab`

#### Installing via ADB

**Step 1: Build APK**
```bash
cd truth-android-client

# Choose the variant you need (example: local debug)
./gradlew assembleLocalDebug
```

**Step 2: Install APK on Device**
```bash
# From project root
adb install -r truth-android-client/app/build/outputs/apk/local/debug/app-local-debug.apk

# Or use full path
adb install -r /path/to/truth-training/truth-android-client/app/build/outputs/apk/local/debug/app-local-debug.apk
```

**Step 3: Launch Application**
```bash
adb shell am start -n com.truth.training.client/.MainActivity
```

**Step 4: Verify Installation**
```bash
# Check if app is installed
adb shell pm list packages | grep truth.training

# Check app version
adb shell dumpsys package com.truth.training.client | grep versionName
```

#### Quick Install Commands

**For Local Development (most common):**
```bash
cd truth-android-client
./gradlew assembleLocalDebug
adb install -r app/build/outputs/apk/local/debug/app-local-debug.apk
adb shell am start -n com.truth.training.client/.MainActivity
```

**For Testing with Mock Data:**
```bash
cd truth-android-client
./gradlew assembleMockDebug
adb install -r app/build/outputs/apk/mock/debug/app-mock-debug.apk
adb shell am start -n com.truth.training.client/.MainActivity
```

**For Production Testing:**
```bash
cd truth-android-client
./gradlew assembleRemoteRelease
adb install -r app/build/outputs/apk/remote/release/app-remote-release-unsigned.apk
adb shell am start -n com.truth.training.client/.MainActivity
```

#### Uninstalling via ADB

```bash
# Uninstall the app
adb uninstall com.truth.training.client

# Verify removal
adb shell pm list packages | grep truth.training
# Should return nothing
```

#### Build Variant Selection Guide

**Choose `local` variant if:**
- You're developing the app
- You have a local Truth Core server running (e.g., on `localhost:8080`)
- You're using Android Emulator (10.0.2.2 maps to host's localhost)
- You need to test with real server but locally

**Choose `mock` variant if:**
- You want to test UI without a real server
- You're testing offline functionality
- You need consistent test data
- You're running automated tests

**Choose `remote` variant if:**
- You're deploying to production
- You're testing with a remote Truth Core server
- You're preparing for Google Play Store release
- You need production-like configuration

**Choose `debug` build if:**
- You're developing or debugging
- You need debug symbols and logging
- You want to use Android Studio debugger
- You're testing new features

**Choose `release` build if:**
- You're preparing for production
- You want optimized performance
- You're submitting to Google Play Store
- You need production-ready build

#### Troubleshooting ADB Installation

**Device not detected:**
```bash
# Check ADB connection
adb devices

# Restart ADB server
adb kill-server
adb start-server
adb devices
```

**Installation fails:**
```bash
# Uninstall existing version first
adb uninstall com.truth.training.client

# Then install again
adb install -r app/build/outputs/apk/local/debug/app-local-debug.apk
```

**Permission denied:**
```bash
# Check USB debugging is enabled on device
# On device: Settings → Developer Options → USB Debugging

# Check ADB has proper permissions
adb devices
# If device shows "unauthorized", accept the prompt on device
```

## First Launch

### Step 1: Launch Application
- Tap "Truth Training" icon from app drawer
- App opens to Dashboard screen

### Step 2: Grant Permissions (if requested)
- Network access (for synchronization)
- Storage access (for database)
- Location access (optional, for node discovery)

### Step 3: Initial Setup
1. App automatically initializes local database
2. No additional setup required for offline use
3. For synchronization, configure server connection in Settings

## Basic Usage

### Navigation

**Bottom Navigation:**
- Dashboard (home icon)
- Events (event icon)
- Contexts (template icon)
- Judgments (judgment icon)
- Nodes (network icon)
- Settings (settings icon)

### Creating Events

1. Tap "Events" in bottom navigation
2. Tap FAB (+) button
3. Fill in event form:
   - Description (required)
   - Select context template (optional)
   - Modify context fields if needed
   - Set start/end dates
4. Tap "Create Event"
5. Event appears in events list

### Managing Context Templates

1. Tap "Contexts" in bottom navigation
2. Tap FAB (+) to create new template
3. Fill in template form:
   - Name (required)
   - Description (optional)
   - Set context fields (all optional)
4. Tap "Save"
5. Template appears in list

### Viewing Events

1. Tap "Events" in bottom navigation
2. Tap on event in list
3. Event detail screen shows:
   - Full event information
   - Associated statements
   - Impacts
   - Judgments
   - Consensus score (if available)

### Adding Impacts

1. Open event detail view
2. Tap "Add Impact"
3. Set impact level (1-5) using slider
4. Add notes (optional)
5. Tap "Save"

### Submitting Judgments

1. Open event detail view
2. Tap "Submit Judgment"
3. Select assessment: 'true', 'false', or 'uncertain'
4. Set confidence level (0.0-1.0) using slider
5. Add reasoning (optional)
6. Tap "Submit"

### Viewing Network Nodes

1. Tap "Nodes" in bottom navigation
2. View discovered nodes
3. Tap refresh to discover new nodes
4. View node details:
   - Address
   - Type (Hub/Leaf)
   - Status (reachable/unreachable)
   - Last seen timestamp

### Synchronization

1. App automatically syncs in background every 15 minutes
2. Manual sync:
   - Go to Dashboard
   - Tap "Sync Now" button
3. View sync status:
   - Online/Offline indicator
   - Last sync time
   - Pending operations count

## Uninstallation

### Step 1: Uninstall Application

#### Method 1: From Settings
1. Go to Settings → Apps
2. Find "Truth Training"
3. Tap "Uninstall"
4. Confirm uninstallation

#### Method 2: From App Drawer
1. Long press "Truth Training" icon
2. Tap "Uninstall" or drag to Uninstall area
3. Confirm uninstallation

### Step 2: Remove Application Data (Optional)

**Note:** Uninstalling the app typically removes all data. If you want to preserve data, backup before uninstalling.

#### Remove Data Manually
1. Go to Settings → Apps
2. Find "Truth Training" (even if uninstalled, may show in list)
3. Tap "Storage"
4. Tap "Clear Data" or "Delete Data"

#### Remove Database Files
```bash
# Using ADB (Android Debug Bridge)
adb shell
run-as com.truth.training.client
rm -rf /data/data/com.truth.training.client/databases/
rm -rf /data/data/com.truth.training.client/files/
exit
```

### Step 3: Verify Removal
1. Check app drawer - "Truth Training" should not appear
2. Go to Settings → Apps - "Truth Training" should not be listed
3. Check storage - app data should be removed

## Data Backup

Before uninstalling, backup your data:

### Method 1: Export Data (if implemented)
1. Go to Settings
2. Tap "Export Data"
3. Save backup file to external storage

### Method 2: Manual Backup via ADB
```bash
# Backup database
adb backup -f truth_training_backup.ab com.truth.training.client

# Restore later
adb restore truth_training_backup.ab
```

### Method 3: Copy Database File
```bash
# Using ADB
adb shell
run-as com.truth.training.client
cp databases/truth_training.db /sdcard/truth_training_backup.db
exit

# Pull to computer
adb pull /sdcard/truth_training_backup.db .
```

## Data Locations

| Component | Location |
|-----------|----------|
| Application | `/data/app/com.truth.training.client/` |
| Database | `/data/data/com.truth.training.client/databases/truth_training.db` |
| Shared Preferences | `/data/data/com.truth.training.client/shared_prefs/` |
| Cache | `/data/data/com.truth.training.client/cache/` |
| External Storage | `/sdcard/Android/data/com.truth.training.client/` |

## Troubleshooting

### App Won't Start
1. Clear app cache:
   - Settings → Apps → Truth Training → Storage → Clear Cache
2. Restart device
3. Reinstall app

### Sync Issues
1. Check network connectivity
2. Verify server is running (if using HTTP mode)
3. Check sync status on Dashboard
4. Try manual sync

### Database Errors
1. Clear app data (will remove all local data):
   - Settings → Apps → Truth Training → Storage → Clear Data
2. Reinstall app

### Performance Issues
1. Clear app cache
2. Restart device
3. Check available storage space
4. Update to latest version

## Related Documentation

- [Android Integration Guide](integration/android/README_INTEGRATION.md) - Technical integration details
- [Android Migration Guide](ANDROID_MIGRATION.md) - Migration instructions
- [Android Discovery Architecture](android_discovery_architecture.md) - Discovery implementation
- [Android Functional Specification](../spec/24-function_mobile_android.md) - Complete functional spec
- [Logging](logging.md) - Log file locations, reading, and clearing logs

_Version: v1.0.0_

