# Implementation Plan: Desktop DB Init & Context UX Hardening

**Branch**: `011-spec1-1-24` | **Date**: 2025-12-03 | **Spec**: `/home/ekwator/Code/truth-training/specs/011-spec1-1-24/spec.md`  
**Input**: Feature specification from `/specs/011-spec1-1-24/spec.md`

## Summary

Desktop initialization must drop legacy SQLite tables and recreate the canonical Truth schema, event creation needs validated context selectors backed by real lookup data, and the UI must expose a working RU/EN localization toggle with documentation that reflects the new behavior. We will reuse `core::storage` helpers to guarantee schema parity, introduce a reusable combobox component fed by `ApiService.getContexts()`, persist locale choice via `AppConfig` + React context, and execute a documentation audit covering specs, quickstarts, and release surfaces.

## Technical Context

**Language/Version**: Rust stable (≥1.75) for Tauri/backend, TypeScript 5.x + React 18 for UI, Markdown docs  
**Primary Dependencies**: Tauri 1.x, `rusqlite`, `parking_lot`, `dirs`, React, Tailwind utility classes, Axios-based `ApiService`, localStorage  
**Storage**: SQLite (WAL) via `core/src/storage.rs`; config JSON at `~/.truth-training/config.json`  
**Testing**: `cargo test --workspace`, `cargo clippy`, `pnpm lint`, `pnpm test`, manual quickstart, link checker (`scripts/doc_refactor/fix_broken_links.py --check`)  
**Target Platform**: Desktop (Linux/Windows/macOS) with offline capability; docs rendered on GitHub  
**Project Type**: Multi-surface (shared Rust core + Tauri backend + React frontend + Markdown documentation)  
**Performance Goals**: `init_app` <1 s on clean DB, context picker search latency <200 ms for ≤100 options, locale toggle re-render <16 ms  
**Constraints**: Remove all legacy tables, enforce Rule 5 normal forms, maintain offline UX, accessible combobox (ARIA), RU/EN parity for key flows, log `context_picker.*` + `translation.missing` events, keep submission disabled when context data is stale/unavailable  
**Scale/Scope**: Single-tenant desktop DB (<10k events), ~100 context rows, documentation updates across ≥5 files, translations for primary screens

## Constitution Check

- ✅ **Rule 1 — Cross-Platform Scope & Parity**: Matching desktop schema/localization with `core` + documentation keeps parity with CLI/server/mobile surfaces.
- ✅ **Rule 5 — Database & Schema Integrity**: Reusing `core::storage::init_db` plus validation tests enforces canonical tables, FK constraints, and migration tracking.
- ✅ **Rule 6 — CI, Tooling & Automation**: Spec-Kit artifacts (research, data-model, quickstart, contracts, tasks) generated here; plan mandates automated tests + link validation.
- ✅ **Rule 7 — Security & Privacy Enforcement**: Locale persistence stores no secrets; DB reset avoids half-applied migrations and documents safe-ops steps.

## Project Structure

### Documentation (this feature)
```text
specs/011-spec1-1-24/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── db-init.md
│   ├── context-picker.md
│   └── localization.md
└── tasks.md
```

### Source Code (repository root)
```text
core/
└── src/storage.rs              # Canonical schema + migrations

ui/desktop/
├── src-tauri/
│   └── src/
│       ├── commands/config.rs  # init_app command
│       └── storage.rs          # Desktop DB wrapper + seeding
├── src/
│   ├── pages/NewEvent.tsx      # Event creation form
│   ├── components/context/     # New context picker module (to add)
│   ├── services/api.ts         # ApiService.getContexts + DTOs
│   └── i18n/                   # Locale definitions + helpers
└── package.json / pnpm-lock.yaml / tsconfig.json

docs/
├── quickstart_desktop.md
├── UI_Desktop.md
├── README.md (release surfaces)
├── spec/23-function_desktop.md
└── other quickstarts for parity references
```

**Structure Decision**: Retain monorepo layout, sharing schema logic by depending on the `core` crate inside the Tauri backend, add a dedicated React component folder for context selectors, and update documentation files under `docs/` and `spec/`.

## Complexity Tracking

No constitutional violations or extra subsystems beyond existing crates; no additional justification required.

## Progress Tracking

| Phase | Output | Status | Notes |
|-------|--------|--------|-------|
| Phase 0 | `research.md` | ✅ Complete | Captures current gaps + risks for init_app, context UX, localization. |
| Phase 1 | `data-model.md`, `contracts/`, `quickstart.md` | ✅ Complete | Defines canonical schema usage, UX/API contracts, logging requirements, and validation steps. |
| Phase 2 | `tasks.md` | ✅ Complete | Task list grouped by user story with file-level guidance. |
