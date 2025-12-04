# Contract: Android Localization Parity

**Feature**: Android localization parity with Desktop  
**User Story**: User Story 4 (Priority: P3)  
**Status**: Draft

## Preconditions

- Android app is launched
- Localization status is determined (RU/EN switching exists or EN-only)

## Contract

### Input
- User interacts with Android app UI
- User may attempt to switch language (if RU/EN switching exists)

### Output
- If RU/EN switching exists: All UI strings update to selected language, preference persists across app restarts
- If EN-only: Documentation clearly states EN-only status in specs, quickstarts, and UI guidelines

### Behavior

1. **Localization Status Audit**:
   - Audit Android app for localization support (check `res/values/`, `res/values-ru/`, locale switching UI)
   - Determine if RU/EN switching exists or if app is EN-only
   - Document status clearly in specs and quickstarts

2. **If RU/EN Switching Exists**:
   - Language switching UI MUST be accessible (e.g., Settings screen, header toggle)
   - All UI strings MUST update to selected language immediately
   - Locale preference MUST persist across app restarts (via `SharedPreferences` or Room config table)
   - Strings MUST be consistent with Desktop translations (if Desktop has RU support)

3. **If EN-Only**:
   - Documentation MUST clearly state EN-only status in:
     - `spec/09-ux-guidelines.md`
     - `docs/UI_Desktop.md`
     - `docs/quickstart_android.md`
     - `truth-android-client/README.md`
   - UI MUST display English strings only
   - No locale switching UI is required

4. **String Consistency**:
   - If RU/EN switching exists, Android strings MUST be consistent with Desktop strings for key screens:
     - Navigation labels
     - Event creation form labels
     - Error messages
     - Settings labels

## Success Criteria

- **SC-004**: Android localization status is clearly documented in specs and quickstarts; if RU/EN switching exists, it works end-to-end with consistent strings; if EN-only, documentation explicitly states this.

## Test Cases

### TC-001: RU/EN Switching (if exists)
1. Open Android app
2. Navigate to Settings or locale toggle
3. Switch language from EN to RU (or vice versa)
4. Navigate through app screens
5. **Expected**: All UI strings update to selected language, preference persists after app restart

### TC-002: EN-Only Documentation (if EN-only)
1. Read `docs/quickstart_android.md`
2. Read `spec/09-ux-guidelines.md`
3. **Expected**: Documentation clearly states Android app is EN-only

### TC-003: String Consistency (if RU/EN exists)
1. Compare Android strings with Desktop strings for key screens
2. **Expected**: Strings are consistent across platforms for navigation, event creation, error messages

## Observability

- Log locale changes: `android.locale.change` (if telemetry exists) or Logcat
- Log missing translations: `android.translation.missing` (if telemetry exists) or Logcat

## References

- `truth-android-client/app/src/main/res/values/strings.xml`
- `truth-android-client/app/src/main/res/values-ru/strings.xml` (if exists)
- `docs/quickstart_android.md`
- `spec/09-ux-guidelines.md`
- `docs/UI_Desktop.md`

