# Quickstart: Server Installation and Management

This document provides step-by-step instructions for installing, using, and uninstalling the Truth Training Server across all platforms.

## Prerequisites

- Built packages available (DEB, RPM, PKG, EXE)
- Target platform with appropriate package manager
- User account with installation permissions (no root/admin required for service management)

## Linux (DEB) Installation

### Step 1: Install Package
```bash
sudo dpkg -i truth-core-server_1.0.0_amd64.deb
```

**Expected Output:**
- Package installs successfully
- Binary placed at `/usr/bin/truth-core-server`
- Service file placed at `~/.config/systemd/user/truth-core-server.service`
- Service enabled and started automatically

### Step 2: Verify Binary Location
```bash
which truth-core-server
# Expected: /usr/bin/truth-core-server

ls -la /usr/bin/truth-core-server
# Expected: Executable file exists, executable permissions
```

### Step 3: Verify Service Status
```bash
systemctl --user status truth-core-server
# Expected: Active (running), enabled, running under current user

systemctl --user is-enabled truth-core-server
# Expected: enabled

ps aux | grep truth-core-server
# Expected: Process running under current user (not root, not truthd)
```

### Step 4: Verify Service Management (No Root)
```bash
systemctl --user stop truth-core-server
systemctl --user start truth-core-server
systemctl --user restart truth-core-server
# Expected: All commands succeed without sudo
```

### Step 5: Verify Auto-Start
```bash
# Log out and log back in, or reboot
systemctl --user status truth-core-server
# Expected: Service automatically started on login/boot
```

### Step 6: Verify Server Endpoints
```bash
curl http://127.0.0.1:8080/api/v1/info
# Expected: JSON response with node information

curl http://127.0.0.1:8080/health
# Expected: Health check response
```

## Linux (RPM) Installation

### Step 1: Install Package
```bash
sudo rpm -i truth-core-server-1.0.0.x86_64.rpm
```

**Expected Output:** Same as DEB installation

### Step 2-6: Verification
Follow same verification steps as DEB installation.

## macOS (.pkg) Installation

### Step 1: Install Package
```bash
sudo installer -pkg truth-core-server-macos.pkg -target /
```

**Expected Output:**
- Package installs successfully
- Binary placed at `/usr/local/bin/truth-core-server`
- LaunchAgent placed at `~/Library/LaunchAgents/com.truth.training.server.plist`
- Service loaded and started automatically

### Step 2: Verify Binary Location
```bash
which truth-core-server
# Expected: /usr/local/bin/truth-core-server

ls -la /usr/local/bin/truth-core-server
# Expected: Executable file exists, executable permissions
```

### Step 3: Verify Service Status
```bash
launchctl list | grep com.truth.training.server
# Expected: Service listed, running under current user

ps aux | grep truth-core-server
# Expected: Process running under current user (not root)
```

### Step 4: Verify Service Management (No Root)
```bash
launchctl stop com.truth.training.server
launchctl start com.truth.training.server
# Expected: All commands succeed without sudo
```

### Step 5: Verify Auto-Start
```bash
# Log out and log back in
launchctl list | grep com.truth.training.server
# Expected: Service automatically started on login
```

### Step 6: Verify Server Endpoints
```bash
curl http://127.0.0.1:8080/api/v1/info
# Expected: JSON response with node information
```

## Windows (EXE) Installation

### Step 1: Install Package
```powershell
.\truth-core-server-windows.exe
# Follow installer wizard
```

**Expected Output:**
- Package installs successfully
- Binary placed at `C:\Program Files\TruthCoreServer\truth-core-server.exe`
- WinSW service installed and started automatically

### Step 2: Verify Binary Location
```powershell
Test-Path "C:\Program Files\TruthCoreServer\truth-core-server.exe"
# Expected: True

Get-Item "C:\Program Files\TruthCoreServer\truth-core-server.exe"
# Expected: File exists, executable
```

### Step 3: Verify Service Status
```powershell
Get-Service -Name "TruthCoreServer"
# Expected: Service exists, Running, Started

Get-Process -Name "truth-core-server"
# Expected: Process running under current user (not SYSTEM)
```

### Step 4: Verify Service Management
```powershell
Stop-Service -Name "TruthCoreServer"
Start-Service -Name "TruthCoreServer"
Restart-Service -Name "TruthCoreServer"
# Expected: All commands succeed
```

### Step 5: Verify Auto-Start
```powershell
# Reboot system
Get-Service -Name "TruthCoreServer"
# Expected: Service automatically started on boot
```

### Step 6: Verify Server Endpoints
```powershell
Invoke-WebRequest -Uri "http://127.0.0.1:8080/api/v1/info"
# Expected: JSON response with node information
```

## Package Upgrade

### Linux (DEB)
```bash
# Install old version first
sudo dpkg -i truth-core-server_0.9.0_amd64.deb

# Upgrade to new version
sudo dpkg -i truth-core-server_1.0.0_amd64.deb

# Verify
which truth-core-server
# Expected: /usr/bin/truth-core-server (new location)

systemctl --user status truth-core-server
# Expected: Service running with new binary
```

### macOS (.pkg)
```bash
# Install old version first
sudo installer -pkg truth-core-server-macos-0.9.0.pkg -target /

# Upgrade to new version
sudo installer -pkg truth-core-server-macos-1.0.0.pkg -target /

# Verify
which truth-core-server
# Expected: /usr/local/bin/truth-core-server

launchctl list | grep com.truth.training.server
# Expected: Service running with new binary
```

### Windows (EXE)
```powershell
# Run new installer
.\truth-core-server-windows-1.0.0.exe

# Verify
Get-Service -Name "TruthCoreServer"
# Expected: Service running with new binary
```

## Uninstallation

### Linux (DEB) - Complete Removal

#### Step 1: Stop and Disable Service
```bash
systemctl --user stop truth-core-server
systemctl --user disable truth-core-server
```

#### Step 2: Remove Package
```bash
sudo dpkg -r truth-core-server
```

#### Step 3: Remove Configuration and Data
```bash
# Remove user configuration
rm -rf ~/.truth-training/

# Remove database (if you want to remove all data)
rm -f ~/.local/share/TruthTraining/truth_training.sqlite
# Or on some systems:
rm -f ${XDG_DATA_HOME:-~/.local/share}/TruthTraining/truth_training.sqlite

# Remove service file (if not removed by package)
rm -f ~/.config/systemd/user/truth-core-server.service
systemctl --user daemon-reload
```

#### Step 4: Verify Removal
```bash
which truth-core-server
# Expected: Command not found

systemctl --user status truth-core-server
# Expected: Service not found or inactive

ls ~/.truth-training/
# Expected: Directory does not exist (if removed)
```

### Linux (RPM) - Complete Removal

#### Step 1: Stop and Disable Service
```bash
systemctl --user stop truth-core-server
systemctl --user disable truth-core-server
```

#### Step 2: Remove Package
```bash
sudo rpm -e truth-core-server
```

#### Step 3: Remove Configuration and Data
```bash
# Remove user configuration
rm -rf ~/.truth-training/

# Remove database
rm -f ${XDG_DATA_HOME:-~/.local/share}/TruthTraining/truth_training.sqlite

# Remove service file
rm -f ~/.config/systemd/user/truth-core-server.service
systemctl --user daemon-reload
```

#### Step 4: Verify Removal
```bash
which truth-core-server
# Expected: Command not found
```

### macOS (.pkg) - Complete Removal

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

# Unregister package (if registered)
sudo pkgutil --forget com.truth.training.server
```

#### Step 3: Remove Configuration and Data
```bash
# Remove user configuration
rm -rf ~/.truth-training/

# Remove database
rm -f ~/Library/Application\ Support/TruthTraining/truth_training.sqlite
```

#### Step 4: Verify Removal
```bash
which truth-core-server
# Expected: Command not found

launchctl list | grep com.truth.training.server
# Expected: Service not found

ls ~/.truth-training/
# Expected: Directory does not exist (if removed)
```

### Windows (EXE) - Complete Removal

#### Step 1: Stop and Remove Service
```powershell
Stop-Service -Name "TruthCoreServer"
sc.exe delete "TruthCoreServer"
```

#### Step 2: Uninstall via Control Panel
```powershell
# Or use Programs and Features in Control Panel
# Find "Truth Core Server" and click Uninstall
```

#### Step 3: Remove Installation Directory
```powershell
Remove-Item -Recurse -Force "C:\Program Files\TruthCoreServer"
```

#### Step 4: Remove Configuration and Data
```powershell
# Remove user configuration
Remove-Item -Recurse -Force "$env:USERPROFILE\.truth-training"

# Remove database
Remove-Item -Force "$env:APPDATA\TruthTraining\truth_training.sqlite"
```

#### Step 5: Verify Removal
```powershell
Test-Path "C:\Program Files\TruthCoreServer\truth-core-server.exe"
# Expected: False

Get-Service -Name "TruthCoreServer"
# Expected: Service not found

Test-Path "$env:USERPROFILE\.truth-training"
# Expected: False (if removed)
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

## Error Scenarios

### Linux: User Service Not Supported
```bash
# On system without systemd user service support
sudo dpkg -i truth-core-server_1.0.0_amd64.deb
# Expected: Installation fails with clear error message
# Error: "User services not supported. Please ensure systemd user service support is enabled."
```

### macOS: LaunchAgent Permission Issues
```bash
# If ~/Library/LaunchAgents/ cannot be created
sudo installer -pkg truth-core-server-macos.pkg -target /
# Expected: Installation fails with clear error message
```

### Windows: Service Installation Failure
```powershell
# If WinSW fails to install service
.\truth-core-server-windows.exe
# Expected: Installation fails with clear error message
# Check Windows Event Viewer for details
```

## Success Criteria

All verification steps must pass:
- ✅ Binary installed to correct standard location
- ✅ Binary named with hyphens (`truth-core-server`)
- ✅ Service installed to user-specific location
- ✅ Service runs under installing user account (not root, not dedicated service user)
- ✅ Service manageable without root/admin privileges
- ✅ Service auto-starts on boot/login
- ✅ Server responds to HTTP requests on port 8080
- ✅ Package upgrade preserves configuration
- ✅ Package uninstall removes all files, services, and optionally data

## Configuration Locations

After installation, configuration is stored at:

| Platform | Config Location | Database Location |
|----------|----------------|-------------------|
| Linux | `~/.truth-training/config.json` | `${XDG_DATA_HOME:-~/.local/share}/TruthTraining/truth_training.sqlite` |
| macOS | `~/.truth-training/config.json` | `~/Library/Application Support/TruthTraining/truth_training.sqlite` |
| Windows | `%USERPROFILE%\.truth-training\config.json` | `%APPDATA%\TruthTraining\truth_training.sqlite` |

## Related Documentation

- [Deployment Guide](Deployment.md) - Detailed deployment instructions
- [CLI Usage](CLI_Usage.md) - Command-line interface reference
- [API Reference](api_reference/API_REFERENCE.md) - HTTP API documentation
- [Install Paths by OS](Install_Paths_By_OS.md) - Platform-specific paths

_Version: v1.0.0_

