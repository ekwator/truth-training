# Build Instructions for Truth Training Platform

---

## ✅ 1. Core (Rust Library + REST API)
**Description**: The core module provides the fundamental functionality of the Truth Training platform including data models, business logic, networking, and API services. This component can be built as a standalone server application with desktop features enabled.

### **Requirements:**

* Rust (latest stable)
* Cargo
* SQLite3

### **Build & Run:**

```bash
# Clone repository
git clone https://github.com/ekwator/truth-training.git
cd truth-training

# Build server binary with desktop features
cargo build --release --bin truth_core_server --features desktop

# Run with default settings (uses truth_training.sqlite and discovery_nodes.sqlite)
target/release/truth_core_server --port 8080
```

**Run Tests:**

```bash
# Test with desktop features
cargo test --features desktop --lib --tests
```

---

## ✅ 2. CLI (Command Line Interface)
**Description**: The Command Line Interface (CLI) tool provides administrative and operational capabilities for the Truth Training platform. The `truthctl` utility enables node management, diagnostics, configuration, and monitoring functions. Built with the p2p-client-sync feature, it supports peer-to-peer synchronization operations.

### **Requirements:**

* Rust (latest stable)
* Cargo

### **Build & Run:**

```bash
# Build CLI tool with p2p client sync feature
cargo build --release -p app --bin truthctl --features p2p-client-sync

# Run CLI tool
target/release/truthctl --help
```

**Supported Platforms:**
- Linux (x86_64)
- Windows (x86_64)
- macOS (x86_64)

---

## ✅ 3. Desktop UI (Tauri)
**Description**: The desktop UI provides a cross-platform native application experience using Tauri framework. It bundles the web frontend with a Rust backend to create lightweight, secure desktop applications. The application supports bundling for Linux (.deb, .AppImage), Windows (.exe, .msi), and macOS (.dmg, .app) platforms with appropriate installers.

### **Requirements:**

* Rust
* Node.js + npm
* Tauri CLI: `npm install -g @tauri-apps/cli@2.9.0`
* System dependencies (Linux):
  ```bash
  sudo apt-get install -y libgtk-3-dev libwebkit2gtk-4.1-dev libappindicator3-dev librsvg2-dev patchelf
  sudo apt-get install -y gcc-mingw-w64-x86-64 nsis
  ```

### **Setup & Build:**

```bash
# Navigate to desktop UI directory
cd ui/desktop

# Install Node.js dependencies
npm install

# Build web assets
npm run build

# Build Tauri app for current platform
tauri build

# Build for specific target (cross-compilation)
tauri build --target x86_64-unknown-linux-gnu
tauri build --target x86_64-pc-windows-gnu
tauri build --target x86_64-apple-darwin
```

**Build Artifacts:**
- Linux: `.deb`, `.AppImage` in `target/x86_64-unknown-linux-gnu/release/bundle/`
- Windows: `.exe`, `.msi` in `target/x86_64-pc-windows-gnu/release/bundle/`
- macOS: `.dmg`, `.app` in `target/x86_64-apple-darwin/release/bundle/`

---

## ✅ 4. Android UI (Native)
**Description**: The Android UI provides a native mobile application experience built with Kotlin and integrated with Rust core functionality via JNI. The application supports multiple build flavors (local, mock, remote) to accommodate different deployment scenarios and environments. The build process compiles Rust libraries for ARM64 and x86_64 architectures and integrates them into the Android application.

### **Requirements:**

* Android Studio (latest)
* Rust toolchain for Android:
  ```bash
  rustup target add aarch64-linux-android x86_64-linux-android
  ```

* Android NDK (version 26.1.10909125)
* Java Development Kit (JDK 17)

### **Build Process:**

```bash
# Build Rust core for Android (mobile feature)
cargo build --release --target aarch64-linux-android --features mobile --lib -p truth_core
cargo build --release --target x86_64-linux-android --features mobile --lib -p truth_core

# Copy libraries to Android project
mkdir -p truth-android-client/app/src/main/jniLibs/arm64-v8a/
mkdir -p truth-android-client/app/src/main/jniLibs/x86_64/
cp target/aarch64-linux-android/release/libtruth_core.so truth-android-client/app/src/main/jniLibs/arm64-v8a/
cp target/x86_64-linux-android/release/libtruth_core.so truth-android-client/app/src/main/jniLibs/x86_64/

# Build Android app
cd truth-android-client
./gradlew assembleRelease
```

**Build Variants:**
- Local flavor: `assembleLocalRelease`
- Mock flavor: `assembleMockRelease`
- Remote flavor: `assembleRemoteRelease`

**Output**: APK files in `app/build/outputs/apk/{flavor}/release/`

---
## ✅ 5. iOS UI (Native)
**Description**: The iOS UI provides a native mobile application experience for iPhone and iPad devices. Built with Swift and integrated with Rust core functionality, the application compiles Rust libraries for ARM64 architecture (device) and simulator targets. The build process prepares static libraries that can be integrated into Xcode projects for final compilation and packaging.

### **Requirements:**

* Xcode
* Rust toolchain for iOS:
  ```bash
  rustup target add aarch64-apple-ios aarch64-apple-ios-sim
  ```

### **Build Process:**

```bash
# Build Rust core for iOS (mobile feature)
cargo build --release --target aarch64-apple-ios --features mobile --lib -p truth_core
cargo build --release --target aarch64-apple-ios-sim --features mobile --lib -p truth_core

# Copy libraries to iOS project
cp target/aarch64-apple-ios/release/libtruth_core.a truth-ios-client/TruthTraining/
```

**Note**: iOS builds require Xcode for final compilation and packaging.

---
## ✅ 6. Server Packages
**Description**: Server packages provide installation-ready distributions of the Truth Core Server for various operating systems. These packages include proper service configuration, dependencies, and installation scripts to deploy the server in production environments. Multiple package formats are supported to accommodate different Linux distributions and platforms.

### **Linux Server Package (.deb)**

```bash
# Build server binary
cargo build --release --bin truth_core_server --features desktop

# Build .deb package
make -C packaging/debian deb
```

### **Cross-Platform Server Packages**

For building server packages for multiple platforms:

```bash
# Build server with desktop features
cargo build --release --bin truth_core_server --features desktop

# Linux packages (.deb, .rpm)
# Requires fpm: sudo gem install fpm
fpm -s dir -t deb -n truth-core-server -v 1.0.0 [files...]
fpm -s dir -t rpm -n truth-core-server -v 1.0.0 [files...]

# Windows installer (via NSIS)
# Requires NSIS and WinSW
# 1. Install NSIS (e.g., via Chocolatey: choco install nsis)
# 2. Download WinSW from https://github.com/winsw/winsw/releases/
# 3. Create installer.nsi with the NSIS script
# 4. Run makensis installer.nsi to create the installer

# Example PowerShell script to create Windows installer:
# $nsisScript = @'
# !include "MUI2.nsh"
# Name "Truth Core Server"
# OutFile "truth-core-server-windows.exe"
# InstallDir "$PROGRAMFILES\\TruthCoreServer"
# Page directory
# Page instfiles
# Section "Install"
#   SetOutPath "$INSTDIR"
#   File "target\\release\\truth_core_server.exe"
#   File "winsw.exe"
#   File "truth_core_server.xml"
#   Rename "$INSTDIR\\winsw.exe" "$INSTDIR\\TruthCoreServer.exe"
#   nsExec::ExecToStack '"$INSTDIR\\TruthCoreServer.exe" install'
#   nsExec::ExecToStack '"$INSTDIR\\TruthCoreServer.exe" start'
# SectionEnd
# '@
# Set-Content -Path installer.nsi -Value $nsisScript
# & "makensis" "installer.nsi"

# macOS package (.pkg)
pkgbuild --root ./payload --install-location / truth-core-server-macos.pkg
```

---

## ⚠️ Common Compilation Issues & Solutions

### Desktop UI (Tauri) Issues

#### 1. Missing Tauri CLI
**Error**: `error: no such command: tauri`
**Solution**:
```bash
npm install -g @tauri-apps/cli@2.9.0
```

#### 2. Missing System Dependencies (Linux)
**Error**: `The system library glib-2.0 required by crate glib-sys was not found`
**Solution**:
```bash
sudo apt-get update
sudo apt-get install -y libgtk-3-dev libwebkit2gtk-4.1-dev libappindicator3-dev librsvg2-dev patchelf
```

#### 3. Missing Windows Cross-Compilation Tools
**Error**: `Error calling dlltool 'x86_64-w64-mingw32-dlltool': No such file or directory`
**Solution**:
```bash
sudo apt-get install -y gcc-mingw-w64-x86-64 binutils-mingw-w64-x86-64
export CC_x86_64_pc_windows_gnu=x86_64-w64-mingw32-gcc
export CXX_x86_64_pc_windows_gnu=x86_64-w64-mingw32-g++
export AR_x86_64_pc_windows_gnu=x86_64-w64-mingw32-ar
export CARGO_TARGET_X86_64_PC_WINDOWS_GNU_LINKER=x86_64-w64-mingw32-gcc
export MAKENSIS_PATH=/usr/bin/makensis
```

#### 4. Icon File Issues
**Error**: `failed to open icon .../icons/32x32.png: No such file or directory`
**Error**: `icon .../icons/icon.png is not RGBA`
**Error**: `couldn't find a square icon to use as AppImage icon`
**Error**: `icons/icon.ico not found; required for generating a Windows Resource file`

**Root Cause**: Tauri requires specific icon formats and sizes for different platforms.

**Solution**: Ensure proper icon files exist in `ui/desktop/src-tauri/icons/`:
```bash
# Required icon files:
# - icon.png (32x32 RGBA PNG)
# - icon-512.png (512x512 RGBA PNG)
# - icon.ico (256x256 ICO for Windows)

# Generate ICO from PNG (if missing):
sudo apt-get install -y imagemagick
convert icons/icon.png -resize 256x256 icons/icon.ico
```

**Icon Requirements**:
- **Linux**: `icon.png` (32x32) + `icon-512.png` (512x512) for AppImage
- **Windows**: `icon.ico` (256x256) for MSI/NSIS installers
- **macOS**: `icon.png` (32x32) + `icon-512.png` (512x512) for DMG
- **Format**: All must be RGBA PNG (not RGB) for transparency support

### Android Build Issues

#### 1. Missing Android NDK
**Error**: `failed to find tool "aarch64-linux-android29-clang": No such file or directory`
**Solution**:
```bash
# Install Android NDK
export ANDROID_NDK_HOME=/path/to/android-ndk
export PATH=$PATH:$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin

# Add Rust targets
rustup target add aarch64-linux-android
rustup target add x86_64-linux-android
```

#### 2. APK Output Path Issues
**Issue**: APK files created in subdirectories (`mock/`, `local/`, `remote/`) instead of `debug/` and `release/`
**Solution**: Check correct output paths:
```bash
# APK files are located at:
truth-android-client/app/build/outputs/apk/mock/release/
truth-android-client/app/build/outputs/apk/local/release/
truth-android-client/app/build/outputs/apk/remote/release/
```

### iOS Build Issues

#### 1. Missing iOS Targets
**Error**: `Target aarch64-apple-ios is not installed`
**Solution**:
```bash
rustup target add aarch64-apple-ios
rustup target add aarch64-apple-ios-sim
```

#### 2. Xcode Not Found
**Error**: `xcodebuild: command not found`
**Solution**:
```bash
# Install Xcode Command Line Tools
xcode-select --install
```

### Cross-Compilation Issues

#### 1. Missing Rust Targets
**Error**: `Target x86_64-pc-windows-gnu is not installed`
**Solution**:
```bash
rustup target add x86_64-unknown-linux-gnu
rustup target add x86_64-pc-windows-gnu
rustup target add x86_64-apple-darwin
```

#### 2. Feature Compilation Errors
**Error**: Using incorrect feature flags
**Solution**: Use correct feature flags as per workflow files:
- For desktop builds: `--features desktop`
- For mobile builds: `--features mobile`
- For CLI builds: `--features p2p-client-sync`

### Build Artifact Issues

#### 1. Wrong Output Paths
**Issue**: Artifacts not found in expected locations
**Solution**: Check correct paths:
- **Desktop**: `target/{target}/release/bundle/`
- **Android**: `truth-android-client/app/build/outputs/apk/{flavor}/{build_type}/`
- **iOS**: `truth-ios-client/build/Products/Release-{platform}/`
- **Server packages**: `packaging/` directory

## ✅ Notes:

* All UIs communicate with the core via **HTTP API** or **Direct FFI/JNI**.
* For testing UI independently, you can run the core as a **local HTTP service** and point UI to `http://127.0.0.1:8080`.
* **Database files**: The system uses `truth_training.sqlite` for main data and `discovery_nodes.sqlite` for the Discovery System by default.
* **Icon files are required** for Tauri builds - ensure `ui/desktop/src-tauri/icons/` contains proper PNG/ICO files.
* **Cross-compilation requires platform-specific tools** - install MinGW for Windows, NDK for Android, Xcode for iOS.
* **Feature flags** are essential: use `desktop` for desktop builds, `mobile` for mobile builds, and `p2p-client-sync` for CLI.

_Version: v1.0.0_

