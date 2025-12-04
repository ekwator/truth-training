# Data Model — Desktop DB Init & Context UX Hardening

**Scope**: Canonical Truth schema reused by desktop `init_app`, validation data for context selectors, and persistence strategy for localization settings.  
**Source of Truth**: `core/src/storage.rs::SCHEMA_SQL`

## 1. Core Entities

### 1.1 Knowledge Base Tables
| Table | Purpose | Key Columns / Constraints |
|-------|---------|---------------------------|
| `category` | High-level domains (Social, Financial, etc.) | `id` PK (INTEGER), `name` UNIQUE, `description` |
| `cause`, `develop`, `effect`, `forma` | Qualitative axes for contexts | `id` PK, `quality` (0/1), FK targets for `context` |
| `impact_type` | Lookup for impact categories | `id` PK, `name`, `description` |

### 1.2 Context Lookup
- **Table**: `context`
- **Fields**: `id` PK, `name`, FKs to `category`, `forma`, `cause`, `develop`, `effect`, plus `description`.
- **Usage**: Combo-box data source and validation whitelist. IDs are SMALLINT-sized (fits `< 10k`).

### 1.3 Truth Events & Statements
- **`truth_events`**: Autoincrement PK, descriptive columns, context FK pointers, `vector`, `detected`, `corrected`, timestamps, `code`, `collective_score`.
- **`statements`**: FK `event_id` → `truth_events.id`, stores narrative text, context, truth_score.
- **`impact`**: FK `event_id`, `type_id`, value (bool/enum) plus metadata.
- **`progress_metrics`**: aggregate metrics tracked per timestamp.

### 1.4 Support Tables
- **`schema_version`**: tracks migrations; seeded with `version='1.0.0'`.
- **`node_ratings`, `group_ratings`, `participants`, `events_ci`, `judgments`, `consensus_ci`, `reputation_history`**: already present in `core` schema; `init_app` must keep them intact to preserve parity with CLI/server surfaces.

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
truth_events.id ───────────> │ statements  │
truth_events.id ───────────> │ impact      │
truth_events.id ───────────> │ progress_metrics (aggregate) │
```

*All FK chains must remain ON to satisfy Constitution Rule 5 (normal forms).*

## 3. Legacy Cleanup & Migration Flow

1. **Pre-flight**: Wrap init in a transaction, turn `PRAGMA foreign_keys = OFF` only while dropping obsolete tables to avoid constraint errors.
2. **Drop list**: `events`, `impacts` (legacy), `summaries`, `judgments` (legacy CI), `logs`, and any table in `sqlite_schema` matching the old inline SQL.
3. **Recreate schema**: Call shared helper (new module re-exporting `core::storage::SCHEMA_SQL` or `init_db`) to run the authoritative SQL + migrations.
4. **Validation query**: Assert `SELECT COUNT(*) FROM sqlite_master WHERE name IN ('events','summaries',...) = 0`. Fail `init_app` if >0.
5. **Version tracking**: Ensure `schema_version` row exists post-run; if missing, insert `1.0.0`.

## 4. Context Validation Dataset

- Desktop fetches `context` rows via `ApiService.getContexts()` (Tauri command → rusqlite query).
- Derived view for UI: `SELECT id, name, category_id, forma_id, cause_id, develop_id, effect_id FROM context ORDER BY name`.
- Provide `ContextOption` DTO with `id: number`, `label: string`, `metadata: {category, forma,...}` to populate combo boxes and tooltips.
- Manual entry path must cross-check typed ID against this dataset before allowing submit; store last sync timestamp to detect staleness.

## 5. Localization Preference Persistence

- **Config JSON**: extend `AppConfig` with `locale` (default `en`), persisted at `~/.truth-training/config.json`.
- **UI**: also writes to `localStorage` for instant React updates; on launch, Tauri command exposes current config → front-end hydrates locale switch.
- **No database schema changes** required; config update keeps localization state out of SQLite to avoid cross-surface conflicts.

## 6. Data Volume & Performance Expectations

- SQLite file per user, expected <10k `truth_events` in desktop scenarios.
- Context lookup table seeded with <100 rows; can be fully loaded into memory for autocomplete.
- `init_app` should complete in <1s on SSD (VACUUM + schema recreation). Use WAL mode to keep writes fast.

## 7. Testing Hooks

- **Rust (Tauri backend)**: add test harness that opens an in-memory SQLite, runs the new init path, and asserts required tables exist + legacy tables absent.
- **React**: component/unit tests for context selector verifying whitelist enforcement.
- **Docs**: link-check script over updated quickstarts to confirm table-of-contents anchors survive GitHub rendering.

