# Research Notes — Android Parity with Desktop UI & Startup Fix

**Spec Input**: `/home/ekwator/Code/truth-training/specs/012-spec1-2-111/spec.md`  
**Date**: 2025-12-03  
**Goal**: Achieve Android functional and behavioral parity with Desktop UI while fixing critical startup bug where app disappears immediately after launch.

## Current Behavior & Findings

### Android app disappears on launch (Critical Bug)
- `AndroidManifest.xml` declares `MainDashboardActivity` (View-based Activity) as the launcher with `android:exported="true"` and proper intent filters (`MAIN` + `LAUNCHER`).
- `MainActivity` (Compose UI) is declared with `android:exported="false"` and has no intent filters, so it cannot be launched directly.
- `MainDashboardActivity` is a legacy View-based Activity that uses `R.layout.activity_main_dashboard` and `TruthViewModel`; it does not display the modern Compose UI.
- `MainActivity` correctly initializes Compose UI with `setContent {}` and `MainNavigation`, but it's not accessible as the launcher.
- **Root Cause**: Launcher Activity points to wrong Activity; MainActivity (Compose) is not exported and has no intent filters.

### Android database schema uses mixed legacy/canonical tables
- `TruthDatabase.kt` declares entities including `EventEntity` (maps to `truth_events`), `ImpactEntity`, `ProgressMetricsEntity`, but also includes `JudgmentEntity`, `SummaryEntity`, `SyncQueueEntity` which may be legacy or part of CI schema.
- `TruthDatabaseMigrations.kt` contains `MIGRATION_1_2` that creates canonical tables (`truth_events`, `impact`, `progress_metrics`, `context`) and migrates data from legacy `events` table, but does not explicitly drop legacy tables (`events`, `impacts`, `summaries`, `logs`) after migration.
- Migration logic attempts to migrate data from `events` to `truth_events`, which contradicts the spec requirement to "drop legacy tables immediately without data migration."
- No automated regression test ensures legacy tables are absent after initialization.
- Schema is defined inline in migrations rather than using shared SQL assets from `core/src/storage.rs`, causing potential drift.

### Context entry UI is numeric-only and lacks validation
- `EventCreateScreen.kt` renders `OutlinedTextField` components for `categoryId`, `formaId`, `causeId`, `developId`, `effectId` with string state that is converted to `Int?` on save.
- No dropdown/autocomplete UI; users must type numeric IDs manually.
- No validation against lookup tables; invalid IDs can be submitted, leading to foreign key constraint violations or inconsistent data.
- `ContextTemplateRepository` already exists and can load contexts from Room database via `listTemplates()` and `getAllTemplatesFlow()`, but `EventCreateScreen` does not use it.
- Contexts are stored in `context` table (via `ContextTemplateEntity`) and can be loaded from embedded database, matching Desktop's embedded approach.

### Localization status unclear
- `strings.xml` exists in `app/src/main/res/values/` but contains only English strings.
- No `values-ru/` directory found, suggesting EN-only status.
- No locale switching UI or persistence mechanism found in Compose screens.
- Documentation does not clarify Android localization status (RU/EN support or EN-only).

### Navigation and ViewModel setup
- `MainActivity` correctly initializes `NavController` and calls `MainNavigation` composable.
- `MainNavigation.kt` defines navigation graph with routes for events, contexts, judgments, nodes.
- Entry screen appears to be `DashboardScreen` or first route in navigation graph, but needs explicit verification.
- ViewModel factories and DI setup exist in `TruthTrainingApplication`, but need verification that all dependencies are injected correctly.

## Opportunities & Decisions

1. **Fix AndroidManifest.xml**: Make `MainActivity` the launcher Activity with `android:exported="true"` and proper intent filters; remove or deprecate `MainDashboardActivity` launcher declaration.
2. **Use shared SQL assets for schema parity**: Extract canonical schema SQL from `core/src/storage.rs` (via `TRUTH_SCHEMA_SQL` constant) into a shared asset file (`app/src/main/assets/schema.sql`) that both Android and Desktop can read, ensuring schema parity.
3. **Drop legacy tables immediately**: Add explicit `DROP TABLE IF EXISTS` statements for `events`, `impacts`, `summaries`, `logs` in database initialization or migration, without data migration (matching Desktop behavior).
4. **Replace numeric inputs with dropdowns**: Create reusable Compose context picker component (similar to Desktop `ContextPicker`) that uses `ContextTemplateRepository.getAllTemplatesFlow()` to populate dropdowns with human-readable labels, with validation that blocks submission if IDs are not in the lookup list.
5. **Implement context validation**: Add validation logic in `EventRepository` or `EventCreateScreen` that checks context IDs against lookup tables before submission, with inline error states.
6. **Document or implement localization**: Audit Android app for RU/EN support; if EN-only, clearly document in specs and quickstarts; if RU/EN exists, ensure strings are consistent with Desktop.
7. **Update documentation**: Update `docs/quickstart_android.md`, `docs/UI_Desktop.md`, `spec/23-function_desktop.md`, `spec/09-ux-guidelines.md` to include Android behavior alongside Desktop.

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Dropping legacy tables might delete user data | Medium | Run explicit `DROP TABLE IF EXISTS` only for tables confirmed as legacy (not part of CI schema); clearly document in quickstart; limit to `events`, `impacts`, `summaries`, `logs` (not `judgments`, `summaries` if they're part of CI schema) |
| Shared SQL asset may introduce build coupling | Low | Extract SQL to `app/src/main/assets/schema.sql` as a text file; Android reads it at runtime; Desktop can read same file; both validate against `core/src/storage.rs` |
| Context dropdown load failures block event creation | Medium | Cache last good list in ViewModel, show inline error state, allow manual entry only when validation can confirm via fallback lookup |
| Navigation graph incomplete or missing routes | Medium | Verify all required screens exist in `MainNavigation.kt`, ensure entry screen is explicitly defined, test navigation flows |
| ViewModel factories fail during initialization | Medium | Ensure proper DI setup in `TruthTrainingApplication`, verify all dependencies are injected before use, add error handling for missing dependencies |
| RU translations incomplete or missing | Low | Prioritize documenting EN-only status if RU support doesn't exist; if RU exists, ensure consistency with Desktop strings |

## Impacted Areas

- **Android Manifest**: `truth-android-client/app/src/main/AndroidManifest.xml` — fix launcher Activity declaration.
- **MainActivity**: `truth-android-client/app/src/main/java/com/truth/training/client/MainActivity.kt` — verify Compose UI initialization and navigation setup.
- **Database**: 
  - `truth-android-client/app/src/main/java/com/truth/training/client/data/database/TruthDatabase.kt` — update entities to match canonical schema, remove legacy entities if confirmed.
  - `truth-android-client/app/src/main/java/com/truth/training/client/data/database/TruthDatabaseMigrations.kt` — add migration to drop legacy tables, use shared SQL asset.
  - `truth-android-client/app/src/main/assets/schema.sql` — new shared SQL asset file (to create).
- **UI Components**:
  - `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventCreateScreen.kt` — replace numeric inputs with dropdowns.
  - `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/components/` — new context picker component (to create).
- **Repository**: `truth-android-client/app/src/main/java/com/truth/training/client/data/repository/EventRepository.kt` — add context validation logic.
- **Documentation**: 
  - `docs/quickstart_android.md`
  - `docs/UI_Desktop.md`
  - `spec/23-function_desktop.md`
  - `spec/09-ux-guidelines.md`
  - `truth-android-client/README.md`

## References

- `truth-android-client/app/src/main/AndroidManifest.xml`
- `truth-android-client/app/src/main/java/com/truth/training/client/MainActivity.kt`
- `truth-android-client/app/src/main/java/com/truth/training/client/MainDashboardActivity.kt`
- `truth-android-client/app/src/main/java/com/truth/training/client/data/database/TruthDatabase.kt`
- `truth-android-client/app/src/main/java/com/truth/training/client/data/database/TruthDatabaseMigrations.kt`
- `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventCreateScreen.kt`
- `truth-android-client/app/src/main/java/com/truth/training/client/data/repository/ContextTemplateRepository.kt`
- `core/src/storage.rs` (canonical schema source)
- `ui/desktop/src/pages/NewEvent.tsx` (Desktop reference for context picker UX)

