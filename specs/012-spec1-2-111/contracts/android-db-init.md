# Contract: Android Database Initialization

**Feature**: Android DB init enforces truth schemas  
**User Story**: User Story 2 (Priority: P1)  
**Status**: Draft

## Preconditions

- Android app is installed on device/emulator
- `TruthDatabase` is initialized via `TruthTrainingApplication.database`
- Shared SQL asset file exists at `app/src/main/assets/schema.sql`

## Contract

### Input
- App launches and `TruthDatabase.getInstance(context)` is called
- Database may be new (clean install) or existing (with legacy tables)

### Output
- Canonical Truth schema is created from shared SQL asset
- All legacy tables (`events`, `impacts`, `summaries`, `logs`) are dropped immediately
- Zero legacy tables remain after initialization
- Database schema matches Desktop SQLite schema structure

### Behavior

1. **Schema Initialization**:
   - `TruthDatabase` initialization MUST read canonical schema from `app/src/main/assets/schema.sql`
   - Schema SQL MUST match `core/src/storage.rs::SCHEMA_SQL` (shared source of truth)
   - All canonical tables MUST be created: `truth_events`, `statements`, `impact`, `progress_metrics`, `context`, `category`, `cause`, `develop`, `effect`, `forma`, `impact_type`, `nodes`, etc.

2. **Legacy Table Removal**:
   - Migration MUST execute `DROP TABLE IF EXISTS events`
   - Migration MUST execute `DROP TABLE IF EXISTS impacts` (legacy, not CI `impact`)
   - Migration MUST execute `DROP TABLE IF EXISTS summaries` (if legacy, not CI `summaries`)
   - Migration MUST execute `DROP TABLE IF EXISTS logs`
   - **No data migration**: Legacy tables are dropped immediately without attempting data migration (matching Desktop behavior)

3. **Validation**:
   - After initialization, automated check MUST verify legacy tables are absent
   - Query: `SELECT COUNT(*) FROM sqlite_master WHERE name IN ('events','impacts','summaries','logs')` MUST return 0
   - If legacy tables remain, initialization MUST fail with error

4. **Room Migration**:
   - `TruthDatabaseMigrations` MUST include migration that drops legacy tables
   - Migration MUST be idempotent (safe to run multiple times)
   - Migration MUST use `DROP TABLE IF EXISTS` to avoid errors if tables don't exist

## Success Criteria

- **SC-002**: Android database initialization results in canonical Truth schema with zero legacy `events` tables, verified by automated tests (instrumented or unit) that inspect schema after initialization.

## Test Cases

### TC-001: Clean Install Initialization
1. Install Android app on clean device/emulator
2. Launch app (triggers database initialization)
3. Inspect database schema via Room database inspector or SQLite
4. **Expected**: Only Truth tables exist (`truth_events`, `impact`, `progress_metrics`, `context`, etc.), no legacy `events` table

### TC-002: Legacy Database Migration
1. Install Android app with existing legacy database (contains `events`, `impacts`, `summaries`, `logs` tables)
2. Launch app (triggers migration)
3. Inspect database schema
4. **Expected**: Legacy tables are dropped, canonical Truth tables exist, no data migration attempted

### TC-003: Regression Protection
1. Run automated test that initializes database
2. Query `sqlite_master` for legacy table names
3. **Expected**: Test fails if any legacy table exists after initialization

## Observability

- Log database initialization events: `android.db.init.start`, `android.db.init.success`, `android.db.init.failure`
- Log legacy table removal: `android.db.legacy.drop.success`, `android.db.legacy.drop.failure`
- Log schema validation: `android.db.schema.validate.success`, `android.db.schema.validate.failure`

## References

- `truth-android-client/app/src/main/java/com/truth/training/client/data/database/TruthDatabase.kt`
- `truth-android-client/app/src/main/java/com/truth/training/client/data/database/TruthDatabaseMigrations.kt`
- `truth-android-client/app/src/main/assets/schema.sql` (to be created)
- `core/src/storage.rs` (canonical schema source)

