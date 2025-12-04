# Contract: Desktop Localization Toggle (RU/EN)

## Actors
- **User**: Desktop operator changing UI language.
- **Frontend**: React layout shell + Settings page locale control.
- **Persistence**: `localStorage` key `truth-locale` + Tauri config `AppConfig.locale`.

## Requirements
1. **Supported Locales**: `en`, `ru`. (Existing ES/FR/DE/AR remain in code but are not selectable until strings exist.)
2. **Toggle Placement**:
   - Quick toggle in top-right nav (icon + dropdown).
   - Detailed selector within Settings → General.
3. **Data Flow**:
   - On app start, Tauri command returns stored `AppConfig` (with `locale`). UI hydrates state and sets `document.documentElement.lang/dir`.
   - When user switches locale, React updates context, writes to `localStorage`, and invokes new Tauri command `save_locale` (or piggybacks on `save_app_config`) to persist `AppConfig`.
4. **Strings**:
   - Wrap all user-facing strings on targeted screens (`Dashboard`, `NewEvent`, `ContextPicker`, `Settings`, localization banners) with `t('...')`.
   - Provide `ru` translations for those keys in `ui/desktop/src/i18n`.
   - Missing keys log warning and fall back to EN.

## Acceptance Criteria
- Locale change updates UI instantly without reload.
- Restarting the app preserves chosen locale (config + localStorage).
- Right-to-left handling unaffected (both EN/RU are `ltr`).
- Toasts, validation errors, and context picker states show translated text.
- Documentation (`spec/23-function_desktop.md`, quickstarts, README release surfaces) explains how to switch languages and notes RU coverage scope.

## Error Handling
- If persistence fails (e.g., filesystem error), show toast “Unable to save locale preference” and keep UI state until next launch.
- If translations for selected locale missing, show fallback English string and surface console warning for QA.

## Observability
- Log `locale.change` event with `{from, to}` plus success/failure.
- Optional metric: track `%` of sessions per locale for release notes.

