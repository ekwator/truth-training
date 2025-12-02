# Tasks: Align Truth Training Android Client with Desktop v1.0.0 Features

**Input**: Design documents from `/specs/007-title-align-truth/`
**Prerequisites**: [plan.md](plan.md)(p[lan.md](lan.md)) ✓, [research.md](research.md)(r[esearch.md](esearch.md)) ✓, [data-model.md](data-model.md)(d[ata-model.md](ata-model.md)) ✓, contracts/ ✓, [quickstart.md](quickstart.md)(q[uickstart.md](uickstart.md)) ✓

## Execution Flow (main)
```
1. Load [plan.md](plan.md)(p[lan.md](lan.md)) from feature directory ✓
2. Load optional design documents ✓
3. Generate tasks by category ✓
4. Apply task rules ✓
5. Number tasks sequentially (T001, T002...) ✓
6. Generate dependency graph ✓
7. Create parallel execution examples ✓
8. Validate task completeness ✓
9. Return: SUCCESS (tasks ready for execution) ✓
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

## Path Conventions
- **Mobile**: `truth-android-client/app/src/main/java/com/truth/training/client/`
- Entity files: `data/database/entities/`
- DAO files: `data/database/daos/`
- Repository files: `data/repository/`
- UI files: `ui/compose/`
- Test files: `app/src/test/java/` or `app/src/androidTest/java/`

## Phase 3.1: Setup
- [x] T001 Update `truth-android-client/app/build.gradle.kts` with Room 2.6.1, WorkManager 2.9.0, Jetpack Compose dependencies
- [x] T002 Update `truth-android-client/app/build.gradle.kts` versionName to "1.0.0", minSdk to 26, targetSdk to 33
- [x] T003 [P] Configure Kotlin compiler options and ProGuard rules for Room and Compose in `truth-android-client/app/proguard-rules.pro`
- [x] T004 Create project structure: `truth-android-client/app/src/main/java/com/truth/training/client/data/database/` (entities/, daos/, subdirectories)

## Phase 3.2: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**
**COLLECTIVE INTELLIGENCE: Ensure tests validate consensus mechanisms and truth convergence**

### Contract Tests (API Endpoints)
- [x] T005 [P] Contract test POST /api/v1/auth in `truth-android-client/app/src/test/java/com/truth/training/client/data/network/contract/ContractAuthTest.kt`
- [x] T006 [P] Contract test GET /api/v1/info in `truth-android-client/app/src/test/java/com/truth/training/client/data/network/contract/ContractInfoTest.kt`
- [x] T007 [P] Contract test GET /api/v1/events in `truth-android-client/app/src/test/java/com/truth/training/client/data/network/contract/ContractEventsListTest.kt`
- [x] T008 [P] Contract test POST /api/v1/events in `truth-android-client/app/src/test/java/com/truth/training/client/data/network/contract/ContractEventCreateTest.kt`
- [x] T009 [P] Contract test GET /api/v1/events/{id} in `truth-android-client/app/src/test/java/com/truth/training/client/data/network/contract/ContractEventGetTest.kt`
- [x] T010 [P] Contract test PUT /api/v1/events/{id} in `truth-android-client/app/src/test/java/com/truth/training/client/data/network/contract/ContractEventUpdateTest.kt`
- [x] T011 [P] Contract test DELETE /api/v1/events/{id} in `truth-android-client/app/src/test/java/com/truth/training/client/data/network/contract/ContractEventDeleteTest.kt`
- [x] T012 [P] Contract test GET /api/v1/contexts in `truth-android-client/app/src/test/java/com/truth/training/client/data/network/contract/ContractContextsListTest.kt`
- [x] T013 [P] Contract test POST /api/v1/contexts in `truth-android-client/app/src/test/java/com/truth/training/client/data/network/contract/ContractContextCreateTest.kt`
- [x] T014 [P] Contract test POST /api/v1/contexts/match in `truth-android-client/app/src/test/java/com/truth/training/client/data/network/contract/ContractContextMatchTest.kt`
- [x] T015 [P] Contract test POST /api/v1/judgments in `truth-android-client/app/src/test/java/com/truth/training/client/data/network/contract/ContractJudgmentSubmitTest.kt`
- [x] T016 [P] Contract test GET /api/v1/judgments/stats/{event_id} in `truth-android-client/app/src/test/java/com/truth/training/client/data/network/contract/ContractJudgmentStatsTest.kt`
- [x] T017 [P] Contract test POST /api/v1/impacts in `truth-android-client/app/src/test/java/com/truth/training/client/data/network/contract/ContractImpactAddTest.kt`

### Integration Tests (User Scenarios)
- [x] T018 [P] Integration test Scenario 1: Event creation with context template in `truth-android-client/app/src/androidTest/java/com/truth/training/client/integration/EventCreationWithTemplateTest.kt`
- [x] T019 [P] Integration test Scenario 2: Context template creation and duplicate detection in `truth-android-client/app/src/androidTest/java/com/truth/training/client/integration/ContextTemplateDuplicateTest.kt`
- [x] T020 [P] Integration test Scenario 3: Judgment submission and consensus in `truth-android-client/app/src/androidTest/java/com/truth/training/client/integration/JudgmentConsensusTest.kt`
- [x] T021 [P] Integration test Scenario 4: Offline-first operation in `truth-android-client/app/src/androidTest/java/com/truth/training/client/integration/OfflineFirstTest.kt`
- [x] T022 [P] Integration test Scenario 5: Template matching in `truth-android-client/app/src/androidTest/java/com/truth/training/client/integration/TemplateMatchingTest.kt`
- [x] T023 [P] Integration test Scenario 6: Cross-platform data consistency in `truth-android-client/app/src/androidTest/java/com/truth/training/client/integration/CrossPlatformSyncTest.kt`

## Phase 3.3: Core Implementation (ONLY after tests are failing)

### Room Database Entities [P] - Can run in parallel (different files)
- [x] T024 [P] EventEntity in `truth-android-client/app/src/main/java/com/truth/training/client/data/database/entities/EventEntity.kt`
- [x] T025 [P] ContextTemplateEntity in `truth-android-client/app/src/main/java/com/truth/training/client/data/database/entities/ContextTemplateEntity.kt`
- [x] T026 [P] JudgmentEntity in `truth-android-client/app/src/main/java/com/truth/training/client/data/database/entities/JudgmentEntity.kt`
- [x] T027 [P] ImpactEntity in `truth-android-client/app/src/main/java/com/truth/training/client/data/database/entities/ImpactEntity.kt`
- [x] T028 [P] SummaryEntity in `truth-android-client/app/src/main/java/com/truth/training/client/data/database/entities/SummaryEntity.kt`
- [x] T029 [P] SyncQueueEntity in `truth-android-client/app/src/main/java/com/truth/training/client/data/database/entities/SyncQueueEntity.kt`

### Room Database DAOs [P] - Can run in parallel (different files)
- [x] T030 [P] EventDao in `truth-android-client/app/src/main/java/com/truth/training/client/data/database/daos/EventDao.kt`
- [x] T031 [P] ContextTemplateDao in `truth-android-client/app/src/main/java/com/truth/training/client/data/database/daos/ContextTemplateDao.kt`
- [x] T032 [P] JudgmentDao in `truth-android-client/app/src/main/java/com/truth/training/client/data/database/daos/JudgmentDao.kt`
- [x] T033 [P] ImpactDao in `truth-android-client/app/src/main/java/com/truth/training/client/data/database/daos/ImpactDao.kt`
- [x] T034 [P] SummaryDao in `truth-android-client/app/src/main/java/com/truth/training/client/data/database/daos/SummaryDao.kt`
- [x] T035 [P] SyncQueueDao in `truth-android-client/app/src/main/java/com/truth/training/client/data/database/daos/SyncQueueDao.kt`

### Room Database Configuration
- [x] T036 Create TruthDatabase class in `truth-android-client/app/src/main/java/com/truth/training/client/data/database/TruthDatabase.kt` (depends on T024-T035)

### DTOs (Data Transfer Objects) [P] - Can run in parallel (different files)
- [x] T037 [P] Update Event DTOs (CreateEventRequest, UpdateEventRequest, EventResponse, EventDetailsResponse) with embedded fields in `truth-android-client/app/src/main/java/com/truth/training/client/data/network/dto/EventDtos.kt`
- [x] T038 [P] Create ContextTemplate DTOs (ContextTemplate, CreateContextRequest, MatchContextRequest, etc.) in `truth-android-client/app/src/main/java/com/truth/training/client/data/network/dto/ContextTemplateDtos.kt`
- [x] T039 [P] Create Judgment DTOs (Judgment, CreateJudgmentRequest, JudgmentStatsResponse) in `truth-android-client/app/src/main/java/com/truth/training/client/data/network/dto/JudgmentDtos.kt`
- [x] T040 [P] Create Impact DTOs (Impact, CreateImpactRequest) in `truth-android-client/app/src/main/java/com/truth/training/client/data/network/dto/ImpactDtos.kt`

### Retrofit API Interface Updates
- [x] T041 Update TruthApi interface with v1.0.0 endpoints (Events, Contexts, Judgments, Impacts) in `truth-android-client/app/src/main/java/com/truth/training/client/data/network/TruthApi.kt` (depends on T037-T040)

### Repository Layer [P] - Can run in parallel (different repositories)
- [x] T042 [P] EventRepository in `truth-android-client/app/src/main/java/com/truth/training/client/data/repository/EventRepository.kt` (depends on T030, T036, T041)
- [x] T043 [P] ContextTemplateRepository in `truth-android-client/app/src/main/java/com/truth/training/client/data/repository/ContextTemplateRepository.kt` (depends on T031, T036, T041)
- [x] T044 [P] JudgmentRepository in `truth-android-client/app/src/main/java/com/truth/training/client/data/repository/JudgmentRepository.kt` (depends on T032, T036, T041)
- [x] T045 [P] ImpactRepository in `truth-android-client/app/src/main/java/com/truth/training/client/data/repository/ImpactRepository.kt` (depends on T033, T036, T041)
- [x] T046 [P] SummaryRepository in `truth-android-client/app/src/main/java/com/truth/training/client/data/repository/SummaryRepository.kt` (depends on T034, T036, T041)

### Sync Infrastructure
- [x] T047 SyncQueueManager in `truth-android-client/app/src/main/java/com/truth/training/client/data/sync/SyncQueueManager.kt` (depends on T035, T036)
- [x] T048 SyncWorker (WorkManager) in `truth-android-client/app/src/main/java/com/truth/training/client/data/sync/SyncWorker.kt` (depends on T047)
- [x] T049 Update TruthRepository to integrate Room database and sync queue in `truth-android-client/app/src/main/java/com/truth/training/client/data/TruthRepository.kt` (depends on T042-T046, T047)

### P2P Synchronization
- [x] T050 P2PSyncManager for event propagation in `truth-android-client/app/src/main/java/com/truth/training/client/p2p/P2PSyncManager.kt`
- [x] T051 P2PMessageHandler for encrypted message handling in `truth-android-client/app/src/main/java/com/truth/training/client/p2p/P2PMessageHandler.kt` (depends on T050)
- [x] T052 Update P2PDiscoveryService to integrate with P2P sync in `truth-android-client/app/src/main/java/com/truth/training/client/p2p/P2PDiscoveryService.kt` (depends on T050, T051)

## Phase 3.4: UI Implementation (Jetpack Compose)

### Compose Screens [P] - Can run in parallel (different screens)
- [x] T053 [P] EventListScreen in `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventListScreen.kt` (depends on T042)
- [x] T054 [P] EventCreateScreen in `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventCreateScreen.kt` (depends on T042, T043)
- [x] T055 [P] EventEditScreen in `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventEditScreen.kt` (depends on T042)
- [x] T056 [P] EventDetailScreen in `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventDetailScreen.kt` (depends on T042, T044, T045)
- [x] T057 [P] ContextEditorScreen in `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/contexts/ContextEditorScreen.kt` (depends on T043)
- [x] T058 [P] ContextTemplateListScreen in `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/contexts/ContextTemplateListScreen.kt` (depends on T043)
- [x] T059 [P] JudgmentSubmissionScreen in `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/judgments/JudgmentSubmissionScreen.kt` (depends on T044)
- [x] T060 [P] DashboardScreen with sync status in `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/DashboardScreen.kt` (depends on T049, T042)

### Navigation and App Structure
- [x] T061 Create Compose navigation graph in `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/MainNavigation.kt` (depends on T053-T060)
- [x] T062 Update MainActivity to use Compose in `truth-android-client/app/src/main/java/com/truth/training/client/MainActivity.kt` (depends on T061)

## Phase 3.5: Integration and Polish

### Integration Tasks
- [x] T063 Initialize Room database in Application class in `truth-android-client/app/src/main/java/com/truth/training/client/TruthTrainingApplication.kt` (depends on T036)
- [x] T064 Configure WorkManager for periodic background sync in `truth-android-client/app/src/main/java/com/truth/training/client/data/sync/SyncConfiguration.kt` (depends on T048). Requirements: periodic sync every 15 minutes, constraints (NetworkType.CONNECTED, optional charging), exponential backoff retry (3 max retries). Update TruthTrainingApplication to use SyncConfiguration. Create SyncConfiguration object with createPeriodicSyncRequest() method. Test: verify periodic sync triggers, constraint enforcement, retry behavior.
- [x] T065 Update NetworkModule to handle v1.0.0 API authentication and interceptors in `truth-android-client/app/src/main/java/com/truth/training/client/data/network/NetworkModule.kt` (depends on T041)

### Unit Tests [P] - Can run in parallel (different files)
- [x] T066 [P] Unit tests for EventDao in `truth-android-client/app/src/test/java/com/truth/training/client/data/database/daos/EventDaoTest.kt`
- [x] T067 [P] Unit tests for ContextTemplateDao in `truth-android-client/app/src/test/java/com/truth/training/client/data/database/daos/ContextTemplateDaoTest.kt`
- [x] T068 [P] Unit tests for JudgmentDao in `truth-android-client/app/src/test/java/com/truth/training/client/data/database/daos/JudgmentDaoTest.kt`
- [x] T069 [P] Unit tests for EventRepository in `truth-android-client/app/src/test/java/com/truth/training/client/data/repository/EventRepositoryTest.kt`. Coverage: createEvent() (local save, sync queue), updateEvent() (local update, conflict handling), deleteEvent() (local delete, queue management), syncFromServer() (server sync, local merge), getAllEventsFlow() (Flow emission, reactive updates), getEventById() (local retrieval, null handling), offline-first behavior validation. Use MockWebServer for TruthApi mocking, in-memory Room database for isolation. Target: ≥95% coverage.
- [x] T070 [P] Unit tests for ContextTemplateRepository in `truth-android-client/app/src/test/java/com/truth/training/client/data/repository/ContextTemplateRepositoryTest.kt`. Coverage: createTemplate() (duplicate detection, local save), updateTemplate() (duplicate validation, conflict resolution), deleteTemplate() (cascade checks, sync queue), matchTemplate() (non-NULL field matching), countDuplicateTemplates() (duplicate logic validation), syncFromServer() (server sync, template merge). Edge cases: templates with all NULL fields, partial fields, duplicate detection with exclude ID. Target: ≥95% coverage.
- [x] T071 [P] Unit tests for SyncQueueManager in `truth-android-client/app/src/test/java/com/truth/training/client/data/sync/SyncQueueManagerTest.kt`. Coverage: queueOperation() (CREATE, UPDATE, DELETE operations), getPendingOperations() (filtering, ordering), markSyncing() (state transitions), markCompleted() (success handling, queue cleanup), markFailed() (retry logic, max retry handling), cleanupFailedOperations() (failed operation removal), conflict resolution (local-wins strategy). Test scenarios: multiple operations for same entity, retry count limits (0, 1, 2, 3+), concurrent operation queuing. Target: ≥95% coverage.

### Performance and Validation
- [x] T072 Performance tests for Room queries in `truth-android-client/app/src/androidTest/java/com/truth/training/client/performance/RoomPerformanceTest.kt`. Benchmarks: pagination query (< 50ms for 35 events), single entity retrieval (< 10ms for event by ID), bulk insert (< 100ms for 100 events), complex query (< 30ms for filtered list with status), Flow emission (< 20ms initial latency). Use AndroidBenchmarkRule, measure average of 10 iterations, test with database sizes: 100, 1000, 10000 events. Store results in PerformanceBenchmark data class (operation, averageTime, minTime, maxTime, databaseSize). Validate query plans and indices.
- [x] T073 Validate UI response times in `truth-android-client/app/src/androidTest/java/com/truth/training/client/performance/UIResponseTimeTest.kt`. Benchmarks: screen rendering (< 200ms for EventListScreen), data loading (< 500ms for initial data fetch), user interaction (< 100ms for button clicks), navigation (< 150ms for screen transitions). Use Espresso for UI automation, androidx.benchmark.macro (API 29+). Test scenarios: cold start with empty database, warm start with cached data, large dataset rendering (100+ events). Test on physical devices. Validate Compose recomposition counts.
- [x] T074 Execute [quickstart.md](quickstart.md)(q[uickstart.md](uickstart.md)) validation scenarios manually. File: `[specs/007-title-align-truth/quickstart.md](quickstart.md)(s[pecs/007-title-align-truth/quickstart.md](pecs/007-title-align-truth/quickstart.md))`. Execute all 6 scenarios: Scenario 1 (Event creation with context template), Scenario 2 (Context template duplicate detection), Scenario 3 (Judgment submission and consensus), Scenario 4 (Offline-first operation), Scenario 5 (Template matching), Scenario 6 (Cross-platform data consistency). Document results with screenshots/logs, verify cross-platform consistency, test offline-first behavior end-to-end. Generate test report in `[docs/TEST_REPORT_ANDROID_v1.0.0.md](../../docs/TEST_REPORT_ANDROID_v1.0.0.md)(d[ocs/TEST_REPORT_ANDROID_v1.0.0.md](ocs/TEST_REPORT_ANDROID_v1.0.0.md))` including: unit test summary (coverage by component, total coverage percentage), performance benchmarks (Room query performance table, UI response time table, comparison vs Desktop UI), integration test results (quickstart scenario validation, cross-platform consistency checks).

### Documentation
- [x] T075 [P] Update `[truth-android-client/README.md](../../truth-android-client/README.md)(t[ruth-android-client/README.md](ruth-android-client/README.md))` with v1.0.0 features and Room database information
- [x] T076 [P] Update `[docs/Truth-training/Truth-training.md](../../docs/Truth-training/Truth-training.md)(d[ocs/Truth-training/Truth-training.md](ocs/Truth-training/Truth-training.md))` marking Android v1.0.0 as complete
- [x] T077 [P] Create `[docs/ANDROID_MIGRATION.md](../../docs/ANDROID_MIGRATION.md)(d[ocs/ANDROID_MIGRATION.md](ocs/ANDROID_MIGRATION.md))` documenting migration from v0.3.0 to v1.0.0

### CI/CD Updates
- [x] T078 Update `.github/workflows/android-build.yml` for v1.0.0 versioning and release artifacts. Requirements: build Debug and Release (AAB) artifacts, run all test suites (unit, integration, performance), cache Gradle dependencies (`.gradle/caches`, `~/.gradle/caches/modules-2`), upload artifacts to GitHub Releases on tag builds. Matrix build for multiple API levels (26, 27, 28, 29, 30, 31, 32, 33). Workflow: test job (run all tests), build job (Debug/Release matrix), release job (upload to GitHub Releases on release event). Optimizations: Gradle build cache, dependency cache, test result caching. Ensure versionName "1.0.0" is used in build. Updated `[docs/CI_Workflows_Artifacts.md](../../docs/CI_Workflows_Artifacts.md)(d[ocs/CI_Workflows_Artifacts.md](ocs/CI_Workflows_Artifacts.md))` with Android build section.

## Dependencies

### Critical Path
- **Setup** (T001-T004) → **Tests** (T005-T023) → **Entities** (T024-T029) → **DAOs** (T030-T035) → **Database** (T036) → **DTOs** (T037-T040) → **API** (T041) → **Repositories** (T042-T046) → **Sync** (T047-T049) → **UI** (T053-T062) → **Integration** (T063-T065) → **Polish** (T066-T078)

### Blocking Dependencies
- T036 (TruthDatabase) requires all entities (T024-T029) and all DAOs (T030-T035)
- T041 (TruthApi) requires all DTOs (T037-T040)
- T042-T046 (Repositories) require T036, T030-T035 (DAOs), T041 (API)
- T049 (TruthRepository) requires all repositories (T042-T046) and sync (T047)
- T053-T060 (UI Screens) require corresponding repositories
- T061 (Navigation) requires all screens (T053-T060)
- T062 (MainActivity) requires T061
- T063 (Application) requires T036
- T064 (SyncConfiguration) requires T048 (SyncWorker)
- T069-T071 (Repository Unit Tests) require T042-T046 (Repositories), T047 (SyncQueueManager)
- T072-T073 (Performance Tests) require T066-T068 (DAO tests), T069-T071 (Repository tests)
- T078 (CI/CD) can run independently, but should wait for all tests passing

### Parallel Opportunities
- **Entities** (T024-T029): All can run in parallel (different files)
- **DAOs** (T030-T035): All can run in parallel (different files)
- **DTOs** (T037-T040): All can run in parallel (different files)
- **Repositories** (T042-T046): All can run in parallel (different files)
- **Compose Screens** (T053-T060): All can run in parallel (different files)
- **Contract Tests** (T005-T017): All can run in parallel (different files)
- **Integration Tests** (T018-T023): All can run in parallel (different files)
- **DAO Unit Tests** (T066-T068): All can run in parallel (different files)
- **Repository Unit Tests** (T069-T071): All can run in parallel (different files)
- **Performance Tests** (T072-T073): Can run in parallel (different test types)

## Parallel Execution Examples

### Example 1: Entity Creation (T024-T029)
```bash
# Launch all entity creation tasks in parallel:
Task: "EventEntity in truth-android-client/app/src/main/java/com/truth/training/client/data/database/entities/EventEntity.kt"
Task: "ContextTemplateEntity in truth-android-client/app/src/main/java/com/truth/training/client/data/database/entities/ContextTemplateEntity.kt"
Task: "JudgmentEntity in truth-android-client/app/src/main/java/com/truth/training/client/data/database/entities/JudgmentEntity.kt"
Task: "ImpactEntity in truth-android-client/app/src/main/java/com/truth/training/client/data/database/entities/ImpactEntity.kt"
Task: "SummaryEntity in truth-android-client/app/src/main/java/com/truth/training/client/data/database/entities/SummaryEntity.kt"
Task: "SyncQueueEntity in truth-android-client/app/src/main/java/com/truth/training/client/data/database/entities/SyncQueueEntity.kt"
```

### Example 2: DAO Creation (T030-T035)
```bash
# Launch all DAO creation tasks in parallel (after entities are complete):
Task: "EventDao in truth-android-client/app/src/main/java/com/truth/training/client/data/database/daos/EventDao.kt"
Task: "ContextTemplateDao in truth-android-client/app/src/main/java/com/truth/training/client/data/database/daos/ContextTemplateDao.kt"
Task: "JudgmentDao in truth-android-client/app/src/main/java/com/truth/training/client/data/database/daos/JudgmentDao.kt"
Task: "ImpactDao in truth-android-client/app/src/main/java/com/truth/training/client/data/database/daos/ImpactDao.kt"
Task: "SummaryDao in truth-android-client/app/src/main/java/com/truth/training/client/data/database/daos/SummaryDao.kt"
Task: "SyncQueueDao in truth-android-client/app/src/main/java/com/truth/training/client/data/database/daos/SyncQueueDao.kt"
```

### Example 3: Contract Tests (T005-T017)
```bash
# Launch contract test tasks in parallel:
Task: "Contract test POST /api/v1/auth in truth-android-client/app/src/test/java/com/truth/training/client/data/network/contract/ContractAuthTest.kt"
Task: "Contract test GET /api/v1/events in truth-android-client/app/src/test/java/com/truth/training/client/data/network/contract/ContractEventsListTest.kt"
Task: "Contract test POST /api/v1/contexts in truth-android-client/app/src/test/java/com/truth/training/client/data/network/contract/ContractContextCreateTest.kt"
# ... (all contract tests)
```

### Example 4: Compose Screens (T053-T060)
```bash
# Launch Compose screen tasks in parallel (after repositories are complete):
Task: "EventListScreen in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventListScreen.kt"
Task: "EventCreateScreen in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventCreateScreen.kt"
Task: "ContextEditorScreen in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/contexts/ContextEditorScreen.kt"
# ... (all Compose screens)
```

### Example 5: Repository Unit Tests (T069-T071)
```bash
# Launch repository unit test tasks in parallel (after repositories are complete):
Task: "Unit tests for EventRepository in truth-android-client/app/src/test/java/com/truth/training/client/data/repository/EventRepositoryTest.kt"
Task: "Unit tests for ContextTemplateRepository in truth-android-client/app/src/test/java/com/truth/training/client/data/repository/ContextTemplateRepositoryTest.kt"
Task: "Unit tests for SyncQueueManager in truth-android-client/app/src/test/java/com/truth/training/client/data/sync/SyncQueueManagerTest.kt"
```

### Example 6: Performance Tests (T072-T073)
```bash
# Launch performance test tasks in parallel (after unit tests are complete):
Task: "Performance tests for Room queries in truth-android-client/app/src/androidTest/java/com/truth/training/client/performance/RoomPerformanceTest.kt"
Task: "Validate UI response times in truth-android-client/app/src/androidTest/java/com/truth/training/client/performance/UIResponseTimeTest.kt"
```

## Notes
- [P] tasks = different files, no dependencies
- Verify tests fail before implementing (TDD)
- Commit after each completed task group
- Avoid: vague tasks, same file conflicts
- Room database version must be managed (currently version 1)
- P2P sync must maintain Ed25519 encryption (Constitution III)
- All UI must be Jetpack Compose (clarification applied)
- minSdk 26, targetSdk 33 (clarification applied)

## Task Generation Rules
*Applied during main() execution*

1. **From Contracts**:
   - Each contract endpoint → contract test task [P]
   - Each endpoint → implementation task in TruthApi

2. **From Data Model**:
   - Each entity (6 total) → entity creation task [P]
   - Each DAO (6 total) → DAO creation task [P]
   - Database configuration → single task

3. **From User Stories (Quickstart)**:
   - Each scenario (8 total) → integration test [P]
   - Quickstart validation → manual testing task

4. **From Plan**:
   - Setup tasks → dependencies, versioning
   - UI tasks → Compose screens (8 screens)
   - Sync tasks → WorkManager, P2P

5. **Ordering**:
   - Setup → Tests → Entities → DAOs → Database → DTOs → API → Repositories → Sync → UI → Integration → Polish

## Validation Checklist
*GATE: Checked by main() before returning*

- [x] All contracts have corresponding tests (T005-T017)
- [x] All entities have model tasks (T024-T029)
- [x] All DAOs have tasks (T030-T035)
- [x] All repositories have tasks (T042-T046)
- [x] All tests come before implementation (T005-T023 before T024+)
- [x] Parallel tasks truly independent (entities, DAOs, DTOs, repositories, screens)
- [x] Each task specifies exact file path
- [x] No task modifies same file as another [P] task
- [x] All quickstart scenarios have integration tests (T018-T023)
- [x] P2P synchronization tasks included (T050-T052)
- [x] Compose UI tasks included (T053-T062)
- [x] Unit test tasks for repositories include coverage targets (≥95%)
- [x] Performance test tasks include benchmark targets (< 50ms, < 200ms)
- [x] Test report generation task included (T074)

---
**Total Tasks**: 78 tasks across 5 phases
**Estimated Parallelization**: ~40% of tasks can run in parallel
**Critical Path Length**: ~25 sequential tasks (excluding parallelizable work)

