# iOS Integration Guide (Truth Core v0.4.0)

## Overview

This guide provides step-by-step instructions for integrating Truth Core with iOS applications using Swift FFI bindings and the minimal mobile feature set.

## Prerequisites

- Xcode 14.0+
- Rust toolchain (≥ 1.75)
- iOS deployment target: iOS 13.0+

## Setup

### 1. Add Rust Targets
```bash
rustup target add aarch64-apple-ios
rustup target add aarch64-apple-ios-sim
```

### 2. Install cbindgen
```bash
cargo install cbindgen
```

## Build Process

### 1. Build iOS Library
```bash
# Build for device
cargo build --release --target aarch64-apple-ios --features mobile

# Build for simulator
cargo build --release --target aarch64-apple-ios-sim --features mobile
```

### 2. Generate Swift Bindings
```bash
cbindgen --config cbindgen.toml --crate truth_core --output ios/truth_core.h
```

### 3. Create Universal Binary (Optional)
```bash
cargo install cargo-lipo
cargo lipo --release --targets aarch64-apple-ios,aarch64-apple-ios-sim
```

## Xcode Integration

### 1. Create Framework
1. Create new iOS Framework project in Xcode
2. Add `libtruth_core.a` to "Link Binary With Libraries"
3. Add `truth_core.h` to "Headers" section
4. Configure Build Settings:
   - **Header Search Paths**: Add path to `truth_core.h`
   - **Library Search Paths**: Add path to `libtruth_core.a`
   - **Other Linker Flags**: Add `-ltruth_core`

### 2. Swift Bridge Header
Create `TruthCore-Bridging-Header.h`:
```objc
#ifndef TruthCore_Bridging_Header_h
#define TruthCore_Bridging_Header_h

#import "truth_core.h"

#endif /* TruthCore_Bridging_Header_h */
```

### 3. Swift Wrapper
Create `TruthCore.swift`:
```swift
import Foundation

public class TruthCore {
    private let core: OpaquePointer?
    
    public init() {
        self.core = truth_core_init()
    }
    
    deinit {
        truth_core_free(core)
    }
    
    public func processJson(_ json: String) -> String? {
        let result = json.withCString { cString in
            truth_core_process_json(cString, strlen(cString))
        }
        
        guard let result = result else { return nil }
        
        let swiftString = String(cString: result)
        truth_core_free_string(result)
        
        return swiftString
    }
    
    public func verifySignature(_ message: String, signature: String, publicKey: String) -> Bool {
        return message.withCString { messagePtr in
            signature.withCString { sigPtr in
                publicKey.withCString { keyPtr in
                    truth_core_verify_signature(messagePtr, sigPtr, keyPtr)
                }
            }
        }
    }
}
```

## FFI Interface

### C Header (truth_core.h)
```c
#ifndef TRUTH_CORE_H
#define TRUTH_CORE_H

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

// Core initialization
void* truth_core_init(void);
void truth_core_free(void* core);

// JSON processing
char* truth_core_process_json(const char* json, size_t json_len);
void truth_core_free_string(char* str);

// Signature verification
bool truth_core_verify_signature(const char* message, const char* signature, const char* public_key);

// P2P operations
int truth_core_sync_with_peer(void* core, const char* peer_url);
int truth_core_get_peer_count(void* core);

#ifdef __cplusplus
}
#endif

#endif // TRUTH_CORE_H
```

### Rust Implementation
```rust
#[cfg(feature = "mobile")]
use std::ffi::{CStr, CString};
use std::os::raw::{c_char, c_void};

#[cfg(feature = "mobile")]
#[no_mangle]
pub extern "C" fn truth_core_init() -> *mut c_void {
    // Initialize core instance
    Box::into_raw(Box::new(TruthCore::new())) as *mut c_void
}

#[cfg(feature = "mobile")]
#[no_mangle]
pub extern "C" fn truth_core_free(core: *mut c_void) {
    if !core.is_null() {
        unsafe {
            let _ = Box::from_raw(core as *mut TruthCore);
        }
    }
}

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

#[cfg(feature = "mobile")]
#[no_mangle]
pub extern "C" fn truth_core_free_string(s: *mut c_char) {
    if !s.is_null() {
        unsafe {
            let _ = CString::from_raw(s);
        }
    }
}
```

## Usage Examples

### Basic JSON Processing
```swift
let truthCore = TruthCore()

let jsonRequest = """
{
    "action": "ping",
    "timestamp": 1640995200
}
"""

if let response = truthCore.processJson(jsonRequest) {
    print("Response: \(response)")
}
```

### Signature Verification
```swift
let message = "Hello, Truth Core!"
let signature = "base64_encoded_signature"
let publicKey = "base64_encoded_public_key"

if truthCore.verifySignature(message, signature: signature, publicKey: publicKey) {
    print("Signature verified successfully")
} else {
    print("Signature verification failed")
}
```

### P2P Synchronization
```swift
let truthCore = TruthCore()

// Sync with peer
let peerUrl = "http://192.168.1.100:8080"
let result = truthCore.syncWithPeer(peerUrl)

if result == 0 {
    print("Sync completed successfully")
} else {
    print("Sync failed with error code: \(result)")
}

// Get peer count
let peerCount = truthCore.getPeerCount()
print("Connected peers: \(peerCount)")
```

## Error Handling

### Swift Error Types
```swift
public enum TruthCoreError: Error {
    case initializationFailed
    case invalidJson
    case signatureVerificationFailed
    case networkError(Int)
    case unknownError
}

public class TruthCore {
    public func processJsonSafely(_ json: String) throws -> String {
        guard let result = processJson(json) else {
            throw TruthCoreError.invalidJson
        }
        return result
    }
    
    public func verifySignatureSafely(_ message: String, signature: String, publicKey: String) throws -> Bool {
        let result = verifySignature(message, signature: signature, publicKey: publicKey)
        if !result {
            throw TruthCoreError.signatureVerificationFailed
        }
        return result
    }
}
```

## Performance Considerations

### Memory Management
- Always call `truth_core_free_string()` for returned strings
- Use `deinit` to properly clean up core instance
- Avoid retaining large objects in Swift wrapper

### Threading
- FFI calls are thread-safe
- Use background queues for heavy operations
- Avoid blocking main thread

### Battery Optimization
- Use minimal P2P protocol for mobile
- Implement smart sync intervals
- Cache frequently accessed data

## Testing

### Unit Tests
```swift
import XCTest
@testable import TruthCore

class TruthCoreTests: XCTestCase {
    var truthCore: TruthCore!
    
    override func setUp() {
        super.setUp()
        truthCore = TruthCore()
    }
    
    override func tearDown() {
        truthCore = nil
        super.tearDown()
    }
    
    func testJsonProcessing() {
        let json = "{\"action\":\"ping\"}"
        let result = truthCore.processJson(json)
        XCTAssertNotNil(result)
    }
    
    func testSignatureVerification() {
        let message = "test message"
        let signature = "valid_signature"
        let publicKey = "valid_public_key"
        
        let result = truthCore.verifySignature(message, signature: signature, publicKey: publicKey)
        XCTAssertTrue(result)
    }
}
```

## Troubleshooting

### Common Issues

**Build Errors:**
- Ensure iOS targets are installed: `rustup target add aarch64-apple-ios`
- Check Xcode Command Line Tools: `xcode-select --install`

**Linking Errors:**
- Verify library search paths in Xcode
- Check that `libtruth_core.a` is added to "Link Binary With Libraries"

**Runtime Errors:**
- Ensure proper memory management
- Check FFI function signatures match C header

### Debug Configuration
```toml
# Cargo.toml for debug builds
[profile.dev]
opt-level = 0
debug = true
overflow-checks = true
```

This integration guide ensures smooth iOS development with Truth Core's minimal mobile feature set while maintaining optimal performance and battery efficiency.
