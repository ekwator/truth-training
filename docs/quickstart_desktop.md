# Quickstart: Desktop UI Installation and Usage

This document provides step-by-step instructions for installing, using, and uninstalling the Truth Training Desktop UI application.

## Prerequisites

- Operating System: Linux, Windows, or macOS
- Minimum system requirements (see release notes)
- Network access for synchronization (optional, works offline)

## Installation

### Linux

#### Step 1: Install Package
```bash
# DEB package
sudo dpkg -i truth-training-desktop_1.0.0_amd64.deb

# Or AppImage (no installation required)
chmod +x truth-training-desktop_1.0.0_amd64.AppImage
./truth-training-desktop_1.0.0_amd64.AppImage
```

#### Step 2: Verify Installation
```bash
# Check if application is in PATH
which truth-training-desktop

# Or launch from applications menu
# Look for "Truth Training" in your applications
```

### macOS

#### Step 1: Install Package
```bash
# DMG package
# Double-click truth-training-desktop_1.0.0_macos.dmg
# Drag application to Applications folder

# Or PKG installer
sudo installer -pkg truth-training-desktop_1.0.0_macos.pkg -target /
```

#### Step 2: Verify Installation
```bash
# Check application location
ls -la /Applications/Truth\ Training.app

# Or launch from Applications folder
open /Applications/Truth\ Training.app
```

### Windows

#### Step 1: Install Package
```powershell
# EXE installer
.\truth-training-desktop_1.0.0_windows.exe
# Follow installer wizard

# Or MSI package
msiexec /i truth-training-desktop_1.0.0_windows.msi
```

#### Step 2: Verify Installation
```powershell
# Check installation location
Test-Path "C:\Program Files\Truth Training\truth-training-desktop.exe"

# Or launch from Start Menu
# Look for "Truth Training" in Start Menu
```

## First Launch

### Step 1: Launch Application
- **Linux:** Launch from applications menu or run `truth-training-desktop`
- **macOS:** Open from Applications folder or Spotlight
- **Windows:** Launch from Start Menu

### Step 2: Initial Configuration
1. Application opens to Dashboard screen
2. Navigate to Settings (Alt+8 or click Settings in menu)
3. Choose connection mode:
   - **Core (Local):** Direct database access (default)
   - **HTTP API:** Connect to remote server
4. If using HTTP mode, configure:
   - Server IP address (default: 127.0.0.1)
   - Server port (default: 8080)
5. Test connection
6. Save configuration

### Step 3: Create First Event
1. Navigate to New Event (Alt+2 or click "New Event" in menu)
2. Fill in event details:
   - Title (required)
   - Description (optional)
   - Select context template (optional)
   - Set dates (optional)
3. Click "Create Event"
4. Event appears on Dashboard

### Resetting the Desktop Database (`init_app`)
If you need to reset the embedded SQLite database to the canonical Truth schema (for example, after installing a new build or testing migrations), run:

```bash
cd ui/desktop
pnpm tauri invoke init_app
```

The command now performs the following actions:

- Drops all legacy `events`/`impacts`/`summaries`/`logs` tables that belonged to the pre-Truth schema.
- Reapplies the canonical Truth schema from `core/src/storage.rs`, runs migrations, and seeds the knowledge base.
- Forces a WAL checkpoint, VACUUM, and rewrites `~/.truth-training/config.json` with defaults (including the RU/EN locale setting).
- Emits `db.init.*` telemetry/log entries so you can confirm the cleanup in logs.

You can invoke it multiple times—it is idempotent. After running, inspect the SQLite file (see the developer quickstart) to verify that tables such as `truth_events`, `statements`, `impact`, `progress_metrics`, and `schema_version` exist, while legacy tables are absent.

**Android Parity**: Android database initialization follows the same workflow:
- Uses shared SQL asset (`app/src/main/assets/schema.sql`) derived from `core/src/storage.rs`
- Drops legacy tables via `MIGRATION_3_4` without data migration
- Validates schema on database open to ensure legacy tables are absent
- Ensures schema parity with Desktop

## Basic Usage

### Navigation

**Keyboard Shortcuts:**
- `Alt+1` - Home (Dashboard)
- `Alt+2` - New Event
- `Alt+3` - Context Editor
- `Alt+4` - Event Summary
- `Alt+5` - Overall Summary
- `Alt+6` - Training Results
- `Alt+7` - Logs
- `Alt+8` - Settings

**Menu Bar:**
- [Home] | [New Event] | [Context Editor] | [Event Summary] | [Overall Summary] | [Training Results] | [Logs] | [Settings]

### Creating Events

1. Click "New Event" or press `Alt+2`
2. Enter event title (required)
3. Optionally select context template to prefill fields
4. Modify context fields if needed using ContextPicker components:
   - Type to search contexts by name or ID
   - Select from dropdown or enter ID manually
   - Invalid IDs are blocked with error message
   - Data is cached for offline use (24h TTL)
5. Set start/end dates
6. Click "Create Event"

### Changing Language

1. **Quick Toggle**: Click language selector in top-right navigation bar
2. **Settings**: Navigate to Settings (`Alt+8`) → Language section
3. **Select Language**: Choose English or Russian from dropdown
4. **Persistence**: Language preference is saved and persists across app restarts
5. **Coverage**: Navigation, Settings, NewEvent, ContextPicker, and toast messages are translated

### Managing Context Templates

1. Navigate to Context Editor (`Alt+3`)
2. Create new template:
   - Enter template name
   - Set context fields (all optional)
   - Click "Create"
3. Edit existing template:
   - Click on template in list
   - Modify fields
   - Click "Save"
4. Delete template:
   - Click delete button (if implemented)

### Viewing Events

1. Dashboard shows recent events
2. Click on event card to view details
3. Event Summary screen shows:
   - Full event information
   - Associated statements
   - Impacts
   - Judgments
   - Consensus score (if available)

### Adding Impacts

1. Open event detail view
2. Click "Add Impact"
3. Set impact level (1-5)
4. Add notes (optional)
5. Click "Save"

### Submitting Judgments

1. Open event detail view
2. Click "Submit Judgment"
3. Select assessment: 'true', 'false', or 'uncertain'
4. Set confidence level (0.0-1.0)
5. Add reasoning (optional)
6. Click "Submit"

### Viewing Logs

1. Navigate to Logs (`Alt+7`)
2. View paginated log entries (35 per page)
3. Use Previous/Next to navigate
4. Click "Clear Logs" to remove all entries

### Exporting Data

1. Navigate to Overall Summary (`Alt+5`)
2. Click "Export Summary"
3. Save text file with summary data

## Uninstallation

### Linux (DEB)

#### Step 1: Remove Package
```bash
sudo dpkg -r truth-training
```

#### Step 2: Remove Configuration and Data
```bash
# Remove user configuration
rm -rf ~/.truth-training/

# Remove database (optional - removes all data)
rm -f ~/.local/share/TruthTraining/truth_training.sqlite
# Or on some systems:
rm -f ${XDG_DATA_HOME:-~/.local/share}/TruthTraining/truth_training.sqlite
```

#### Step 3: Verify Removal
```bash
which truth-training-desktop
# Expected: Command not found

ls ~/.truth-training/
# Expected: Directory does not exist (if removed)
```

### Linux (AppImage)

#### Step 1: Remove Application
```bash
# Simply delete the AppImage file
rm truth-training-desktop_1.0.0_amd64.AppImage
```

#### Step 2: Remove Configuration and Data
```bash
# Remove user configuration
rm -rf ~/.truth-training/

# Remove database (optional)
rm -f ~/.local/share/TruthTraining/truth_training.sqlite
```

### macOS

#### Step 1: Remove Application
```bash
# Remove from Applications
rm -rf /Applications/Truth\ Training.app

# Or use uninstaller if provided
```

#### Step 2: Remove Configuration and Data
```bash
# Remove user configuration
rm -rf ~/.truth-training/

# Remove database (optional)
rm -f ~/Library/Application\ Support/TruthTraining/truth_training.sqlite
```

#### Step 3: Remove Preferences (if any)
```bash
# Remove preferences
rm -f ~/Library/Preferences/com.truth.training.plist
```

### Windows

#### Step 1: Uninstall via Control Panel
```powershell
# Use Programs and Features in Control Panel
# Find "Truth Training" and click Uninstall
```

#### Step 2: Remove Installation Directory
```powershell
Remove-Item -Recurse -Force "C:\Program Files\Truth Training"
```

#### Step 3: Remove Configuration and Data
```powershell
# Remove user configuration
Remove-Item -Recurse -Force "$env:USERPROFILE\.truth-training"

# Remove database (optional)
Remove-Item -Force "$env:APPDATA\TruthTraining\truth_training.sqlite"
```

#### Step 4: Remove Start Menu Shortcuts
```powershell
Remove-Item -Recurse -Force "$env:APPDATA\Microsoft\Windows\Start Menu\Programs\Truth Training"
```

## Data Backup

Before uninstalling, backup your data:

### Linux/macOS
```bash
# Backup configuration
cp -r ~/.truth-training ~/.truth-training.backup

# Backup database
cp ~/.local/share/TruthTraining/truth_training.sqlite ~/truth_training.sqlite.backup
# Or on macOS:
cp ~/Library/Application\ Support/TruthTraining/truth_training.sqlite ~/truth_training.sqlite.backup
```

### Windows
```powershell
# Backup configuration
Copy-Item -Recurse "$env:USERPROFILE\.truth-training" "$env:USERPROFILE\.truth-training.backup"

# Backup database
Copy-Item "$env:APPDATA\TruthTraining\truth_training.sqlite" "$env:USERPROFILE\truth_training.sqlite.backup"
```

## Configuration Locations

| Platform | Config Location | Database Location |
|----------|----------------|-------------------|
| Linux | `~/.truth-training/config.json` | `${XDG_DATA_HOME:-~/.local/share}/TruthTraining/truth_training.sqlite` |
| macOS | `~/.truth-training/config.json` | `~/Library/Application Support/TruthTraining/truth_training.sqlite` |
| Windows | `%USERPROFILE%\.truth-training\config.json` | `%APPDATA%\TruthTraining\truth_training.sqlite` |

## Troubleshooting

### Application Won't Start
```bash
# Check system logs
# Linux: journalctl --user -u truth-training-desktop
# macOS: Console.app
# Windows: Event Viewer

# Check configuration file
cat ~/.truth-training/config.json
```

### Connection Issues
1. Navigate to Settings
2. Test connection
3. Verify server is running (if using HTTP mode)
4. Check firewall settings
5. Verify IP address and port

### Database Errors
```bash
# Check database file permissions
ls -la ~/.local/share/TruthTraining/truth_training.sqlite

# Check if database is locked
lsof ~/.local/share/TruthTraining/truth_training.sqlite
```

### Sync Issues
1. Check sync status on Dashboard
2. Verify network connectivity
3. Check server logs (if using HTTP mode)
4. Review application logs (Logs screen)

## Related Documentation

- [Desktop UI Guide](UI_Desktop.md) - Complete UI reference
- [Desktop Functional Specification](../spec/23-function_desktop.md) - Detailed functional spec
- [Deployment Guide](Deployment.md) - Server deployment instructions
- [Logging](logging.md) - Log file locations, reading, and clearing logs

_Version: v1.0.0_

