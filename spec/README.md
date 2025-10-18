# Truth Training – Cross-Platform Spec Kit v0.4.0

Purpose: A comprehensive guide for building cross-platform Truth Training applications. This Spec-Kit provides precise instructions for AI agents and developers to build mobile (iOS + Android) and desktop versions using platform-specific features and FFI interfaces.

## Overview

The `truth-training` repository contains a **cross-platform core library** (`truth_core`) that integrates with different UIs depending on the target platform:

- **Mobile targets** (iOS + Android): Use a minimal subset of the core with FFI interfaces
- **Desktop targets** (Linux, Windows, macOS): Retain the full feature set including HTTP server, CLI, and async runtime

The core library uses **Cargo features** to conditionally compile platform-specific code, ensuring optimal performance and minimal binary size for each target.

## Platform-Specific Features

### Desktop Features (`desktop`)
```toml
[features]
desktop = ["p2p-client-sync", "auth"]
```

**Included Dependencies:**
- `actix-web` - HTTP server framework
- `tokio` - Async runtime
- `reqwest` - HTTP client
- `clap` - CLI argument parsing
- `env_logger` - Logging
- `jsonwebtoken` - JWT authentication
- `utoipa` - OpenAPI documentation

**Capabilities:**
- Full HTTP REST API server
- CLI management tools (`truthctl`)
- Complete P2P synchronization
- Web-based administration interface
- Comprehensive logging and diagnostics

### Mobile Features (`mobile`)
```toml
[features]
mobile = []
```

**Excluded Dependencies:**
- ❌ `actix-web` - Not compiled on mobile
- ❌ `tokio` - Not compiled on mobile  
- ❌ `reqwest` - Not compiled on mobile
- ❌ `clap` - Not compiled on mobile
- ❌ `env_logger` - Not compiled on mobile

**Included Capabilities:**
- Minimal P2P protocol implementation
- Ed25519 cryptographic operations
- SQLite database access
- FFI interface for native mobile apps
- JSON signature verification (Android)

## Build Instructions

### Mobile Android
```bash
# Build Android shared library
cargo build --release --target aarch64-linux-android --features mobile

# Output: target/aarch64-linux-android/release/libtruth_core.so
```

**FFI Integration:**
- Generate JNI bindings for Kotlin/Java integration
- Use `src/android/mod.rs` for Android-specific functions
- Implement JSON signature verification for secure communication

### Mobile iOS
```bash
# Build iOS framework
cargo build --release --target aarch64-apple-ios --features mobile

# Output: target/aarch64-apple-ios/release/libtruth_core.a
```

**FFI Integration:**
- Generate Swift bindings using `cbindgen`
- Create `.framework` bundle for Xcode integration
- Implement minimal async runtime using `smol` or lightweight threading

### Desktop (All Platforms)
```bash
# Build desktop application with full features
cargo build --release --features desktop

# Output: target/release/truth_core (executable)
#         target/release/libtruth_core.so (shared library)
```

**Capabilities:**
- Full HTTP server on port 8080
- CLI tools (`truthctl`) for node management
- Complete P2P network functionality
- Web-based administration interface

## Source Code Guidelines

### Feature-Gated Modules

**Desktop-Only Modules:**
```rust
#[cfg(feature = "desktop")]
mod api;
#[cfg(feature = "desktop")]
mod server_diagnostics;
#[cfg(feature = "desktop")]
mod main;
```

**Mobile-Only Modules:**
```rust
#[cfg(feature = "mobile")]
mod android;
#[cfg(feature = "mobile")]
mod mobile_ffi;
```

**Shared Core Logic:**
```rust
// Always compiled - no feature gates
mod p2p;
mod expert;
mod sync;
```

### Conditional Compilation Examples

**HTTP Server (Desktop Only):**
```rust
#[cfg(feature = "desktop")]
pub async fn start_server(port: u16) -> Result<(), Box<dyn std::error::Error>> {
    use actix_web::{web, App, HttpServer};
    // Desktop-specific server implementation
}
```

**Mobile FFI Interface:**
```rust
#[cfg(feature = "mobile")]
#[no_mangle]
pub extern "C" fn truth_core_process_json(
    json_ptr: *const c_char,
    json_len: usize
) -> *mut c_char {
    // Mobile-specific FFI implementation
}
```

## Directory Structure

```
truth-core/
├── src/
│   ├── core/           # Shared core logic (always compiled)
│   │   ├── p2p/        # P2P protocol implementation
│   │   ├── expert/     # Expert system algorithms
│   │   ├── sync/       # Synchronization logic
│   │   └── models/     # Data models and schemas
│   ├── desktop/        # Desktop-only modules
│   │   ├── api.rs      # HTTP REST API
│   │   ├── cli.rs      # CLI command handling
│   │   └── server.rs   # HTTP server implementation
│   ├── mobile/         # Mobile-only modules
│   │   ├── android.rs  # Android JNI bindings
│   │   ├── ios.rs      # iOS FFI bindings
│   │   └── ffi.rs      # Cross-platform FFI interface
│   └── lib.rs          # Main library entry point
├── Cargo.toml          # Feature definitions
└── build.rs            # Build script for FFI generation
```

## Cross-Platform Async Guidance

### Desktop Async Runtime
- **Use Tokio**: Full-featured async runtime
- **Actix-web**: HTTP server framework
- **Reqwest**: HTTP client for external requests

```rust
#[cfg(feature = "desktop")]
use tokio::runtime::Runtime;
use actix_web::{web, App, HttpServer};
```

### Mobile Async Runtime
- **Use Smol**: Lightweight async runtime
- **Avoid Tokio**: Too heavy for mobile
- **Minimal Threading**: Use `std::thread` for simple tasks

```rust
#[cfg(feature = "mobile")]
use smol::Task;
use std::thread;

// Mobile-specific async implementation
```

### P2P Networking
- **Desktop**: Full UDP beacons + HTTP sync
- **Mobile**: Minimal UDP discovery + JSON over HTTP
- **Shared**: Ed25519 cryptographic operations

## FFI Generation Commands

### Android JNI Bindings
```bash
# Generate JNI headers
cbindgen --config cbindgen.toml --crate truth_core --output android/truth_core.h

# Build Android library
cargo ndk --target aarch64-linux-android --android-platform 29 build --release
```

### iOS Swift Bindings
```bash
# Generate Swift bindings
cbindgen --config cbindgen.toml --crate truth_core --output ios/truth_core.h

# Build iOS framework
cargo build --release --target aarch64-apple-ios
```

## Cargo.toml Feature Configuration

```toml
[features]
default = []
desktop = ["p2p-client-sync", "auth"]
mobile = []

# Desktop-only dependencies
[target.'cfg(feature = "desktop")'.dependencies]
actix-web = "4"
tokio = { version = "1.37", features = ["full"] }
reqwest = { version = "0.11", default-features = false }
clap = { version = "4", features = ["derive"] }
env_logger = "0.11"

# Mobile-only dependencies  
[target.'cfg(feature = "mobile")'.dependencies]
smol = "1.3"

# Shared dependencies (always included)
[dependencies]
core_lib = { path = "core" }
serde = { version = "1.0", features = ["derive"] }
rusqlite = { version = "0.31", features = ["bundled"] }
ed25519-dalek = "2.0"
```

## Testing Strategy

### Platform-Specific Tests
```bash
# Test desktop features
cargo test --features desktop

# Test mobile features  
cargo test --features mobile

# Test shared core logic
cargo test --no-default-features
```

### Cross-Platform Integration Tests
```bash
# Android integration tests
cargo test --target aarch64-linux-android --features mobile

# iOS integration tests
cargo test --target aarch64-apple-ios --features mobile
```

## Core Specifications

- **01 Product Vision**: `spec/01-product-vision.md` — Cross-platform decentralized truth verification
- **02 Requirements**: `spec/02-requirements.md` — Platform-specific functional requirements
- **03 Architecture**: `spec/03-architecture.md` — Cross-platform architecture with FFI interfaces
- **04 Data Model**: `spec/04-data-model.md` — Shared SQLite schema and models
- **05 HTTP API**: `spec/05-api.md` — Desktop-only REST endpoints
- **06 Expert System**: `spec/06-expert-system.md` — Shared truth assessment algorithms
- **07 Event Rating Protocol**: `spec/07-event-rating-protocol.md` — Cross-platform trust system
- **08 P2P & Sync**: `spec/08-p2p-sync.md` — Platform-adaptive peer synchronization
- **09 UX Guidelines**: `spec/09-ux-guidelines.md` — Platform-specific UI standards
- **10 CLI Specification**: `spec/10-cli.md` — Desktop-only CLI tools

## Cross-Platform Development

- **18 Cross-Platform Architecture**: `spec/18-cross-platform-architecture.md` — Platform-specific feature architecture
- **19 Build Instructions**: `spec/19-build-instructions.md` — Cross-platform build commands and CI/CD
- **20 Cargo Configuration**: `spec/20-cargo-configuration.md` — Complete Cargo.toml with feature definitions

## Integration Guides

- **Android Integration**: `integration/android/README_INTEGRATION.md` — JNI setup and JSON verification
- **iOS Integration**: `integration/ios/README_INTEGRATION.md` — Swift bindings and FFI setup  
- **Desktop Integration**: `integration/desktop/README_INTEGRATION.md` — HTTP API and CLI usage

## Version History

- **v0.4.0**: Cross-platform architecture with mobile/desktop feature separation
- **v0.3.0**: Android JSON signature verification and P2P stabilization
- **v0.2.x**: Legacy desktop-only implementation

## Quick Start Commands

```bash
# Desktop development
cargo build --features desktop
cargo run --features desktop -- --port 8080

# Android development  
cargo build --target aarch64-linux-android --features mobile

# iOS development
cargo build --target aarch64-apple-ios --features mobile
```

This Spec-Kit ensures consistent cross-platform development while maintaining optimal performance and minimal resource usage for each target platform.