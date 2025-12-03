1. Fix init_app and DB schemas
Files: ui/desktop/src-tauri/src/commands/config.rs, ui/desktop/src-tauri/src/storage.rs, core/src/storage.rs
Tasks:
Rewrite the SQL in init_app so that it cleans up and recreates truth_events, not the outdated events.
Pull the schema from core/src/storage.rs (tables truth_events, statements, impacts, progress_metrics, context, etc.). It's better to move the finished SQL to a shared module or call storage::init_db.
Add a test/check to ensure that legacy tables are missing from the DB after init_app. Update spec/23-function_desktop.md and docs/quickstart_desktop.md (Settings section) after the fix, removing the warning about broken constitution and describing the new behavior.
2. Make context value selection convenient and validable.
Files: ui/desktop/src/pages/NewEvent.tsx, ApiService.getContexts, reference components
Tasks:
Instead of numeric fields, use drop-down lists/combo boxes. You can use data from ApiService.getContexts() and Data_Schema.
Add auto-completion and manual input (optional) with validation.
Validation: prevent submission of IDs that are not in the lists (or highlight and block the submission).
Update the functional specification (spec/23-function_desktop.md) and quickstart, describing the new UI elements. 3. Perform a light/deep localization check and document the status
Files: core/src/storage.rs (seed locale), ui/desktop/src/i18n, spec/09-ux-guidelines.md, spec/23-function_desktop.md, docs/UI_Desktop.md
Tasks:
Determine whether there is an actual RU/EN language selection mechanism at the UI level. If not, either add a language switcher (simple toggle + localized strings) or officially document that the UI is currently EN-only, despite seed support.
Bring the documentation (spec/docs) in line with the current state: either describe the localization and its usage, or document the limitation.
4. General documentation audit (spec/ and docs/)
Steps:
Compare key sections (build instructions, cargo configuration, test plan, roadmap) with the current code/scripts. Pay special attention to sections where outdated information was previously noted (migrations, UI descriptions, API/Tauri commands, quickstarts).
Create a checklist of affected documents and update them based on the results.
5. After changes, update the specifications and quickstart docs.
Update spec/23-function_desktop.md, docs/UI_Desktop.md, all quickstarts, README (Release Surfaces), and, if necessary, spec/18/19/20.
Check the logic of links/tables to ensure GitHub displays them correctly.