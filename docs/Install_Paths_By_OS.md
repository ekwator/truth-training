## Installation layout and runtime data paths by OS

This document summarizes where the Desktop UI installers place the app and where runtime files (configuration, database, knowledge base) are created.

Version baseline: Desktop UI v0.1.3

### Linux (Deb/AppImage)

- Installer outputs (CI):
  - Deb: `target/x86_64-unknown-linux-gnu/release/bundle/deb/*.deb`
  - AppImage: `target/x86_64-unknown-linux-gnu/release/bundle/appimage/*.AppImage`

- App install location (Deb):
  - Executable and resources under system paths managed by dpkg (e.g., `/usr/bin`, `/usr/lib/...`), depending on distribution conventions.

- Runtime data created by the app:
  - Configuration file: `~/.truth-training/config.json`
  - Knowledge base override (optional): `~/.truth-training/Data_Schema.md`
  - SQLite database: `${XDG_DATA_HOME:-~/.local/share}/TruthTraining/truth_training.sqlite`

### macOS (.app, .dmg)

- Installer outputs (CI):
  - App bundle: `target/x86_64-apple-darwin/release/bundle/macos/Truth Training.app`
  - Disk image: `target/x86_64-apple-darwin/release/bundle/dmg/*.dmg`

- App install location:
  - Drag-and-drop into `/Applications` (typical), the app bundle is self-contained.

- Runtime data created by the app:
  - Configuration file: `~/.truth-training/config.json`
  - Knowledge base override (optional): `~/.truth-training/Data_Schema.md`
  - SQLite database: `~/Library/Application Support/TruthTraining/truth_training.sqlite`

### Windows (NSIS .exe, MSI .msi)

- Installer outputs (CI):
  - Windows cross (Ubuntu runner): `target/x86_64-pc-windows-gnu/release/bundle/nsis/*.exe`
  - Windows native (Windows runner):
    - NSIS: `target/release/bundle/nsis/*.exe`
    - MSI: `target/release/bundle/msi/*.msi`

- App install location:
  - Typically under `C:\Program Files\Truth Training\` (exact path depends on installer and architecture).

- Runtime data created by the app:
  - Configuration file: `%USERPROFILE%\.truth-training\config.json`
  - Knowledge base override (optional): `%USERPROFILE%\.truth-training\Data_Schema.md`
  - SQLite database: `%APPDATA%\TruthTraining\truth_training.sqlite`

### Resolution notes and sources

- Config path (all OS): defined in `ui/desktop/src-tauri/src/commands/config.rs` via `~/.truth-training/config.json`.
- Knowledge base resolution order: `~/.truth-training/Data_Schema.md`, then dev fallback, then built-in defaults. See `ui/desktop/src-tauri/src/commands/knowledge_base.rs`.
- SQLite path: created using `directories::ProjectDirs` with qualifier `("com", "truth-training", "TruthTraining")`, then `data_dir()/truth_training.sqlite`. See `ui/desktop/src-tauri/src/storage.rs`.


