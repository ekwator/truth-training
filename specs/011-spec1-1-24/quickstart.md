# Quickstart — Desktop DB Init & Context UX Hardening

Use this guide after pulling the feature branch `011-spec1-1-24` to validate DB initialization, context validation UX, localization toggle, and documentation updates.

## Prerequisites
- Rust toolchain (stable), Node.js 18+, pnpm, Tauri CLI (`cargo install tauri-cli`).
- SQLite CLI (`sqlite3`) for schema spot-checks.
- Fresh working tree: `git checkout 011-spec1-1-24 && pnpm install && cargo fetch`.

## 1. Rebuild & Smoke-Test `init_app`
1. Delete old desktop DB (optional): `rm -f ~/.local/share/truth-training/TruthTraining/truth_training.sqlite`.
2. Run Tauri backend unit tests: `cargo test -p ui-desktop -- init_app`.
3. Invoke init command without UI:
   ```bash
   pnpm tauri invoke init_app
   ```
4. Inspect schema:
   ```bash
   sqlite3 ~/.local/share/truth-training/TruthTraining/truth_training.sqlite ".tables"
   ```
   Ensure `truth_events`, `statements`, `impact`, `progress_metrics`, `context`, `category`, `cause`, `develop`, `effect`, `forma`, `impact_type`, `schema_version` exist and **no** `events`/`summaries`/`logs` tables remain.
5. Re-run `pnpm tauri invoke init_app` to confirm idempotency (second run should succeed without recreating legacy tables).

## 2. Validate Context Picker UX
1. Start desktop UI in dev mode: `pnpm tauri dev`.
2. Navigate to `New Event`.
3. Confirm:
   - Context dropdown loads list (see “Last synced …” timestamp).
   - Manual input of invalid ID (e.g., `9999`) locks submission and shows inline error.
   - Auto-complete search returns results matching typed text.
4. Create a valid event selecting one or more contexts; submission should succeed and toast text must be localized.

## 3. Verify RU/EN Localization Toggle
1. With the dev build running, open Settings → General and switch locale to Russian.
2. Confirm:
   - Navigation labels, context picker text, validation toasts display Russian strings.
   - Refreshing the app keeps Russian selected (config + localStorage).
   - Switching back to English restores EN strings instantly.

## 4. Documentation Checklist
Run through the audit list and update each file before final review:

| Document | Expected Update |
|----------|-----------------|
| `spec/23-function_desktop.md` | Describe new init flow, context picker UX, RU/EN toggle. |
| `docs/quickstart_desktop.md` | Remove legacy warning, add instructions mirroring Sections 1–3 above. |
| `docs/UI_Desktop.md` | Update screenshots/text for dropdowns and locale switch. |
| `README.md` (Release Surfaces) | Note DB parity + localization behavior. |
| `docs/quickstart_core.md` & other quickstarts | Cross-reference DB init + localization where relevant. |

After edits, run link check (optional but recommended):
```bash
python scripts/doc_refactor/fix_broken_links.py --check
```

## 5. Regression Guardrails
- `cargo fmt && cargo clippy --workspace --all-targets`
- `cargo test --workspace`
- `pnpm lint && pnpm test`
- Manual check: run app offline, ensure cached context list still validates IDs.

## Troubleshooting
- **`tauri invoke` not found**: run `cargo install tauri-cli` or use `pnpm tauri invoke`.
- **Legacy tables persist**: ensure no open connection holds the DB; stop the app, rerun init, and inspect logs for table names that failed to drop.
- **Context fetch fails**: check backend logs for `list_contexts` errors; ensure DB seeding ran (see `ui/desktop/src-tauri/src/storage.rs::seed_knowledge_base`).
- **Locale doesn’t persist**: verify `~/.truth-training/config.json` contains `"locale": "ru"` and that Settings saved without filesystem errors.

