<!-- Archived from [docs/Install_Paths_By_OS.md](docs/Install_Paths_By_OS.md) -->

⚙️ Version Reference: See [spec/README.md](spec/README.md) or [docs/VERSION_REGISTRY.md](docs/VERSION_REGISTRY.md) for current version map.

## Installation Layout and Runtime Data Paths by OS

This document summarizes where the various Truth Training executables (Desktop UI, Server, CLI) are built, and where they look for configuration files and databases on each supported operating system. It includes build artifact names for each GitHub Actions workflow.

### Artifact Types and Build Locations (produced by CI)

#### Desktop UI (Tauri)
- Bundled as system installers:
  - Linux: `.deb`, `.AppImage` (see paths below)
  - macOS: `.app`, `.dmg`
  - Windows: `.exe` (NSIS), `.msi`
- Artifacts appear as:
  - `truth_training-linux` (AppImage/.deb)
  - `truth_training-macos` (.app/.dmg)
  - `truth_training-windows-*` (.exe/.msi)

#### Server (Standalone application; for running as service)
- Built binaries:
  - Linux: `target/x86_64-unknown-linux-gnu/release/truth_core_server`
  - macOS: `target/x86_64-apple-darwin/release/truth_core_server`
  - Windows: `target/release/truth_core_server.exe`
- Service installer artifacts (from server-package.yml workflow):
  - Linux: `.deb`/`.rpm` with systemd unit
  - macOS: `.pkg` with LaunchDaemon plist
  - Windows: `.exe` installer using NSIS & WinSW
- Artifacts:
  - `truth_core_server-linux-bin`, `truth_core_server-macos-bin`, `truth_core_server-windows-bin`, etc.
  - Server-package workflow: `truth-core-server-linux`, `truth-core-server-macos`, `truth-core-server-windows`

#### CLI Tools
- Example: `truthctl`
- Output binary appears as:
  - `target/release/truthctl` (Linux/macOS)
  - `target/release/truthctl.exe` (Windows)

---

### Where Each App Stores Config and Data Files

Regardless of how the app is installed (from package, workflow artifact, or manual build), **configuration and database files are created in the user's home directory** unless CLI arguments specify otherwise.

**Desktop UI and Server (Tauri, truth_core_server):**
- Configuration file:  `~/.truth-training/config.json` (Linux/macOS), `%USERPROFILE%\.truth-training\config.json` (Windows)
- SQLite database:     `${XDG_DATA_HOME:-~/.local/share}/TruthTraining/truth_training.sqlite` (Linux), `~/Library/Application Support/TruthTraining/truth_training.sqlite` (macOS), `%APPDATA%\TruthTraining\truth_training.sqlite` (Windows)
- Knowledge base override (optional):
  - [~/.truth-training/Data_Schema.md](~/.truth-training/Data_Schema.md) (Linux/macOS)
  - `%USERPROFILE%\.truth-training\[Data_Schema.md](Data_Schema.md)` (Windows)

**CLI Tools (e.g., truthctl):**
- By default, the CLI stores config and DB in the current working directory unless you provide `--db` and `--peers` arguments, but may also use paths like:
  - Config JSON: `~/.truthctl/config.json` (see app/src/config_utils.rs)
  - Database: Default is `truth_db.sqlite` in CWD, override with `--db path/to/file.sqlite`

---

### Local/Manual Builds
- All executables and artifacts will appear in the `target/release/` subdirectory of your workspace.
- You can run UI, server, and CLI binaries directly; they will use the above config/database locations, based on the current user's home.
- Running with different users or arguments allows running isolated environments on the same system.

---

### Platform Table: Where artifacts go and where data is stored

| Platform      | UI Installer              | Server Artifact/Installer              | CLI Binary           | Config File Location                        | DB File Location                                                          |
|--------------|---------------------------|----------------------------------------|----------------------|---------------------------------------------|---------------------------------------------------------------------------|
| Linux        | `.deb`, `.AppImage`       | `.deb`, `.rpm`, or plain binary        | `truthctl`           | `~/.truth-training/config.json`, `~/.truthctl/config.json` | `${XDG_DATA_HOME:-~/.local/share}/TruthTraining/truth_training.sqlite`     |
| macOS        | `.app`, `.dmg`            | `.pkg` or plain binary                 | `truthctl`           | `~/.truth-training/config.json`, `~/.truthctl/config.json` | `~/Library/Application Support/TruthTraining/truth_training.sqlite`         |
| Windows      | `.exe` (NSIS), `.msi`     | `.exe` (NSIS+WinSW) or plain binary    | `truthctl.exe`       | `%USERPROFILE%\.truth-training\config.json`, `%USERPROFILE%\.truthctl\config.json` | `%APPDATA%\TruthTraining\truth_training.sqlite`                |

---

### Additional Notes
- You can use the same repo and working directory to build/run all three (UI, Server, CLI) from source on any platform; all outputs are placed under `target/release`, and config is written per user.
- Service installers (from server-package.yml) register truth_core_server to auto-start as a background process, storing all data/configs in the same user scope as described above.
- The Desktop UI and Server share config layout by default; the CLI may use custom locations depending on invocation.

_Version: v1.0.0_

