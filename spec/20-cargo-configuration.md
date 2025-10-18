# Cross-Platform Cargo.toml Configuration
Version: v0.4.0
Updated: 2025-01-18
Spec ID: 20

## Overview

This document provides the complete Cargo.toml configuration for Truth Training's cross-platform architecture, including feature definitions, conditional dependencies, and platform-specific optimizations.

## Complete Cargo.toml

```toml
[package]
name = "truth_core"
version = "0.4.0"
description = "Cross-platform Truth Training core library with desktop and mobile support"
edition = "2021"
authors = ["Truth Training Team"]
license = "MIT"
repository = "https://github.com/truth-training/truth-core"
keywords = ["p2p", "truth", "verification", "distributed", "consensus"]
categories = ["network-programming", "cryptography", "database"]

[lib]
name = "truth_core"
path = "src/lib.rs"
crate-type = ["cdylib", "rlib"]

[workspace]
members = ["app", "core", "ui-tui", "ui-web"]
resolver = "2"

[features]
default = []
desktop = ["p2p-client-sync", "auth", "http-server", "cli", "web-ui"]
mobile = ["minimal-p2p", "ffi", "mobile-crypto"]

# Desktop-specific features
p2p-client-sync = []
auth = []
http-server = []
cli = []
web-ui = []

# Mobile-specific features
minimal-p2p = []
ffi = []
mobile-crypto = []

# Optional features
logging = []
metrics = []
testing = []

[dependencies]
# Core shared dependencies (always included)
core_lib = { version = "0.4.0", path = "core" }
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"
rusqlite = { version = "0.31", features = ["bundled"] }
uuid = { version = "1.8", features = ["v4", "serde"] }
chrono = { version = "0.4", features = ["serde"] }
once_cell = "1.19"
anyhow = "1.0"
rand = "0.8"
ed25519-dalek = { version = "2.0", features = ["rand_core"] }
hex = "0.4"
log = "0.4"
base64ct = "=1.7.3"
sha2 = "0.10"
thiserror = "1.0"
base64 = "0.22"

# Desktop-only dependencies
[target.'cfg(feature = "desktop")'.dependencies]
actix-web = "4"
actix-cors = "0.6"
tokio = { version = "1.37", features = ["full"] }
reqwest = { version = "0.11", default-features = false, features = ["json", "rustls-tls"] }
clap = { version = "4", features = ["derive"] }
get_if_addrs = "0.5"
r2d2 = "0.8"
r2d2_sqlite = "0.24.0"
env_logger = "0.11"
jsonwebtoken = "9"
utoipa = { version = "4", features = ["actix_extras"] }
utoipa-swagger-ui = { version = "5", features = ["actix-web"] }

# Mobile-only dependencies
[target.'cfg(feature = "mobile")'.dependencies]
smol = "1.3"
jni = "0.21"

# Android-specific dependencies
[target.'cfg(target_os = "android")'.dependencies]
getrandom = { version = "0.2", features = ["std"] }

# iOS-specific dependencies
[target.'cfg(target_os = "ios")'.dependencies]
objc = "0.2"

# Optional feature dependencies
[target.'cfg(feature = "logging")'.dependencies]
tracing = "0.1"
tracing-subscriber = "0.3"

[target.'cfg(feature = "metrics")'.dependencies]
prometheus = "0.13"

[target.'cfg(feature = "testing")'.dependencies]
tokio-test = "0.4"
mockall = "0.12"

# Development dependencies
[dev-dependencies]
criterion = "0.5"
proptest = "1.4"
```

## Feature Configuration Details

### Desktop Features

**`desktop`** - Main desktop feature flag
- Enables full HTTP server, CLI tools, and web UI
- Includes all P2P client synchronization features
- Requires authentication and authorization

**`p2p-client-sync`** - P2P synchronization
- Bidirectional peer synchronization
- Conflict resolution algorithms
- Trust propagation mechanisms

**`auth`** - Authentication and authorization
- JWT token management
- Role-based access control (RBAC)
- User management and permissions

**`http-server`** - HTTP REST API server
- Actix-web based HTTP server
- RESTful API endpoints
- CORS configuration
- OpenAPI documentation

**`cli`** - Command-line interface
- truthctl administrative CLI
- Peer management commands
- Node configuration tools
- Diagnostics and health checks

**`web-ui`** - Web-based user interface
- Swagger UI for API documentation
- Administrative web interface
- Real-time network visualization

### Mobile Features

**`mobile`** - Main mobile feature flag
- Enables minimal P2P protocol
- Includes FFI interfaces for native apps
- Mobile-optimized cryptographic operations

**`minimal-p2p`** - Minimal P2P implementation
- Basic peer discovery
- Essential synchronization
- Battery-efficient networking

**`ffi`** - Foreign Function Interface
- JNI bindings for Android
- C FFI bindings for iOS
- Cross-platform FFI utilities

**`mobile-crypto`** - Mobile cryptographic operations
- Ed25519 signature verification
- JSON message signing
- Secure key management

## Platform-Specific Dependencies

### Desktop Platforms
```toml
# Linux, Windows, macOS
[target.'cfg(not(any(target_os = "android", target_os = "ios")))'.dependencies]
actix-web = "4"
tokio = { version = "1.37", features = ["full"] }
reqwest = "0.11"
clap = "4"
env_logger = "0.11"
```

### Android Platform
```toml
[target.'cfg(target_os = "android")'.dependencies]
jni = "0.21"
getrandom = { version = "0.2", features = ["std"] }
smol = "1.3"
```

### iOS Platform
```toml
[target.'cfg(target_os = "ios")'.dependencies]
objc = "0.2"
smol = "1.3"
```

## Conditional Compilation Examples

### Desktop-Only Modules
```rust
// src/desktop/mod.rs
#[cfg(feature = "desktop")]
pub mod api;

#[cfg(feature = "desktop")]
pub mod cli;

#[cfg(feature = "desktop")]
pub mod server;
```

### Mobile-Only Modules
```rust
// src/mobile/mod.rs
#[cfg(feature = "mobile")]
pub mod android;

#[cfg(feature = "mobile")]
pub mod ios;

#[cfg(feature = "mobile")]
pub mod ffi;
```

### Shared Core Modules
```rust
// src/core/mod.rs
// Always compiled - no feature gates
pub mod p2p;
pub mod expert;
pub mod sync;
pub mod models;
pub mod storage;
```

## Build Profiles

### Release Profile
```toml
[profile.release]
opt-level = 3
lto = true
codegen-units = 1
panic = "abort"
strip = true
```

### Mobile Release Profile
```toml
[profile.mobile-release]
inherits = "release"
opt-level = "z"  # Optimize for size
lto = "fat"
codegen-units = 1
```

### Development Profile
```toml
[profile.dev]
opt-level = 0
debug = true
overflow-checks = true
```

## Testing Configuration

### Unit Tests
```toml
[target.'cfg(feature = "testing")'.dependencies]
tokio-test = "0.4"
mockall = "0.12"
proptest = "1.4"
```

### Integration Tests
```toml
[dev-dependencies]
criterion = "0.5"
tempfile = "3.8"
```

## Cross-Compilation Targets

### Android Targets
```bash
# Add Android targets
rustup target add aarch64-linux-android
rustup target add x86_64-linux-android
rustup target add armv7-linux-androideabi
```

### iOS Targets
```bash
# Add iOS targets
rustup target add aarch64-apple-ios
rustup target add aarch64-apple-ios-sim
rustup target add x86_64-apple-ios
```

### Desktop Targets
```bash
# Add desktop targets
rustup target add x86_64-pc-windows-gnu
rustup target add x86_64-apple-darwin
rustup target add x86_64-unknown-linux-gnu
```

## Build Script Configuration

### build.rs
```rust
fn main() {
    // Generate FFI bindings
    if cfg!(feature = "mobile") {
        println!("cargo:rerun-if-changed=src/mobile/");
        println!("cargo:rerun-if-changed=cbindgen.toml");
    }
    
    // Platform-specific build configuration
    if cfg!(target_os = "android") {
        println!("cargo:rustc-link-lib=log");
    }
    
    if cfg!(target_os = "ios") {
        println!("cargo:rustc-link-lib=framework=Foundation");
    }
}
```

### cbindgen.toml
```toml
language = "C"
header = "/* Auto-generated FFI bindings for Truth Core */"
include_version = true
namespace = "truth_core"
autogen_warning = "/* Warning: auto-generated file, do not edit manually */"

[export]
include = ["truth_core_process_json", "truth_core_verify_signature"]
exclude = ["*"]

[export.rename]
"truth_core_process_json" = "process_json"
"truth_core_verify_signature" = "verify_signature"
```

## Performance Optimization

### Desktop Optimization
```toml
[profile.desktop-release]
inherits = "release"
opt-level = 3
lto = true
codegen-units = 1
```

### Mobile Optimization
```toml
[profile.mobile-release]
inherits = "release"
opt-level = "z"
lto = "fat"
codegen-units = 1
strip = true
```

This configuration ensures optimal performance and minimal resource usage for each target platform while maintaining code reusability and consistency across the Truth Training ecosystem.
