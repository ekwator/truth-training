# Data Model: Installer Packaging

## Entities

### InstallationPackage

Represents a distributable package format containing the server executable, configuration templates, and service definitions.

**Attributes**:
- `format`: Package format (DEB, RPM, PKG, EXE)
- `platform`: Target operating system (Linux, macOS, Windows)
- `version`: Package version (semantic versioning)
- `executable_path`: Standard system path where executable is installed
- `config_path`: Standard system path where configuration files are placed (future)
- `service_path`: User-specific path where service definition is installed

**Relationships**:
- Contains one `ServiceDefinition`
- Contains one `InstallationLayout`
- Produced by one `BuildProcess`

**Validation Rules**:
- Package name must use hyphens: `truth-core-server` (not underscores)
- Executable name must match package name: `truth-core-server` (Linux/macOS), `truth-core-server.exe` (Windows)
- Version must follow semantic versioning (MAJOR.MINOR.PATCH)

### ServiceDefinition

Represents the configuration that defines how the server runs as a background service.

**Attributes**:
- `type`: Service type (systemd-user, launchagent, winsw-service)
- `name`: Service identifier (truth-core-server, com.truth.training.server, TruthCoreServer)
- `executable_path`: Full path to server executable
- `user_account`: User account that runs the service (installing user, not dedicated service user)
- `restart_policy`: Service restart behavior (always, on-failure, never)
- `auto_start`: Whether service starts automatically on system boot/login
- `environment_variables`: Key-value pairs for service environment (e.g., RUST_LOG=info)

**Relationships**:
- Belongs to one `InstallationPackage`
- References one `InstallationLayout` (executable path)

**Validation Rules**:
- Must NOT specify dedicated service user (User=truth, UserName, etc.)
- Must use user-specific service locations (not system-wide)
- Executable path must match `InstallationLayout.executable_path`

**State Transitions**:
- `installed` → `enabled` → `running` (after installation and start)
- `running` → `stopped` (manual stop or system shutdown)
- `stopped` → `running` (manual start or auto-start on boot/login)
- `enabled` → `disabled` → `removed` (during uninstall)

### InstallationLayout

Represents the filesystem structure where package files are placed.

**Attributes**:
- `executable_location`: Standard system directory for executables
  - Linux: `/usr/bin/`
  - macOS: `/usr/local/bin/`
  - Windows: `%PROGRAMFILES%\TruthCoreServer\`
- `config_location`: Standard system directory for configuration files
  - Linux: `/etc/truth-core-server/` (future)
  - macOS: `/usr/local/etc/truth-core-server/` (future)
  - Windows: `%APPDATA%\TruthCoreServer\` (future)
- `service_location`: User-specific directory for service definitions
  - Linux: `~/.config/systemd/user/`
  - macOS: `~/Library/LaunchAgents/`
  - Windows: Windows Service Registry (via WinSW)

**Relationships**:
- Belongs to one `InstallationPackage`
- Referenced by one `ServiceDefinition`

**Validation Rules**:
- Executable location must follow OS conventions (FHS for Linux, macOS standards, Windows standards)
- Service location must be user-specific (not require root/admin)
- Paths must use forward slashes in package definitions (Windows handled by installer)

## Platform-Specific Mappings

### Linux (DEB/RPM)
```
InstallationPackage {
  format: "DEB" | "RPM"
  platform: "Linux"
  executable_path: "/usr/bin/truth-core-server"
  service_path: "~/.config/systemd/user/truth-core-server.service"
}

ServiceDefinition {
  type: "systemd-user"
  name: "truth-core-server"
  executable_path: "/usr/bin/truth-core-server"
  user_account: "$USER"  # Installing user
  restart_policy: "always"
  auto_start: true
}
```

### macOS (.pkg)
```
InstallationPackage {
  format: "PKG"
  platform: "macOS"
  executable_path: "/usr/local/bin/truth-core-server"
  service_path: "~/Library/LaunchAgents/com.truth.training.server.plist"
}

ServiceDefinition {
  type: "launchagent"
  name: "com.truth.training.server"
  executable_path: "/usr/local/bin/truth-core-server"
  user_account: "$USER"  # Installing user
  restart_policy: "always"  # KeepAlive=true
  auto_start: true  # RunAtLoad=true
}
```

### Windows (EXE)
```
InstallationPackage {
  format: "EXE"
  platform: "Windows"
  executable_path: "%PROGRAMFILES%\\TruthCoreServer\\truth-core-server.exe"
  service_path: "Windows Service Registry"  # Via WinSW
}

ServiceDefinition {
  type: "winsw-service"
  name: "TruthCoreServer"
  executable_path: "%BASE%\\truth-core-server.exe"
  user_account: "$USER"  # Installing user
  restart_policy: "always"  # Automatic start mode
  auto_start: true
}
```

## Naming Conventions

### Binary Naming
- **Development**: `truth_core_server` (underscores, matches Rust crate name)
- **Packaged**: `truth-core-server` (hyphens, matches package name)
- **Windows**: `truth-core-server.exe`

### Package Naming
- **DEB**: `truth-core-server_<version>_<arch>.deb`
- **RPM**: `truth-core-server-<version>.<arch>.rpm`
- **PKG**: `truth-core-server-macos.pkg`
- **EXE**: `truth-core-server-windows.exe`

### Service Naming
- **Linux**: `truth-core-server.service` (systemd user service)
- **macOS**: `com.truth.training.server.plist` (LaunchAgent)
- **Windows**: `TruthCoreServer` (Windows Service name)

## Migration Considerations

### Existing Installations
- Old naming: `truth_core_server` (underscores)
- New naming: `truth-core-server` (hyphens)
- Upgrade path: Package upgrade should handle binary rename automatically
- Service migration: Old service stopped, new service started (handled by postinst/prerm scripts)

### Configuration Preservation
- Existing config files in `/etc/truth-core/` preserved during upgrade
- Future config location: `/etc/truth-core-server/` (new installations)
- Migration script: Move config from old to new location if needed

_Version: v1.0.0_
