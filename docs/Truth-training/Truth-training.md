<!-- Archived from [docs/Truth-training/Truth-training.md](docs/Truth-training/Truth-training.md) -->

# Comparative Analysis: Android vs Desktop UI

**Analysis date:** 2025-11-02 (updated)  
**Versions:** Android v1.0.0 vs Desktop UI v1.0.0

---

## 📊 Quick summary

| Criterion | Android Client | Desktop UI |
|----------|----------------|------------|
| **Version** | v1.0.0 (stable) | v1.0.0 (stable baseline) |
| **Status** | ✅ Feature parity with v1.0.0 | Stable release |
| **Language/Framework** | Kotlin + Android SDK | React/TypeScript + Tauri |
| **Architecture** | Native Android (JNI) | Hybrid (Web + Rust) |
| **Core integration** | JNI + HTTP API | Tauri Commands + HTTP API |
| **Screens** | ~6 basic | 8 full-featured |
| **Testing** | Basic (JUnit) | Comprehensive (Jest + Playwright) |
| **CI/CD** | Partial coverage | Full coverage |

---

## 🔢 Versions and development status

### Android Client
- **Version:** `1.0.0` (in `app/build.gradle.kts`)
- **Status:** ✅ Stable release (aligned with Desktop)
- **Core version:** v1.0.0 (synced)
- **API compatibility:** v1.0.0 (Context Templates, embedded fields, full feature set)

### Desktop UI
- **Version:** `1.0.0` (in `package.json`, `tauri.conf.json`, `Cargo.toml`)
- **Status:** Stable baseline release
- **Core version:** v1.0.0 (synced)
- **API compatibility:** v1.0.0 (Context Templates, embedded fields)

**✅ Alignment status:** Android v1.0.0 is fully aligned with Desktop v1.0.0 in functionality and API coverage.

---

## 🏗️ Architecture and technology

### Android Client

**Technology stack:**
- **Language:** Kotlin 2.0.20
- **Framework:** Android SDK (API 24+, target 35)
- **Build system:** Gradle (Kotlin DSL)
- **Networking:** Retrofit 2.11.0, OkHttp 4.12.0
- **Crypto:** BouncyCastle (Ed25519)
- **Async:** Kotlin Coroutines
- **Architecture:** MVVM (ViewModel + LiveData)

**Core integration:**
- **JNI:** Dynamic loading of `libtruthcore.so` (arm64-v8a, x86_64)
- **HTTP API:** Retrofit for REST endpoints
- **Protocol:** JSON with Ed25519 signatures
- **Core features:** Minimal set (`mobile` feature flag)

**Structure:**
```
truth-android-client/
├── app/src/main/
│   ├── java/com/truth/training/client/
│   │   ├── ui/              # Activities (Login, Dashboard)
│   │   ├── data/            # Repository, Network, DTOs
│   │   ├── p2p/             # P2P discovery (NSD), server, client
│   │   ├── core/crypto/     # Ed25519 cryptography
│   │   └── TruthCore.kt     # JNI bridge
│   └── jniLibs/             # libtruthcore.so (arm64, x86_64)
```

### Desktop UI

**Technology stack:**
- **Frontend:** React 18.2.0, TypeScript 5.2.2
- **Build:** Vite 6.4.1, Tauri 2.9.0
- **Backend:** Rust (Tauri), SQLite (rusqlite 0.31 bundled)
- **State:** Zustand 4.4.7
- **Networking:** Axios 1.6.2
- **Testing:** Jest 29.7.0, Playwright 1.40.1
- **UI components:** Headless UI, Heroicons

**Core integration:**
- **Tauri commands:** Direct Rust FFI calls
- **HTTP API:** Axios for REST endpoints
- **Local storage:** SQLite via the Tauri backend
- **Core features:** Full set (`desktop` feature flag)

**Structure:**
```
ui/desktop/
├── src/
│   ├── pages/               # 8 screens (Dashboard, NewEvent, ContextEditor, etc.)
│   ├── components/          # React components
│   ├── services/            # API, offline queue, validation
│   ├── stores/              # Zustand state management
│   └── types/               # TypeScript types
└── src-tauri/
    ├── src/
    │   ├── commands/        # Tauri commands (events, judgments, contexts)
    │   └── storage.rs       # SQLite wrapper
    └── Cargo.toml
```

---

## 📱 Screens and functionality

### Android Client

**Implemented screens (6):**

1. **MainActivity** — Truth Core initialization, runtime info
2. **MainDashboardActivity** — Primary dashboard with action buttons:
   - Sync Peers
   - Submit Claim
   - Get Claims
   - Analyze Text
   - Get Stats
   - JSON output
3. **DashboardActivity** — Displays:
   - Connection state
   - Last sync time
   - Node info (version, nodeId)
   - Stats (peers, edges, avgTrust)
   - Error messages
4. **LoginActivity** — Authentication (JWT tokens)
5. **P2PActivity** — P2P discovery and messaging:
   - NSD discovery (`_truthnode._tcp.`)
   - Ping / arbitrary JSON
   - LAN peer listing
6. **JsonTestActivity** — JSON communication tests with Core
7. **PushTestActivity** — Push notification tests

**Functionality:**
- ✅ Basic API access (info, stats, graph)
- ✅ JWT authentication
- ✅ Ed25519 message signatures
- ✅ P2P discovery (NSD)
- ✅ Mock mode for offline testing
- ✅ Secure messaging over LAN
- ❌ **Missing:** Event management (events)
- ❌ **Missing:** Context Templates system
- ❌ **Missing:** Judgments/Consensus
- ❌ **Missing:** Expert system UI
- ❌ **Missing:** Offline-first architecture

### Desktop UI

**Implemented screens (8):**

1. **Dashboard (Home)** — Alt+1
   - Event list with pagination
   - Sync status (online/offline, pending operations)
   - Create Event button
   - Template matching for events
   - Navigation to other screens

2. **New Event** — Alt+2
   - Event creation form
   - Context template selector (dropdown)
   - Field prefilling from templates
   - Embedded context fields (category_id, forma_id, cause_id, develop_id, effect_id)
   - Date validation

3. **Context Editor** — Alt+3 (v1.0.0)
   - Create context templates
   - Duplicate detection (409 Conflict)
   - FK validation
   - Prefill from events ("Create Template" button)

4. **Event Summary** — Alt+4
   - Event details
   - Impacts, Judgments
   - Consensus info

5. **Overall Summary** — Alt+5
   - Aggregate metrics
   - Export to TXT

6. **Training Results** — Alt+6
   - Training results
   - Statistics

7. **Logs** — Alt+7
   - Log viewer (35 lines/page)
   - Clear logs

8. **Settings** — Alt+8
   - Connection mode (Core/HTTP)
   - Server configuration (IP, port)
   - Test connection
   - Persistence in `~/.truth-training/config.json`

**Functionality:**
- ✅ Full event management (CRUD)
- ✅ Context Templates system (v1.0.0)
- ✅ Template matching and duplicate detection
- ✅ Judgments and Consensus calculation
- ✅ Offline-first with local queue
- ✅ SQLite persistence
- ✅ Knowledge Base integration
- ✅ Performance optimization (<200ms navigation)
- ✅ Comprehensive testing (unit, integration, E2E)

---

## 🔌 Truth Core integration

### Android Client

**Integration methods:**

1. **JNI Bridge** (`TruthCore.kt`):
   ```kotlin
   TruthCore.initNode()
   TruthCore.getInfo() // JSON string
   TruthCore.freeString(ptr)
   ```

2. **HTTP API** (Retrofit):
   ```kotlin
   POST /api/v1/auth
   GET /api/v1/info
   GET /api/v1/stats
   GET /graph/json
   POST /api/v1/push (with Ed25519 signature)
   ```

3. **P2P Protocol:**
   - NSD discovery (`_truthnode._tcp.`)
   - JSON messaging with signatures
   - LAN communication

**Supported endpoints:**
- ✅ `/api/v1/auth` — Authentication
- ✅ `/api/v1/info` — Node info
- ✅ `/api/v1/stats` — Statistics
- ✅ `/graph/json` — Trust graph
- ✅ `/api/v1/push` — Push events (signed)
- ❌ `/api/v1/events` — **Not implemented**
- ❌ `/api/v1/contexts` — **Not implemented**
- ❌ `/api/v1/judgments` — **Not implemented**

### Desktop UI

**Integration methods:**

1. **Tauri Commands** (FFI):
   ```typescript
   invoke('create_event_fast', { ... })
   invoke('get_event_fast', { id })
   invoke('list_contexts')
   invoke('match_context', { ... })
   ```

2. **HTTP API** (Axios):
   ```typescript
   POST /events
   GET /events
   GET /contexts
   POST /contexts
   POST /contexts/match
   POST /judgments
   ```

**Supported endpoints:**
- ✅ `/events` — Full CRUD
- ✅ `/contexts` — Context Templates (v1.0.0)
- ✅ `/contexts/by-name/{name}`
- ✅ `/contexts/match` — Template matching
- ✅ `/contexts/from-event` — Create template from event
- ✅ `/judgments` — Submit and list
- ✅ `/impacts` — Impact management
- ✅ `/knowledge-base` — Dynamic context loading

**Data models:**
- Desktop uses embedded context fields (v1.0.0)
- Android expects `context_id` (legacy format)

---

## 🧪 Testing

### Android Client

**Test types:**
- ✅ Unit tests (JUnit 4.13.2)
- ✅ Android Instrumentation tests (Espresso)
- ✅ Mock mode for offline testing
- ❌ Contract tests — **missing**
- ❌ Integration tests — **missing**
- ❌ E2E tests — **missing**

**Coverage:**
- Basic coverage of networking components
- MockTruthApi for serverless tests

### Desktop UI

**Test types:**
- ✅ Unit tests (Jest + React Testing Library)
- ✅ Contract tests (API contracts validation)
- ✅ Integration tests (create-event-flow, dashboard-flow)
- ✅ E2E tests (Playwright)
- ✅ Performance tests (navigation, pagination, memory)
- ✅ Offline queue tests

**Coverage:**
- Comprehensive automated suite
- CI/CD integration with automatic runs
- Performance benchmarks (<200ms navigation, <100ms pagination)

---

## 🚀 CI/CD and build

### Android Client

**CI workflow:** `.github/workflows/android-build.yml`
- ✅ Runs after Cross-Platform Build
- ✅ Builds Rust core for Android targets
- ✅ Android SDK/NDK setup
- ✅ Gradle APK build
- ✅ Artifact upload
- ⚠️ **Partial coverage:** No automated releases

**Build flavors:**
- `local` — `http://10.0.2.2:8080` (emulator)
- `remote` — `https://truth-core.example.com`
- `mock` — Mock API endpoints

**Artifacts:**
- APKs for arm64-v8a and x86_64
- `libtruth_core.so` (from Cross-Platform workflow)

### Desktop UI

**CI workflow:** `.github/workflows/desktop.yml`
- ✅ Fully automated build
- ✅ Linux (DEB, AppImage), Windows (EXE, MSI), macOS (DMG)
- ✅ Tauri build for all platforms
- ✅ Artifact upload and release publishing
- ✅ Automatic releases on tags

**Artifacts:**
- `Truth Training_1.0.0_amd64.deb`
- `Truth Training_1.0.0_amd64.AppImage`
- `Truth Training_1.0.0_x64-setup.exe`
- `Truth Training_1.0.0_x64.dmg`
- `Truth Training_1.0.0_x64_en-US.msi`

---

## ⚠️ Critical differences and issues

### 1. Version incompatibility

**Issue:** Android v0.3.0 does not support the v1.0.0 API:
- ❌ Context Templates endpoints missing
- ❌ Embedded context fields not implemented
- ❌ Template matching unavailable
- ❌ New validation rules ignored

**Resolution:** Upgrade Android to v1.0.0 API.

### 2. Missing features on Android

**Not implemented on Android:**
- Event management (events CRUD)
- Context Templates system
- Judgments and Consensus
- Offline-first architecture
- Local persistence (SharedPreferences only for tokens)
- Expert system UI
- Template matching

### 3. Architectural differences

| Aspect | Android | Desktop |
|--------|---------|---------|
| **State management** | ViewModel + LiveData | Zustand |
| **Local storage** | SharedPreferences (tokens) | SQLite (full DB) |
| **Offline strategy** | None | Local-wins + queue |
| **P2P** | NSD + LAN messaging | HTTP P2P sync |
| **Crypto** | Android Keystore + BouncyCastle | Rust core |

### 4. API compatibility

**Android supports:**
- `/api/v1/auth`, `/api/v1/info`, `/api/v1/stats`, `/graph/json`
- Legacy format (expects `context_id`)

**Desktop supports:**
- All v1.0.0 endpoints
- Embedded fields (`category_id`, `forma_id`, etc.)
- Context Templates endpoints

---

## 📋 Synchronization recommendations

### Priority 1: Upgrade to v1.0.0

1. **Update version:**
   ```kotlin
   versionName = "1.0.0"  // in build.gradle.kts
   ```

2. **Add Context Templates API:**
   - Implement `GET /contexts`, `POST /contexts`
   - Add `match_context` endpoint
   - Update DTOs for embedded fields

3. **Update event models:**
   - Remove `context_id`
   - Add `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`

### Priority 2: Add missing features

1. **Events management:**
   - Event create/edit screen
   - Event list with pagination
   - Template selection UI

2. **Context Editor:**
   - Template creation screen
   - Template matching display
   - Duplicate detection UI

3. **Offline support:**
   - Local SQLite storage
   - Offline queue
   - Sync status indicator

### Priority 3: Improve testing

1. Contract tests for all endpoints
2. Integration tests for workflows
3. E2E tests with the real server

---

## 📈 Maturity metrics

| Metric | Android | Desktop |
|--------|---------|---------|
| **Version** | 0.3.0 (60%) | 1.0.0 (100%) |
| **Functionality** | 30% | 100% |
| **Testing** | 20% | 95% |
| **CI/CD** | 50% | 100% |
| **Documentation** | 40% | 90% |
| **API compatibility** | 40% | 100% |

**Overall maturity:**
- Android: **~40%** (early development)
- Desktop: **~97%** (production-ready)

---

## 🔗 Documentation references

### Android
- [truth-android-client/README.md](truth-android-client/README.md)
- [truth-android-client/README_ANDROID.md](truth-android-client/README_ANDROID.md)
- [integration/android/README_INTEGRATION.md](integration/android/README_INTEGRATION.md)
- [integration/android/README_BUILD_ANDROID.md](integration/android/README_BUILD_ANDROID.md)
- [docs/ANDROID_MIGRATION.md](docs/ANDROID_MIGRATION.md)
- [docs/ANDROID_TEST_FIX_SUGGESTIONS.md](docs/ANDROID_TEST_FIX_SUGGESTIONS.md)\
- [docs/TEST_REPORT_ANDROID_v1.0.0.md](docs/TEST_REPORT_ANDROID_v1.0.0.md)

### Desktop UI
- [docs/UI_Desktop.md](docs/UI_Desktop.md)
- [ui/desktop/CHANGELOG.md](ui/desktop/CHANGELOG.md)
- `specs/002-ui-desktop-integration/`
- `specs/003-truth-training-desktop/`
- **API Reference:** [docs/api_reference/API_REFERENCE.md](docs/api_reference/API_REFERENCE.md)
- **Version Registry:** [docs/VERSION_REGISTRY.md](docs/VERSION_REGISTRY.md)

---

**Conclusion:** Desktop UI is a production-ready application with full v1.0.0 feature coverage, while Android remains at an early stage and needs substantial work to reach feature parity.

