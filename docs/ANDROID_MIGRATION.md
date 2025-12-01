# Android Client Migration Guide: v0.3.0 → v1.0.0

**Date:** 2025-11-02  
**Version:** Android Client v1.0.0

---

## 📋 Overview

The Android client upgraded from v0.3.0 (pre-release) to v1.0.0, reaching full feature parity with the Desktop UI v1.0.0 baseline.

### Key changes

1. **Version:** `0.3.0` → `1.0.0`
2. **minSdk:** `24` → `26`
3. **targetSdk:** `35` → `33`
4. **Architecture:** Added Room Database (SQLite) for offline-first storage
5. **UI Framework:** Migrated to Jetpack Compose
6. **API Compatibility:** Full support for v1.0.0 endpoints with embedded context fields

---

## 🏗️ Architecture changes

### Room Database

**New:** Full Room integration for local persistence.

**Files:**
- `data/database/TruthDatabase.kt` — main Room database entry point
- `data/database/entities/` — Event, ContextTemplate, Judgment, Impact, Summary, SyncQueue entities
- `data/database/daos/` — DAO interfaces with Flow support

**Schema versions:**
- **Version 1:** Base schema (events, contexts, judgments, impacts, summaries, sync_queue)
- **Version 2:** Adds knowledge base entities (category, cause, develop, effect, forma, impact_type, progress_metrics) and updates `EventEntity` for embedded context fields
- Migration V1→V2 defined in `TruthDatabaseMigrations.kt` (`MIGRATION_1_2`)

**Migration behavior:**
- SharedPreferences remain for tokens only
- Business data (events/templates/judgments) now lives in Room
- WorkManager keeps Room synchronized with the server
- Room automatically applies schema migrations

### Offline-first workflow

**New:** Operations are stored locally before hitting the network.

**Components:**
- `SyncQueueManager` — queues pending operations
- `SyncWorker` (WorkManager) — background sync worker
- `SyncQueueEntity` — persisted queue entries

**Behavior:**
- CREATE/UPDATE/DELETE writes happen locally immediately
- Operations enter the queue for later sync
- Background sync flushes the queue when connectivity returns

### P2P synchronization

**New:** Direct peer-to-peer sync between Android devices.

**Components:**
- `P2PSyncManager` — propagates events over P2P
- `P2PMessageHandler` — decrypts/verifies envelopes
- `P2PDiscoveryService` — NSD-based peer discovery

**Functionality:**
- Automatic discovery via `_truthnode._tcp.`
- Ed25519-signed payloads for integrity
- Local-wins conflict resolution to match Desktop

---

## 🔄 API changes

### Embedded context fields (v1.0.0)

`context_id` was replaced with embedded fields.

**Legacy v0.3.0 format**
```kotlin
data class Event(
    val context_id: Int?
)
```

**v1.0.0 format**
```kotlin
data class Event(
    val category_id: Int?,
    val forma_id: Int?,
    val cause_id: Int?,
    val develop_id: Int?,
    val effect_id: Int?
)
```

**Data migration guidance**
- Update existing events with embedded fields
- New events must supply embedded fields only
- Server keeps temporary compatibility but client-side `context_id` is deprecated

### Context Templates API

**New:** Full CRUD support.

Endpoints:
- `GET /api/v1/contexts` — list templates
- `POST /api/v1/contexts` — create template (duplicate detection, validation)
- `POST /api/v1/contexts/match` — suggest template based on provided fields
- `POST /api/v1/contexts/from-event` — generate template from an event payload

### Judgments API

**New:** Judgments with consensus statistics.

Endpoints:
- `POST /api/v1/judgments` — submit judgments
- `GET /api/v1/judgments?event_id={id}` — list judgments for an event
- `GET /api/v1/judgments/stats/{event_id}` — consensus stats

---

## 🎨 UI updates

### Jetpack Compose adoption

**New:** Compose replaces legacy XML-first UI.

**Screens:**
- `EventListScreen`, `EventCreateScreen`, `EventDetailScreen`
- `ContextTemplateListScreen`, `ContextTemplateEditorScreen`
- `JudgmentListScreen`, `JudgmentSubmissionScreen`

**Navigation:**
- `MainNavigation` built on Compose Navigation + NavHost
- Material 3 design system baseline

**Migration notes:**
- Legacy activities remain for compatibility
- Compose screens are the default entry points
- `MainActivity` bootstraps Compose root content

---

## 📦 Dependencies

### Added libraries

```kotlin
// Room Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.9.0")

// Jetpack Compose
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.navigation:navigation-compose:2.7.6")
```

### Existing versions (unchanged)
- Kotlin 2.0.20
- Retrofit 2.11.0
- OkHttp 4.12.0

---

## 🚀 Migration steps

### 1. Update dependencies

```bash
cd truth-android-client
./gradlew clean build
```

### 2. Data migration

**Automatic:**
- Room applies schema migrations
- SharedPreferences tokens remain untouched
- Embedded fields populate from API responses

**Manual (when migrating on-device data):**
```kotlin
// Example placeholder: map legacy context_id -> embedded fields
// Run once during first v1.0.0 launch if legacy data must be preserved
```

### 3. Update the Application class

`TruthTrainingApplication` now initializes Room.

```xml
<application
    android:name=".TruthTrainingApplication"
    ... >
</application>
```

### 4. Testing checklist

1. Integration tests (`./gradlew connectedAndroidTest`)
2. Offline-first flow (airplane mode → create event → reconnect)
3. Sync validation (ensure queue drains when network returns)
4. P2P sync validation with two devices on the same LAN

---

## ⚠️ Breaking changes

### 1. Embedded fields only
- `context_id` is removed from client payloads
- Server compatibility will be removed in a future release

### 2. minSdk raised to 26
- Android 7.0 (API 24-25) is no longer supported
- Minimum requirement: Android 8.0 (API 26)

### 3. Application class required
- `android:name=".TruthTrainingApplication"` is mandatory in the manifest
- Remove custom database initialization from activities/fragments

---

## ✅ Migration checklist

- [ ] Update `app/build.gradle.kts` with new dependencies
- [ ] Add `TruthTrainingApplication` to the manifest
- [ ] Validate device coverage for minSdk 26
- [ ] Migrate any legacy `context_id` data
- [ ] Update tests for new repositories/DAOs
- [ ] Validate offline-first flows
- [ ] Test P2P sync between devices
- [ ] Refresh documentation and release notes

---

## 📚 Additional resources

- **Specification:** [specs/007-title-align-truth/plan.md](https://github.com/ekwator/truth-training/blob/main/specs/007-title-align-truth/plan.md)
- **Data model:** [specs/007-title-align-truth/data-model.md](https://github.com/ekwator/truth-training/blob/main/specs/007-title-align-truth/data-model.md)
- **API contracts:** [specs/007-title-align-truth/contracts/openapi.yaml](https://github.com/ekwator/truth-training/blob/main/specs/007-title-align-truth/contracts/openapi.yaml)
- **Quickstart:** [specs/007-title-align-truth/quickstart.md](https://github.com/ekwator/truth-training/blob/main/specs/007-title-align-truth/quickstart.md)
- **Test report:** [docs/TEST_REPORT_ANDROID_v1.0.0.md](TEST_REPORT_ANDROID_v1.0.0.md)
- **Test fix suggestions:** [docs/ANDROID_TEST_FIX_SUGGESTIONS.md](ANDROID_TEST_FIX_SUGGESTIONS.md)
- **Version registry:** [docs/VERSION_REGISTRY.md](VERSION_REGISTRY.md)

---

**Status:** ✅ Migration complete — Android v1.0.0 is ready for production.


