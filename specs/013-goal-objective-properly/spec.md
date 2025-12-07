# Feature Specification: Android UI Registration and Launch Configuration

**Feature Branch**: `013-goal-objective-properly`  
**Created**: 2025-12-06  
**Status**: Draft  
**Input**: User description: "Goal: The objective is to properly register the required UI screens and activities in AndroidManifest.xml so the application opens normally and stays visible. Requirements: 1. Update AndroidManifest.xml: - Ensure the correct launcher Activity is declared. - Ensure the Activity is exported and visible on launch. - Add proper intent filters for main launcher behavior. - Prevent premature closing of the UI after onCreate(). 2. Validate that the main NavigationHost is initialized correctly. 3. Ensure the initial composable screen (e.g., DashboardScreen or NodesScreen) is assigned as the entry point. 4. Ensure any required ViewModels are attached through the correct factory. 5. Add any missing screens into the navigation graph if required. 6. Provide all necessary Kotlin and manifest changes as patches ready for PR generation. Important Rule: - The CLI part of the application is used exclusively for verifying the Core, Server, and CLI functionality by the Cursor AI agent. It must not be modified for UI-related changes. Deliverables: - A validated plan for required changes use the input project specification document docs/archive/prompt/ekwator/specify1/plan/plan1.md - Patches for manifest, navigation, and affected Kotlin files. - A correctly structured PR generated from the final specification. - For this specification, use the input project specification document spec/24-function_mobile_android.md _Version: v1.0.0_"

## Related Documents

- [Android Mobile Client Functional Specification](../24-function_mobile_android.md)
- [Implementation Plan](../../docs/archive/prompt/ekwator/specify1/plan/plan1.md)

## Clarifications

### Session 2025-12-06

- Q: Какой экран должен быть начальным при запуске приложения? → A: DashboardScreen (главный экран с событиями и статистикой)
- Q: В чём конкретно проблема с запуском приложения? → A: Приложение запускается, но показывает пустой/чёрный экран
- Q: Каково текущее состояние графа навигации относительно DashboardScreen? → A: Unknown, needs inspection
- Q: Какой паттерн ViewModel factory используется в проекте? → A: ViewModelFactory отсутствует или не настроен

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Application Launches Successfully (Priority: P1)

When a user installs and opens the Truth Training Android application, the app should launch immediately and display the DashboardScreen (main screen with events and statistics) without crashing, closing prematurely, or showing a blank/black screen.

**Why this priority**: This is the most fundamental requirement - the application must be launchable and stable. Without this, no other functionality can be accessed or tested.

**Independent Test**: Can be fully tested by installing the APK on a physical Android device, launching the app from the launcher, and verifying that the main screen appears and remains visible for at least 30 seconds without crashing.

**Acceptance Scenarios**:

1. **Given** the application is installed on an Android device, **When** the user taps the app icon in the launcher, **Then** the application opens and displays the DashboardScreen (not a blank/black screen) within 2 seconds
2. **Given** the application has launched successfully, **When** the user does not interact with the screen for 30 seconds, **Then** the application remains visible and does not close automatically
3. **Given** the application is running, **When** the user presses the home button and then returns to the app, **Then** the application resumes to the same screen state without restarting

---

### User Story 2 - Correct Launcher Activity Configuration (Priority: P1)

The AndroidManifest.xml must declare the correct Activity as the launcher with proper intent filters, ensuring the system can identify and launch the application correctly.

**Why this priority**: Without proper manifest configuration, the application cannot be launched by the Android system. This is a prerequisite for any user interaction.

**Independent Test**: Can be fully tested by examining AndroidManifest.xml to verify the launcher Activity declaration, intent filters, and exported attributes. Then verify by attempting to launch the app from the launcher.

**Acceptance Scenarios**:

1. **Given** AndroidManifest.xml exists, **When** examining the manifest file, **Then** MainActivity (or designated launcher Activity) is declared with `<intent-filter>` containing `android.intent.action.MAIN` and `android.intent.category.LAUNCHER`
2. **Given** the launcher Activity is declared, **When** the Activity is examined, **Then** it has `android:exported="true"` attribute set (if targeting Android 12+)
3. **Given** the manifest is configured correctly, **When** the APK is installed, **Then** the application icon appears in the device launcher

---

### User Story 3 - Navigation Graph Initialization (Priority: P2)

The main NavigationHost must be initialized correctly in the launcher Activity, and the initial screen must be properly assigned as the entry point in the navigation graph.

**Why this priority**: Even if the app launches, users cannot navigate if the navigation system is not properly initialized. This enables all subsequent user interactions.

**Independent Test**: Can be fully tested by launching the app and verifying that the DashboardScreen (initial screen) is displayed, and that navigation to other screens works correctly.

**Acceptance Scenarios**:

1. **Given** MainActivity is the launcher Activity, **When** the Activity's `onCreate()` method executes, **Then** the NavigationHost is initialized and the initial screen route is set correctly
2. **Given** the navigation graph is configured, **When** the app launches, **Then** the DashboardScreen (initial composable screen) is displayed without errors
3. **Given** the navigation graph includes all required screens, **When** attempting to navigate to any screen defined in the graph, **Then** navigation succeeds without runtime exceptions

---

### User Story 4 - ViewModel Factory Integration (Priority: P2)

All required ViewModels must be properly attached through the correct factory, ensuring that screen state management works correctly from the first launch. **Note**: ViewModelFactory is currently missing and must be created and configured.

**Why this priority**: ViewModels are essential for screen functionality. Without proper factory setup, screens may crash or fail to load data, making the app unusable. Since ViewModelFactory is currently missing, this is a blocking requirement.

**Independent Test**: Can be fully tested by launching the app and verifying that screens with ViewModels load their data correctly and handle state changes without errors.

**Acceptance Scenarios**:

1. **Given** a screen requires a ViewModel, **When** the screen is displayed, **Then** the ViewModel is created through the factory and attached correctly
2. **Given** the ViewModel factory is configured, **When** the screen initializes, **Then** the ViewModel's state is observed and displayed correctly
3. **Given** ViewModels are properly initialized, **When** the app is rotated or recreated, **Then** ViewModel state is preserved and screens continue to function

---

### User Story 5 - Complete Navigation Graph Registration (Priority: P3)

All screens referenced in the application must be registered in the navigation graph, ensuring that navigation between screens works without runtime errors.

**Why this priority**: While the app can launch with just the initial screen, complete navigation functionality requires all screens to be properly registered. This enables the full user experience.

**Independent Test**: Can be fully tested by attempting to navigate to each screen defined in the application and verifying that navigation succeeds without "destination not found" errors.

**Acceptance Scenarios**:

1. **Given** screens are defined in the codebase, **When** examining the navigation graph, **Then** all screens are registered with unique route identifiers
2. **Given** screens are registered in the navigation graph, **When** navigating to any screen via navigation actions, **Then** the screen displays correctly without runtime exceptions
3. **Given** the navigation graph is complete, **When** using back navigation, **Then** the previous screen is restored correctly

---

### Edge Cases

- What happens when the device is low on memory during app launch?
- How does the system handle Activity recreation after configuration changes (rotation, theme change)?
- What happens if the navigation graph contains invalid routes or missing destinations?
- How does the app handle missing ViewModel factory dependencies?
- What happens when the initial screen's data fails to load (network error, database error)?
- How does the app behave if AndroidManifest.xml contains conflicting launcher declarations?
- **Current Issue**: Application launches but displays blank/black screen - must be resolved by ensuring NavigationHost is properly initialized and DashboardScreen is correctly set as start destination

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: AndroidManifest.xml MUST declare the correct launcher Activity with `<intent-filter>` containing `android.intent.action.MAIN` and `android.intent.category.LAUNCHER`
- **FR-002**: The launcher Activity MUST have `android:exported="true"` attribute set (required for Android 12+)
- **FR-003**: The launcher Activity MUST initialize NavigationHost correctly in `onCreate()` method
- **FR-004**: The navigation graph MUST include DashboardScreen as the initial screen (entry point)
- **FR-005**: DashboardScreen MUST be assigned as the start destination in the navigation graph
- **FR-006**: All ViewModels required by screens MUST be attached through the correct factory (ViewModelFactory must be created and configured as it is currently missing)
- **FR-007**: The application MUST prevent premature closing of the UI after `onCreate()` completes
- **FR-012**: The application MUST display DashboardScreen content (not blank/black screen) immediately after launch
- **FR-008**: All screens referenced in the application MUST be registered in the navigation graph with unique route identifiers
- **FR-009**: The navigation graph MUST load without runtime exceptions during app startup
- **FR-010**: MainActivity (or launcher Activity) MUST call `setContent {}` with the correct NavigationHost composable
- **FR-011**: The CLI part of the application MUST remain untouched (used exclusively for Core, Server, and CLI verification by Cursor AI agent)

### Key Entities *(include if feature involves data)*

- **MainActivity**: The launcher Activity that serves as the application entry point. Key attributes: exported flag, intent filters, NavigationHost initialization
- **NavigationHost**: The Compose navigation component that manages screen navigation. Key attributes: navigation graph, start destination, route definitions
- **Navigation Graph**: The collection of all registered screens and their routes. Key attributes: screen routes, navigation actions, argument definitions
- **ViewModel Factory**: The factory responsible for creating ViewModel instances. Key attributes: dependency injection, lifecycle awareness, state preservation

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Application launches successfully on 100% of test devices (minimum 2 physical Android devices) without crashes within 2 seconds of tapping the launcher icon
- **SC-002**: Application remains visible and stable for at least 30 seconds after launch without user interaction, with zero premature closures
- **SC-003**: AndroidManifest.xml validation passes with zero errors or warnings related to launcher Activity configuration
- **SC-004**: Navigation graph loads successfully on app startup with zero runtime exceptions related to missing destinations or invalid routes
- **SC-005**: DashboardScreen (initial screen) displays correctly on 100% of launches without blank/black screens or error states (resolves current blank screen issue)
- **SC-006**: All ViewModels required by the initial screen are created and attached successfully through a properly configured ViewModelFactory, with zero factory-related errors (ViewModelFactory must be implemented as it is currently missing)
- **SC-007**: Navigation to all registered screens succeeds without "destination not found" errors (100% success rate)
- **SC-008**: Application passes instrumentation tests verifying manifest configuration, Activity launch, and navigation initialization

## Implementation Notes

### Reference Documents

This specification should be implemented in conjunction with:
- [Android Mobile Client Functional Specification](../24-function_mobile_android.md) - Provides complete Android architecture and screen definitions
- [Implementation Plan](../../docs/archive/prompt/ekwator/specify1/plan/plan1.md) - Contains detailed step-by-step implementation plan

### Constraints

- **CLI Exclusion**: The CLI part of the application must not be modified. It is used exclusively for verifying Core, Server, and CLI functionality by the Cursor AI agent.
- **Platform**: This feature is Android-specific and does not affect Desktop UI or Server components.

### Testing Requirements

- Unit tests for manifest validation
- Instrumentation tests for Activity launch and navigation initialization
- Physical device testing on at least 2 Android devices (required before PR approval)
- Navigation graph validation tests

### Implementation Discovery

- **Navigation Graph State**: Current state of navigation graph regarding DashboardScreen registration and start destination is unknown and requires inspection during implementation phase. This inspection must be completed before making navigation graph modifications.

### Deliverables

- Updated AndroidManifest.xml with correct launcher Activity configuration
- Modified MainActivity.kt with proper NavigationHost initialization
- Updated navigation graph with all required screens and correct start destination
- ViewModelFactory implementation and configuration (currently missing, must be created)
- Test cases for manifest and navigation validation
- Documentation updates in spec/ and docs/ directories with cross-references

---

_Version: v1.0.0_
