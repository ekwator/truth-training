# Feature Specification: Desktop DB Init & Context UX Hardening

**Feature Branch**: `011-spec1-1-24`  
**Created**: 2025-12-03  
**Status**: Draft  
**Input**: User description: "Fix init_app and DB schemas, modernize context selection UI/validation, verify localization state, and align desktop specs/quickstarts/docs."

## Clarifications

### Session 2025-12-03

- Q: Должны ли мы реализовать RU/EN переключатель или просто задокументировать EN-only состояние? → A: Реализуем полноценный RU/EN переключатель в UI с полным набором локализованных строк.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Desktop DB init enforces truth schemas (Priority: P1)

Desktop operators running `init_app` must end up with the canonical Truth tables (`truth_events`, `statements`, `impacts`, `progress_metrics`, `context`, etc.) even if their SQLite file still contains legacy `events`-style tables.

**Why this priority**: Broken migrations block every other workflow; without a reliable schema reset, desktop builds cannot be trusted.

**Independent Test**: Launch desktop app against a temp DB, run `init_app`, and query schema to prove that canonical tables exist and legacy tables do not.

**Acceptance Scenarios**:

1. **Given** a clean SQLite file, **When** `init_app` runs, **Then** all Truth tables are created from `storage::init_db` definitions and the command succeeds.
2. **Given** a DB that still has `events` and other deprecated tables, **When** `init_app` runs, **Then** the legacy tables are dropped/replaced and an automated check confirms they are absent.

---

### User Story 2 - Context pickers are list-backed and validated (Priority: P2)

Event creators on the desktop UI need dropdown/combo widgets that list contexts from `ApiService.getContexts` (plus manual input with autocomplete) and refuse IDs that the backend does not know.

**Why this priority**: Wrong context IDs corrupt downstream analytics; validation at input time avoids silent data loss.

**Independent Test**: Mock `getContexts` to return a sample list, exercise the `NewEvent` page, and ensure submissions succeed only when IDs match the list; invalid IDs stay highlighted and blocked.

**Acceptance Scenarios**:

1. **Given** contexts fetched from the API, **When** the user opens the picker, **Then** they see human-friendly labels with search/autocomplete and can pick multiple contexts without typing numeric IDs.
2. **Given** the user manually enters an ID that is not in the fetched list, **When** they attempt to submit, **Then** the field is highlighted, submission is blocked, and an error explains the mismatch.

---

### User Story 3 - Localization posture & docs stay in sync (Priority: P3)

Product/Docs teams need a clear answer on whether RU/EN switching exists in the desktop UI; the answer must be reflected in specs, quickstarts, and UI docs together with the new initialization/context behavior.

**Why this priority**: Mismatched documentation causes operator errors and breaks trust with contributors.

**Independent Test**: Audit the UI to confirm actual localization toggle or lack thereof, update `spec/23-function_desktop.md`, `docs/quickstart_desktop.md`, `docs/UI_Desktop.md`, README release surfaces, and any quickstarts referencing the feature, then review links for correctness.

**Acceptance Scenarios**:

1. **Given** the current desktop build, **When** a user follows the updated docs, **Then** they can find and use the RU/EN localization toggle and the steps match what the UI exposes.
2. **Given** the documentation set, **When** a release reviewer scans build instructions, DB warnings, and UI descriptions, **Then** no pages reference the removed initialization warning, and all link tables render correctly on GitHub.

---

### Edge Cases

- DB initialization is run against a file that already contains some, but not all, Truth tables—init must reconcile differences without leaving mixed schemas.
- API call for contexts fails or returns stale data—the UI must surface the error, provide a retry affordance, keep submission disabled, and rely on cached data only after explicit confirmation.
- Localization toggle exists but only switches partially translated strings—the doc update must either cover fallback behavior or block release until strings are complete.
- Documentation build includes anchors/tables; updated links must be verified so GitHub markdown renders without broken references.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: `ui/desktop/src-tauri/src/commands/config.rs::init_app` MUST invoke the canonical schema migration (ideally via `storage::init_db`) so it recreates `truth_events`, `statements`, `impacts`, `progress_metrics`, `context`, and any related Truth tables.
- **FR-002**: DB initialization MUST drop/rename legacy `events` tables and include an automated regression test that fails if deprecated tables remain after `init_app`.
- **FR-003**: `NewEvent` context fields MUST render dropdown/combo components populated from `ApiService.getContexts()` and allow search-based selection instead of raw numeric inputs.
- **FR-004**: Manual context entry MUST validate against the fetched list; invalid IDs must block submission with inline error states, emit a telemetry/log event, and keep submission disabled until corrected.
- **FR-005**: Documentation (`spec/23-function_desktop.md`, `docs/quickstart_desktop.md`, README release surfaces, `docs/UI_Desktop.md`, relevant quickstarts) MUST be updated to describe the new initialization flow, context UI, and localization posture, removing prior warnings about broken constitutions.
- **FR-006**: Desktop UI MUST expose an RU/EN toggle (e.g., settings or header control) backed by `ui/desktop/src/i18n`, ensuring all user-visible strings have EN+RU translations; if a RU string is missing, the UI MUST fall back to English and display a one-time toast/console warning (`translation.missing`) for QA.
- **FR-007**: A documentation audit checklist MUST be produced covering build instructions, cargo configuration, test plan, roadmap, migrations, UI descriptions, API/Tauri commands, and quickstarts, with each outdated area updated or flagged.

### Key Entities *(include if feature involves data)*

- **truth_events**: Canonical event records created via desktop UI; includes foreign keys to statements, contexts, and impacts, plus metadata timestamps used by downstream analytics.
- **statements / impacts / progress_metrics**: Supporting tables that capture narrative statements, qualitative impact assessments, and progress tracking metrics referenced by each event.
- **context**: Lookup table delivered by `ApiService.getContexts()`; contains human-readable labels, hierarchy info, and UUID/ID keys used by the new dropdowns and validation logic.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Running `init_app` on a clean or legacy DB results in the expected Truth schema with zero legacy tables, verified by an automated test that fails otherwise.
- **SC-002**: 100% of desktop event submissions use context IDs sourced from the validated dropdown list; attempted invalid submissions are prevented and logged via `context_picker.validation.failure`.
- **SC-003**: QA confirms the RU/EN toggle works end-to-end (strings, locale persistence, docs) and release materials document how to switch languages.
- **SC-004**: Updated docs remove the legacy initialization warning and pass a link/table validation sweep (no broken anchors across spec/docs/quickstarts).
