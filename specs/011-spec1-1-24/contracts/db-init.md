# Contract: Desktop `init_app` Schema Reset

## Actors
- **Invoker**: Tauri command `init_app` (`ui/desktop/src-tauri/src/commands/config.rs`)
- **Storage**: SQLite DB managed by `ui/desktop/src-tauri/src/storage.rs` (rusqlite)
- **Source Schema**: `core::storage::init_db` (Truth canonical schema)

## Preconditions
1. Desktop app has access to the DB file and config path.
2. Rusqlite connection can be locked for exclusive access during init.
3. `core` schema SQL is available to the Tauri backend (via shared crate or extracted SQL module).

## Flow
1. **Reset config**: overwrite `~/.truth-training/config.json` with defaults (now includes `locale`).
2. **Begin reset transaction**:
   - `PRAGMA foreign_keys = OFF;`
   - `PRAGMA journal_mode = WAL;`
3. **Drop legacy tables**: run `DROP TABLE IF EXISTS` for `events`, `impacts`, `summaries`, `judgments`, `logs`, plus any other tables detected by `sqlite_master` whose schema matches the deprecated inline SQL.
4. **Vacuum**: `PRAGMA wal_checkpoint(TRUNCATE); VACUUM;`
5. **Recreate schema**: call shared helper to execute canonical SQL and run migrations/validation.
6. **Verification query**:
   ```sql
   SELECT name FROM sqlite_master
   WHERE type='table'
     AND name IN ('events','impacts','summaries','judgments','logs');
   ```
   Fail if any rows remain.
7. **Return status**: `CoreStatus { ok: true, message: "Initialized config and database" }`.

## Acceptance Criteria
- After `init_app`, `schema_version` contains at least version `1.0.0`.
- Tables `truth_events`, `statements`, `impact`, `progress_metrics`, `context`, `category`, `cause`, `develop`, `effect`, `forma`, `impact_type` all exist.
- Legacy tables listed above do **not** exist.
- Automated test covers both clean DB and DB pre-seeded with legacy tables.

## Failure Cases
- Schema verification fails → return `CoreStatus { ok: false, message }` and leave DB untouched (rollback transaction).
- Rusqlite errors bubble up with actionable message (include table name).
- Config write errors abort before DB reset (no partial changes).

## Observability
- Emit structured log per major step (config reset, drop, schema recreate, validation).
- Capture metrics: duration of init, count of dropped tables.

