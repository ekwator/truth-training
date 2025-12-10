# Changelog

## [1.0.0-Prot] — Prototype Release for Truth Discovery and Collective Intelligence

**Release Date**: 2025-01-XX  
**Status**: Prototype Release

For a narrative overview of this release, see [`release-info.txt`](release-info.txt).

### 🎉 Feature Implementation: Impacts, Judgments, and Network Nodes

This release implements core user interaction features across both Android and Desktop platforms, enabling users to assess events, submit judgments, and monitor network infrastructure.

#### 📱 Android Client Enhancements

**Impacts**
- **Add Impact Screen**: Full implementation for adding impact assessments to events
  - Impact level selection (1-5 scale) with visual slider
  - Optional notes field for detailed observations
  - Integration with Event Summary screen
  - Full Russian localization support
- **Impact Display**: List view showing all impacts for an event with level indicators
- **Data Model**: `ImpactEntity` with Room database integration
- **API Integration**: RESTful API endpoints for impact submission and retrieval

**Judgments**
- **Submit Judgment Screen**: Complete implementation for ternary judgment submission
  - Assessment selection: Confirm (true), Reject (false), or Abstain (uncertain)
  - Confidence level slider (0.0-1.0) with percentage display
  - Optional reasoning field for detailed explanations
  - Form validation and error handling
  - Full Russian localization support
- **Judgment Display**: List view showing all judgments with color-coded assessment types
- **Data Model**: `JudgmentEntity` with Room database integration
- **API Integration**: RESTful API endpoints for judgment submission and retrieval

**Network Nodes**
- **Network Nodes Screen**: Complete node discovery and management interface
  - Full Russian localization (all strings translated)
  - Node type display (Hub/Leaf) using `NodeTypeMapper`
  - Reachability status indicators (Online/Offline)
  - TTL (Time To Live) display and expiration tracking
  - Manual actions: Refresh, Discover, Cleanup, Health Check
  - Filtering by type and reachability status
  - Navigation integration from Settings screen
- **Node Detail Screen**: Detailed view for individual nodes
  - Complete node information display (address, type, status, timestamps, TTL, source, node ID)
  - Age calculation and expiration tracking
  - Full Russian localization
  - Refresh and close actions

**Localization Improvements**
- **Complete Russian Translation**: All Network Nodes screen strings translated
  - Added entries to `strings.xml` and `strings-ru.xml`
  - Removed hardcoded English strings
  - Consistent terminology across all UI elements
- **String Resources**: Comprehensive localization for:
  - Impact-related strings (add_impact, impact_level, impact_notes, etc.)
  - Judgment-related strings (submit_judgment, assessment, confidence, reasoning, etc.)
  - Node-related strings (all_types, node_type_hub, node_type_leaf, node_status_reachable, etc.)

#### 🖥️ Desktop UI Enhancements

**Impacts Implementation**
- **AddImpactModal Component**: React modal dialog for adding impacts
  - Impact level slider (1-5) with visual feedback
  - Optional notes field
  - Form validation and error handling
  - Emoji support (constitutional requirement Rule 8)
  - Localization support (EN/RU)
  - Integration with Event Summary page
- **Impact Display**: Enhanced Event Summary page showing:
  - List of all impacts with level range display
  - Empty state messages
  - Automatic list updates after impact submission

**Judgments Implementation**
- **SubmitJudgmentModal Component**: React modal dialog for submitting judgments
  - Assessment selection (confirm/reject/abstain) with radio buttons
  - Confidence slider (0.0-1.0) with percentage display
  - Optional reasoning field
  - Form validation and error handling
  - Emoji support (constitutional requirement Rule 8)
  - Localization support (EN/RU)
  - Integration with Event Summary page
- **Judgment Display**: Enhanced Event Summary page showing:
  - List of all judgments with color-coded assessment types
  - Confidence percentage display
  - Empty state messages
  - Automatic list updates after judgment submission

**Network Nodes Implementation**
- **NodeDetailView Component**: React modal dialog for detailed node information
  - Complete node field display (address, type, status, timestamps, TTL, source, node ID)
  - `expires_in` and `age` calculations
  - Node type mapping using `NodeTypeMapper` utility
  - Refresh and close actions
  - Emoji support (constitutional requirement Rule 8)
  - Localization support (EN/RU)
- **NodesPanel Enhancement**: Made node rows clickable
  - Click handler integration
  - State management for selected node
  - Opens `NodeDetailView` modal on node click

**Utility Functions**
- **ImpactLevelMapper** (`ui/desktop/src/utils/impactLevelMapper.ts`): Utility for mapping impact levels (1-5) to boolean values and helper functions
- **NodeTypeMapper** (`ui/desktop/src/utils/nodeTypeMapper.ts`): Utility for mapping technical node types to user-friendly types (Hub/Leaf)

**Type Definitions**
- **Impact Types** (`ui/desktop/src/types/impacts.ts`): TypeScript interfaces for `Impact` and `AddImpactRequest`
- **NodeRecord Enhancement**: Added `created_at` field to align with data model

### 🧪 Testing & Quality Assurance

**Unit Tests**
- **ImpactLevelMapper Tests** (`ui/desktop/tests/unit/impactLevelMapper.test.ts`): Comprehensive test coverage for impact level mapping functions
- **NodeTypeMapper Tests** (`ui/desktop/tests/unit/nodeTypeMapper.test.ts`): Complete test coverage for node type mapping functions

**Component Tests**
- **AddImpactModal Tests** (`ui/desktop/tests/component/AddImpactModal.test.tsx`): Component rendering, form interaction, validation, and submission tests
- **Headless UI Mocks**: Added mocks for `@headlessui/react` components in `setupTests.ts` for proper Jest/JSDOM environment

**Jest Configuration**
- Updated `ui/desktop/jest.config.ts` to include `tests/unit/` and `tests/component/` directories in `testMatch`

### 🔧 Code Quality & Build Improvements

**Rust Compiler Warnings Fixed**
- **events.rs**: Fixed unused variable `limit` warning (prefixed with `_`)
- **knowledge_base.rs**: Added `#[allow(dead_code)]` attributes for reserved functions:
  - `create_temp_tables` (reserved for Phase 5, User Story 3)
  - `fill_temp_tables` (reserved for Phase 5, User Story 3)
  - `validate_temp_table_fks` (reserved for Phase 5, User Story 3)
  - `atomic_swap` (reserved for Phase 5, User Story 3)
- **Build Status**: All Rust compilation warnings resolved, clean build achieved

**CI/CD Workflow Enhancements**
- **Android Build Workflow** (`.github/workflows/android-build.yml`): Major improvements
  - **Universal Build**: All flavors (local, mock, remote) now build for both debug and release
  - **AAB & APK**: Both AAB and APK files are built for all flavors in release mode
  - **Tag-Independent**: Builds execute regardless of tags (removed tag-based conditions)
  - **Test Exclusion**: Tests are skipped for tag pushes (as requested)
  - **Release Artifacts**: All built files (debug and release, all flavors) are included in GitHub Release
  - **Tag Handling**: Improved tag name extraction for both push tags and release events

### 📚 Documentation Updates

**Quickstart Guides**
- **Android Quickstart** (`docs/quickstart_android.md`): Updated with detailed instructions for:
  - Adding Impacts: Step-by-step guide with screenshots descriptions
  - Submitting Judgments: Complete workflow documentation
  - Viewing Network Nodes: Navigation and usage instructions
- **Desktop Quickstart** (`docs/quickstart_desktop.md`): Updated with detailed instructions for:
  - Adding Impacts: Modal usage and form interaction
  - Submitting Judgments: Assessment selection and confidence setting
  - Viewing Network Nodes: Node detail view and panel interaction

### 🔐 Privacy and Confidentiality

**Core Principle Maintained**: No user actions are logged or persistently stored. The application does not track, record, or save any user interactions, navigation patterns, clicks, or behavioral data. This ensures complete privacy and anonymity.

**Key Privacy Guarantees:**
- ✅ **No User Action Logging**: No clicks, navigation, or interaction history is stored
- ✅ **No Persistent User Tracking**: No identifiers, session data, or behavioral analytics
- ✅ **No Telemetry Collection**: No user activity is transmitted or stored
- ✅ **Ephemeral Logs Only**: Only system-level logs (errors, sync operations) are temporarily stored for debugging purposes

This confidentiality principle is enforced across all platforms (Desktop UI, Android, Server, CLI) and is a core architectural requirement.

### 🎓 For Developers

This release demonstrates cross-platform feature parity between Android and Desktop implementations, with consistent user experience, data models, and API integration patterns. All new features follow the established architectural patterns and maintain privacy compliance.

**Technologies Used:**
- **Android**: Jetpack Compose, Room Database, Retrofit, Kotlin Coroutines
- **Desktop**: React 18, TypeScript, Headless UI, Zustand, Tauri
- **Testing**: Jest, React Testing Library, JUnit, Android Instrumentation Tests
- **Build Tools**: Gradle, npm/pnpm, Cargo

---

## [1.0.0-Develop] — Development Release for Learning and Practice

**Release Date**: 2025-01-XX  
**Status**: Development Release

This development release is based on v1.0.0 with enhanced localization support (partial), improved documentation, and refined CI/CD workflows. This version is specifically designed for developers learning to use Cursor AI IDE and modern cross-platform development practices.

For a narrative overview of this release, see [`release-info-v1_0_0-Develop.txt`](release-info-v1_0_0-Develop.txt).

**Important Notes:**
- **Privacy & Confidentiality**: No user actions are logged or persistently stored. Only system-level logs (errors, sync operations) are permitted. This is a core architectural requirement enforced across all platforms. See [SECURITY.md](SECURITY.md) for details.
- **Localization Status**: 
  - Desktop UI: **Localization not fully implemented** - Language toggle exists but does not persist to config.json, database is not seeded with selected language, interface remains in English. Full implementation requires Spec-Kit planning.
  - Android Client: English-only (EN). Russian localization not implemented. Full localization parity requires Spec-Kit planning.
- **Feature Implementation**: All future features and improvements must follow the Spec-Kit workflow (`/speckit.specify`, `/speckit.plan`, `/speckit.task`, `/speckit.implementation`) with detailed implementation plans. See [spec/15-prompts-and-automation.md](spec/15-prompts-and-automation.md) and [CONTRIBUTING.md](CONTRIBUTING.md) for requirements.

### 🌐 Localization Status

#### Desktop UI: Localization Not Fully Implemented ⚠️
- **Current Status**: Language toggle component exists but localization is **not functional**
- **Issues**:
  - Language selection is stored in application memory (localStorage) but **not persisted to config.json**
  - Database is **not seeded** with knowledge base data according to selected language
  - Interface **remains in English** regardless of language selection
  - **UI Issue**: On Settings screen, there is an **extra duplicate language selection field** in the bottom section (should be removed)
- **Translation Files**: Translation files (`ru.ts`) exist but are not applied to UI
- **Requirement**: Full localization implementation requires Spec-Kit workflow (`/speckit.specify`, `/speckit.plan`, `/speckit.task`, `/speckit.implementation`) with detailed plan to:
  - Fix config.json persistence
  - Implement database seeding on locale change
  - Apply translations to UI components
  - Remove duplicate language selection field from Settings screen

#### Android Client: English-Only (EN) ⚠️
- **Current Status**: English-only localization
- **Missing**: Russian translation file (`values-ru/strings.xml`)
- **Missing**: Language selector UI component
- **Missing**: Locale-aware knowledge base seeding integration
- **Requirement**: Full localization implementation requires Spec-Kit workflow with detailed plan

### 📚 Documentation Enhancements

#### Privacy & Confidentiality Documentation
- **Security Policy**: Updated [SECURITY.md](SECURITY.md) with explicit no-user-action-logging policy
- **Architectural Enforcement**: Database schemas do not include tables for user action logging
- **Platform-Wide Compliance**: Applied consistently across Desktop UI, Android, Server, and CLI
- **System Logging Only**: Only system-level logs (errors, sync operations) are permitted for debugging

#### Updated Functional Specifications
- **Core Specification**: Updated `spec/22-function_core.md` with locale-aware knowledge base seeding details and removed unused logging module
- **Desktop Specification**: Updated `spec/23-function_desktop.md` with complete localization implementation, removed legacy tables mention, removed logs functions
- **Android Specification**: Updated `spec/24-function_mobile_android.md` with localization status, screen parity with Desktop UI (7 screens), and implementation status warnings
- **All Specs**: Updated all spec files (01-24) to v1.0.0 with 2025-01-XX dates

#### CLI Tool Documentation
- **Build Workflow**: New `cli-build.yml` workflow for `truthctl` binary builds (Linux, Windows, macOS)
- **Artifact Documentation**: Expanded artifact descriptions in `docs/Truth-training/Truth-training.md`
- **CLI Description**: Complete CLI tool description with capabilities and limitations

### 🔧 CI/CD Improvements

#### Tauri CLI Build Fixes
- **Installation Method**: Fixed Tauri CLI installation from `cargo install` to `npm install -g @tauri-apps/cli@2.9.0` to resolve flate2 compilation errors
- **Build Commands**: Updated all `cargo tauri build` commands to `tauri build` after npm installation
- **Cross-Platform**: Verified builds on Linux, Windows, and macOS

#### CLI Build Workflow
- **Package Targeting**: Corrected CLI build workflow to target `app` package for `truthctl` binary (`-p app --bin truthctl`)
- **Feature Flags**: Enabled `p2p-client-sync` features for CLI builds
- **Artifact Publishing**: Automated artifact upload and release publishing

#### Workflow Optimizations
- **Branch Filtering**: Workflows no longer run on `push` events to `main` branch (still run for other branches and PRs)
- **Conditional Execution**: Added `if` conditions to prevent unnecessary workflow runs
- **Cross-Platform Artifacts**: Improved artifact generation and publishing across all platforms

### ⚠️ Known Limitations & Future Work

#### Desktop UI
- **Localization**: Not fully implemented - Language toggle exists but is not functional
  - **Issues**: 
    - Language selection not persisted to `config.json`
    - Database not seeded with knowledge base data according to selected language
    - Interface remains in English regardless of language selection
    - **UI Issue**: On Settings screen, there is an **extra duplicate language selection field** in the bottom section (should be removed)
  - **Translation Files**: Translation files (`ru.ts`) exist but are not applied to UI
  - **Requirement**: Full localization implementation requires Spec-Kit workflow (`/speckit.specify`, `/speckit.plan`, `/speckit.task`, `/speckit.implementation`) with detailed plan to fix config persistence, database seeding, UI translation application, and remove duplicate language field

#### Android Client
- **Localization**: English-only (EN) - Russian localization not implemented
  - Missing: `values-ru/strings.xml` translation file
  - Missing: Language selector UI component
  - Missing: Locale-aware knowledge base seeding integration
  - **Requirement**: Full localization implementation requires Spec-Kit workflow with detailed plan
- **Screen Coverage**: Not all activities/screens are fully implemented compared to Desktop UI
  - Current: NodesScreen fully integrated; other screens (Dashboard, New Event, Context Editor, Event Summary, Overall Summary, Training Results, Settings) exist but require navigation integration
  - Target: 7 screens matching Desktop UI (Logs screen removed for privacy compliance)
  - **Requirement**: Screen integration requires Spec-Kit workflow with detailed implementation plan
- **Recommendation**: Use Spec-Kit (`/speckit.specify`, `/speckit.plan`, `/speckit.task`, `/speckit.implementation`) for all future Android improvements

#### iOS Client
- **Status**: Basic project structure exists, but full implementation is required
- **Missing**: Complete UI implementation, feature parity with Desktop/Android
- **Missing**: Localization support
- **Requirement**: Full iOS client development requires Spec-Kit workflow with detailed implementation plan
- **Recommendation**: Use Spec-Kit for all iOS development to ensure consistency with Desktop/Android

#### CLI Tool (`truthctl`)
- **Status**: Core functionality implemented (node management, sync, discovery)
- **Limitation**: Direct CLI commands for events and context templates are planned for future releases
- **Current Workaround**: Use HTTP API endpoints (`/api/v1/events`, `/api/v1/contexts`) or Desktop UI
- **Requirement**: Future CLI enhancements require Spec-Kit workflow with detailed plan

### 📊 Platform Comparison

- **Desktop UI**: Most complete implementation with **localization not fully functional** (language toggle exists but not working), all 7 screens (Logs screen removed for privacy), comprehensive testing, and production-ready features
- **Android Client**: Partial implementation with EN-only localization, NodesScreen fully integrated, other screens require navigation integration. Full localization and screen completion require Spec-Kit planning
- **iOS Client**: Project structure only, requires full implementation via Spec-Kit workflow
- **Core Library**: Full feature support with locale-aware knowledge base seeding, no user action logging
- **CLI Tool**: Functional for node management and sync, with planned enhancements requiring Spec-Kit workflow

### 🧪 Quality & Validation

- **Specification Updates**: Functional specifications reflect current localization implementation and privacy requirements
- **Documentation Integrity**: Comprehensive documentation updated to v1.0.0, all spec files aligned
- **Privacy Compliance**: No user action logging enforced architecturally across all platforms
- **Code Quality**: Removed unused `logging.rs` module (dead code violation), fixed clippy warnings
- **CI/CD Validation**: All workflows validated and optimized
- **Cross-Platform Compatibility**: Verified across Desktop, Android, Core, and CLI
- **Build Verification**: All components compile successfully after code cleanup

### 🎓 For Beginner Developers

This release is created for beginner developers to practice using Cursor AI IDE. When studying this project with Cursor AI IDE, you can receive hints and guidance on both implementing this project and independently developing additional features. You can learn the technologies and programming languages used in the project, and Cursor AI IDE can autonomously make changes to the project.

**Technologies Used:**
- **Backend Core**: Rust, Actix-web, Tokio, rusqlite, ed25519-dalek, serde, clap
- **Desktop UI**: TypeScript, JavaScript, React 18, Vite, Zustand, Axios, Tailwind CSS, Headless UI, Tauri 2.9.0
- **Android Client**: Kotlin 2.0.20, Android SDK, Jetpack Compose, Room, Retrofit, OkHttp, WorkManager, Kotlin Coroutines, BouncyCastle, Gradle
- **iOS Client**: Swift, SwiftUI (planned), Rust FFI bindings
- **Testing**: Jest, React Testing Library, Playwright, JUnit, Android Instrumentation Tests
- **Build Tools**: Cargo, npm/pnpm, Gradle, Xcode
- **CI/CD**: GitHub Actions, Python 3.11
- **Database**: SQLite 3.x
- **Cryptography**: Ed25519 signatures, ChaCha20-Poly1305 encryption

---

## [1.0.0] — Unified Discovery & Sync Release — Network of Anonymous Trust, fully realized

This tag represents the **first and only GA release** of Truth Training where the entire anonymous-trust stack lands together: Rust core, truthctl CLI, desktop UI, Android client, automation, and specs, all aligned on `v1.0.0`.  
For a narrative overview of this release, see [`release-info-v1_0_0.txt`](release-info-v1_0_0.txt); the sections below break down the same milestone by subsystem and historical sub-tags.

## [1.0.0-Release] — Unified Discovery & Sync Release

**Release Date**: 2025-01-XX  
**Status**: Stable Production Release

### 🎉 Cross-Platform Discovery Milestone

This release delivers a fully unified cross-platform node discovery and synchronization system across Rust Desktop (Tauri), CLI (truthctl), Android, and Server platforms. All components now share identical discovery protocols, JSON schemas, SQLite database schemas, and synchronization semantics.

### 🔍 Unified Node Discovery System

#### LAN/Wi-Fi UDP Multicast Discovery
- **Standard Multicast Address**: 239.255.0.1:52525 across all platforms
- **Unified Packet Format**: Identical JSON structure (LanAnnouncement) with ed25519 signature verification
- **Cross-Platform Compatibility**: Verified packet exchange between Desktop (Rust) ↔ Android (Kotlin)
- **Background Workers**: 
  - Desktop: Tokio interval-based worker in `ui/desktop/src-tauri/src/discovery.rs`
  - Android: WorkManager periodic sync every 15 minutes
  - Server: Background schedulers with configurable intervals

#### Global Registry Polling
- **HTTPS Polling**: Consistent JSON payload formats across all platforms
- **Flexible Response Formats**: Supports both envelope (`{nodes: [...]}`) and array formats
- **Error Handling**: Graceful handling of empty/malformed registry URLs
- **Configurable URLs**: Registry URL configuration with default fallback values

#### Node Reachability & TTL Cleanup
- **HTTP Reachability Checks**: Unified timeout and retry logic across all platforms
- **TTL-Based Cleanup**: Automatic removal of stale/unreachable nodes
- **Consistent TTL Rules**: Same defaults and cleanup logic across Desktop, CLI, Server, and Android
- **Deterministic Merge Rules**: Node deduplication (Local > Global, then last_seen, then lexicographic address)

### 📱 Android Integration

#### Room Database Integration
- **NodeEntity & NodeDao**: Complete Room database integration with canonical SQLite schema
- **Migration MIGRATION_2_3**: Creates `nodes` table matching Desktop/CLI schema
- **Full CRUD Operations**: Upsert, TTL queries, and cleanup operations
- **Android Instrumentation Tests**: NodeDaoTest.kt with comprehensive test coverage

#### Discovery Repository
- **High-Level Operations**: Global registry polling (HTTP GET) and UDP/LAN discovery integration
- **TTL Logic & Deduplication**: Consistent with Desktop/CLI implementations
- **JSON Format Conversion**: Helpers for cross-platform compatibility
- **Cross-Platform Tests**: Compatibility tests (Desktop ↔ Android format conversion)

#### WorkManager Background Sync
- **NodeSyncWorker**: Periodic sync every 15 minutes
- **TTL Decrement & Cleanup**: Automatic node lifecycle management
- **Reachability Checks**: HTTP health checks for discovered nodes
- **Database Updates**: Updates via DiscoveryRepository
- **Worker Lifecycle Tests**: NodeSyncWorkerTest.kt

#### UDP Multicast LAN Discovery Client
- **LanDiscoveryClient**: Standard multicast address (239.255.0.1:52525)
- **Node Broadcasting**: Android node identity/address broadcasting
- **Listener Implementation**: Receives announcements from other nodes
- **Payload Conversion**: Converts to NodeRecord format
- **Signature Verification**: ed25519 via Ed25519CryptoManager

#### Compose UI Integration
- **NodesScreen.kt**: Complete node list UI with filters and actions
- **NodesViewModel.kt**: State management for discovery operations
- **Manual Actions**: Refresh, discover, cleanup, and health check buttons
- **Navigation Integration**: Integrated into MainNavigation.kt

### 🖥️ Desktop Integration

#### Background Discovery Worker
- **DiscoveryManager**: Lifecycle management (start/stop/restart) with clean shutdown
- **Tokio Integration**: Interval-based worker in `ui/desktop/src-tauri/src/discovery.rs`
- **Drop Trait**: Clean shutdown implementation
- **Core Bridge**: Integration with core discovery functions

#### React UI (NodesPanel)
- **Node List Display**: Reachability badges and TTL countdown indicators
- **Manual Actions**: Refresh, discover, cleanup, and health check buttons
- **Tauri Commands**: Integration with Tauri backend commands

#### Settings Persistence
- **Settings Storage**: Discovery settings in `ui/desktop/src-tauri/src/settings.rs`
- **Configurable Options**: Interval, TTL, and registry URL configuration
- **Settings UI**: Integration into Dashboard and Settings pages
- **Default Values**: Fallback values for out-of-the-box operation

### 💻 CLI Integration

#### Complete Command Suite
- **`truthctl nodes list`**: Table and JSON output formats with filtering (type, reachability, source)
- **`truthctl nodes add/remove`**: Manual node addition with validation, removal by address or node_id
- **`truthctl nodes discover`**: Local (LAN/Wi-Fi) and global registry discovery with type filtering
- **`truthctl nodes sync`**: Incremental sync with peer servers, bidirectional sync support
- **`truthctl nodes cleanup`**: TTL-based node pruning
- **`truthctl nodes health-check`**: Reachability verification
- **`truthctl nodes validate`**: Schema parity verification and migration status check

### 🔄 Synchronization System

#### Peer-to-Peer Sync
- **Incremental Sync**: Via `/api/v1/nodes/sync` endpoint
- **Bidirectional Sync**: Conflict resolution with deterministic merge rules
- **Manual Triggers**: Sync initiation via CLI and UI
- **Sync Statistics**: Detailed statistics and error handling

### 🧪 Testing & Quality Assurance

#### Comprehensive Test Coverage
- **CLI E2E Tests**: Complete test suite in `tests/cli_sync.rs` (list/discover/sync/cleanup/validate commands)
- **TTL Consistency Tests**: `tests/integration/test_ttl_consistency.rs` (TTL defaults, minimum enforcement, cleanup rules)
- **JSON Enum Serialization Tests**: `tests/integration/test_json_enum_serialization.rs` (NodeType/NodeSource encoding)
- **Android Instrumentation Tests**: NodeDaoTest, NodeDiscoveryTest, NodeSyncWorkerTest
- **Cross-Platform Compatibility Tests**: Desktop ↔ Android format conversion verification

#### Real-Device E2E Testing
- **Cross-Device Tests**: Linux Desktop ↔ Android device, CLI ↔ Android ↔ Desktop
- **Device Testing**: All 107 Android instrumentation tests pass on real device (RMX3261 - 11)
- **UDP Multicast Compatibility**: Verified via `tests/integration/test_udp_multicast_compatibility.rs`
- **Packet Roundtrip Tests**: Full packet format compatibility confirmed between Rust and Kotlin

#### Code Quality
- **Zero Compiler Warnings**: All Rust modules clean
- **Structured Logging**: Discovery event counters (nodes discovered, updated, pruned)
- **Observability**: TTL cleanup statistics, reachability check results, global registry polling metrics

### 📚 Documentation

- **Architecture Documentation**: [docs/android_discovery_architecture.md](docs/android_discovery_architecture.md) (navigation and device testing)
- **Compatibility Documentation**: [docs/cross_platform_discovery_compatibility.md](docs/cross_platform_discovery_compatibility.md) (full implementation status)
- **Post-Integration Hardening**: [docs/post_integration_hardening.md](docs/post_integration_hardening.md) (complete hardening phase)
- **CLI Usage**: [docs/CLI_Usage.md](docs/CLI_Usage.md) (command reference)
- **README Updates**: Added "Running Tests" section with all platform instructions

### 🔧 Bug Fixes & Improvements

- **Android Navigation**: Fixed integration (NodesScreen now accessible via navigation)
- **Test Feature Flags**: Fixed `lan_announcement_roundtrip` test feature flag gating
- **Compiler Warnings**: Fixed unused variable warnings in `src/p2p/node.rs`
- **Error Handling**: Improved handling for empty/malformed registry URLs
- **Logging**: Enhanced structured logging across all discovery components

### 📦 Version Information

All components aligned to v1.0.0:
- `core_lib`: v1.0.0
- `truth_core`: v1.0.0
- `app` (truthctl): v1.0.0
- `truth-ui-desktop`: v1.0.0
- `truth-android-client`: v1.0.0

### 🔗 References

- **Cross-Platform Compatibility**: [docs/cross_platform_discovery_compatibility.md](docs/cross_platform_discovery_compatibility.md)
- **Android Architecture**: [docs/android_discovery_architecture.md](docs/android_discovery_architecture.md)
- **Post-Integration Hardening**: [docs/post_integration_hardening.md](docs/post_integration_hardening.md)
- **CLI Usage**: [docs/CLI_Usage.md](docs/CLI_Usage.md)

---

## [1.0.0] — First Stable Baseline — Cross-Platform Unified Release

**Release Date**: 2025-11-02  
**Status**: Stable Production Release

### 🎉 Cross-Platform Milestone

This release represents the first unified stable version across all platforms:
- ✅ **Core/Server/CLI**: v1.0.0 (stable)
- ✅ **Desktop UI**: v1.0.0 (stable)
- ✅ **Android Client**: v1.0.0 (stable) — **NEW**

### 📱 Android Client v1.0.0 (New)

The Android client has been completely rewritten to achieve full feature parity with Desktop v1.0.0:

- **Offline-First Architecture**: Room database with automatic background sync
- **Jetpack Compose UI**: Modern Material 3 design with 9 complete screens
- **96% Test Coverage**: Comprehensive unit, integration, and performance tests
- **Performance Targets Met**: All benchmarks under thresholds
- **Full Feature Parity**: Events, Context Templates, Judgments, Impacts, P2P sync

See [truth-android-client/CHANGELOG.md](truth-android-client/CHANGELOG.md) for detailed Android-specific changes.

### 🖥️ Desktop UI & Core (v1.0.0)

## [1.0.0-desktop] — Desktop UI — Context Fields Embedded
- **Breaking Change**: Removed `context_id` foreign key from events; embedded context fields directly in events
- **Data Model Refactor**:
  - Events now store `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id` directly (nullable FK references)
  - Removed dependency on `context_id` foreign key lookup
  - Improved query performance (no JOINs required for context information)
- **Context Template System**:
  - New Context Editor UI screen for template management
  - Template selection auto-prefills event form fields
  - NULL-aware duplicate detection (compares only non-NULL fields)
  - NULL-aware template matching for display (consistent with duplicate detection)
  - "[Create Template]" button for events without matching templates
- **API Enhancements**:
  - Updated `POST /events` accepts embedded fields, rejects `context_id`
  - New `GET /contexts` endpoint for listing templates
  - New `POST /contexts` endpoint for creating templates with duplicate detection
  - New `GET /contexts/by-name/{name}` endpoint
  - New `POST /contexts/match` endpoint for template matching
  - New `POST /contexts/from-event` endpoint for creating templates from events
- **Validation & Data Integrity**:
  - Foreign key validation rejects invalid references immediately (400 error)
  - Duplicate template detection prevents creation (409 Conflict)
  - All FK references validated before persistence
- **UI Improvements**:
  - Context template dropdown selector on NewEvent page
  - Field prefilling from templates (modifiable before save)
  - Context name display in event list when template matched
  - New ContextEditor page for template creation and editing
- **Migration Notes**:
  - **BREAKING**: Manual database migration required (no automatic migrations)
  - Existing events with `context_id` must be migrated manually
  - Database schema changes: remove `context_id`, add five embedded fields
- **Version Bump**: All crates (core_lib, truth_core, app, desktop UI) bumped to v1.0.0

## [0.5.0] — Constitution Compliance (v2.1.0) — Truth Without Author
- **Constitution Alignment**: Full compliance with v2.1.0 principles implemented
- **Anonymous Confession Mode**: 
  - Plaintext-at-rest storage policy with UI warnings
  - CLI `confess` subcommand for anonymous event submission
  - Desktop UI toggle for confession mode with risk disclosure banner
- **Truth Without Author**:
  - Removed author metadata from `TruthEvent` model (legacy fields deprecated)
  - Transport envelope-based validation via `X-Public-Key`, `X-Signature`, `X-Timestamp` headers
  - Envelope signature verification middleware with anti-replay protection
- **Ternary Judgments**:
  - Updated judgment model to support `confirm`/`reject`/`abstain` signals
  - API endpoint `/api/v1/judgments` for ternary assessment submission
  - UI filter and statistics for judgment recognition types
- **Independent Confirmations**:
  - Anti-fraud enforcement via distinct envelope sender nodes
  - Node reputation tracking based on historical accuracy
  - Status weight and decay score computation for consensus
- **Wi‑Fi Direct / LAN-Based Nearby Sync**:
  - UDP broadcast discovery on port 35878 for local mesh nodes
  - Bidirectional HTTP sync with discovered peers
  - CLI flags `--nearby-sync` and `--nearby-interval-ms` for runtime control
  - Desktop UI settings panel for nearby sync configuration
  - Server-side persisted configuration that survives restarts
  - Dynamic start/stop via API endpoints `/api/v1/nearby_sync/start` and `/api/v1/nearby_sync/stop`
- **Core Library Enhancements**:
  - Weight computation (`status_weight`) and decay scoring (`decay_score`) algorithms
  - `NodeReputation` model for distributed trust tracking
  - `SyncEnvelope` model for P2P transport verification
- **CLI Improvements**:
  - `truthctl confess` command for anonymous confessions with plaintext warning
  - `truthctl judge` command for ternary judgment submission
- **Desktop UI Updates**:
  - Confession mode toggle on NewEvent page with plaintext-at-rest warning banner
  - "Confession" badge indicator on event cards
  - Recognition filter on Events page (confirm/reject/abstain)
  - Settings page controls for nearby sync (enable/disable, broadcast interval)
  - Ternary judgment assessment types throughout UI
- **API Enhancements**:
  - Anonymous event submission via `POST /events` (author fields forbidden)
  - Schema hardening with `#[serde(deny_unknown_fields)]` on requests
  - Configuration endpoints for nearby sync settings
- **Testing & Documentation**:
  - Contract tests for `/events` and `/judgments` endpoints
  - Integration tests for quickstart compliance scenarios
  - Constitution compliance documentation ([docs/Constitution-Compliance.md](docs/Constitution-Compliance.md))
- **Cross-Platform Fixes**:
  - Conditional compilation for `get_if_addrs` (excluded on Windows and Android)
  - Windows fallback for local IP detection
  - Android build compatibility improvements
  - Thread safety fixes for SQLite connection sharing across async tasks

## [0.4.2] — Desktop Integration & Cross-Platform Builds
- **Desktop UI**: Complete Tauri integration with React frontend
- **Cross-Platform Builds**: Production-ready packages for Linux (DEB/RPM), Windows (NSIS/MSI), macOS (DMG)
- **CI/CD Pipeline**: Automated builds for desktop, Android, and iOS platforms
- **Icon System**: Dynamic icon generation for all platforms (PNG/ICO formats)
- **Build System**: Comprehensive troubleshooting documentation and dependency management
- **Mobile Integration**: Android and iOS client frameworks with Rust FFI bridges
- **Documentation**: Complete build instructions and troubleshooting guides
- **Version Management**: Unified versioning across all components

## [0.4.0-pre] — Collective Intelligence Layer
- Introduced Semantic Correlation Layer for contextual claim evaluation
- Enhanced API with semantic scoring and trust correlation
- Integrated offline reliability module for P2P nodes
- Refactored app-core boundaries for future modular UI integration
- Implemented continuous integration workflows with test suites
- Added comprehensive desktop UI integration (Tauri/React)
- Production-ready builds for Linux DEB/RPM packages
- Enhanced security model with Ed25519 signature verification

## [0.3.0] — Core Stabilization & Crypto Verification
- Unified crypto engine with message signing and key verification
- Improved P2P sync consistency with verified headers
- Enhanced truthctl diagnostics and structured logging
- Expanded test coverage for distributed trust metrics
- Documentation alignment (/spec/, /docs/)
- Strengthened security model with cryptographic verification
- Improved error handling and resilience

## [0.2.7-pre] — Local Peer Analytics & Sync History
- Added peer_history table and metrics
- Added /api/v1/network/local endpoint
- Added CLI commands: peers stats, peers history
- Updated Spec Kit traceability and documentation
- Enables decentralized peer diagnostics without global scoring

## [0.2.3-pre] — Distributed Trust Propagation
- Distributed trust propagation in P2P sync:
  - Blend local/remote trust (0.8/0.2), time decay after 7 days
  - Trust deltas in SyncResult; logs show propagated changes
  - CLI: `truthctl ratings trust [--verbose]`, enhanced peers sync-all output
- Docs/spec updated: CLI usage, event rating protocol, architecture
- Version bump across workspace

## [0.2.1-pre] — CLI Tool & Architecture Separation
- New CLI tool: `truthctl` (subcommands: sync, verify, ratings, status)
- Clean separation: `core` ↔ `app` ↔ P2P node; `truthctl` moved to `app/`
- Architecture docs updated ([docs/architecture.md](docs/architecture.md), [spec/03-architecture.md](spec/03-architecture.md), [spec/11-decision-log.md](spec/11-decision-log.md))
- Feature-gated P2P sync (`p2p-client-sync`)

## [0.2.0] — Ratings System & Verified P2P Sync
- Ratings system: `node_ratings` and `group_ratings` with merge/conflict resolution
- Graph API: `/graph/json` (filtered) and `/graph/summary` (aggregated)
- Verified P2P sync: headers (`X-Public-Key`, `X-Signature`, `X-Timestamp`, `X-Ratings-Hash`), reconciliation, hash check
- Docs and Spec Kit aligned; tests green (`cargo check`, `clippy`, `test`)
