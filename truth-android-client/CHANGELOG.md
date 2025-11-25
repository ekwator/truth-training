# Android Client Changelog

## [1.0.0] — First Stable Release — Full Feature Parity with Desktop v1.0.0

**Release Date**: 2025-11-02  
**Status**: Stable Production Release

### 🎉 Highlights

- ✅ **Full Feature Parity**: Complete alignment with Desktop UI v1.0.0 functionality
- ✅ **Offline-First Architecture**: All operations work offline with automatic background sync
- ✅ **Room Database**: Modern SQLite persistence layer with Flow-based reactive updates
- ✅ **Jetpack Compose UI**: Complete UI rewrite using modern Material 3 design
- ✅ **96% Test Coverage**: Comprehensive test suite with unit, integration, and performance tests
- ✅ **Performance Targets Met**: All benchmarks under target thresholds

### 🚀 Major Features

#### Core Infrastructure
- **Room Database Integration**
  - Full SQLite persistence with Room 2.6.1
  - 6 entity models: Event, ContextTemplate, Judgment, Impact, Summary, SyncQueue
  - Reactive Flow-based data queries for real-time UI updates
  - Database indices optimized for pagination and filtering

- **Offline-First Architecture**
  - All CRUD operations work offline with local-first approach
  - SyncQueueManager for tracking pending synchronization operations
  - Background sync via WorkManager (every 15 minutes)
  - Local-wins conflict resolution strategy
  - Network state detection and automatic sync triggering

#### User Interface
- **Jetpack Compose Screens**
  - EventListScreen: Modern list view with embedded context fields
  - EventCreateScreen: Form-based event creation with template selection
  - EventEditScreen: Full event editing capabilities
  - EventDetailScreen: Comprehensive event details with judgments and impacts
  - ContextTemplateEditorScreen: Create and edit context templates
  - ContextTemplateListScreen: Browse and manage templates
  - JudgmentSubmissionScreen: Submit ternary judgments (true/false/uncertain)
  - DashboardScreen: Sync status and quick access to features

- **Material 3 Design**
  - Modern Material Design 3 components
  - Dark theme support
  - Responsive layouts optimized for mobile devices
  - Smooth animations and transitions

#### Context Template System
- **Template Management**
  - Full CRUD operations for context templates
  - NULL-aware duplicate detection (compares only non-NULL fields)
  - Template matching for event creation
  - Template selection auto-prefills event form fields
  - "[Create Template]" option for unmatched events

- **Embedded Context Fields**
  - Events store context fields directly (`category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`)
  - No `context_id` foreign key (aligned with Desktop v1.0.0)
  - Improved query performance without JOINs

#### Judgments & Consensus
- **Judgment Submission**
  - Submit ternary judgments (confirm/reject/abstain)
  - Confidence level (0.0-1.0) and reasoning text
  - Real-time statistics (true_count, false_count, uncertain_count, avg_confidence)
  - Consensus calculation matching Desktop logic

#### Synchronization
- **Server Sync**
  - Bi-directional synchronization with Desktop v1.0.0 API
  - Automatic background sync every 15 minutes
  - Manual sync trigger support
  - Exponential backoff retry policy (3 max retries)

- **P2P Synchronization**
  - Direct encrypted event propagation between Android clients
  - NSD (Network Service Discovery) for peer discovery
  - Ed25519 signature verification for message integrity
  - Encrypted P2P message handling

#### Testing & Quality
- **Unit Tests** (≥95% coverage achieved)
  - DAO tests: EventDao, ContextTemplateDao, JudgmentDao
  - Repository tests: EventRepository, ContextTemplateRepository
  - SyncQueueManager tests: Queue operations, retry logic, conflict resolution

- **Integration Tests**
  - Scenario 1: Event creation with context template
  - Scenario 2: Context template duplicate detection
  - Scenario 3: Judgment submission and consensus
  - Scenario 4: Offline-first operation
  - Scenario 5: Template matching
  - Scenario 6: Cross-platform data consistency

- **Performance Tests** (All targets met)
  - Room queries: Pagination (< 50ms), single entity (< 10ms), bulk insert (< 100ms)
  - UI response: Screen rendering (< 200ms), data loading (< 500ms), interactions (< 100ms)

#### API Compatibility
- **v1.0.0 API Endpoints**
  - `/api/v1/auth` - Authentication
  - `/api/v1/info` - Server info
  - `/api/v1/events` - Event CRUD operations
  - `/api/v1/contexts` - Context template management
  - `/api/v1/judgments` - Judgment submission and statistics
  - `/api/v1/impacts` - Impact management
  - `/graph/json` - Graph data export

### 📦 Technical Details

- **Kotlin**: 2.0.20
- **Android SDK**: minSdk 26, targetSdk 33, compileSdk 35
- **Room**: 2.6.1
- **WorkManager**: 2.9.0
- **Jetpack Compose**: BOM 2024.02.00
- **Retrofit**: 2.11.0 (HTTP API client)
- **Testing**: JUnit 4.13.2, Espresso 3.6.1, MockWebServer

### 🔄 Migration from v0.3.0

**Breaking Changes**:
- Database schema changed (Room migration from v0.3.0)
- API endpoints updated to v1.0.0 format (embedded context fields)
- UI completely rewritten in Jetpack Compose

**Migration Steps**:
1. Uninstall previous v0.3.0 app (data will be lost)
2. Install v1.0.0 from release artifacts
3. Re-authenticate with server
4. Initial sync will download all data from server

**Note**: Local database from v0.3.0 is not compatible with v1.0.0 schema. Data must be re-synced from server.

### 📊 Test Results

- **Unit Test Coverage**: 96.3% (target: ≥95%) ✅
- **Integration Tests**: 6/6 passing ✅
- **Performance Benchmarks**: All targets met ✅
- **CI/CD Pipeline**: Fully functional ✅

See `docs/TEST_REPORT_ANDROID_v1.0.0.md` for detailed test results.

### 📚 Documentation

- **Migration Guide**: `docs/ANDROID_MIGRATION.md`
- **Feature Comparison**: `docs/Truth-training/Truth-training.md`
- **Test Report**: `docs/TEST_REPORT_ANDROID_v1.0.0.md`
- **CI/CD Workflows**: `docs/CI_Workflows_Artifacts.md`

### 🐛 Known Issues

None. All identified issues have been resolved.

### 🙏 Acknowledgments

This release represents a complete rewrite of the Android client to achieve feature parity with Desktop v1.0.0. Special thanks to the contributors who made this cross-platform alignment possible.

---

## [0.3.0-pre] — Previous Pre-Release

- Initial Android client implementation
- Basic event viewing and creation
- P2P discovery via NSD
- JWT authentication
- SharedPreferences-based storage

**Note**: v0.3.0 is not compatible with v1.0.0. Upgrade requires clean install.

