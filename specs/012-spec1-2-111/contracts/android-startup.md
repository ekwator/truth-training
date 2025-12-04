# Contract: Android App Startup

**Feature**: Android app launches and displays UI without disappearing  
**User Story**: User Story 1 (Priority: P1)  
**Status**: Draft

## Preconditions

- Android app is installed on device/emulator (API 24+)
- `AndroidManifest.xml` declares correct launcher Activity
- `MainActivity` (Compose UI) is properly initialized

## Contract

### Input
- User launches Android app via launcher icon or system intent

### Output
- `MainActivity` (Compose UI) is displayed as the launcher Activity
- Main navigation screen (e.g., `DashboardScreen`) is visible and remains stable
- App does not close itself or disappear immediately after launch

### Behavior

1. **AndroidManifest.xml Configuration**:
   - `MainActivity` MUST be declared with `android:exported="true"`
   - `MainActivity` MUST have intent filters: `<action android:name="android.intent.action.MAIN" />` and `<category android:name="android.intent.category.LAUNCHER" />`
   - `MainDashboardActivity` MUST NOT be declared as launcher (remove or set `exported="false"` and remove intent filters)
   - `MainActivity` MUST have correct theme (e.g., `@style/Theme.Material3.DayNight.NoActionBar`)

2. **MainActivity Initialization**:
   - `MainActivity.onCreate()` MUST call `setContent {}` with valid Compose UI
   - `MainNavigation` composable MUST be initialized with `NavController`
   - Navigation graph MUST have an explicitly defined entry screen (e.g., `DashboardScreen`)
   - `TruthTrainingApplication.database` MUST be accessible and initialized
   - `TruthCore.initNode()` MUST be called (if required)

3. **Navigation Setup**:
   - `NavController` MUST be created via `rememberNavController()`
   - All required routes MUST be defined in `MainNavigation.kt`
   - Entry screen MUST be the first visible UI element

4. **Error Handling**:
   - If database initialization fails, app MUST show error state (not crash)
   - If navigation graph is incomplete, app MUST show fallback screen
   - If ViewModel factories fail, app MUST handle gracefully with error state

## Success Criteria

- **SC-001**: Android app launches successfully and displays main UI screen without disappearing; 100% of launch attempts result in visible, stable UI (verified via automated UI tests or manual testing).

## Test Cases

### TC-001: Fresh Install Launch
1. Install Android app on clean device/emulator
2. Launch app via launcher icon
3. **Expected**: `MainActivity` (Compose UI) is displayed, main navigation screen is visible, app remains stable

### TC-002: Navigation Stability
1. Launch app
2. Navigate between screens (events, contexts, judgments, nodes)
3. **Expected**: All screens render correctly, app remains stable, no crashes

### TC-003: Database Initialization Failure
1. Simulate database initialization failure (e.g., corrupt database file)
2. Launch app
3. **Expected**: App shows error state, allows retry, does not crash

## Observability

- Log app launch events: `android.startup.launch.success`, `android.startup.launch.failure`
- Log navigation events: `android.navigation.screen.displayed`
- Log database initialization events: `android.db.init.success`, `android.db.init.failure`

## References

- `truth-android-client/app/src/main/AndroidManifest.xml`
- `truth-android-client/app/src/main/java/com/truth/training/client/MainActivity.kt`
- `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/MainNavigation.kt`

