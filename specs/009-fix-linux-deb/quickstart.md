# Quickstart: Package Installation Verification

This document provides step-by-step verification scenarios for package installation across all platforms.

## Prerequisites

- Built packages available (DEB, RPM, PKG, EXE)
- Target platform with appropriate package manager
- User account with installation permissions (no root/admin required for service management)

## Linux (DEB) Installation

### Step 1: Install Package
```bash
sudo dpkg -i truth-core-server_1.0.0_amd64.deb
```

**Expected Output**:
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

## Linux (RPM) Installation

### Step 1: Install Package
```bash
sudo rpm -i truth-core-server-1.0.0.x86_64.rpm
```

**Expected Output**: Same as DEB installation

### Step 2-5: Verification
Follow same verification steps as DEB installation.

## macOS (.pkg) Installation

### Step 1: Install Package
```bash
sudo installer -pkg truth-core-server-macos.pkg -target /
```

**Expected Output**:
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

## Windows (EXE) Installation

### Step 1: Install Package
```powershell
.\truth-core-server-windows.exe
# Follow installer wizard
```

**Expected Output**:
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

### Step 4: Verify Service Management (No Admin)
```powershell
Stop-Service -Name "TruthCoreServer"
Start-Service -Name "TruthCoreServer"
Restart-Service -Name "TruthCoreServer"
# Expected: All commands succeed (if user service supported)
```

### Step 5: Verify Auto-Start
```powershell
# Reboot system
Get-Service -Name "TruthCoreServer"
# Expected: Service automatically started on boot
```

## Package Upgrade Verification

### Linux (DEB)
```bash
# Install old version first
sudo dpkg -i truth-core-server_0.9.0_amd64.deb

# Upgrade to new version
sudo dpkg -i truth-core-server_1.0.0_amd64.deb

# Verify
which truth-core-server
# Expected: /usr/bin/truth-core-server (new location, not /usr/local/bin)

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

## Package Uninstall Verification

### Linux (DEB)
```bash
sudo dpkg -r truth-core-server

# Verify removal
which truth-core-server
# Expected: Command not found

systemctl --user status truth-core-server
# Expected: Service not found or inactive
```

### macOS (.pkg)
```bash
sudo pkgutil --forget com.truth.training.server
# Manual removal of files required

# Verify removal
which truth-core-server
# Expected: Command not found

launchctl list | grep com.truth.training.server
# Expected: Service not found
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

## Success Criteria

All verification steps must pass:
- ✅ Binary installed to correct standard location
- ✅ Binary named with hyphens (`truth-core-server`)
- ✅ Service installed to user-specific location
- ✅ Service runs under installing user account (not root, not dedicated service user)
- ✅ Service manageable without root/admin privileges
- ✅ Service auto-starts on boot/login
- ✅ Package upgrade preserves configuration
- ✅ Package uninstall removes all files and services

_Version: v1.0.0_
