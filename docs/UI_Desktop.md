## Desktop UI Updates (Tauri)

This document summarizes the recent Desktop UI (Tauri) work and current behavior.

### Key Features
- Events stored in SQLite (rusqlite 0.31, bundled SQLite). Database initialized at app startup in the OS data directory.
- Event listing and details are read from SQLite via Tauri commands:
  - `list_events_fast(page, per_page)` – paginated list
  - `get_event_fast(event_id)` – single event
- Judgment flow (end-to-end):
  - SQLite table `judgments` with fields: `id`, `event_id`, `assessment ('true'|'false'|'uncertain')`, `confidence_level (0..1)`, `reasoning`, `submitted_at` (RFC3339)
  - Tauri commands:
    - `submit_judgment_fast(request)`
    - `judgments_list_fast(event_id, page, per_page)`
    - `get_judgment_stats(event_id)`
  - UI: Judge modal on `EventCard` submits data through `ApiService.createJudgment` (Tauri branch)

### Knowledge Base Context
- Create Event modal now loads Knowledge Base contexts dynamically via Tauri:
  - Command: `knowledge_base_list()` parses `docs/Data_Schema.md` (section 1. knowledge_base) and returns a flat list of `{ id, label }`.
  - Selected context is saved in the event payload (stored in `category` field for now).

### UI/UX Adjustments
- Removed oversized SVG icons across the app; replaced with text-only indicators to avoid large visual artifacts:
  - `SyncStatus` – text statuses only (Synced/Offline/N pending)
  - Empty states on `Dashboard` and `Events` – simple text placeholders
  - `EventCard` meta line – text placeholders for participants/consensus
  - `JudgmentCard` – participant/time displayed as text
  - `ErrorBoundary` – no warning icon
  - `Toaster` – text-only; close button is a text label

### Files Touched (high level)
- UI (React):
  - `ui/desktop/src/pages/Dashboard.tsx`, `ui/desktop/src/pages/Events.tsx`
  - `ui/desktop/src/components/Dashboard/{CreateEventButton,EventCard}.tsx`
  - `ui/desktop/src/components/JudgmentPanel/JudgmentCard.tsx`
  - `ui/desktop/src/components/system/{SyncStatus,Toaster,ErrorBoundary,Modal}.tsx`
  - `ui/desktop/src/services/api.ts` (Tauri branches for Events, Judgments, KB)
- Tauri (Rust):
  - `ui/desktop/src-tauri/src/storage.rs` (DB init, events/judgments CRUD helpers)
  - `ui/desktop/src-tauri/src/commands/{events,judgments,knowledge_base}.rs`
  - `ui/desktop/src-tauri/src/main.rs` (command registration, DB state)
  - `ui/desktop/src-tauri/Cargo.toml` (rusqlite 0.31 + bundled)

### CI Notes
- Desktop builds stabilized for Linux/Windows/macOS (rusqlite bundled avoids system sqlite conflicts).
- Mobile workflows (Android/iOS) build only `-p truth_core` and can download artifacts from Cross-Platform runs.

### Next Steps
- Optional: replace Markdown parsing with a bundled JSON for KB to improve reliability in packaged builds.
- Add views for detailed Event/Judgment navigation (beyond modals).

