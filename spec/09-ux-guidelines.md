## UX Guidelines

See `docs/ui_guidelines.md`. This spec aligns endpoint names with current API.

Principles
- No business logic in UI; use API/FFI.
- Show expert wizard with questions and rationale.
- Visualize progress trends; sync status.

CLI UX (truthctl)
- Subcommands mirror domain objects: `peers add/list`, `sync` (with `--pull-only`).
- Consistent flags and defaults: `--db truth_db.sqlite`, `--peers peers.json`, `--verbose`.
- Human-first output by default; JSON output can be added as a follow-up.
- Avoid destructive actions; confirm before overwriting `peers.json`.
- Align names with REST endpoints where possible; avoid inventing new terms.
