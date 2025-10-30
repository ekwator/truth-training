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


