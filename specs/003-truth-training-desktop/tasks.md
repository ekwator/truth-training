# Tasks: Truth Training Desktop UI — Text-Only Interface

**Input**: Design documents from `/specs/003-truth-training-desktop/`
**Prerequisites**: plan.md (required), research.md, data-model.md, contracts/

## Phase 3.1: Setup
- [x] T001 Update UI version to 0.2.0 surfaced on Home (ui/desktop frontend)
- [x] T002 Ensure text-only base styles (remove residual icons/emojis) in `ui/desktop/src` [P]
- [x] T003 Configure logs pagination constant (35) in Tauri backend and UI [P]

## Phase 3.2: Tests First (TDD)
- [ ] T004 [P] Contract test: list_kb_contexts schema in `specs/003-truth-training-desktop/contracts/` tests
- [ ] T005 [P] Contract test: create_event input/output schema
- [ ] T006 [P] Contract test: list_events filter + paging
- [ ] T007 [P] Contract test: add_impact payload and response
- [ ] T008 [P] Contract test: list_logs(page,35) shape; clear_logs side-effect
- [ ] T009 Integration test: New Event flow blocks save on empty KB
- [ ] T010 Integration test: Offline-first with Local-wins conflict simulation
- [ ] T011 Integration test: Overall Summary export produces fixed .txt

## Phase 3.3: Core Implementation (ONLY after tests are failing)
- [x] T012 [P] Data model: add/verify Event schema (context_id required) in `ui/desktop/src-tauri/src/storage.rs`
- [x] T013 [P] Data model: Impact (level 1..5) in `storage.rs`
- [x] T014 [P] Data model: Summary (1:1 per Event) in `storage.rs`
- [x] T015 Service/command: `list_kb_contexts` parse from `docs/Data_Schema.md` in `ui/desktop/src-tauri/src/commands/knowledge_base.rs`
- [x] T016 Service/command: `create_event` validates Context presence; forbid save if KB empty
- [x] T017 Service/command: `list_events` with paging
- [x] T018 Service/command: `add_impact` validation and insert
- [x] T019 Service/command: `list_logs` + `clear_logs`; store logs and paginate 35
- [x] T020 UI: Top menu bar with navigation (Home/New Event/Event Summary/Overall Summary/Training Results/Logs)
- [x] T021 UI: Home screen content and actions (Create New Event, Refresh Data)
- [x] T022 UI: New Event form (name, description, Context dropdown from KB, dates); Save/Clear/Go to Event Summary
- [x] T023 UI: Event Summary list → details (description, impact, notes, recommendations) + Add Impact/Edit/Save/Back
- [x] T024 UI: Overall Summary with metrics + text table and Export (.txt)
- [x] T025 UI: Training Results filters + ASCII progress/average
- [x] T026 UI: Logs screen (scrollable, 35 lines/page, Clear/Save/Refresh)
- [x] T027 Keyboard shortcuts Alt+1..Alt+6 for screen navigation

## Phase 3.4: Integration
- [x] T028 Wire UI to Tauri commands (invoke) for all screens
- [x] T029 Offline queue + background sync; enforce Local-wins on conflicts
- [x] T030 Export service for Overall Summary to `.txt` fixed template

## Phase 3.5: Polish
- [ ] T031 [P] Unit tests: validation rules (dates order, impact level)
- [ ] T032 [P] Performance: navigation + pagination < 100ms on baseline
- [ ] T033 [P] Docs: update `docs/UI_Desktop.md` with text-only flows and shortcuts
- [ ] T034 [P] CI: ensure desktop builds (Linux/Windows/macOS) still pass

## Dependencies
- T004–T011 before T012–T027 (TDD)
- T012–T014 before T016–T019 (models before services)
- T015 before T022 (KB contexts in UI)
- T024 before T030 (export requires data aggregation)
- T028 before T029 (wiring before sync policy)

## Parallel Execution Examples
```
# Contracts tests in parallel
Run T004, T005, T006, T007, T008 together

# Data-model in parallel (different sections of storage.rs if separated)
Run T012, T013, T014

# UI screens in parallel (different files)
Run T020, T021, T026
```

## Notes
- [P] tasks only when files don’t conflict.
- Commit after each task; keep diffs focused.
