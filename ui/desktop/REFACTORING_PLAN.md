# Desktop UI Storage Refactoring Plan

**Date**: 2025-12-09  
**Purpose**: Refactor Desktop UI to use Rust core system functions from `core/src/storage.rs` directly, removing Android-client-based wrapper functions

## Function Mappings

### Events Operations

| Wrapper Function (Desktop) | Core Function | Notes |
|---------------------------|---------------|-------|
| `storage.insert_truth_event()` | `core::storage::add_truth_event()` | Direct replacement. Core function takes `NewTruthEvent` struct. |
| `storage.get_truth_event_with_names()` | `core::storage::get_truth_event()` + entity name queries | Core function returns `TruthEvent` without names. Entity names must be resolved separately in frontend. |
| `storage.list_truth_events_with_names()` | `core::storage::load_truth_events()` + entity name queries | Core function returns `Vec<TruthEvent>` without names. Pagination and entity name resolution must be handled in frontend. |

### Impact Operations

| Wrapper Function (Desktop) | Core Function | Notes |
|---------------------------|---------------|-------|
| `storage.insert_impact()` | `core::storage::add_impact()` | Direct replacement. Core function signature: `add_impact(conn, event_id, type_id, value, notes)`. Note: Desktop wrapper uses `impact_level` (i32) but core uses `type_id` (i64) and `value` (bool). |

### Statement Operations

| Wrapper Function (Desktop) | Core Function | Notes |
|---------------------------|---------------|-------|
| N/A (no wrapper exists) | `core::storage::add_statement()` | Direct use. |
| N/A (no wrapper exists) | `core::storage::load_statements()` | Direct use. |
| N/A (no wrapper exists) | `core::storage::get_statements_for_event()` | Direct use. |

### Judgment Operations

**⚠️ SCHEMA MISMATCH**: Desktop UI uses legacy `judgments` table (simple schema with string IDs), while core uses `judgments_ci` table (CI schema with UUIDs, participants, signatures).

| Wrapper Function (Desktop) | Core Function | Notes |
|---------------------------|---------------|-------|
| `storage.insert_judgment()` | `core::collective_intelligence::ci_insert_judgment()` | **DEFERRED**: Requires schema migration. Desktop uses simple `judgments` table, core uses `judgments_ci` with participants. |
| `storage.list_judgments_for_event()` | `core::collective_intelligence::ci_get_judgments_by_event()` | **DEFERRED**: Requires schema migration. |
| `storage.get_judgment_stats()` | Custom query or `core::collective_intelligence::ci_get_judgments_by_event()` + processing | **DEFERRED**: Requires schema migration. |

**Migration Path**: 
1. Create migration script to convert `judgments` → `judgments_ci`
2. Create default participant for Desktop UI
3. Update commands to use CI functions
4. Remove wrapper functions

**Current Status**: Wrapper functions kept for backward compatibility until migration is implemented.

### Knowledge Base Operations

| Wrapper Function (Desktop) | Core Function | Notes |
|---------------------------|---------------|-------|
| N/A (no wrapper exists) | `core::storage::get_all_contexts()` | Direct use. |
| N/A (no wrapper exists) | `core::storage::add_context()` | Direct use. |
| N/A (no wrapper exists) | `core::storage::seed_knowledge_base()` | Direct use. |

### Metrics/Summary Operations

| Wrapper Function (Desktop) | Core Function | Notes |
|---------------------------|---------------|-------|
| `storage.get_overall_metrics()` | `core::storage::load_metrics()` or custom query | Core function returns `Vec<ProgressMetrics>`. May need to calculate overall metrics from latest entry or aggregate. |
| `storage.list_event_summaries()` | `core::storage::load_truth_events()` + processing | Core function returns all events. Process in frontend to create summaries. |

## Entity Name Resolution Strategy

### Requirements

The following commands need entity names (category, forma, cause, develop, effect):
1. **Events**: `get_event_fast`, `list_events_fast` - Need entity names for display
2. **Context Templates**: Display context field names in templates list

### Strategy: Frontend Entity Name Resolution

**Approach**: Fetch entity names separately and merge with event data in TypeScript frontend.

**Implementation**:
1. Create utility function `ui/desktop/src/utils/entityNames.ts`:
   - `fetchEntityNames()`: Fetches all entity names (category, forma, cause, develop, effect) via Tauri command
   - `resolveEventEntityNames(event)`: Merges entity names with event data
   - Cache entity names in memory to avoid repeated queries

2. Create Tauri command `get_entity_names` in `ui/desktop/src-tauri/src/commands/knowledge_base.rs`:
   - Returns all entity names from knowledge base tables
   - Called once on app initialization or when knowledge base is reseeded

3. Update frontend services:
   - `events.ts`: Use `resolveEventEntityNames()` after fetching events
   - `contextEditor.ts`: Use entity names for context field display

### Alternative: Lightweight Tauri Commands with JOINs

If frontend resolution is too complex, create lightweight Tauri commands that:
- Call core functions
- Perform JOIN queries for entity names
- Return combined data

**Decision**: Use frontend resolution for flexibility and to keep Tauri commands simple.

## Migration Steps

### Step 1: Analysis & Planning (T143-T145)
- ✅ Document function mappings (this document)
- ✅ Identify entity name resolution requirements
- ✅ Create refactoring plan

### Step 2: Refactor Database Initialization (T146-T148)
- Refactor `Db::initialize()` to use `core::storage::init_db()` and `core::storage::seed_knowledge_base()`
- Update `run_migrations()` to delegate to `core::storage::run_migrations()`
- Remove or refactor `get_locale_from_config()` (English-only mode)

### Step 3: Create Entity Name Resolution Utility (T152)
- Create `ui/desktop/src/utils/entityNames.ts`
- Create Tauri command `get_entity_names`
- Test entity name resolution

### Step 4: Refactor Tauri Commands - Events (T149-T153)
- Update `events.rs` to use core functions
- Update frontend service to use entity name resolution

### Step 5: Refactor Tauri Commands - Impacts (T154-T156)
- Update `impacts.rs` to use core functions
- Update frontend service

### Step 6: Refactor Tauri Commands - Statements (T157-T159)
- Update `statements.rs` to use core functions
- Update frontend service

### Step 7: Refactor Tauri Commands - Judgments (T160-T163)
- Update `judgments.rs` to use CI functions
- Update frontend service

### Step 8: Refactor Tauri Commands - Knowledge Base (T164-T167)
- Update `knowledge_base.rs` to use core functions
- Update frontend service

### Step 9: Refactor Tauri Commands - Metrics/Summary (T168-T170)
- Update commands to use core functions
- Update frontend service

### Step 10: Remove Wrapper Functions (T171-T179)
- Remove all wrapper functions from `storage.rs`
- Keep only `Db::initialize()` (refactored) and necessary helpers

### Step 11: Testing & Verification (T180-T188)
- Create integration tests for refactored commands
- Create E2E test for storage refactoring
- Verify no functionality is lost
- Verify error handling

## Error Handling

### Core Error Types

Core functions return `Result<T, CoreError>`. Desktop wrapper functions return `Result<T, String>`.

**Migration**: Convert `CoreError` to `String` in Tauri commands:
```rust
.map_err(|e| e.to_string())
```

### Error Propagation

Ensure errors from core functions are properly propagated to frontend:
- Core errors → Tauri command errors → Frontend service errors → UI error display

## Testing Strategy

1. **Unit Tests**: Test each refactored Tauri command independently
2. **Integration Tests**: Test command + core function integration
3. **E2E Tests**: Test full flow from frontend → Tauri → Core → Database
4. **Regression Tests**: Verify all existing functionality still works

## Rollback Plan

If refactoring causes issues:
1. Keep old wrapper functions in a backup branch
2. Revert Tauri commands to use wrapper functions
3. Fix issues and retry refactoring

## Success Criteria

- ✅ All wrapper functions removed from `storage.rs` (except judgments - deferred)
- ✅ All Tauri commands use core functions directly (except judgments - deferred)
- ✅ Entity name resolution works in frontend
- ✅ Integration tests created for refactored commands
- ⏳ All existing tests pass (requires manual verification)
- ⏳ No functionality lost (requires manual verification)
- ⏳ Error handling works correctly (requires manual verification)

## Completion Status

**Phase 2.5: Core Storage Integration Refactoring - ~90% Complete**

### Completed ✅
- Analysis & Planning (T143-T145)
- Database Initialization Refactoring (T146-T147)
- Events Commands Refactoring (T149-T153)
- Impacts Commands Refactoring (T154)
- Knowledge Base Commands Refactoring (T164-T167)
- Metrics/Summary Commands Refactoring (T168-T169)
- Wrapper Function Removal (T171, T175-T179)
- Integration Tests (T180, T181, T184, T185)

### Deferred ⏳
- Judgments Commands (T160-T163, T172-T174) - Requires schema migration from `judgments` to `judgments_ci`
- Statements Commands (T157-T159) - N/A (no statements commands exist)

### Pending Manual Verification ⏳
- T187: Verify no functionality is lost
- T188: Verify error handling

### Notes
- Judgments refactoring is deferred due to schema mismatch between Desktop's legacy `judgments` table and core's `judgments_ci` table
- All other commands successfully refactored to use core functions
- Entity name resolution implemented and tested in frontend
- Integration tests created for all refactored command categories

