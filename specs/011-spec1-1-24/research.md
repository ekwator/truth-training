# Research Notes — Desktop DB Init & Context UX Hardening

**Spec Input**: `/home/ekwator/Code/truth-training/specs/011-spec1-1-24/spec.md`  
**Date**: 2025-12-03  
**Goal**: Align desktop initialization with Truth schema, modernize context selection UX/validation, and finalize localization/documentation posture.

## Current Behavior & Findings

### Desktop `init_app` uses legacy schema
- `ui/desktop/src-tauri/src/commands/config.rs::init_app` truncates a short list of legacy tables (`events`, `impacts`, `summaries`, `judgments`, `logs`) and then recreates the same outdated schema inline.
- The SQL omits Truth tables defined in `core/src/storage.rs` (`truth_events`, `statements`, `impact`, `progress_metrics`, etc.) and never references `storage::init_db`, so desktop DBs drift from the canonical schema.
- No automated verification ensures that obsolete tables are removed after initialization.

### Canonical schema already exists in reusable modules
- `core/src/storage.rs` exposes `init_db(conn)` plus `SCHEMA_SQL` that includes all Truth tables, FK constraints, indexes, and `schema_version` tracking.
- `ui/desktop/src-tauri/src/storage.rs` embeds a near-identical schema (including the `knowledge_base` seed) but still ships compatibility tables for `events`/`impacts` etc.
- Reusing a single SQL source (e.g., pulling the core schema via shared crate or SQL file) will guarantee parity and satisfy Constitution Rule 5 (schema integrity).

### Context entry UI is numeric-only and trusts user input
- `ui/desktop/src/pages/NewEvent.tsx` renders plain `<input type="number">` fields for `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`. There is no dropdown, autocomplete, or validation beyond optional numeric parsing.
- `ApiService.getContexts()` already fetches a typed list from either the Tauri backend (`list_contexts` command) or HTTP API, but the UI only uses it to prefill fields when selecting a template.
- Manual entries can introduce IDs that don't exist in the lookup tables, leading to rejected inserts or inconsistent analytics later.

### Localization scaffolding exists without UI integration
- `ui/desktop/src/i18n/index.ts` defines helpers, supported locales (currently EN/ES/FR/DE/AR) and default strings, but there is no React provider, toggle component, or persisted selection beyond helper functions writing to `localStorage`.
- No Russian translations exist even though specs require RU parity with EN; the Settings screen lacks any locale control.
- Documentation does not clarify the current localization state, which conflicts with Constitution Rule 1 (cross-platform parity) and user expectations.

### Documentation & warnings
- `docs/quickstart_desktop.md` still references the legacy initialization flow and lacks any warning removal plan after the schema fix.
- `spec/23-function_desktop.md`, `docs/UI_Desktop.md`, and README release surfaces currently mention DB inconsistencies and do not describe localization steps.
- The spec demands a full audit of build instructions, cargo configuration, test plans, and quickstarts; no consolidated checklist exists today.

## Opportunities & Decisions

1. **Reuse `core::storage::init_db`** inside the Tauri command (or expose a shared helper) to guarantee schema parity and automatically gain migrations/validation logic.
2. **Drop or rename legacy tables** by running explicit `DROP TABLE IF EXISTS` statements before initialization, then assert their absence via a Tauri-side test (e.g., SQL query or integration test harness).
3. **Introduce a reusable context selector component** (combobox w/ search + manual entry) backed by `getContexts` results, with validation that blocks submission if IDs are not part of the dataset.
4. **Implement RU/EN toggle** (likely via a Settings control or header switch) using `i18n` helpers, add Russian translations for high-traffic pages, and persist choice via `localStorage` + config for offline parity.
5. **Create a documentation audit checklist** enumerating all files to touch; after implementation, update quickstarts/specs/README to remove the broken warning and describe new flows (DB reset, context picker, localization).

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Dropping legacy tables might delete user data | Medium | Run schema backup/export before drop; clearly document in quickstart; limit to tables no longer referenced by core |
| Sharing schema code between `core` and desktop may introduce build coupling | Low | Extract SQL constants into a `core-storage-sql` module or reuse `core` crate as dependency for desktop backend |
| Context list fetch failures block event creation | Medium | Cache last good list, show inline error, allow manual entry only when validation can confirm via fallback list |
| RU translations incomplete | Medium | Prioritize Settings, Navigation, New Event screens; fall back to EN for missing keys and track via lint |
| Documentation audit scope creep | Medium | Template checklist to mark each doc, run link checker script (`scripts/doc_refactor/...`) before completion |

## Impacted Areas

- Rust: `ui/desktop/src-tauri/src/commands/config.rs`, `ui/desktop/src-tauri/src/storage.rs`, possible shared crate exports from `core/src/storage.rs`.
- TypeScript/React: `ui/desktop/src/pages/NewEvent.tsx`, `ui/desktop/src/components/*`, `ui/desktop/src/i18n`, `ui/desktop/src/services/api.ts`.
- Docs/specs: `spec/23-function_desktop.md`, `docs/quickstart_desktop.md`, `docs/UI_Desktop.md`, `README.md` (release surfaces), other quickstarts as listed in spec.
- Tests: new Tauri integration test for schema cleanup, React component tests (if setup) or Cypress-style manual steps captured in quickstart.

## References

- `ui/desktop/src-tauri/src/commands/config.rs`
- `ui/desktop/src-tauri/src/storage.rs`
- `core/src/storage.rs`
- `ui/desktop/src/pages/NewEvent.tsx`
- `ui/desktop/src/services/api.ts`
- `ui/desktop/src/i18n/index.ts`
- `docs/quickstart_desktop.md`
- `spec/23-function_desktop.md` (to be updated post-implementation)

