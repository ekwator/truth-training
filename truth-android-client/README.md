# Truth Training — Android Client
Truth Android Client v1.0.0
=================================

**Version:** 1.0.0 (stable)  
**Status:** ✅ Feature parity with Desktop UI v1.0.0  
**Date:** 2025-11-02

Requirements:
- Android Studio (Giraffe+), JDK 17
- Android SDK 26+ (minSdk 26, targetSdk 33)
- Truth Core Server v1.0.0+

Build:
```bash
./gradlew assembleLocalDebug
```

Base configuration:
- `BASE_URL` is defined via `BuildConfig` and product flavors:
  - local: `http://10.0.2.2:8080`
  - remote: replace with your server, e.g. `https://truth-core.example.com`

## Key capabilities in v1.0.0

### ✅ Full functionality
- **Room Database** — offline-first architecture backed by local SQLite
- **Context Templates** — create, edit, search, and reuse context templates
- **Events Management** — full CRUD with embedded context fields (v1.0.0 API)
- **Judgments & Consensus** — submit judgments and view consensus statistics
- **P2P Synchronization** — direct sync between Android clients
- **Jetpack Compose UI** — modern Material 3 interface
- **Background Sync** — automated sync via WorkManager

### Architecture highlights
- **Offline-first:** all mutations are stored locally and synced in the background
- **Room Database:** SQLite via Room with Flow-powered reactive UI
- **Repository Pattern:** unified data access across Room + Retrofit APIs
- **Sync Queue:** tracks pending operations and replays them when online

Integration with Truth Core v1.0.0:
- **API Endpoints:** full coverage (Events, Contexts, Judgments, Impacts, Sync)
- **Authentication:** JWT via `Authorization: Bearer <token>`
- **Embedded fields:** events use `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`
- **Token storage:** JWT kept in SharedPreferences, auto-refresh handled by `RefreshAuthenticator`

## Testing

Unit tests:
```bash
./gradlew test
```

Instrumented / integration tests:
```bash
./gradlew connectedAndroidTest
```

Contract tests (API endpoints):
```bash
./gradlew test --tests "*Contract*"
```

Integration notes:
- Additional API details: `truthcore_api/api_reference_link.md` → [`docs/api_reference/API_REFERENCE.md`](../docs/api_reference/API_REFERENCE.md)
- Platform comparison: [`docs/Truth-training/Truth-training.md`](../docs/Truth-training/Truth-training.md)

Mock flavor:
- Build: `./gradlew assembleMockDebug`
- Payloads: `app/src/mock/assets/api/*.json`
- Backend: `MockTruthApi`, enabled by the `mock` flavor

Interacting with Truth Core from Android:
- `MainDashboardActivity` exposes quick actions:
  - Sync Peers, Submit Claim, Get Claims, Analyze Text, Get Stats
- Responses are rendered as JSON on screen
- Example request: `{"action":"get_stats"}`

Local P2P discovery:
- Discovers peers via NSD (`_truthnode._tcp.`) and exchanges JSON payloads
- `P2PActivity`: LAN peer list, ping/custom JSON send, response viewer
- Requirements: devices on the same Wi-Fi network and networking permissions in `AndroidManifest.xml`

Secure P2P messaging:
- Generates Ed25519 keys via Android Keystore (`truth_node_key`, 2048-bit backing key)
- Every outgoing message carries `signature` and `public_key` (Base64)
- Truth Core verifies signatures and returns `{ "status": "error", "reason": "invalid_signature" }` on failure
- `P2PActivity` displays the tail of the public key for quick identification
- Rust core validates RSA/Ed25519 signatures for all inbound JSON packets

## Project structure

### Room Database
- `data/database/TruthDatabase.kt` — main DB entry point
- `data/database/entities/` — `EventEntity`, `ContextTemplateEntity`, `JudgmentEntity`, `ImpactEntity`, `SummaryEntity`, `SyncQueueEntity`
- `data/database/daos/` — DAO interfaces with Flow support

### Repositories
- `data/repository/EventRepository.kt` — offline-first event management
- `data/repository/ContextTemplateRepository.kt` — context templates
- `data/repository/JudgmentRepository.kt` — judgments and consensus
- `data/repository/ImpactRepository.kt` — impacts tracking
- `data/repository/SummaryRepository.kt` — summaries

### Sync infrastructure
- `data/sync/SyncQueueManager.kt` — queueing + retry logic
- `data/sync/SyncWorker.kt` — WorkManager worker for background sync

### P2P
- `p2p/P2PSyncManager.kt` — event propagation over P2P
- `p2p/P2PMessageHandler.kt` — encrypted message handler
- `p2p/P2PDiscoveryService.kt` — NSD-based discovery

### UI (Jetpack Compose)
- `ui/compose/events/` — list, create, and detail screens
- `ui/compose/contexts/` — templates list/editor/selector
- `ui/compose/judgments/` — judgments list and submission

## Ed25519 P2P signatures

Each P2P payload is signed before transmission:
```json
{
  "payload": { "type": "EVENT_SYNC", "event_id": "...", "...": "..." },
  "signature": "<base64>",
  "public_key": "<base64>"
}
```

## Migration from v0.3.0

Detailed instructions: [`docs/ANDROID_MIGRATION.md`](../docs/ANDROID_MIGRATION.md)

Key changes:
- Version bump `0.3.0` → `1.0.0`
- `minSdk` 24 → 26
- Room database replaces legacy storage
- Jetpack Compose UI replaces legacy activities/fragments
- Embedded context fields instead of `context_id`

## Additional documentation

- **Feature comparison:** `docs/Truth-training/Truth-training.md`
- **API reference (human-readable + OpenAPI link):** `docs/api_reference/API_REFERENCE.md`
- **Migration guide:** `docs/ANDROID_MIGRATION.md`
- **Specification:** `specs/007-title-align-truth/spec.md`
- **Data model:** `specs/007-title-align-truth/data-model.md`
- **API contracts (OpenAPI):** `specs/007-title-align-truth/contracts/openapi.yaml`
