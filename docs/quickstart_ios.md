# Quickstart: iOS Mobile App Installation and Usage

This document provides step-by-step instructions for installing, using, and uninstalling the Truth Training iOS mobile application.

## Prerequisites

- iOS device running iOS 13.0 or higher
- Network access for synchronization (optional, works offline)
- App Store access (for App Store distribution) OR ability to install via TestFlight/Enterprise distribution

## Installation

### From App Store

#### Step 1: Search for App
1. Open App Store
2. Search for "Truth Training"
3. Tap on the app

#### Step 2: Install
1. Tap "Get" or price button
2. Authenticate with Face ID, Touch ID, or Apple ID password
3. Wait for download and installation
4. Tap "Open" when installation completes

### From TestFlight

#### Step 1: Install TestFlight
1. Install TestFlight from App Store (if not already installed)
2. Accept invitation email or tap TestFlight link

#### Step 2: Install App
1. Open TestFlight app
2. Find "Truth Training" in list
3. Tap "Install"
4. Wait for installation
5. Tap "Open" when done

### From Enterprise Distribution

#### Step 1: Install Profile
1. Open distribution link in Safari
2. Tap "Install" on profile
3. Go to Settings → General → Profiles
4. Tap on profile and install

#### Step 2: Install App
1. Open distribution link again
2. Tap "Install" on app
3. Wait for installation
4. Trust developer in Settings → General → Device Management

## First Launch

### Step 1: Launch Application
- Tap "Truth Training" icon from home screen
- App opens to Dashboard screen

### Step 2: Grant Permissions (if requested)
- Network access (for synchronization)
- Local network access (for node discovery)
- Notifications (optional, for sync status)

### Step 3: Initial Setup
1. App automatically initializes local database
2. No additional setup required for offline use
3. For synchronization, configure server connection in Settings

## Basic Usage

### Navigation

**Tab Bar Navigation:**
- Dashboard (home icon)
- Events (event icon)
- Contexts (template icon)
- Judgments (judgment icon)
- Nodes (network icon)
- Settings (settings icon)

### Creating Events

1. Tap "Events" tab
2. Tap "+" button in top right
3. Fill in event form:
   - Description (required)
   - Select context template (optional)
   - Modify context fields if needed
   - Set start/end dates using date pickers
4. Tap "Create" in top right
5. Event appears in events list

### Managing Context Templates

1. Tap "Contexts" tab
2. Tap "+" to create new template
3. Fill in template form:
   - Name (required)
   - Description (optional)
   - Set context fields (all optional)
4. Tap "Save"
5. Template appears in list

### Viewing Events

1. Tap "Events" tab
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
3. Set impact level (1-5) using stepper or picker
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

1. Tap "Nodes" tab
2. View discovered nodes
3. Pull down to refresh and discover new nodes
4. Tap on node to view details:
   - Address
   - Type (Hub/Leaf)
   - Status (reachable/unreachable)
   - Last seen timestamp

### Synchronization

1. App automatically syncs in background
2. Manual sync:
   - Go to Dashboard
   - Pull down to refresh
   - Or tap "Sync Now" button
3. View sync status:
   - Online/Offline indicator
   - Last sync time
   - Pending operations count

## Uninstallation

### Step 1: Uninstall Application

#### Method 1: From Home Screen
1. Long press "Truth Training" icon
2. Tap "Remove App"
3. Tap "Delete App"
4. Confirm deletion

#### Method 2: From Settings
1. Go to Settings → General → iPhone Storage
2. Find "Truth Training"
3. Tap on app
4. Tap "Delete App"
5. Confirm deletion

### Step 2: Remove Application Data

**Note:** Uninstalling the app automatically removes all app data. No manual cleanup required.

#### Verify Data Removal
1. Go to Settings → General → iPhone Storage
2. "Truth Training" should not appear in list
3. Check iCloud backup (if enabled) - app data should be removed

### Step 3: Remove from iCloud (if applicable)
1. Go to Settings → [Your Name] → iCloud → Manage Storage
2. Find "Truth Training" (if present)
3. Tap and delete data

## Data Backup

Before uninstalling, backup your data:

### Method 1: iCloud Backup
1. Ensure iCloud Backup is enabled:
   - Settings → [Your Name] → iCloud → iCloud Backup
2. Backup will include app data automatically

### Method 2: iTunes/Finder Backup
1. Connect device to computer
2. Open iTunes (macOS Catalina+) or Finder (macOS Catalina+)
3. Select device
4. Click "Back Up Now"
5. App data included in backup

### Method 3: Export Data (if implemented)
1. Go to Settings in app
2. Tap "Export Data"
3. Share file via AirDrop, email, or Files app

### Method 4: Manual Database Copy (Advanced)
```bash
# Using libimobiledevice (requires jailbreak or developer tools)
idevicebackup2 backup ~/backup_directory
# Database will be in backup_directory
```

## Data Locations

| Component | Location |
|-----------|----------|
| Application | `/var/containers/Bundle/Application/[UUID]/TruthTraining.app/` |
| Documents | `/var/mobile/Containers/Data/Application/[UUID]/Documents/` |
| Database | `/var/mobile/Containers/Data/Application/[UUID]/Library/Application Support/truth_training.sqlite` |
| Preferences | `/var/mobile/Containers/Data/Application/[UUID]/Library/Preferences/` |
| Cache | `/var/mobile/Containers/Data/Application/[UUID]/Library/Caches/` |

**Note:** UUIDs are unique per installation and change on reinstall.

## Troubleshooting

### App Won't Start
1. Force quit and restart:
   - Swipe up from bottom (or double-tap home button on older devices)
   - Swipe up on Truth Training app card
2. Restart device
3. Delete and reinstall app

### Sync Issues
1. Check network connectivity
2. Verify server is running (if using HTTP mode)
3. Check sync status on Dashboard
4. Try manual sync (pull to refresh)

### Database Errors
1. Delete and reinstall app (will remove all local data)
2. Restore from backup if needed

### Performance Issues
1. Restart device
2. Check available storage space:
   - Settings → General → iPhone Storage
3. Update to latest iOS version
4. Update app to latest version

### Installation Issues
1. Check available storage space
2. Restart device
3. Sign out and sign back into App Store
4. Update iOS to latest version

## Related Documentation

- [iOS Integration Guide](integration/ios/README_INTEGRATION.md) - Technical integration details
- [Cross-Platform Architecture](../spec/18-cross-platform-architecture.md) - Platform architecture
- [Core Functional Specification](../spec/22-function_core.md) - Core library reference

_Version: v1.0.0_

