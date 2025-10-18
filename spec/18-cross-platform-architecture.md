# Cross-Platform Architecture Specification
Version: v0.4.0
Updated: 2025-01-18
Spec ID: 18

## Overview

This specification defines the cross-platform architecture for Truth Training, enabling the core library (`truth-core`) to run on mobile (iOS + Android) and desktop platforms with platform-specific optimizations and feature sets.

## Platform Strategy

### Desktop Platforms (Linux, Windows, macOS)
- **Full Feature Set**: HTTP server, CLI tools, complete P2P networking
- **Async Runtime**: Tokio with full features
- **Dependencies**: Actix-web, Reqwest, Clap, Env_logger
- **Deployment**: Standalone executable or shared library

### Mobile Platforms (iOS, Android)
- **Minimal Feature Set**: Core P2P protocol, cryptographic operations, FFI interface
- **Async Runtime**: Smol or lightweight threading
- **Dependencies**: Minimal set, no HTTP server or CLI
- **Deployment**: FFI bindings (.so for Android, .framework for iOS)

## Feature Architecture

### Cargo Features Definition

```toml
[features]
default = []
desktop = ["p2p-client-sync", "auth", "http-server", "cli"]
mobile = ["minimal-p2p", "ffi"]

# Desktop-specific features
http-server = []
cli = []
auth = []

# Mobile-specific features  
minimal-p2p = []
ffi = []
```

### Conditional Compilation Strategy

**Desktop-Only Modules:**
```rust
#[cfg(feature = "desktop")]
mod api;              // HTTP REST API
#[cfg(feature = "desktop")]
mod cli;              // Command-line interface
#[cfg(feature = "desktop")]
mod server;           // HTTP server implementation
#[cfg(feature = "desktop")]
mod diagnostics;      // Server diagnostics
```

**Mobile-Only Modules:**
```rust
#[cfg(feature = "mobile")]
mod android;          // Android JNI bindings
#[cfg(feature = "mobile")]
mod ios;              // iOS FFI bindings
#[cfg(feature = "mobile")]
mod ffi;              // Cross-platform FFI interface
```

**Shared Core Modules:**
```rust
// Always compiled - no feature gates
mod p2p;              // P2P protocol implementation
mod expert;            // Expert system algorithms
mod sync;              // Synchronization logic
mod models;            // Data models and schemas
mod storage;           // Database operations
mod crypto;            // Cryptographic operations
```

## Directory Structure

```
truth-core/
├── src/
│   ├── core/                    # Shared core logic
│   │   ├── p2p/
│   │   │   ├── mod.rs           # P2P module exports
│   │   │   ├── encryption.rs    # Ed25519 crypto operations
│   │   │   ├── node.rs          # P2P node implementation
│   │   │   └── sync.rs          # Synchronization protocol
│   │   ├── expert/
│   │   │   ├── mod.rs           # Expert system exports
│   │   │   └── simple.rs        # Truth assessment algorithms
│   │   ├── models/
│   │   │   ├── mod.rs           # Data model exports
│   │   │   └── events.rs        # Event and statement models
│   │   └── storage/
│   │       ├── mod.rs           # Storage exports
│   │       └── operations.rs    # Database operations
│   ├── desktop/                 # Desktop-only modules
│   │   ├── mod.rs               # Desktop module exports
│   │   ├── api.rs               # HTTP REST API implementation
│   │   ├── cli.rs               # CLI command handling
│   │   ├── server.rs            # HTTP server implementation
│   │   └── diagnostics.rs       # Server health checks
│   ├── mobile/                  # Mobile-only modules
│   │   ├── mod.rs               # Mobile module exports
│   │   ├── android.rs           # Android JNI bindings
│   │   ├── ios.rs               # iOS FFI bindings
│   │   ├── ffi.rs               # Cross-platform FFI interface
│   │   └── async.rs             # Mobile async runtime
│   └── lib.rs                   # Main library entry point
├── Cargo.toml                   # Feature definitions
├── build.rs                     # Build script for FFI generation
└── cbindgen.toml                # FFI binding configuration
```

## Platform-Specific Implementations

### Desktop Implementation

**HTTP Server (Desktop Only):**
```rust
#[cfg(feature = "desktop")]
pub async fn start_http_server(port: u16) -> Result<(), Box<dyn std::error::Error>> {
    use actix_web::{web, App, HttpServer};
    
    HttpServer::new(|| {
        App::new()
            .service(web::resource("/api/v1/info").to(api::info))
            .service(web::resource("/api/v1/stats").to(api::stats))
            .service(web::resource("/sync").to(api::sync))
    })
    .bind(format!("0.0.0.0:{}", port))?
    .run()
    .await?;
    
    Ok(())
}
```

**CLI Interface (Desktop Only):**
```rust
#[cfg(feature = "desktop")]
pub fn run_cli() -> Result<(), Box<dyn std::error::Error>> {
    use clap::{Parser, Subcommand};
    
    #[derive(Parser)]
    #[command(name = "truthctl")]
    struct Cli {
        #[command(subcommand)]
        command: Commands,
    }
    
    #[derive(Subcommand)]
    enum Commands {
        Sync { peer: String },
        Status,
        Peers { action: String },
    }
    
    let cli = Cli::parse();
    // CLI implementation
    Ok(())
}
```

### Mobile Implementation

**Android JNI Bindings:**
```rust
#[cfg(feature = "mobile")]
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;

#[cfg(feature = "mobile")]
#[no_mangle]
pub extern "system" fn Java_com_truth_training_client_TruthCore_processJsonRequest(
    mut env: JNIEnv,
    _class: JClass,
    request: JString,
) -> jstring {
    let input: String = match env.get_string(&request) {
        Ok(jstr) => jstr.into(),
        Err(_) => return env.new_string(r#"{"error":"invalid_input"}"#).unwrap().into_raw(),
    };

    // Process JSON request using core logic
    let response = process_json_request(&input);
    
    match env.new_string(response) {
        Ok(jstr) => jstr.into_raw(),
        Err(_) => env.new_string(r#"{"error":"response_failed"}"#).unwrap().into_raw(),
    }
}
```

**iOS FFI Bindings:**
```rust
#[cfg(feature = "mobile")]
use std::ffi::{CStr, CString};
use std::os::raw::c_char;

#[cfg(feature = "mobile")]
#[no_mangle]
pub extern "C" fn truth_core_process_json(
    json_ptr: *const c_char,
    json_len: usize
) -> *mut c_char {
    let json_str = unsafe {
        let slice = std::slice::from_raw_parts(json_ptr as *const u8, json_len);
        match std::str::from_utf8(slice) {
            Ok(s) => s,
            Err(_) => return std::ptr::null_mut(),
        }
    };

    // Process JSON using core logic
    let response = process_json_request(json_str);
    
    match CString::new(response) {
        Ok(c_string) => c_string.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}
```

## Async Runtime Strategy

### Desktop Async Runtime
- **Primary**: Tokio with full features
- **HTTP**: Actix-web for server implementation
- **Client**: Reqwest for external HTTP requests
- **Concurrency**: Full async/await support

```rust
#[cfg(feature = "desktop")]
use tokio::runtime::Runtime;
use actix_web::{web, App, HttpServer};

#[cfg(feature = "desktop")]
pub async fn desktop_async_task() -> Result<(), Box<dyn std::error::Error>> {
    // Desktop-specific async implementation
    Ok(())
}
```

### Mobile Async Runtime
- **Primary**: Smol for lightweight async
- **Fallback**: `std::thread` for simple tasks
- **Avoid**: Tokio (too heavy for mobile)
- **Networking**: Minimal HTTP client or custom implementation

```rust
#[cfg(feature = "mobile")]
use smol::Task;
use std::thread;

#[cfg(feature = "mobile")]
pub fn mobile_async_task() -> Result<(), Box<dyn std::error::Error>> {
    // Mobile-specific async implementation using smol
    Ok(())
}
```

## P2P Networking Adaptation

### Desktop P2P
- **Full Protocol**: UDP beacons + HTTP sync endpoints
- **Features**: Complete peer discovery, bidirectional sync, conflict resolution
- **Performance**: Optimized for high-bandwidth connections

### Mobile P2P
- **Minimal Protocol**: Basic UDP discovery + JSON over HTTP
- **Features**: Essential peer discovery, unidirectional sync
- **Performance**: Optimized for low-bandwidth, battery-efficient operation

### Shared P2P Components
- **Cryptography**: Ed25519 signature operations
- **Data Models**: Event, statement, and impact structures
- **Storage**: SQLite database operations
- **Sync Logic**: Core synchronization algorithms

## Build Configuration

### Cargo.toml Platform Configuration

```toml
[package]
name = "truth_core"
version = "0.4.0"
edition = "2021"

[lib]
name = "truth_core"
path = "src/lib.rs"
crate-type = ["cdylib", "rlib"]

[features]
default = []
desktop = ["p2p-client-sync", "auth", "http-server", "cli"]
mobile = ["minimal-p2p", "ffi"]

# Desktop-only dependencies
[target.'cfg(feature = "desktop")'.dependencies]
actix-web = "4"
actix-cors = "0.6"
tokio = { version = "1.37", features = ["full"] }
reqwest = { version = "0.11", default-features = false, features = ["json", "rustls-tls"] }
clap = { version = "4", features = ["derive"] }
env_logger = "0.11"
jsonwebtoken = "9"
utoipa = { version = "4", features = ["actix_extras"] }
utoipa-swagger-ui = { version = "5", features = ["actix-web"] }

# Mobile-only dependencies
[target.'cfg(feature = "mobile")'.dependencies]
smol = "1.3"
jni = "0.21"

# Shared dependencies (always included)
[dependencies]
core_lib = { path = "core" }
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"
rusqlite = { version = "0.31", features = ["bundled"] }
uuid = { version = "1.8", features = ["v4", "serde"] }
chrono = { version = "0.4", features = ["serde"] }
rand = "0.8"
ed25519-dalek = { version = "2.0", features = ["rand_core"] }
hex = "0.4"
log = "0.4"
base64 = "0.22"
thiserror = "1.0"
```

## FFI Generation

### Android JNI Headers
```bash
# Generate JNI headers
cbindgen --config cbindgen.toml --crate truth_core --output android/truth_core.h

# Build Android library
cargo ndk --target aarch64-linux-android --android-platform 29 build --release --features mobile
```

### iOS Swift Bindings
```bash
# Generate Swift bindings
cbindgen --config cbindgen.toml --crate truth_core --output ios/truth_core.h

# Build iOS framework
cargo build --release --target aarch64-apple-ios --features mobile
```

### cbindgen.toml Configuration
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

## Testing Strategy

### Platform-Specific Testing
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

# Desktop integration tests
cargo test --features desktop
```

## Performance Considerations

### Desktop Optimization
- **Memory**: Full feature set, higher memory usage acceptable
- **CPU**: Multi-threaded async operations
- **Network**: High-bandwidth P2P operations
- **Storage**: Full SQLite with all features

### Mobile Optimization
- **Memory**: Minimal footprint, efficient memory usage
- **CPU**: Single-threaded or lightweight threading
- **Network**: Battery-efficient, low-bandwidth operations
- **Storage**: Minimal SQLite with essential features only

## Security Considerations

### Cross-Platform Security
- **Cryptography**: Ed25519 signatures on all platforms
- **Authentication**: Platform-appropriate authentication methods
- **Data Integrity**: Consistent verification across platforms
- **Privacy**: Platform-specific privacy implementations

### Platform-Specific Security
- **Desktop**: JWT authentication, HTTPS endpoints
- **Mobile**: JSON signature verification, secure FFI interfaces
- **Shared**: Ed25519 cryptographic operations, secure P2P communication

This architecture ensures optimal performance and functionality for each target platform while maintaining code reusability and consistency across the Truth Training ecosystem.
