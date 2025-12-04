# Data Model — Android Parity with Desktop UI & Startup Fix

**Scope**: Canonical Truth schema for Android Room database, shared SQL assets for schema parity, context validation data for dropdowns, and localization persistence strategy.  
**Source of Truth**: `core/src/storage.rs::SCHEMA_SQL` (via shared SQL asset file)

## 1. Core Entities

### 1.1 Knowledge Base Tables
| Table | Purpose | Key Columns / Constraints | Room Entity |
|-------|---------|---------------------------|-------------|
| `category` | High-level domains (Social, Financial, etc.) | `id` PK (INTEGER), `name` UNIQUE, `description` | `CategoryEntity` |
| `cause`, `develop`, `effect`, `forma` | Qualitative axes for contexts | `id` PK, `quality` (0/1), FK targets for `context` | `CauseEntity`, `DevelopEntity`, `EffectEntity`, `FormaEntity` |
| `impact_type` | Lookup for impact categories | `id` PK, `name`, `description` | `ImpactTypeEntity` |

### 1.2 Context Lookup
- **Table**: `context`
- **Room Entity**: `ContextTemplateEntity`
- **Fields**: `id` PK, `name`, FKs to `category`, `forma`, `cause`, `develop`, `effect`, plus `description`.
- **Usage**: Dropdown data source and validation whitelist for Android `EventCreateScreen`. IDs are INTEGER (fits `< 10k`).
- **Data Source**: Embedded Room database, loaded via `ContextTemplateRepository.getAllTemplatesFlow()`.

### 1.3 Truth Events & Statements
- **`truth_events`**: Autoincrement PK, descriptive columns, context FK pointers (`category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`), `vector`, `detected`, `corrected`, timestamps, `code`, `collective_score`. Room Entity: `EventEntity`.
- **`statements`**: FK `event_id` → `truth_events.id`, stores narrative text, context, truth_score. (Note: May not be implemented in Android yet; verify if needed.)
- **`impact`**: FK `event_id`, `type_id`, value (INTEGER 0/1) plus metadata. Room Entity: `ImpactEntity`.
- **`progress_metrics`**: aggregate metrics tracked per timestamp. Room Entity: `ProgressMetricsEntity`.

### 1.4 Support Tables
- **`schema_version`**: tracks migrations; seeded with `version='1.0.0'`. (Note: May not be implemented in Android Room; verify if needed for parity.)
- **`nodes`**: Discovery table for cross-platform node tracking. Room Entity: `NodeEntity`.
- **`judgments`, `summaries`**: CI schema tables (part of canonical schema, not legacy). Room Entities: `JudgmentEntity`, `SummaryEntity`.
- **`sync_queue`**: Sync queue table. Room Entity: `SyncQueueEntity`.

### 1.5 Legacy Tables (MUST BE DROPPED)
- **`events`**: Legacy table (replaced by `truth_events`). Must be dropped immediately without data migration.
- **`impacts`** (legacy): Legacy table (replaced by `impact`). Must be dropped immediately.
- **`summaries`** (legacy): Legacy table (if different from CI `summaries`). Must be dropped immediately.
- **`logs`**: Legacy logging table. Must be dropped immediately.

## 2. Relationships

```
category 1─┐
          ├─< context >─┐
forma   1─┘             │
cause   1───────────────┤
develop 1───────────────┤
effect  1───────────────┘
                             ┌─────────────┐
context.id ────────────────> │ truth_events│
truth_events.id ───────────> │ impact      │
truth_events.id ───────────> │ progress_metrics (aggregate) │
```

*All FK chains must remain ON to satisfy Constitution Rule 5 (normal forms). Room enforces FK constraints via `PRAGMA foreign_keys = ON` in database initialization.*

## 3. Shared SQL Asset for Schema Parity

1. **Extract canonical schema**: Create `truth-android-client/app/src/main/assets/schema.sql` containing the canonical SQL from `core/src/storage.rs::SCHEMA_SQL`.
2. **Android reads at runtime**: Room database initialization reads `schema.sql` from assets and executes it to ensure schema parity with Desktop.
3. **Validation**: Both Android and Desktop validate against the same SQL source, preventing schema drift.

## 4. Legacy Cleanup & Migration Flow

1. **Pre-flight**: Wrap migration in a transaction, ensure `PRAGMA foreign_keys = ON` is set.
2. **Drop list**: `events`, `impacts` (legacy), `summaries` (if legacy, not CI), `logs`. Use `DROP TABLE IF EXISTS` statements in Room migration.
3. **Recreate schema**: Execute shared SQL asset (`schema.sql`) to create canonical tables.
4. **Validation query**: Assert `SELECT COUNT(*) FROM sqlite_master WHERE name IN ('events','impacts','summaries','logs') = 0`. Fail initialization if >0.
5. **Version tracking**: Ensure `schema_version` row exists post-run; if missing, insert `1.0.0`.

## 5. Context Validation Dataset

- Android loads `context` rows via `ContextTemplateRepository.getAllTemplatesFlow()` (Room DAO query).
- Derived view for UI: `SELECT id, name, category_id, forma_id, cause_id, develop_id, effect_id FROM context ORDER BY name`.
- Provide `ContextOption` data class with `id: Int`, `label: String`, `metadata: ContextFields` to populate dropdowns and tooltips.
- Manual entry path must cross-check typed ID against this dataset before allowing submit; store last sync timestamp to detect staleness.
- Validation logic in `EventRepository` or `EventCreateScreen` blocks submission if IDs are not in the lookup list.

## 6. Localization Preference Persistence

- **SharedPreferences**: Store `locale` (default `en`) in Android `SharedPreferences` or Room database config table.
- **UI**: Compose state updates immediately; on launch, read preference and hydrate locale switch.
- **No database schema changes** required for locale; config update keeps localization state separate from SQLite to avoid cross-surface conflicts.
- **Status**: Must be audited to determine if RU/EN switching exists or if EN-only; document clearly in specs and quickstarts.

## 7. Data Volume & Performance Expectations

- SQLite file per Android app instance, expected <10k `truth_events` in typical scenarios.
- Context lookup table seeded with <100 rows; can be fully loaded into memory for dropdown population.
- Database initialization should complete in <1s on modern Android devices. Use WAL mode (`PRAGMA journal_mode=WAL`) to keep writes fast.
- Context dropdown load should complete in <200ms for ≤100 options.

## 8. Testing Hooks

- **Android (Room/Instrumented tests)**: Add test harness that opens an in-memory Room database, runs the new init path, and asserts required tables exist + legacy tables absent.
- **Compose UI tests**: Component tests for context picker verifying whitelist enforcement and dropdown behavior.
- **Docs**: Link-check script over updated quickstarts to confirm table-of-contents anchors survive GitHub rendering.

