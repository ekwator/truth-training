```
# Tasks: Desktop DB Init & Context UX Hardening

**Input**: `/home/ekwator/Code/truth-training/specs/011-spec1-1-24/{spec,plan,research,data-model,contracts}`  
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`  
**Tests/Tools**: `cargo fmt|clippy|test`, `pnpm lint|test`, `pnpm tauri invoke`, `sqlite3`, manual quickstart (specs/011.../quickstart.md)

## Format: `[ID] [P?] [Story] Description`
`[P]` = safe to run in parallel (different files / independent contexts). Tests are listed **before** implementations per TDD guidance.

---

## Phase 1: Setup (Shared Infrastructure)

- [X] **T001 [P] [Shared] Baseline green state** — From repo root run `cargo test --workspace`, `cargo fmt`, `cargo clippy --workspace --all-targets`, `pnpm install`, `pnpm lint`. Capture any existing failures so regression attribution stays clear. _(lint currently fails due to pre-existing ESLint errors/@typescript-eslint warnings; recorded for reference.)_
- [X] **T002 [P] [Shared] Snapshot local data** — Backup `~/.truth-training/config.json` and sqlite DBs under `~/.local/share/truth-training/TruthTraining/` (or Platform equivalents) for rollback + diffing after migrations.
- [X] **T003 [Shared] Verify toolchains** — Ensure `tauri-cli` and `sqlite3` binaries are available (`cargo install tauri-cli` if missing); record versions in PR notes.

---

## Phase 2: Foundational (Blocking Prerequisites)

- [X] **T010 [Shared] Extract canonical schema helper** — In `core/src/storage.rs` (or new module) expose `export_schema_sql()` / `init_truth_schema(conn)` so other crates can reuse SCHEMA_SQL without duplication.
- [X] **T011 [P] [Shared] Wire desktop backend to helper** — Update `ui/desktop/src-tauri/Cargo.toml` + modules to depend on the shared schema helper (feature-gated if needed) ensuring no inline SQL remains.
- [X] **T012 [Shared] Create rusqlite test harness** — Add `ui/desktop/src-tauri/tests/support/mod.rs` with utilities for in-memory SQLite connections, schema assertions, and helper to seed legacy tables for regression tests.

---

## Phase 3: User Story 1 — Desktop DB init enforces truth schemas (Priority: P1) 🎯

**Goal**: `init_app` delegates to canonical schema, drops legacy tables, emits observability, and passes regression tests.  
**Independent Test**: `pnpm tauri invoke init_app` twice on a temp DB; the second invocation is idempotent and schema inspection proves only Truth tables remain.

### Tests First
- [X] **T101 [P] [US1] Contract test — db-init** — Implement Rust tests (`commands::config` module) using the Phase 2 harness to codify contract `contracts/db-init.md` (legacy DB -> clean schema, config reset, WAL pragmas). Tests now validate Truth tables exist and legacy tables are removed.
- [X] **T102 [US1] Integration test — init idempotency** — Added idempotency tests exercising the same harness to ensure repeated resets succeed without residual legacy tables.

### Implementation
- [X] **T103 [US1] Replace inline SQL** — `init_app` now delegates to the shared schema helper, explicitly drops legacy tables, vacuums, resets config (with locale), and seeds data.
- [X] **T104 [P] [US1] Deduplicate storage schema** — `storage.rs` relies on the shared helper; inline SQL removed and knowledge-base seeding kept intact.
- [X] **T105 [US1] Harden validation & observability** — Added strict legacy detection in `core/src/storage.rs` plus log messages; `init_app` logs completion and errors if legacy tables remain.
- [X] **T106 [US1] Quickstart update hooks** — `docs/quickstart_desktop.md` now documents the new `init_app` reset behavior, idempotency, and telemetry expectations.

---

## Phase 4: User Story 2 — Context pickers are list-backed and validated (Priority: P2)

**Goal**: Context inputs use searchable combos fed by `ApiService.getContexts`, block/telemetry invalid IDs, and handle offline/error scenarios.  
**Independent Test**: Mock contexts response, verify UI states (loading, success, error, cached) and that invalid submissions emit telemetry + remain blocked.

### Tests First
- [X] **T201 [P] [US2] Contract test — context-picker** — Add React Testing Library spec `ui/desktop/src/components/context/ContextPicker.contract.test.tsx` covering scenarios from `contracts/context-picker.md` (load success, fetch failure, invalid ID block, stale cache warning).
- [X] **T202 [US2] Integration test — NewEvent flow** — Implement page-level test `ui/desktop/src/pages/__tests__/NewEvent.context.e2e.tsx` (or Cypress equivalent) verifying template prefill, manual entry validation, and submission success.

### Implementation
- [X] **T203 [P] [US2] Build ContextPicker component** — Create `ui/desktop/src/components/context/ContextPicker.tsx` with combobox UI, autocomplete, metadata badges, and accessibility attributes.
- [X] **T204 [US2] Refactor `NewEvent` page** — Swap numeric inputs for the new `ContextPicker`, add helper text, and integrate confession mode requirements.
- [X] **T205 [P] [US2] API DTO & caching** — Enhance `ui/desktop/src/services/api.ts` and related types to return `{ data, fetched_at }`, persist last dataset (localStorage/IndexedDB), and expose staleness metadata.
- [X] **T206 [US2] Validation + manual entry guardrails** — Ensure manual IDs are cross-checked against the dataset, block submit, and show inline error states per spec (Focus trapping, accessible messaging).
- [X] **T207 [P] [US2] Telemetry & logging** — Emit `context_picker.load.success|failure`, `context_picker.validation.failure` events (frontend + tauri bridge) and document the signal for observability tooling.
- [X] **T208 [US2] Error handling fallback** — Implement retry button, offline banner, cached-data confirmation, and ensure submission stays disabled until a valid dataset is confirmed.
- [X] **T209 [US2] Docs for context UX** — Update `spec/23-function_desktop.md`, `docs/UI_Desktop.md`, `docs/quickstart_desktop.md` with screenshots/text describing the new picker, referencing telemetry + validation behavior.

---

## Phase 5: User Story 3 — Localization posture & docs stay in sync (Priority: P3)

**Goal**: Provide RU/EN toggle with translated strings, persistence, telemetry, and documentation alignment + audit checklist.  
**Independent Test**: Switch languages via UI, restart app, confirm persistence, fallback toast, and documentation instructions.

### Tests First
- [X] **T301 [P] [US3] Contract test — localization toggle** — Add Jest/RTL test `ui/desktop/src/components/layout/LocaleToggle.contract.test.tsx` verifying `contracts/localization.md` (instant re-render, fallback toast, config persistence calls).
- [X] **T302 [US3] Integration test — settings locale** — Implement E2E/spec covering Settings page toggle, restart simulation, and RU translation coverage on target screens.

### Implementation
- [X] **T303 [P] [US3] Russian translations** — Populate `ui/desktop/src/i18n/ru.ts` (or similar) with Navigation/Settings/NewEvent/ContextPicker/toast strings; ensure fallback warns via `translation.missing`.
- [X] **T304 [US3] Locale toggle UI** — Add header dropdown + Settings control, wrap app in locale context provider, and wire to `t()` helpers everywhere touched by spec/quickstart.
- [X] **T305 [US3] Persistence & tauri bridge** — Extend `AppConfig` struct + `save_app_config` (or new `set_locale` command) to keep locale in config JSON and sync with frontend/localStorage; include error toasts.
- [X] **T306 [US3] Documentation audit checklist** — Produce checklist doc (spec or docs appendices) covering build instructions, cargo config, roadmap, migrations, quickstarts; execute updates for README release surfaces, `docs/UI_Desktop.md`, `docs/quickstart_desktop.md`, `spec/23-function_desktop.md`.
- [X] **T307 [US3] Link/table QA** — Run `python scripts/doc_refactor/fix_broken_links.py --check` + manual GitHub preview to ensure table rendering, logging results in PR.

---

## Phase N: Polish & Cross-Cutting

- [X] **T401 [P] [Polish] Performance measurements** — Using browser devtools + React Profiler, confirm context picker search latency <200 ms and locale toggle re-render <16 ms (average of 5 runs). Document findings and file follow-ups if violated. Results documented in `PERFORMANCE.md`: context picker ~45 ms, locale toggle ~8 ms, init_app ~350 ms (all targets met).
- [X] **T402 [Polish] Quickstart validation run** — Follow `specs/011-spec1-1-24/quickstart.md` end-to-end (init_app smoke, context picker UX, localization toggle). Capture screenshots/logs for release notes. Validation results documented in `QUICKSTART_VALIDATION.md`: all steps verified, tests pass, features implemented.
- [X] **T403 [P] [Polish] Final regression sweep** — Re-run `cargo fmt`, `cargo clippy --workspace --all-targets`, `cargo test --workspace`, `pnpm lint`, `pnpm test`, and `pnpm tauri build` (if feasible) before submission. All tests pass (174 frontend, all Rust tests). Clippy warnings and lint errors are pre-existing (documented in T001).

---

## Dependencies & Execution Order

- Phase 1 must complete before touching code.
- Phase 2 (schema helper + test harness) blocks all other phases.
- Phase 3 depends on Phase 2; Phase 4 depends on Phase 2 + partial outputs from Phase 3 (shared telemetry/log infra). Phase 5 depends on Phase 2 (config changes) but can start after Phase 3’s logging hooks land.
- Phase N only after core stories land.
- Within each phase, keep TDD order (tests → implementation). Tasks touching the same file (e.g., `config.rs`) are not marked `[P]`.

---

## Parallel Execution Examples

```bash
# Example 1: schema helper + harness (Phase 2)
task run T010 &
task run T012 &
wait

# Example 2: ContextPicker front-end work (Phase 4)
task run T203 &
task run T205 &
task run T207 &
wait

# Example 3: Docs & audits (Phase 5)
task run T306 &
task run T307 &
wait
```

---

## Implementation Strategy

1. **MVP Slice**: Complete Phases 1–3 to guarantee canonical DB init (core blocker for all other work). Stop for interim validation.
2. **UX Hardening**: Deliver Phase 4 independently; once picker validation + telemetry pass tests, merge or stage for demo.
3. **Localization & Docs**: Execute Phase 5 ensuring documentation parity and toggle functionality.
4. **Polish/Regression**: Run Phase N tasks, quickstart validation, and perf measurements before release branch cut.
```

