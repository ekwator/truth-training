# Tasks: Context Fields Embedded in Events — UI Template Editor Screen

**Branch**: `006-context-fields-embedded`  
**Input**: Design documents from `/specs/006-context-fields-embedded/`

Conventions:
- [P] = can run in parallel with others in same group
- TDD-first: write failing tests before implementation
- File paths are absolute or relative to repository root

## Ordered Task List

### Phase 3.1: Setup & Preparation

**T001. Update version numbers across all crates to v1.0.0** [X] [P]
- Paths: `Cargo.toml` (workspace root), `core/Cargo.toml`, `Cargo.toml` (root), `app/Cargo.toml`, `ui/desktop/package.json`
- Action: Update version field in all Cargo.toml files and package.json to "1.0.0"
- Output: All crates versioned as v1.0.0
- Dependencies: none

**T002. Update CHANGELOG.md with v1.0.0 breaking changes** [P]
- Path: `CHANGELOG.md`
- Action: Add new section documenting context_id removal, embedded fields addition, Context Editor UI, breaking changes
- Output: CHANGELOG entry for v1.0.0
- Dependencies: none

**T003. Update docs/VERSION_REGISTRY.md with v1.0.0 baseline marker** [P]
- Path: `docs/VERSION_REGISTRY.md`
- Action: Add entry marking v1.0.0 as first stable baseline with context fields embedded
- Output: Version registry updated
- Dependencies: none

### Phase 3.2: Contract Tests (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**

**T004. Implement contract test for updated POST /events endpoint** [P]
- Path: `tests/contract/api_events_test.rs`
- Action: Update existing contract test to assert POST /events accepts embedded fields (category_id, forma_id, cause_id, develop_id, effect_id), rejects context_id, validates FK references (returns 400 for invalid FKs)
- Output: Failing contract test (will pass after T018)
- Dependencies: T001

**T005. Implement contract tests for context template endpoints** [P]
- Path: `tests/contract/api_contexts_test.rs`
- Action: Create contract tests for GET /contexts, POST /contexts, GET /contexts/by-name/{name}, POST /contexts/match, POST /contexts/from-event. Assert duplicate detection (non-NULL field comparison), FK validation, error codes (400, 409)
- Output: Failing contract tests (will pass after T019-T023)
- Dependencies: T001

**T006. Implement integration test for context template workflows** [P]
- Path: `tests/integration/context_templates.rs`
- Action: Create integration test covering: template selection prefills event form, event creation with embedded fields, template matching (non-NULL comparison), duplicate detection, FK validation, creating template from event
- Output: Failing integration test (will pass after all implementation tasks)
- Dependencies: T001

### Phase 3.3: Core Data Models (ONLY after tests are failing)

**T007. Update TruthEvent and NewTruthEvent structs in core** [P]
- Path: `core/src/models.rs`
- Action: Remove `context_id` field from `TruthEvent` and `NewTruthEvent`. Add `category_id: Option<i64>`, `forma_id: Option<i64>`, `cause_id: Option<i64>`, `develop_id: Option<i64>`, `effect_id: Option<i64>` to both structs
- Output: Updated model structs compile
- Dependencies: T004, T005, T006

**T008. Add NewContext struct for template creation** [P]
- Path: `core/src/models.rs`
- Action: Add `NewContext` struct with fields: `name: String`, `category_id: Option<i64>`, `forma_id: Option<i64>`, `cause_id: Option<i64>`, `develop_id: Option<i64>`, `effect_id: Option<i64>`, `description: Option<String>`
- Output: NewContext struct added
- Dependencies: T004, T005, T006

### Phase 3.4: Storage Layer Implementation

**T009. Update truth_events table schema in storage.rs**
- Path: `core/src/storage.rs`
- Action: Update SCHEMA_SQL to remove `context_id` column, add `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id` columns (all INTEGER nullable with FK constraints). Update `get_truth_event` query to fetch new fields. Add indexes for FK fields.
- Output: Schema updated (note: no automatic migration - manual migration expected)
- Dependencies: T007

**T010. Update add_truth_event function for embedded fields**
- Path: `core/src/storage.rs`
- Action: Modify `add_truth_event` to accept `NewTruthEvent` with embedded fields, validate FK references (reject invalid FKs with error), update INSERT statement to use new fields instead of context_id
- Output: Function accepts and validates embedded fields
- Dependencies: T009

**T011. Add FK validation helper function**
- Path: `core/src/storage.rs`
- Action: Create helper function `validate_foreign_key(conn: &Connection, table: &str, field_id: Option<i64>) -> Result<bool, CoreError>` that checks if FK reference exists. Return error if non-NULL ID doesn't exist.
- Output: FK validation helper
- Dependencies: T009

**T012. Add context template helper functions**
- Path: `core/src/storage.rs`
- Action: Add functions: `get_all_contexts(conn: &Connection) -> Result<Vec<Context>, CoreError>`, `get_context_by_name(conn: &Connection, name: &str) -> Result<Option<Context>, CoreError>`, `add_context(conn: &Connection, new_ctx: NewContext) -> Result<i64, CoreError>` with duplicate detection (non-NULL field comparison)
- Output: Context template CRUD functions
- Dependencies: T008, T009

**T013. Add NULL-aware template matching function**
- Path: `core/src/storage.rs`
- Action: Create `match_context_template(conn: &Connection, category_id: Option<i64>, forma_id: Option<i64>, cause_id: Option<i64>, develop_id: Option<i64>, effect_id: Option<i64>) -> Result<Option<Context>, CoreError>` that compares only non-NULL fields using SQL WHERE clauses
- Output: Template matching function (non-NULL field comparison)
- Dependencies: T012

**T014. Add NULL-aware duplicate detection function**
- Path: `core/src/storage.rs`
- Action: Create `check_duplicate_context(conn: &Connection, new_ctx: &NewContext) -> Result<bool, CoreError>` that checks if template with identical non-NULL field combination exists. Compare only non-NULL fields, ignore NULL values.
- Output: Duplicate detection function (non-NULL field comparison)
- Dependencies: T012

### Phase 3.5: API Layer Implementation

**T015. Update POST /events endpoint handler**
- Path: `src/api.rs`
- Action: Update `AddEventRequest` struct to remove `context_id`, add five optional fields. Update `add_event` handler to validate FK references (call T011 helper), reject invalid FKs with 400 error, use new `NewTruthEvent` structure
- Output: POST /events accepts embedded fields, validates FKs
- Dependencies: T010, T011

**T016. Implement GET /contexts endpoint**
- Path: `src/api.rs`
- Action: Add handler `list_contexts` that calls `get_all_contexts` and returns list with total count
- Output: GET /contexts endpoint
- Dependencies: T012

**T017. Implement POST /contexts endpoint**
- Path: `src/api.rs`
- Action: Add handler `create_context` that validates FK references, checks duplicates (T014), creates template, returns 409 Conflict if duplicate detected (non-NULL fields match)
- Output: POST /contexts endpoint with duplicate detection
- Dependencies: T012, T014

**T018. Implement GET /contexts/by-name/{name} endpoint**
- Path: `src/api.rs`
- Action: Add handler `get_context_by_name` that calls `get_context_by_name` storage function, returns 404 if not found
- Output: GET /contexts/by-name/{name} endpoint
- Dependencies: T012

**T019. Implement POST /contexts/match endpoint**
- Path: `src/api.rs`
- Action: Add handler `match_context` that calls `match_context_template` (T013) and returns `{matched: bool, template: Context | null}`
- Output: POST /contexts/match endpoint (non-NULL field comparison)
- Dependencies: T013

**T020. Implement POST /contexts/from-event endpoint**
- Path: `src/api.rs`
- Action: Add handler `create_context_from_event` that fetches event, extracts embedded fields, checks duplicates, creates template with provided name and description
- Output: POST /contexts/from-event endpoint
- Dependencies: T012, T014

**T021. Register new API endpoints in main routing**
- Path: `src/api.rs` or `src/main.rs`
- Action: Add routes for all new context template endpoints: `/contexts`, `/contexts/by-name/{name}`, `/contexts/match`, `/contexts/from-event`
- Output: All endpoints accessible
- Dependencies: T016, T017, T018, T019, T020

### Phase 3.6: UI TypeScript Types

**T022. Update Event interface to remove context_id, add embedded fields** [P]
- Path: `ui/desktop/src/types/events.ts`
- Action: Remove `context_id: string` field, add `category_id?: number`, `forma_id?: number`, `cause_id?: number`, `develop_id?: number`, `effect_id?: number` to Event and CreateEventRequest interfaces
- Output: Updated TypeScript types
- Dependencies: T015

**T023. Create Context template TypeScript types** [P]
- Path: `ui/desktop/src/types/contexts.ts`
- Action: Create new file with interfaces: `ContextTemplate`, `CreateContextRequest`, `MatchContextRequest`, `CreateContextFromEventRequest` matching API schemas
- Output: Context template types
- Dependencies: T016

### Phase 3.7: UI API Service Updates

**T024. Update createEvent API call for embedded fields**
- Path: `ui/desktop/src/services/api.ts`
- Action: Update `createEvent` function to send embedded fields instead of context_id. Map UI payload to new API structure
- Output: createEvent uses embedded fields
- Dependencies: T015, T022

**T025. Add context template API service functions**
- Path: `ui/desktop/src/services/api.ts`
- Action: Add functions: `getContexts()`, `getContextByName(name: string)`, `createContext(request: CreateContextRequest)`, `matchContext(request: MatchContextRequest)`, `createContextFromEvent(request: CreateContextFromEventRequest)`
- Output: Context template API service
- Dependencies: T016, T017, T018, T019, T020, T023

### Phase 3.8: UI Components

**T026. Update NewEvent page for template selection and field prefilling** [X]
- Path: `ui/desktop/src/pages/NewEvent.tsx`
- Action: Add context template dropdown selector. When template selected, prefill five context fields. Allow user to modify prefilled fields. Update form submission to send embedded fields instead of context_id
- Output: Template selection and prefilling working
- Dependencies: T024, T025

**T027. Create ContextEditor page component** [X] [P]
- Path: `ui/desktop/src/pages/ContextEditor.tsx`
- Action: Create new React component with form fields: name, category_id, forma_id, cause_id, develop_id, effect_id, description. Include duplicate detection error display. Handle "Template already exists" message. Support prefilling from event
- Output: Context Editor screen
- Dependencies: T025

**T028. Update Events list page for template matching and Create Template button** [X]
- Path: `ui/desktop/src/pages/Events.tsx`
- Action: Update event list display to call matchContext API for each event. Display context template name if match found (non-NULL field comparison), otherwise show "[Create Template]" button. Button opens ContextEditor with fields prefilled
- Output: Template matching display and Create Template button
- Dependencies: T025, T027

### Phase 3.9: Documentation Updates

**T029. Update docs/Data_Schema.md with new event structure** [P]
- Path: `docs/Data_Schema.md`
- Action: Update truth_events table schema to show embedded fields, remove context_id. Document FK validation, template matching, duplicate detection behavior
- Output: Data schema documentation updated
- Dependencies: T009

**T030. Update docs/UI_Desktop.md with Context Editor screen** [P]
- Path: `docs/UI_Desktop.md`
- Action: Document new Context Editor screen, template selection workflow, Create Template button behavior
- Output: UI documentation updated
- Dependencies: T027, T028

**T031. Update spec/09-ux-guidelines.md with context template patterns** [P]
- Path: `spec/09-ux-guidelines.md`
- Action: Document UX patterns for template selection, field prefilling, duplicate detection feedback
- Output: UX guidelines updated
- Dependencies: T026, T027

### Phase 3.10: Validation & Polish

**T032. Run contract tests and verify they pass**
- Path: `tests/contract/`
- Action: Execute `cargo test --test contract` and verify all contract tests pass
- Output: All contract tests green
- Dependencies: T004, T005, T015, T016, T017, T018, T019, T020

**T033. Run integration tests and verify they pass**
- Path: `tests/integration/`
- Action: Execute `cargo test --test integration context_templates` and verify integration test passes
- Output: Integration test green
- Dependencies: T006, all implementation tasks

**T034. Verify quickstart.md validation steps**
- Path: `specs/006-context-fields-embedded/quickstart.md`
- Action: Execute all quickstart steps manually and verify expected outcomes
- Output: Quickstart validation complete
- Dependencies: All implementation tasks

**T035. Update docs to note manual migration requirement** [P]
- Path: `docs/Data_Schema.md`, `docs/Deployment.md`
- Action: Add explicit note that no automatic database migrations are executed. Manual migration script/instructions required for existing context_id data
- Output: Migration notes documented
- Dependencies: T009

## Dependencies

**Critical Path**:
1. Setup (T001-T003) → can run in parallel
2. Contract Tests (T004-T006) → can run in parallel, MUST complete before implementation
3. Models (T007-T008) → can run in parallel, after tests
4. Storage (T009-T014) → sequential (T009 blocks all, T012 blocks T013-T014)
5. API (T015-T021) → T015 depends on storage, T016-T020 can run after T012, T021 depends on all endpoints
6. UI Types (T022-T023) → can run in parallel
7. UI Services (T024-T025) → depends on API and types
8. UI Components (T026-T028) → depends on services
9. Documentation (T029-T031, T035) → can run in parallel
10. Validation (T032-T034) → depends on all implementation

**Test-First Dependency**:
- T004-T006 (contract/integration tests) MUST be written and FAIL before T007-T035 (implementation)

## Parallel Execution Examples

**Group A** (Setup - can all run immediately):
```
T001: Update version numbers
T002: Update CHANGELOG.md
T003: Update docs/VERSION_REGISTRY.md
```

**Group B** (Contract Tests - can all run after T001):
```
T004: Contract test POST /events
T005: Contract tests context endpoints
T006: Integration test context templates
```

**Group C** (Models - can run after Group B):
```
T007: Update TruthEvent structs
T008: Add NewContext struct
```

**Group D** (Documentation - can run after implementation):
```
T029: Update docs/Data_Schema.md
T030: Update docs/UI_Desktop.md
T031: Update spec/09-ux-guidelines.md
T035: Add migration notes
```

## Notes

- **Breaking Change**: Removal of context_id is a breaking change requiring v1.0.0 version bump
- **No Automatic Migrations**: Database migrations must be manual. Document migration path but do not implement automatic scripts.
- **NULL Handling**: All duplicate detection and template matching uses non-NULL field comparison only
- **FK Validation**: Invalid FK references are rejected immediately (400 error), not logged as warnings
- **TDD**: Tests (T004-T006) MUST fail before implementation begins
- **Verification**: T032-T034 must pass before considering feature complete

