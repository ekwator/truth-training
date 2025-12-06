# Building Truth Core for Android

**Version:** v1.0.0  
**Target:** Android Client v1.0.0 (stable, full feature parity with Desktop UI)

## Prerequisites
- Rust toolchain (≥ 1.75)
- Cargo
- Android NDK (r25+)
- Set `NDK_HOME` in your shell environment
- Android SDK 26+ (minSdk 26, targetSdk 33)
- JDK 17

## Build steps (cargo)
```bash
rustup target add aarch64-linux-android x86_64-linux-android

# Build shared libraries for Android
cargo build --release --target aarch64-linux-android --features mobile --lib -p truth_core
cargo build --release --target x86_64-linux-android --features mobile --lib -p truth_core

# Outputs:
# target/aarch64-linux-android/release/libtruth_core.so
# target/x86_64-linux-android/release/libtruth_core.so
```

You can copy the produced `.so` files into the Android client's `app/src/main/jniLibs/<abi>/` folders:
- `app/src/main/jniLibs/arm64-v8a/libtruth_core.so`
- `app/src/main/jniLibs/x86_64/libtruth_core.so`

**Note:** The Android client v1.0.0 uses Room Database for offline-first storage and communicates with the Rust core via both JNI (for core functions) and HTTP API (for REST endpoints). The JNI bridge is primarily used for initialization and low-level operations, while most data operations go through the HTTP API.

## JSON Bridge

The Android client communicates with the Rust core through JSON messages via the JNI bridge function:

`Java_com_truth_training_client_TruthCore_processJsonRequest(env, class, request)`

### Request Format

All requests must be valid JSON with an `"action"` field:

```json
{"action": "get_state"}
{"action": "ping"}
{"action": "get_info"}
{"action": "get_stats"}
```

### Response Format

Responses are JSON objects with either:
- `"status": "ok"` for successful operations
- `"error": "error_type"` for failures

### Sample Requests and Responses

#### Get State
**Request:**
```json
{"action": "get_state"}
```

**Response:**
```json
{
  "status": "ok",
  "state": "connected",
  "version": "1.0.0",
  "uptime": 12345
}
```

#### Ping
**Request:**
```json
{"action": "ping"}
```

**Response:**
```json
{
  "status": "ok",
  "reply": "pong",
  "timestamp": 1710000000
}
```

#### Get Info
**Request:**
```json
{"action": "get_info"}
```

**Response:**
```json
{
  "status": "ok",
  "name": "truth-core",
  "version": "1.0.0",
  "features": ["p2p-client-sync", "mobile", "jwt"],
  "peer_count": 0
}
```

**See also:** [sample_responses/info.json](sample_responses/info.json) for HTTP API response format.

#### Get Stats
**Request:**
```json
{"action": "get_stats"}
```

**Response:**
```json
{
  "status": "ok",
  "events": 120,
  "statements": 340,
  "impacts": 21,
  "node_ratings": 8,
  "group_ratings": 2,
  "avg_trust_score": 0.62,
  "avg_propagation_priority": 0.71,
  "avg_relay_success_rate": 0.84,
  "active_nodes": 7
}
```

**See also:** [sample_responses/stats.json](sample_responses/stats.json) for HTTP API response format.

## Extended JSON API for Android (v1.0.0)

**Note:** The Android client v1.0.0 primarily uses HTTP REST API endpoints (see [Android Integration Guide](README_INTEGRATION.md)) for most operations. The JNI JSON bridge is used for core initialization and low-level operations.

The following actions extend the JSON bridge to cover P2P and semantic workflows:

- sync_peers: triggers peer discovery/synchronization and returns current peer list
- submit_claim: registers a new claim for evaluation/storage (legacy, use HTTP API `/api/v1/events` instead)
- get_claims: returns stored claims (summary) (legacy, use HTTP API `/api/v1/events` instead)
- analyze_text: performs a basic semantic analysis on text

### sync_peers
Request:
```json
{"action": "sync_peers"}
```
Response:
```json
{
  "status": "ok",
  "peers": ["node1.local", "node2.local"]
}
```

### submit_claim
Request:
```json
{"action": "submit_claim", "claim": "Earth is round"}
```
Response:
```json
{
  "status": "received",
  "claim": "Earth is round"
}
```

### get_claims
Request:
```json
{"action": "get_claims"}
```
Response:
```json
{
  "status": "ok",
  "claims": ["Earth is round", "Truth is distributed"]
}
```

### analyze_text
Request:
```json
{"action": "analyze_text", "text": "truth requires context"}
```
Response:
```json
{
  "status": "ok",
  "sentiment": "neutral",
  "keywords": ["truth", "context"]
}
```

#### Error Responses
**Invalid JSON:**
```json
{"error": "invalid_json"}
```

**Unknown Action:**
```json
{
  "error": "unknown_action",
  "received_action": "unknown_action"
}
```

## Android Client v1.0.0 Architecture

**Primary Communication:**
- **HTTP REST API**: Most operations use Retrofit with REST endpoints (`/api/v1/events`, `/api/v1/contexts`, `/api/v1/judgments`, etc.)
- **JNI Bridge**: Used for core initialization (`TruthCore.initNode()`) and low-level operations

**Storage:**
- **Room Database**: Offline-first SQLite persistence with reactive Flow-based queries
- **Sync Queue**: Pending operations queued for background sync via WorkManager

**Key Features:**
- Full CRUD operations for Events, Context Templates, Judgments, Impacts
- Embedded context fields (`category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`)
- P2P discovery via UDP multicast (239.255.0.1:52525) and global registry polling
- Background sync every 15 minutes via WorkManager

**See Also:**
- [Android Integration Guide](README_INTEGRATION.md) - Complete API reference and Retrofit setup
- [Android Migration Guide](../../ANDROID_MIGRATION.md) - Migration from v0.3.0 to v1.0.0
- [Android Quickstart](../../quickstart_android.md) - Installation and usage instructions

**Sample Response Files:**
- [sample_responses/info.json](sample_responses/info.json) - `/api/v1/info` HTTP API response format
- [sample_responses/stats.json](sample_responses/stats.json) - `/api/v1/stats` HTTP API response format
- [sample_responses/graph.json](sample_responses/graph.json) - `/graph/json` HTTP API response format

_Version: v1.0.0_
