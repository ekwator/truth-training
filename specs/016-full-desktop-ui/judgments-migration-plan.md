# Judgments Schema Migration Plan

**Branch**: `016-full-desktop-ui` | **Date**: 2025-12-09  
**Related Tasks**: Remaining tasks for judgments schema migration

## Summary

This document outlines the migration plan for the judgments schema from legacy format to Collective Intelligence (CI) format. The migration involves:

1. Migrating data from legacy `judgments` table (no participant_id, references truth_events) to CI `judgments_ci` table (with participant_id, references events_ci)
2. Creating participants for existing judgments
3. Mapping truth_events to events_ci where applicable
4. Updating Desktop UI code to use CI functions
5. Ensuring data integrity throughout migration

## Current State Analysis

### Legacy Judgments Schema (Desktop UI)
- **Table**: `judgments` (legacy)
- **Schema**:
  ```sql
  CREATE TABLE judgments (
      id TEXT PRIMARY KEY,
      event_id TEXT/INTEGER,  -- references truth_events.id
      assessment TEXT NOT NULL,
      confidence_level REAL NOT NULL,
      reasoning TEXT,
      submitted_at TEXT NOT NULL
  )
  ```
- **Location**: `ui/desktop/src-tauri/src/storage.rs`
- **Usage**: Desktop UI wrapper functions

### CI Judgments Schema (Core)
- **Table**: `judgments_ci` (in code) or `judgments` (in SCHEMA_SQL - **INCONSISTENCY**)
- **Schema**:
  ```sql
  CREATE TABLE judgments (
      id TEXT PRIMARY KEY,
      participant_id TEXT NOT NULL REFERENCES participants(id),
      event_id TEXT NOT NULL REFERENCES events_ci(id),
      assessment TEXT NOT NULL,
      confidence_level REAL NOT NULL,
      reasoning TEXT,
      submitted_at INTEGER NOT NULL,
      signature TEXT NOT NULL,
      UNIQUE(participant_id, event_id)
  )
  ```
- **Location**: `core/src/storage.rs`
- **Functions**: `ci_insert_judgment()`, `ci_get_judgments_by_event()`

### Key Issues Identified

1. **Table Name Inconsistency**: 
   - SCHEMA_SQL defines `judgments` table with CI schema
   - Code uses `judgments_ci` table name
   - **Resolution needed**: Unify table name

2. **Schema Mismatch**:
   - Legacy: event_id references `truth_events` (INTEGER)
   - CI: event_id references `events_ci` (TEXT/UUID)
   - Legacy: No participant_id or signature
   - CI: Requires participant_id and signature

3. **Data Mapping Challenge**:
   - Legacy judgments reference `truth_events`
   - CI judgments reference `events_ci`
   - Need strategy for mapping truth_events → events_ci

## Migration Strategy

### Phase 1: Schema Unification and Preparation

#### Step 1.1: Resolve Table Name Inconsistency
- **Task**: Determine if table should be `judgments` or `judgments_ci`
- **Decision**: Use `judgments` (as defined in SCHEMA_SQL) for consistency
- **Action**: Update `core/src/storage.rs` to use `judgments` instead of `judgments_ci`

#### Step 1.2: Create Migration Detection Function
- **Task**: Enhance `legacy_judgments_exists()` function to detect legacy schema
- **Action**: Function already exists in `core/src/storage.rs:424`, verify it works correctly

#### Step 1.3: Ensure CI Tables Exist
- **Task**: Verify `participants` and `events_ci` tables exist
- **Action**: Check SCHEMA_SQL includes these tables (already present)

### Phase 2: Participant Creation Strategy

#### Step 2.1: Default Desktop Participant
- **Task**: Create a default participant for Desktop UI
- **Strategy**: 
  - Generate or use a default public key for Desktop UI
  - Create participant record if not exists
  - All migrated judgments will reference this participant

#### Step 2.2: Participant ID Generation
- **Task**: Ensure all legacy judgments get a participant_id
- **Strategy**:
  - For each unique judgment source, create a participant
  - Or use single default participant for Desktop UI
  - Generate UUIDs for participant IDs

### Phase 3: Event Mapping Strategy

#### Step 3.1: Truth Events to Events CI Mapping
- **Task**: Map truth_events to events_ci
- **Options**:
  1. **Create events_ci entries for each truth_event** (recommended)
     - Extract data from truth_events
     - Create corresponding events_ci entry
     - Map IDs (truth_events.id INTEGER → events_ci.id TEXT/UUID)
  
  2. **Hybrid approach**
     - Only create events_ci for truth_events that have judgments
     - Keep truth_events unchanged
     - Create mapping table or use conversion logic

#### Step 3.2: Event ID Conversion
- **Task**: Convert event_id from INTEGER (truth_events) to TEXT/UUID (events_ci)
- **Strategy**:
  - Generate UUID for each truth_event → events_ci mapping
  - Store mapping in temporary table or in-memory during migration
  - Use mapping to convert judgment.event_id

### Phase 4: Migration Implementation

#### Step 4.1: Create Migration Function
- **Location**: `core/src/storage.rs`
- **Function**: `migrate_legacy_judgments_to_ci()`
- **Logic**:
  1. Check if legacy judgments exist using `legacy_judgments_exists()`
  2. If yes, proceed with migration:
     a. Create default participant (if needed)
     b. For each truth_event with judgments:
        - Create events_ci entry (if not exists)
        - Store mapping (truth_events.id → events_ci.id)
     c. For each legacy judgment:
        - Get mapped event_id (truth_events → events_ci)
        - Generate or get participant_id
        - Generate signature (use empty or placeholder for legacy data)
        - Insert into CI judgments table
     d. Verify data integrity
     e. Optionally: Drop legacy judgments table

#### Step 4.2: Migration Safety
- **Transaction**: Wrap entire migration in SQL transaction
- **Rollback**: On error, rollback all changes
- **Backup**: Optionally backup database before migration
- **Verification**: After migration, verify:
  - All judgments migrated
  - No data loss
  - Foreign key constraints satisfied
  - Participant counters updated

### Phase 5: Desktop UI Code Update

#### Step 5.1: Update Storage Wrapper
- **File**: `ui/desktop/src-tauri/src/storage.rs`
- **Tasks**:
  - Replace `insert_judgment()` to use `core::collective_intelligence::ci_insert_judgment()`
  - Replace `list_judgments_for_event()` to use `ci_get_judgments_by_event()`
  - Replace `get_judgment_stats()` to query CI judgments
  - Remove legacy wrapper functions

#### Step 5.2: Update Tauri Commands
- **File**: `ui/desktop/src-tauri/src/commands/judgments.rs`
- **Tasks**:
  - Update commands to use CI functions
  - Ensure participant_id is provided
  - Handle signature generation

#### Step 5.3: Update Frontend Types
- **File**: `ui/desktop/src/types/judgments.ts`
- **Tasks**:
  - Update `Judgment` interface to include `participant_id` and `signature`
  - Ensure compatibility with CI schema

### Phase 6: Testing and Validation

#### Step 6.1: Migration Testing
- **Test Cases**:
  1. Migrate database with legacy judgments
  2. Verify all judgments migrated correctly
  3. Verify participant records created
  4. Verify events_ci entries created
  5. Verify foreign key constraints
  6. Test rollback on error

#### Step 6.2: Integration Testing
- **Test Cases**:
  1. Desktop UI can create new judgments via CI functions
  2. Desktop UI can list judgments for events
  3. Desktop UI can get judgment statistics
  4. No regression in existing functionality

#### Step 6.3: Data Integrity Testing
- **Test Cases**:
  1. Verify no data loss
  2. Verify participant counters correct
  3. Verify signatures (even if placeholder)
  4. Verify unique constraint (participant_id, event_id)

## Implementation Tasks

### Priority 1: Core Migration Logic
- [ ] T-MIG-001: Resolve table name inconsistency (judgments vs judgments_ci)
- [ ] T-MIG-002: Create `migrate_legacy_judgments_to_ci()` function
- [ ] T-MIG-003: Implement participant creation logic
- [ ] T-MIG-004: Implement truth_events → events_ci mapping
- [ ] T-MIG-005: Implement judgment data migration
- [ ] T-MIG-006: Add migration to `run_migrations()` function

### Priority 2: Desktop UI Updates
- [ ] T-MIG-007: Update `storage.rs` wrapper functions
- [ ] T-MIG-008: Update Tauri commands in `judgments.rs`
- [ ] T-MIG-009: Update TypeScript types
- [ ] T-MIG-010: Remove legacy wrapper functions

### Priority 3: Testing
- [ ] T-MIG-011: Unit tests for migration function
- [ ] T-MIG-012: Integration tests for migrated data
- [ ] T-MIG-013: End-to-end tests for Desktop UI

## Risks and Mitigation

### Risk 1: Data Loss During Migration
- **Mitigation**: 
  - Use transactions
  - Create database backup
  - Verify counts before/after

### Risk 2: Foreign Key Violations
- **Mitigation**:
  - Ensure participants exist before migration
  - Ensure events_ci exist before migration
  - Use temporary mapping table

### Risk 3: Signature Generation for Legacy Data
- **Mitigation**:
  - Use placeholder signatures for legacy data
  - Document that legacy data has placeholder signatures
  - Optionally: Allow re-signing later

### Risk 4: Performance Impact
- **Mitigation**:
  - Run migration during low usage
  - Batch operations where possible
  - Test with large datasets

## Success Criteria

1. All legacy judgments successfully migrated to CI schema
2. No data loss during migration
3. All foreign key constraints satisfied
4. Desktop UI functions correctly with CI schema
5. Migration is idempotent (can be run multiple times safely)
6. Tests pass for all migration scenarios

## Notes

- **Table Name Issue**: Code uses `judgments_ci` but SCHEMA_SQL defines `judgments`. This needs to be resolved first.
- **Signature Requirement**: CI schema requires signatures. Legacy data doesn't have signatures. Strategy: Use placeholder signatures or generate dummy signatures.
- **Events CI Dependency**: Migration requires events_ci table and entries. Need strategy for creating events_ci from truth_events.

## References

- Legacy judgments: `ui/desktop/src-tauri/src/storage.rs:67-164`
- CI judgments: `core/src/storage.rs:2041-2070`
- Schema definition: `core/src/storage.rs:278-288`
- Refactoring plan: `ui/desktop/REFACTORING_PLAN.md:32-46`

