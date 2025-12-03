## Deployment: Desktop UI with Background Server Service

This document describes how the Desktop UI bundles and installs the background service `truth-training-server` and how services are installed and verified across OSes.

### Server Binary

- Built by CI alongside the Tauri Desktop app:
  - Linux/macOS: `target/release/truth-training-server`
  - Windows: `target/release/truth-training-server.exe`

### Linux (Deb/RPM)

- Tauri bundler is configured to include:
  - Binary to `/usr/local/bin/truth-training-server`
  - systemd unit: `/lib/systemd/system/truth-training-server.service`
  - Post-install script: `/DEBIAN/postinst` (Deb)

- Post-install script performs:
  - `systemctl daemon-reload`
  - `systemctl enable truth-training-server`
  - `systemctl start truth-training-server`

- Verify:
  - `systemctl status truth-training-server`

### Windows (NSIS/MSI)

- CI downloads WinSW into `packaging/windows/winsw.exe` and installs a Windows service using:
  - Config: `packaging/windows/truth-training-server.xml`
  - Postinstall: `packaging/windows/postinstall.ps1`

- Verify:
  - `sc query TruthTrainingServer`

### macOS (App/DMG)

- LaunchDaemon plist: `packaging/macos/com.truth.training.server.plist`
- Postinstall script loads and starts daemon:
  - Copies plist to `/Library/LaunchDaemons/`
  - `launchctl load /Library/LaunchDaemons/com.truth.training.server.plist`
  - `launchctl start com.truth.training.server`

- Verify:
  - `launchctl list | grep truth.training.server`

### Desktop UI Integration

- The UI should be able to connect to the local server at `http://127.0.0.1:8080` (default) when in HTTP mode. Settings screen allows switching modes and testing connectivity.

## Installation Instructions

For detailed installation instructions, see [quickstart_server.md](quickstart_server.md). This section provides a summary.

### Linux (DEB/RPM)

#### Installation
```bash
# DEB package
sudo dpkg -i truth-core-server_1.0.0_amd64.deb

# RPM package
sudo rpm -i truth-core-server-1.0.0.x86_64.rpm
```

#### Post-Installation Verification
```bash
# Verify binary location
which truth-core-server
# Expected: /usr/bin/truth-core-server

# Verify service status
systemctl --user status truth-core-server
# Expected: Active (running), enabled

# Verify server endpoints
curl http://127.0.0.1:8080/api/v1/info
# Expected: JSON response with node information
```

### macOS (.pkg)

#### Installation
```bash
sudo installer -pkg truth-core-server-macos.pkg -target /
```

#### Post-Installation Verification
```bash
# Verify binary location
which truth-core-server
# Expected: /usr/local/bin/truth-core-server

# Verify service status
launchctl list | grep com.truth.training.server
# Expected: Service listed, running

# Verify server endpoints
curl http://127.0.0.1:8080/api/v1/info
# Expected: JSON response with node information
```

### Windows (EXE/MSI)

#### Installation
```powershell
# EXE installer
.\truth-core-server-windows.exe
# Follow installer wizard

# Or MSI package
msiexec /i truth-core-server-windows.msi
```

#### Post-Installation Verification
```powershell
# Verify binary location
Test-Path "C:\Program Files\TruthCoreServer\truth-core-server.exe"
# Expected: True

# Verify service status
Get-Service -Name "TruthCoreServer"
# Expected: Service exists, Running

# Verify server endpoints
Invoke-WebRequest -Uri "http://127.0.0.1:8080/api/v1/info"
# Expected: JSON response with node information
```

## Uninstallation Instructions

For detailed uninstallation instructions including data removal, see [quickstart_server.md](quickstart_server.md). This section provides a summary.

### Linux (DEB/RPM)

#### Step 1: Stop and Disable Service
```bash
systemctl --user stop truth-core-server
systemctl --user disable truth-core-server
```

#### Step 2: Remove Package
```bash
# DEB
sudo dpkg -r truth-core-server

# RPM
sudo rpm -e truth-core-server
```

#### Step 3: Remove Configuration and Data (Optional)
```bash
# Remove user configuration
rm -rf ~/.truth-training/

# Remove database (removes all data)
rm -f ~/.local/share/TruthTraining/truth_training.sqlite
# Or on some systems:
rm -f ${XDG_DATA_HOME:-~/.local/share}/TruthTraining/truth_training.sqlite

# Remove service file (if not removed by package)
rm -f ~/.config/systemd/user/truth-core-server.service
systemctl --user daemon-reload
```

### macOS (.pkg)

#### Step 1: Stop and Unload Service
```bash
launchctl stop com.truth.training.server
launchctl unload ~/Library/LaunchAgents/com.truth.training.server.plist
```

#### Step 2: Remove Package Files
```bash
# Remove binary
sudo rm -f /usr/local/bin/truth-core-server

# Remove LaunchAgent
rm -f ~/Library/LaunchAgents/com.truth.training.server.plist

# Unregister package
sudo pkgutil --forget com.truth.training.server
```

#### Step 3: Remove Configuration and Data (Optional)
```bash
# Remove user configuration
rm -rf ~/.truth-training/

# Remove database (removes all data)
rm -f ~/Library/Application\ Support/TruthTraining/truth_training.sqlite
```

### Windows (EXE/MSI)

#### Step 1: Stop and Remove Service
```powershell
Stop-Service -Name "TruthCoreServer"
sc.exe delete "TruthCoreServer"
```

#### Step 2: Uninstall via Control Panel
```powershell
# Use Programs and Features in Control Panel
# Find "Truth Core Server" and click Uninstall
```

#### Step 3: Remove Installation Directory
```powershell
Remove-Item -Recurse -Force "C:\Program Files\TruthCoreServer"
```

#### Step 4: Remove Configuration and Data (Optional)
```powershell
# Remove user configuration
Remove-Item -Recurse -Force "$env:USERPROFILE\.truth-training"

# Remove database (removes all data)
Remove-Item -Force "$env:APPDATA\TruthTraining\truth_training.sqlite"
```

## Data Backup Before Uninstallation

Before uninstalling, you may want to backup your data:

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

After installation, configuration and data are stored at:

| Platform | Config Location | Database Location |
|----------|----------------|-------------------|
| Linux | `~/.truth-training/config.json` | `${XDG_DATA_HOME:-~/.local/share}/TruthTraining/truth_training.sqlite` |
| macOS | `~/.truth-training/config.json` | `~/Library/Application Support/TruthTraining/truth_training.sqlite` |
| Windows | `%USERPROFILE%\.truth-training\config.json` | `%APPDATA%\TruthTraining\truth_training.sqlite` |

For more details, see [Install_Paths_By_OS.md](Install_Paths_By_OS.md).

## Related Documentation

- [Quickstart: Server](quickstart_server.md) - Complete installation and usage guide
- [Install Paths by OS](Install_Paths_By_OS.md) - Platform-specific paths
- [CI Workflows Artifacts](CI_Workflows_Artifacts.md) - Build artifacts information

---

## Database Migration Notes (v1.0.0)

**⚠️ IMPORTANT**: This release includes breaking changes to the database schema. **No automatic migrations are executed**. Manual migration is required for existing databases.

### Migration Requirements

1. **Backup Database**: Before migration, create a backup of your existing database.

2. **Schema Changes**:
   - Remove `context_id` column from `truth_events` table
   - Add five new nullable columns: `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`
   - Add foreign key constraints and indexes for new columns

3. **Data Migration**:
   - For existing events with `context_id`, extract context template values from `context` table
   - Populate embedded fields in `truth_events` based on template values
   - Ensure all FK references are valid before completing migration

4. **Migration Script**:
   A manual migration script should be provided or executed by database administrators. The script should:
   - Preserve existing event data
   - Map `context_id` → embedded fields based on template data
   - Validate all FK references after migration
   - Report any orphaned or invalid data

5. **Verification**:
   After migration, verify:
   - All events have valid embedded fields or NULL values
   - No orphaned FK references exist
   - Indexes are created for query performance
   - Template matching works correctly

For detailed migration instructions, see [docs/Data_Schema.md](Data_Schema.md) section "Migration Notes".



