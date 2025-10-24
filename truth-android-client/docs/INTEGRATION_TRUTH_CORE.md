# Truth Core Integration (JNI)

## Overview
This Android client loads the Rust-based Truth Core library (`libtruthcore.so`) at runtime via JNI.

## Steps to Update
1. Build the Rust core using:

```bash
./scripts/build-android.sh
```

2. Copy the resulting `.so` files into:

```
app/src/main/jniLibs/arm64-v8a/
app/src/main/jniLibs/x86_64/
```

3. Rebuild the Android app:

```bash
./gradlew assembleDebug
```

## Verification
Launch the app → you should see JSON output from the core (e.g., node info, status, version).

## JNI Functions
The following functions are available from the Rust core:

- `TruthCore.initNode()` - Initialize the Truth runtime
- `TruthCore.getInfo()` - Get runtime information as JSON string
- `TruthCore.freeString(ptr: Long)` - Free memory allocated by Rust

## Architecture
- `MainActivity` initializes the core on app launch
- `TruthCore.kt` provides the JNI interface
- Native libraries are loaded from `jniLibs/` directory
- NDK configuration supports `arm64-v8a` and `x86_64` architectures

## JSON Data Exchange Test

The app can now send JSON-encoded commands to the Truth Core backend.

### Example Requests
```json
{"action": "init"}
{"action": "get_state"}
{"action": "update_state"}
```

### Example Response
```json
{
  "status": "ok",
  "data": {
    "peers": 0,
    "sync_mode": "offline",
    "uptime": 12
  }
}
```

### Testing
- Launch `JsonTestActivity` to test JSON communication
- Enter JSON request in the input field
- Tap "Send Request" to get response from Truth Core

## Trusted JSON Message Verification

Since v0.4 development phase, the Rust core validates all incoming JSON messages signed by Android clients.

Each message includes:
- `payload` — The actual JSON command or data.
- `signature` — Base64-encoded RSA or Ed25519 signature.
- `public_key` — Base64 public key (corresponding to the signer).

Example:
```json
{
  "payload": { "action": "push_claim", "value": 42 },
  "signature": "MEQCIA8f...==",
  "public_key": "MIIBIjANBgkqh..."
}
```

The core performs verification before any semantic processing.
If verification fails, it returns:

```json
{ "status": "error", "reason": "invalid_signature" }
```

Otherwise:

```json
{ "status": "ok", "trusted": true }
```
