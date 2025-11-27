# Research: Fix Installer Packaging for Truth Core Server

## Research Questions

### 1. Systemd User Services for Packaging

**Question**: How to package systemd user services that don't require root privileges?

**Decision**: Use systemd user services installed to `~/.config/systemd/user/` instead of system-wide `/lib/systemd/system/`.

**Rationale**:
- User services run under the installing user account without root
- Standard location: `~/.config/systemd/user/` (XDG Base Directory spec)
- Managed via `systemctl --user` commands
- Auto-start on login via `systemctl --user enable --now`
- No need for `User=` directive in service file (runs as current user)

**Alternatives Considered**:
- System-wide service with `User=` directive: Rejected - requires root for installation and service management
- LaunchAgent (macOS-style): Not applicable to Linux systemd

**Implementation Notes**:
- Service file must NOT include `User=` or `Group=` directives
- Use `%i` or `%u` specifiers if user-specific paths needed
- Post-install script: `systemctl --user daemon-reload && systemctl --user enable --now truth-core-server`

### 2. FPM Packaging for User Services

**Question**: How to use FPM to package user services without systemd integration flags?

**Decision**: Do NOT use `--deb-systemd` flag. Instead, manually copy service file to user directory in postinst script.

**Rationale**:
- `--deb-systemd` flag installs to `/lib/systemd/system/` (system-wide, requires root)
- Manual installation in postinst allows user-specific location
- FPM can package files to any location, postinst handles service registration

**Alternatives Considered**:
- Use `--deb-systemd` with custom path: Not supported by FPM
- Separate service package: Overcomplicated for single-user installation

**Implementation Notes**:
- FPM command: `fpm -s dir -t deb -n truth-core-server -v <version> --prefix /usr bin/truth-core-server=/usr/bin/truth-core-server`
- Service file packaged to temporary location, moved in postinst
- Postinst creates `~/.config/systemd/user/` if missing

### 3. macOS LaunchAgent vs LaunchDaemon

**Question**: Should macOS use LaunchAgent (user) or LaunchDaemon (system) for user installation?

**Decision**: Use LaunchAgent installed to `~/Library/LaunchAgents/` instead of LaunchDaemon.

**Rationale**:
- LaunchDaemon requires root and runs as system user
- LaunchAgent runs under user account, no root required
- Standard location: `~/Library/LaunchAgents/com.truth.training.server.plist`
- Managed via `launchctl load ~/Library/LaunchAgents/com.truth.training.server.plist`

**Alternatives Considered**:
- LaunchDaemon: Rejected - requires root, runs as system user
- App bundle with embedded service: Overcomplicated for server binary

**Implementation Notes**:
- Plist file: Remove any `UserName` key (runs as current user)
- Postinstall script: `launchctl load ~/Library/LaunchAgents/com.truth.training.server.plist`
- Auto-start: `RunAtLoad` key in plist handles this

### 4. Windows User Service Installation

**Question**: How to install Windows service under user account without admin privileges?

**Decision**: Use WinSW with user service mode (requires Windows 10+ and user service support).

**Rationale**:
- WinSW 3.0+ supports user services (no admin required)
- Service runs under installing user account
- Standard Windows service management tools work

**Alternatives Considered**:
- Scheduled Task: Less standard, different management interface
- System service with user account: Requires admin during installation

**Implementation Notes**:
- WinSW config: No special user service flags needed (runs as installing user by default)
- Installation: `winsw.exe install` (no admin if user service supported)
- Service name: `TruthCoreServer` (no user prefix needed)

### 5. Binary Renaming During Build

**Question**: How to rename `truth_core_server` binary to `truth-core-server` during packaging?

**Decision**: Rename binary during packaging step, not during Rust build.

**Rationale**:
- Rust binary name comes from Cargo.toml `[[bin]]` name
- Changing Cargo.toml affects development builds
- Packaging step rename is standard practice (e.g., `cp truth_core_server truth-core-server`)
- Keeps development and production naming separate

**Alternatives Considered**:
- Change Cargo.toml binary name: Rejected - breaks development workflow
- Symbolic link: Rejected - adds complexity, not standard

**Implementation Notes**:
- Packaging script: `cp target/release/truth_core_server bin/truth-core-server`
- Windows: `cp target/release/truth_core_server.exe bin/truth-core-server.exe`
- All packaging scripts use renamed binary

### 6. Edge Case: User Service Permission Issues

**Question**: What happens when user can't create user services?

**Decision**: Installer should fail gracefully with clear error message and instructions.

**Rationale**:
- Better to fail early than create broken installation
- Clear error message helps user understand issue
- Instructions guide user to resolve (e.g., check systemd user service support)

**Implementation**:
- Postinst script checks: `systemctl --user` command availability
- If unavailable: Print error, exit with non-zero code
- Error message: "User services not supported. Please ensure systemd user service support is enabled."

### 7. Edge Case: User Account Deletion

**Question**: What happens if user account is deleted after installation?

**Decision**: Service remains orphaned (standard OS behavior). Package uninstall should handle cleanup.

**Rationale**:
- OS handles user account deletion (removes home directory)
- Service files in `~/.config/systemd/user/` are automatically removed with home directory
- Package uninstall (postrm) should verify and clean up if service still exists

**Implementation**:
- Postrm script: Check if service exists, stop and disable if found
- Graceful handling: Don't fail if service already removed by OS

## Resolved Clarifications

1. **User service permission issues**: Installer fails gracefully with clear error message
2. **User account deletion**: Service removed with home directory (OS behavior), postrm handles cleanup

## Technology Choices Summary

| Technology | Purpose | Version/Notes |
|------------|---------|---------------|
| FPM | DEB/RPM packaging | Ruby gem, latest stable |
| pkgbuild | macOS .pkg creation | macOS built-in tool |
| NSIS | Windows installer | 3.x, via Chocolatey |
| WinSW | Windows service wrapper | 3.0.3+ (user service support) |
| systemd | Linux service management | User services (systemd 232+) |
| launchctl | macOS service management | LaunchAgent (user services) |

## Platform-Specific Paths

### Linux (DEB/RPM)
- Executable: `/usr/bin/truth-core-server`
- Config: `/etc/truth-core-server/` (future)
- Service: `~/.config/systemd/user/truth-core-server.service`

### macOS (.pkg)
- Executable: `/usr/local/bin/truth-core-server`
- Config: `/usr/local/etc/truth-core-server/` (future)
- Service: `~/Library/LaunchAgents/com.truth.training.server.plist`

### Windows (EXE)
- Executable: `%PROGRAMFILES%\TruthCoreServer\truth-core-server.exe`
- Config: `%APPDATA%\TruthCoreServer\` (future)
- Service: Windows Service (via WinSW)

## Next Steps

All research questions resolved. Ready for Phase 1 design.

_Version: v1.0.0_
