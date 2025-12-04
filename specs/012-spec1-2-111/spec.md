# Feature Specification: Android Parity with Desktop UI & Startup Fix

**Feature Branch**: `012-spec1-2-111`  
**Created**: 2025-12-03  
**Status**: Draft  
**Input**: User description: "Extend the 001-init-schema-fix specification so that truth-android-client fully matches the functionality and behavioral parity of UI Desktop, while also fixing the critical Android issue where the application disappears immediately after launch."

## Clarifications

### Session 2025-12-03

- Q: Should Android use the same core schema initialization as Desktop? → A: Yes, Android MUST call shared initialization logic or mirror the same SQL creation flow used by Desktop (`core_lib::storage::init_db` equivalent).
- Q: What is the root cause of Android app disappearing on launch? → A: AndroidManifest.xml declares `MainDashboardActivity` as launcher, but `MainActivity` (Compose UI) is not exported and has no intent filters. Additionally, MainActivity may be missing proper UI initialization.
- Q: Which screen should be the entry point for Android app launch? → A: MainActivity (Compose UI) should be made the launcher Activity; MainDashboardActivity should be removed or deprecated.
- Q: What should be the primary data source for context dropdowns in Android? → A: Embedded database (Room) — contexts are stored locally and loaded during database initialization, matching Desktop's embedded approach.
- Q: How should legacy tables be handled during migration? → A: Drop legacy tables immediately — data is not migrated, only table removal. This matches Desktop behavior where legacy tables are dropped without data migration.
- Q: How should Android access the canonical schema from core/src/storage.rs? → A: Shared schema via SQL assets — use a common SQL file as an asset that is read by both Android and Desktop to ensure schema parity.
- Q: Should Android emit telemetry events for context validation failures like Desktop? → A: Optional — telemetry events (e.g., `context_picker.validation.failure`) should be added only if telemetry infrastructure already exists in Android app; otherwise, local logging is sufficient.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Android app launches and displays UI (Priority: P1) 🎯

Android users must be able to launch the app and see the main UI screen without the app disappearing immediately after launch.

**Why this priority**: Critical bug blocking all Android functionality; app is unusable if it disappears on launch.

**Independent Test**: Launch the Android app on an emulator or device, verify that the main screen (Dashboard or Navigation) is visible and remains visible, and confirm the app does not close itself or disappear.

**Acceptance Scenarios**:

1. **Given** a fresh Android app installation, **When** the user launches the app, **Then** `MainActivity` (Compose UI) is displayed as the launcher, the main navigation screen is visible and remains stable, and the app does not close itself.
2. **Given** the Android app is running, **When** the user navigates between screens, **Then** all screens render correctly and the app remains stable.

---

### User Story 2 - Android DB init enforces truth schemas (Priority: P1) 🎯

Android app initialization must use the canonical Truth schema (`truth_events`, `statements`, `impacts`, `progress_metrics`, `context`, etc.) and remove all legacy `events` table references, matching Desktop behavior.

**Why this priority**: Schema parity is required for cross-platform data consistency; legacy tables cause data corruption and migration issues.

**Independent Test**: Initialize Android app database, inspect schema via Room database inspector or SQLite, and verify that only Truth tables exist and legacy `events` table is absent.

**Acceptance Scenarios**:

1. **Given** a clean Android app installation, **When** the app initializes the database, **Then** all Truth tables are created from the shared SQL asset (canonical schema) and no legacy `events` table exists.
2. **Given** an Android app with existing legacy tables, **When** the app initializes or migrates the database, **Then** legacy tables are dropped immediately (no data migration), and automated checks confirm they are absent.

---

### User Story 3 - Android context dropdowns match Desktop UX (Priority: P2)

Android event creators need dropdown/combo widgets that list contexts from embedded database (Room) with validation, matching Desktop ContextPicker behavior. Contexts are seeded during database initialization and stored locally.

**Why this priority**: Consistent UX across platforms reduces user confusion; validation prevents data corruption from invalid context IDs.

**Independent Test**: Open Android NewEvent screen, verify context fields use dropdowns instead of numeric inputs, test manual entry validation, and confirm invalid IDs are blocked.

**Acceptance Scenarios**:

1. **Given** contexts are seeded in the embedded database during initialization, **When** the user opens a context field on NewEvent screen, **Then** they see a dropdown with human-friendly labels loaded from Room database and can select contexts without typing numeric IDs.
2. **Given** the user manually enters an invalid context ID, **When** they attempt to submit the event, **Then** the field is highlighted, submission is blocked, and an error explains the mismatch.

---

### User Story 4 - Android localization parity with Desktop (Priority: P3)

Android app must document or implement RU/EN language switching to match Desktop localization posture, ensuring consistent user experience across platforms.

**Why this priority**: Localization parity is important for international users; documentation clarity prevents confusion about platform differences.

**Independent Test**: Audit Android app for localization support, verify strings are consistent with Desktop if switching exists, or document EN-only status clearly in specs and quickstarts.

**Acceptance Scenarios**:

1. **Given** the Android app has localization support, **When** a user switches language, **Then** all UI strings update to the selected language and the preference persists across app restarts.
2. **Given** the Android app is EN-only, **When** a user reads the documentation, **Then** the EN-only status is clearly documented in specs, quickstarts, and UI guidelines.

---

### Edge Cases

- Android app launches but MainActivity crashes before UI renders—must ensure proper error handling and fallback to a stable screen.
- Database migration fails during initialization—must handle gracefully with user-visible error and recovery option.
- Context lookup data is unavailable (database not initialized, migration failed)—must show error state, allow retry, and prevent submission until valid data is available from embedded database.
- Android app is launched with existing legacy database—migration must drop legacy tables immediately without attempting data migration (matching Desktop behavior); only Truth tables are preserved.
- Navigation graph is incomplete or missing routes—app must have a valid entry screen and all required routes defined.
- ViewModel factories fail during initialization—must ensure proper DI setup and lifecycle management to prevent crashes.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Android `TruthDatabase` initialization MUST use the canonical Truth schema from a shared SQL asset file (readable by both Android and Desktop) derived from `core/src/storage.rs` to create `truth_events`, `statements`, `impacts`, `progress_metrics`, `context`, and related Truth tables. This ensures schema parity across platforms.
- **FR-002**: Android database initialization MUST drop/remove all legacy `events` table references immediately without data migration (matching Desktop behavior). Automated regression tests (instrumented or unit) MUST fail if deprecated tables remain after initialization.
- **FR-003**: Android `AndroidManifest.xml` MUST declare `MainActivity` (Compose UI) as the launcher Activity with `android:exported="true"`, proper intent filters (`MAIN` + `LAUNCHER`), and correct theme. `MainDashboardActivity` MUST be removed or deprecated to prevent app from disappearing on launch.
- **FR-004**: Android `MainActivity` MUST call `setContent {}` with a valid Compose UI (via `MainNavigation` composable) and ensure NavigationHost is initialized before displaying any screen. The entry screen must be explicitly defined in the navigation graph.
- **FR-005**: Android context fields on NewEvent screen MUST render dropdown/combo components populated from embedded Room database (contexts seeded during initialization) instead of raw numeric inputs, matching Desktop ContextPicker behavior.
- **FR-006**: Android context validation MUST prevent submission of IDs not present in the taxonomy/context lookup lists; invalid IDs must block submission with inline error states. Telemetry events (e.g., `context_picker.validation.failure`) are optional and should be added only if telemetry infrastructure exists; otherwise, local logging is sufficient.
- **FR-007**: Android localization status MUST be determined and documented: if RU/EN switching exists, strings must be consistent with Desktop; if EN-only, this must be clearly documented in `spec/09-ux-guidelines.md`, `docs/UI_Desktop.md`, and Android quickstart.
- **FR-008**: Documentation (`spec/23-function_desktop.md`, `docs/quickstart_desktop.md`, `docs/quickstart_android.md`, `docs/UI_Desktop.md`, spec files 18-21) MUST be updated to include Android behavior alongside Desktop, describing Init workflow, dropdown UI, validation rules, and localization status.
- **FR-009**: Android ViewModel factories and DI setup MUST be correct to prevent crashes during launch; all required dependencies must be injected before use.
- **FR-010**: Android navigation graph MUST include all required screens with proper routes and arguments; entry screen must be explicitly defined and be the first visible UI element.

### Key Entities *(include if feature involves data)*

- **truth_events**: Canonical event records created via Android UI; includes embedded context fields (`category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`) matching Desktop schema.
- **context**: Lookup table used by Android dropdowns; contains human-readable labels and ID keys for validation, sourced from embedded Room database and seeded during database initialization.
- **TruthDatabase**: Room database instance in Android app; must match Desktop SQLite schema structure and exclude legacy tables.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Android app launches successfully and displays main UI screen without disappearing; 100% of launch attempts result in visible, stable UI (verified via automated UI tests or manual testing).
- **SC-002**: Android database initialization results in canonical Truth schema with zero legacy `events` tables, verified by automated tests (instrumented or unit) that inspect schema after initialization.
- **SC-003**: 100% of Android event submissions use context IDs sourced from validated dropdown lists; attempted invalid submissions are prevented with inline error states.
- **SC-004**: Android localization status is clearly documented in specs and quickstarts; if RU/EN switching exists, it works end-to-end with consistent strings; if EN-only, documentation explicitly states this.
- **SC-005**: Updated documentation includes Android behavior alongside Desktop; all quickstarts, specs, and UI guidelines reflect cross-platform parity where applicable.
