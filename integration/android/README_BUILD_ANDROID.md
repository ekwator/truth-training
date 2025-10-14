# Building Truth Core for Android

## Prerequisites
- Rust + cargo
- Android NDK (r25+)
- Set `NDK_HOME` in your shell environment

## Build steps
```bash
chmod +x scripts/build-android.sh
./scripts/build-android.sh
```

Output .so files will appear in:

android-libs/arm64-v8a/libtruthcore.so
android-libs/x86_64/libtruthcore.so

These can be copied into the Android client's:

truth-android-client/app/src/main/jniLibs/

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
  "version": "0.3.0",
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
  "version": "0.3.0",
  "features": ["p2p-client-sync", "jwt"],
  "peer_count": 0
}
```

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

## Extended JSON API for Android

The following actions extend the JSON bridge to cover P2P and semantic workflows:

- sync_peers: triggers peer discovery/synchronization and returns current peer list
- submit_claim: registers a new claim for evaluation/storage
- get_claims: returns stored claims (summary)
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

