⚙️ Version Reference: See spec/README.md or docs/VERSION_REGISTRY.md for current version map.

## CI Workflows: Directory Structure and Artifacts

This document explains the relevant directory structure for the Desktop UI builds and the artifact files produced by the GitHub Actions workflows defined in `.github/workflows/desktop.yml`.

### Key Repository Directories

- `ui/desktop/`
  - Desktop UI (web + Tauri project root)
  - `package.json` — Desktop UI npm project
  - `src/` — React/TypeScript source
  - `src-tauri/` — Tauri Rust backend and config
    - `Cargo.toml` — Tauri Rust crate manifest
    - `src/` — Rust sources (commands, main)
    - `tauri.conf.json` — Tauri app configuration (bundle targets, icons)
    - `icons/` — App icons used for bundling
      - `icon.png` — Source PNG icon
      - `icon.ico` — ICO icon (generated in CI when missing)
- `docs/`
  - Project documentation (this file lives here)
- `target/` (created by builds)
  - Cargo/Tauri output directory; contains platform-specific bundles

### Workflow Jobs Overview

The workflow `Desktop CI` defines three jobs:

1) `build-test-linux-windows` (runs on Ubuntu)
- Cross-compiles Linux and Windows (GNU) targets
- Generates Windows `.exe` (NSIS) only (MSI is ignored during cross-compilation)
- Uploads Linux and Windows-cross artifacts

2) `build-test-macos` (runs on macOS)
- Builds macOS `.app` and `.dmg`

3) `build-windows-native` (runs on Windows)
- Builds Windows NSIS (`.exe`) and MSI (`.msi`) installers natively
- Uploads both installers

### Artifact Names and Paths

Artifact names are unique per job to avoid name collisions.

- Linux artifacts (from `build-test-linux-windows`):
  - Name: `truth_training-linux`
  - Paths uploaded:
    - `target/x86_64-unknown-linux-gnu/release/bundle/deb/*.deb`
    - `target/x86_64-unknown-linux-gnu/release/bundle/appimage/*.AppImage`

- Windows cross artifacts (from `build-test-linux-windows`):
  - Name: `truth_training-windows-cross`
  - Paths uploaded:
    - `target/x86_64-pc-windows-gnu/release/bundle/nsis/*.exe`

- macOS artifacts (from `build-test-macos`):
  - Name: `truth_training-macos`
  - Paths uploaded:
    - `target/x86_64-apple-darwin/release/bundle/dmg/*.dmg`
    - `target/x86_64-apple-darwin/release/bundle/macos/*.app`

- Windows native artifacts (from `build-windows-native`):
  - Name: `truth_training-windows-native`
  - Paths uploaded:
    - `target/release/bundle/nsis/*.exe`
    - `target/release/bundle/msi/*.msi`

### Icon Preparation in CI

The Tauri bundler relies on icons listed in `ui/desktop/src-tauri/tauri.conf.json`.

- On Ubuntu and macOS runners, the workflow installs ImageMagick and generates `icons/icon.ico` from `icons/icon.png` if missing.
- On Windows runners, ImageMagick is installed via Chocolatey and `magick` is used to generate `icon.ico` when needed.
- `tauri.conf.json` includes `icons/icon.ico` in the bundle icon list to satisfy MSI packaging requirements on Windows.

### Typical Bundle Outputs

After successful builds, expect the following files under `target/.../bundle/...`:

- Linux:
  - `.deb` at `target/x86_64-unknown-linux-gnu/release/bundle/deb/Truth Training_<version>_amd64.deb`
  - `.AppImage` at `target/x86_64-unknown-linux-gnu/release/bundle/appimage/Truth Training_<version>_x86_64.AppImage`

- macOS:
  - `.app` at `target/x86_64-apple-darwin/release/bundle/macos/Truth Training.app`
  - `.dmg` at `target/x86_64-apple-darwin/release/bundle/dmg/Truth Training_<version>_x64.dmg`

- Windows (cross, Ubuntu):
  - NSIS `.exe` at `target/x86_64-pc-windows-gnu/release/bundle/nsis/Truth Training_<version>_x64-setup.exe`

- Windows (native):
  - NSIS `.exe` at `target/release/bundle/nsis/Truth Training_<version>_x64-setup.exe`
  - MSI `.msi` at `target/release/bundle/msi/Truth Training_<version>_x64_en-US.msi`

Note: Exact filenames can vary slightly based on Tauri bundler conventions and version.

### Notes on Tests and Linting

- `npm test` runs unit and integration tests under `ui/desktop/`
- Some jobs enable `continue-on-error` for tests where non-critical failures should not block packaging

### Release Tagging

- Tags like `v0.4.2` are created and pushed to mark release points
- Releases can be created via GitHub CLI (`gh release create`) and can attach the produced artifacts


