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

---

## Android Build Workflow

This section explains the Android client build process and artifacts produced by the GitHub Actions workflow defined in `.github/workflows/android-build.yml`.

### Key Repository Directories

- `truth-android-client/`
  - Android application project root
  - `app/` — Android app module
    - `build.gradle.kts` — App-level Gradle configuration
    - `src/main/java/` — Kotlin source code
    - `src/test/java/` — Unit tests
    - `src/androidTest/java/` — Instrumented tests
    - `src/main/jniLibs/` — Native libraries (Rust truth_core)
  - `gradle/` — Gradle wrapper and configuration

### Android Workflow Jobs Overview

The workflow `Android Build & Test` defines three jobs:

1) **`test`** (runs on Ubuntu)
   - Sets up JDK 17
   - Caches Gradle dependencies and build artifacts
   - Runs unit tests (`./gradlew test`)
   - Runs integration tests (`./gradlew connectedAndroidTest`)
   - Runs performance tests (`./gradlew connectedAndroidTest --tests "*performance*"`)
   - Uploads test results artifacts

2) **`build`** (runs on Ubuntu, matrix: Debug/Release)
   - Installs Rust toolchain (aarch64-linux-android, x86_64-linux-android)
   - Sets up Android SDK and NDK
   - Builds Rust libraries (`truth_core`) for Android targets
   - Copies native libraries to Android project
   - Sets up JDK 17
   - Caches Gradle dependencies
   - Builds Debug APK or Release AAB based on matrix
   - Uploads build artifacts

3) **`release`** (runs on Ubuntu, triggered by release event)
   - Downloads all build artifacts from `build` job
   - Creates GitHub Release with artifacts attached
   - Generates release notes automatically

### Android Artifact Names and Paths

Artifact names are unique per build type:

- **Debug APK** (from `build` job, `buildType: debug`):
  - Name: `android-debug-apk`
  - Paths uploaded:
    - `truth-android-client/app/build/outputs/apk/**/*.apk`
  - Retention: 30 days

- **Release AAB** (from `build` job, `buildType: release`):
  - Name: `android-release-aab`
  - Paths uploaded:
    - `truth-android-client/app/build/outputs/bundle/**/*.aab`
  - Retention: 30 days

- **Test Results** (from `test` job):
  - Name: `test-results`
  - Paths uploaded:
    - `truth-android-client/app/build/test-results/**/*`
    - `truth-android-client/app/build/outputs/androidTest-results/**/*`
  - Retention: 7 days

### Typical Build Outputs

After successful builds, expect the following files:

- **Debug APK**:
  - `truth-android-client/app/build/outputs/apk/debug/app-debug.apk`
  - Size: ~15-25 MB (includes debug symbols)

- **Release AAB**:
  - `truth-android-client/app/build/outputs/bundle/release/app-release.aab`
  - Size: ~10-15 MB (optimized, signed for Google Play)

### Version Configuration

- **versionName**: "1.0.0" (configured in `truth-android-client/app/build.gradle.kts`)
- **versionCode**: 1
- **minSdk**: 26 (Android 8.0)
- **targetSdk**: 33 (Android 13)
- **compileSdk**: 35

### Gradle Caching Strategy

The workflow caches:
- **Gradle dependencies**: `~/.gradle/caches`, `~/.gradle/wrapper`, `truth-android-client/.gradle`
- **Gradle build cache**: `truth-android-client/.gradle/caches`, `truth-android-client/.gradle/build`
- Cache keys based on Gradle wrapper version and build file hashes

### Native Library Build Process

1. Rust toolchain installed with Android targets:
   - `aarch64-linux-android` (ARM64)
   - `x86_64-linux-android` (x86_64)

2. Rust libraries built with `cargo build --release --target <target> --features mobile --lib -p truth_core`

3. Native libraries copied to:
   - `truth-android-client/app/src/main/jniLibs/arm64-v8a/libtruth_core.so`
   - `truth-android-client/app/src/main/jniLibs/x86_64/libtruth_core.so`

### Test Execution

- **Unit Tests**: JUnit tests for DAOs, Repositories, Sync infrastructure
- **Integration Tests**: Android instrumented tests for full user scenarios
- **Performance Tests**: Room database and UI response time benchmarks
- Tests run with `--continue` flag to report all failures

### Release Process

When a GitHub release is created:
1. `release` job triggers automatically
2. Downloads Debug APK and Release AAB artifacts
3. Creates GitHub Release with:
   - Tag name from release event
   - Auto-generated release notes
   - All Android artifacts attached
4. Artifacts available for download from GitHub Releases page

### Notes

- Performance tests may be skipped in CI if no device/emulator is available (uses `|| true`)
- Rust library build requires NDK toolchain setup
- AAB (Android App Bundle) is preferred for Google Play distribution
- APK is provided for direct installation/testing


