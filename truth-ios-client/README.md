# Truth Training iOS Client

The iOS client for the Truth Training platform, built with SwiftUI and integrated with the Truth Core Rust library.

## Features

- **SwiftUI Interface**: Modern, responsive UI built with SwiftUI
- **Truth Core Integration**: Direct integration with the Rust core library via FFI
- **JSON Processing**: Process JSON requests through the Truth Core engine
- **Signature Verification**: Ed25519 signature verification for secure communication
- **P2P Synchronization**: Peer-to-peer synchronization capabilities

## Prerequisites

- Xcode 14.0+
- iOS 13.0+ deployment target
- Rust toolchain (for building the core library)

## Building

### Using Xcode

1. Open `TruthTraining.xcodeproj` in Xcode
2. Select your target device or simulator
3. Build and run (⌘+R)

### Using Command Line

```bash
cd truth-ios-client
xcodebuild -scheme TruthTraining -sdk iphoneos -configuration Release
```

## Truth Core Integration

The iOS client integrates with the Truth Core Rust library through FFI bindings:

- **Header**: `truth_core.h` - C interface definitions
- **Library**: `libtruth_core.a` - Compiled Rust library
- **Swift Wrapper**: `TruthCore.swift` - Swift interface to the C library

### Building the Rust Library

The Rust library must be built separately for iOS targets:

```bash
# Add iOS targets
rustup target add aarch64-apple-ios
rustup target add aarch64-apple-ios-sim

# Build for device
cargo build --release --target aarch64-apple-ios --features mobile --lib -p truth_core

# Build for simulator
cargo build --release --target aarch64-apple-ios-sim --features mobile --lib -p truth_core
```

## Architecture

### SwiftUI Components

- **TruthTrainingApp**: Main app entry point
- **ContentView**: Primary interface with JSON processing
- **TruthCore**: Swift wrapper for the Rust core library

### FFI Interface

The iOS client communicates with the Rust core through C FFI:

```c
// Core initialization
void* truth_core_init(void);
void truth_core_free(void* core);

// JSON processing
char* truth_core_process_json(const char* json, size_t json_len);
void truth_core_free_string(char* str);

// Signature verification
bool truth_core_verify_signature(const char* message, const char* signature, const char* public_key);

// P2P operations
int32_t truth_core_sync_with_peer(void* core, const char* peer_url);
int32_t truth_core_get_peer_count(void* core);
```

## Usage

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
}
```

## Development

### Project Structure

```
truth-ios-client/
├── TruthTraining.xcodeproj/     # Xcode project file
├── TruthTraining/               # Source code
│   ├── TruthTrainingApp.swift   # App entry point
│   ├── ContentView.swift        # Main UI
│   ├── TruthCore.swift          # Swift wrapper
│   ├── TruthCore-Bridging-Header.h  # Objective-C bridge
│   ├── truth_core.h             # C header
│   ├── libtruth_core.a          # Rust library
│   └── Assets.xcassets/         # App assets
└── README.md                    # This file
```

### Testing

Run tests in Xcode or via command line:

```bash
xcodebuild test -scheme TruthTraining -destination 'platform=iOS Simulator,name=iPhone 14'
```

## CI/CD Integration

The iOS client is integrated with GitHub Actions for automated building and testing. See `.github/workflows/ios-build.yml` for the complete CI/CD pipeline.

## License

This project is part of the Truth Training platform. See the main repository for licensing information.
