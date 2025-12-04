/speckit.specify
Goal: Extend the 001-init-schema-fix specification so that truth-android-client fully matches the functionality and behavioral parity of UI Desktop, while also fixing the critical Android issue where the application disappears immediately after launch.

Scope:
This update expands the existing feature specifications for 001-init-schema-fix and adds Android-specific implementation requirements. The goal is cross-platform parity between Desktop UI and Android UI, including initialization behavior, context dropdown logic, validation, documentation alignment, and localization clarity.

Additionally, this specification MUST include a dedicated fix for truth-android-client's startup failure: the app launches, instantly disappears, but continues running in the background.

Do NOT modify the CLI component. The CLI is exclusively for automated verification of Core, Server, and CLI behavior by the Cursor AI agent.

------------------------------------------------------------
A. ANDROID PARITY WITH DESKTOP UI
------------------------------------------------------------

### A1. Init App & Schema Parity
Match the Desktop implementation:

- File group: android/app/src/main/java/.../storage, android/app/src/main/java/.../init, core/src/storage.rs
- Requirements:
  - Android must call the shared initialization logic or mirror the same SQL creation flow used by Desktop (`core_lib::storage::init_db` equivalent).
  - Ensure that Android no longer contains any references to the legacy `events` table.
  - Add regression protection ensuring that the outdated schema NEVER reappears.
  - Add automated tests for schema initialization if possible on Android (instrumented or unit).

### A2. Context Dropdown Parity (FR-006 included)
Match the Desktop flow:

- Replace numeric ID fields with dropdowns / combo boxes backed by lookup data from Core or embedded DB.
- All template-prefilled values MUST be editable before save.
- Validation MUST prevent submission of IDs not present in the taxonomy/context lookup lists.
- Add UI tests (Compose), ideally in a dedicated module.
- Update specifications and quickstart docs for Android to describe the new dropdowns.

### A3. Localization Parity
- Determine whether the Android client supports RU/EN switching.
- If not, document that Android is currently EN-only (same as Desktop).
- If language switching exists, confirm that strings are consistent with UI Desktop and seed locale in the database.
- Update spec/09-ux-guidelines.md, docs/UI_Desktop.md, and Android quickstart accordingly.

### A4. Documentation Audit Parity
- Align spec/ and docs/ to include Android behavior alongside Desktop.
- Update Android quickstart to reflect Init workflow, dropdown UI, validation rules, and localization status.

------------------------------------------------------------
B. ANDROID STARTUP FIX (CRITICAL)
------------------------------------------------------------

Goal:
Fix the issue where truth-android-client launches and immediately disappears while continuing running in the background.

### Android Requirements:

#### B1. AndroidManifest.xml
- Declare the correct launcher Activity.
- Ensure the Activity is exported and visible.
- Add correct intent filters for MAIN + LAUNCHER.
- Prevent premature closing after onCreate() (common cause: missing UI content or missing setContent {}).
- Ensure correct theme is applied (no blank/splash-only activity unless intended).

#### B2. Navigation Initialization
- Ensure NavigationHost is initialized properly and not skipped.
- Ensure the app doesn’t finish() itself by mistake before displaying UI.

#### B3. Initial Screen
- Explicitly define the entry screen: DashboardScreen, NodesScreen, or whichever is correct for the project.
- Ensure that this composable is the first visible UI element.

#### B4. ViewModel Factories
- Ensure all ViewModels use the correct factory and lifecycle scope.
- Fix any missing DI injections causing crashes during launch.

#### B5. Navigation Graph
- Add any missing screens to the navigation graph.
- Verify all destinations have proper routes and arguments.

### Deliverables for Android:
- Patches for:
  - AndroidManifest.xml (launcher activity, intent filters, exported=true, theme)
  - MainActivity.kt (setContent, NavigationHost, ViewModel initialization)
  - NavigationGraph.kt (entry screen, routes)
  - Any additional files required to stabilize UI
- Updated documentation in spec and quickstarts.

------------------------------------------------------------
C. GLOBAL RULES
------------------------------------------------------------

- Preserve strict separation: **Android UI changes must NOT modify CLI**.
- All deliverables MUST come as patch sets ready for PR generation.
- Update:
  - spec/23-function_desktop.md
  - docs/quickstart_desktop.md
  - docs/quickstart_android.md (new or extended)
  - docs/UI_Desktop.md
  - Any spec files referencing Init or Context flows (spec/18–21)
- Ensure the Android changes appear in the same feature branch: 001-init-schema-fix.

------------------------------------------------------------
D. OUTPUT FORMAT
------------------------------------------------------------

- Produce a unified updated specification for Android parity and Android startup fix.
- Include:
  - Updated definitions
  - Acceptance criteria
  - Tasks (Txxx) aligned with existing numbering convention
  - Cross-platform validation items
  - References to updated docs
- Produce patch-ready modifications for all required files.

