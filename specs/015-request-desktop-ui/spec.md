# Feature Specification: Desktop UI Synchronization Based on Android Client Implementation

**Feature Branch**: `015-request-desktop-ui`  
**Created**: 2025-01-XX  
**Status**: Draft  
**Input**: User description: "Request for Desktop UI Synchronization Based on Android Client Implementation"

## Overview

The Android client user interface has been fully implemented and documented. This feature synchronizes the Desktop UI with the Android UI strictly at the level of **visual structure, navigation, rendering behavior, component states, and localization logic**, while **preserving all Desktop-specific functional features**.

**Key Principle**: Visual layer rebuild only — no functional refactoring unrelated to UI presentation, Desktop-only workflows, or Desktop-exclusive tooling should be modified.

## Primary Documentation References

1. **Main UI Specification**  
   Location: `docs/ANDROID_UI_SPECIFICATION.md`  
   Contains the complete specification of all 13 Android screens, navigation flows, components, algorithms, and UI behaviors.

2. **Implementation Report**  
   Location: `docs/ANDROID_UI_IMPLEMENTATION_REPORT.md`  
   Includes the synchronization guide, parity notes, and differences relevant to Desktop UI.

3. **Updated Functional Specification**  
   Location: `spec/24-function_mobile_android.md`  
   Reflects the current Android implementation across screens and validation rules.

4. **Localization Documentation**  
   Location: `specs/014-android-localization/LOCALIZATION_IMPLEMENTATION.md`  
   Contains full details of the localization system, including dictionary structure and key mapping.

## User Scenarios & Testing

### User Story 1 - Visual UI Parity with Android (Priority: P1)

As a user, I want the Desktop UI to have the same visual appearance, navigation flow, and component behavior as the Android client, so that I have a consistent experience across platforms.

**Why this priority**: This is the core requirement - establishing visual and behavioral parity between Desktop and Android UIs is essential for user experience consistency.

**Independent Test**: Can be fully tested by comparing Desktop UI screens side-by-side with Android screens and verifying that visual structure, navigation patterns, and component behaviors match.

**Acceptance Scenarios**:

1. **Given** I am on the Desktop Dashboard screen, **When** I view the screen layout, **Then** it matches the Android Dashboard screen structure (sync status card, quick stats, action buttons)
2. **Given** I am creating a new event on Desktop, **When** I interact with the form, **Then** all fields, validation, and template selection behavior matches Android New Event screen
3. **Given** I navigate between screens on Desktop, **When** I use navigation controls, **Then** the navigation flow matches Android navigation patterns
4. **Given** I view context fields on Desktop, **When** I see event or template context, **Then** they display entity names (not IDs) using the same algorithm as Android

---

### User Story 2 - Component Parity (Priority: P1)

As a user, I want Desktop UI components (ContextPicker, DatePickerField, template selection, etc.) to behave identically to their Android equivalents, so that I can use the same interaction patterns on both platforms.

**Why this priority**: Component parity ensures consistent user experience and reduces learning curve when switching between platforms.

**Independent Test**: Can be fully tested by using each component on Desktop and verifying it matches Android component behavior, validation, and error handling.

**Acceptance Scenarios**:

1. **Given** I use ContextPicker on Desktop, **When** I select a context entity, **Then** it validates the selection and displays errors the same way as Android ContextPicker
2. **Given** I use DatePickerField on Desktop, **When** I select dates, **Then** date normalization and validation rules match Android DatePickerField
3. **Given** I select a template on Desktop, **When** I choose a template for event creation, **Then** the template selection flow matches Android (flag-based navigation, form pre-filling)

---

### User Story 3 - Localization System Fix (Priority: P1)

As a user, I want to switch between English and Russian languages on Desktop, and have the UI update correctly with all strings translated, while preserving my event data.

**Why this priority**: Localization is currently broken on Desktop, and it's a critical feature for user accessibility. The database re-seeding must preserve event data integrity.

**Independent Test**: Can be fully tested by switching language in Settings, verifying UI updates, checking that event data is preserved, and confirming context fields display correctly with localized names.

**Acceptance Scenarios**:

1. **Given** I am using Desktop UI in English, **When** I switch to Russian in Settings, **Then** all UI strings update to Russian and event data is preserved
2. **Given** I switch language on Desktop, **When** the knowledge base is re-seeded, **Then** event context field IDs remain valid and display localized entity names
3. **Given** I view an event after language change, **When** I see context fields, **Then** they display entity names in the selected language (not IDs)
4. **Given** I switch language, **When** context templates are cleared, **Then** existing events remain unchanged

---

### User Story 4 - Navigation Synchronization (Priority: P2)

As a user, I want Desktop navigation to follow the same patterns as Android (template selection flow, view judgments flow, etc.), so that navigation is intuitive and consistent.

**Why this priority**: Navigation patterns affect user experience significantly, but are secondary to visual parity and localization.

**Independent Test**: Can be fully tested by navigating through all screens and verifying that navigation flows match Android patterns, including flag-based conditional routing.

**Acceptance Scenarios**:

1. **Given** I am on Desktop New Event screen, **When** I click "Select Template", **Then** I navigate to Context Templates screen with flag set, and selecting a template returns me with fields filled
2. **Given** I click "View Judgments" on Desktop Dashboard, **When** I navigate to Events list, **Then** selecting an event takes me to Judgments screen (not Event Details)
3. **Given** I navigate between screens on Desktop, **When** I use back navigation, **Then** it follows the same back stack behavior as Android

---

### User Story 5 - Validation Rules Parity (Priority: P2)

As a user, I want Desktop form validation to match Android validation rules exactly, so that I get consistent feedback and error handling across platforms.

**Why this priority**: Validation rules ensure data integrity, but are secondary to visual parity and core functionality.

**Independent Test**: Can be fully tested by submitting forms with invalid data and verifying that validation errors match Android validation behavior.

**Acceptance Scenarios**:

1. **Given** I create an event on Desktop, **When** I leave context fields empty, **Then** I see validation errors matching Android (all context fields required)
2. **Given** I set End Timestamp before Start Timestamp on Desktop, **When** I try to save, **Then** I see validation error matching Android (End cannot be less than Start)
3. **Given** I create a template on Desktop, **When** I use duplicate context fields, **Then** I see duplicate detection error matching Android behavior

---

### Edge Cases

- What happens when knowledge base entities are not loaded yet? (Context fields should fallback to ID display)
- How does system handle language switch during active form editing? (Form state should be preserved or user warned)
- What happens when template selection fails? (Error should be displayed, navigation should handle gracefully)
- How does system handle date normalization edge cases (timezone, DST)? (Dates should normalize to start of day consistently)
- What happens when database re-seeding fails during language change? (Transaction should rollback, previous language restored)

## Requirements

### Functional Requirements

- **FR-001**: Desktop UI MUST match Android UI visual structure for all 13 screens (Dashboard, New Event, Event List, Event Detail, Event Edit, Context Templates, New Template, Judgments, Judgment Submission, Overall Summary, Training Results, Settings, Nodes). Note: In Desktop UI, Nodes functionality is embedded in Dashboard via NodesPanel component, not a separate screen. Event Detail and Event Edit are both implemented in EventSummary.tsx with mode switching via isEditing state.
- **FR-002**: Desktop UI MUST implement the same navigation patterns as Android, including flag-based conditional routing for template selection and view judgments flows
- **FR-003**: Desktop UI MUST implement ContextPicker component with same validation, error handling, and entity name resolution as Android
- **FR-004**: Desktop UI MUST implement DatePickerField component with same date normalization algorithm and validation rules as Android
- **FR-005**: Desktop UI MUST replace all hardcoded strings with localization keys, using the same key structure as Android (`values/strings.xml`, `values-ru/strings.xml`)
- **FR-006**: Desktop UI MUST implement database re-seeding with temporary tables solution when language changes, preserving event data and FK relationships
- **FR-007**: Desktop UI MUST implement context field display algorithm that resolves entity names from knowledge base, with fallback to ID if name not found
- **FR-008**: Desktop UI MUST implement template selection flow matching Android (flag-based navigation, form pre-filling via state management)
- **FR-009**: Desktop UI MUST implement same validation rules as Android for events (name required, description required, all context fields required, date validation)
- **FR-010**: Desktop UI MUST implement same validation rules as Android for templates (name required, all context fields required, duplicate detection based on context fields only)
- **FR-011**: Desktop UI MUST implement corrected flag auto-calculation algorithm matching Android (tracks initial End Timestamp, auto-sets when changed)
- **FR-012**: Desktop UI MUST preserve all Desktop-specific functional features (command-line tools, developer tools, Desktop-only workflows)
- **FR-013**: Desktop UI MUST NOT modify Desktop-only interaction flows or underlying Desktop logic layer
- **FR-014**: Desktop UI MUST clear context templates when language changes, matching Android behavior
- **FR-015**: Desktop UI MUST preserve event data during language change, ensuring context field IDs remain valid

### Key Entities

- **UI Screen**: Represents a single screen/view in the Desktop application. Must match Android screen structure while preserving Desktop-specific logic.
- **Navigation State**: Represents the current navigation state, including flags for conditional routing (selectTemplateForEvent, viewJudgments). Must match Android navigation patterns.
- **Localization Key**: Represents a string resource key used for translation. Must match Android key structure for consistency.
- **Context Field Display**: Represents the resolved display value for a context field (entity name or ID). Must use same resolution algorithm as Android.
- **Template Context**: Represents the context fields from a selected template. Must be passed via state management matching Android pattern.

## Success Criteria

### Measurable Outcomes

- **SC-001**: All 13 Desktop UI screens visually match their Android equivalents (100% visual parity verified by side-by-side comparison)
- **SC-002**: Desktop ContextPicker component behavior matches Android ContextPicker (validation, error handling, entity resolution) - 100% functional parity
- **SC-003**: Desktop DatePickerField component behavior matches Android DatePickerField (date normalization, validation rules) - 100% functional parity
- **SC-004**: Desktop localization system works reliably - all UI strings update correctly when language changes, with 0 hardcoded strings remaining
- **SC-005**: Database re-seeding during language change preserves 100% of event data - no data loss, all FK relationships maintained
- **SC-006**: Context field display shows entity names (not IDs) in 100% of cases where knowledge base entities are loaded
- **SC-007**: Template selection flow on Desktop matches Android flow - flag-based navigation works correctly, form pre-filling functions identically
- **SC-008**: All validation rules match Android - event validation, template validation, date validation produce identical error messages and behavior
- **SC-009**: Navigation patterns match Android - template selection, view judgments, event editing flows work identically
- **SC-010**: Desktop-specific functional features remain 100% intact - no Desktop-only workflows or tools are broken or modified
- **SC-011**: Language switching completes successfully in under 5 seconds, including database re-seeding and UI update
- **SC-012**: Users can complete primary tasks (create event, select template, switch language) with same number of clicks/steps as Android

## Technical Approach

### Visual Layer Rebuild

- Recreate Desktop UI layout, components, and visual behaviors to match Android implementation
- Maintain Desktop-specific functional logic; only UI structure, rendering, and navigation behavior are modified
- Use Android UI specification as authoritative reference for visual structure

### Navigation Synchronization

- Align navigation flow, initial screen logic, and routing structure with Android UI behaviors
- Implement flag-based conditional routing for template selection and view judgments
- Preserve Desktop-only command-line, developer, or management tools

### Component Parity

Rebuild Desktop equivalents of:
- **ContextPicker**: Searchable combobox with validation, entity name resolution, error handling
- **DatePickerField**: Date picker with normalization algorithm, validation rules, clear capability
- **Template Selection UI**: Flag-based navigation, state management for form pre-filling
- **Event Editing UI**: Read-only fields display, editable fields with validation
- **Status Indicators**: Sync status, navigation headers matching Android

### Localization System Fix

- Replace all hardcoded textual elements with localization keys
- Use documented localization structure from Android (`values/strings.xml`, `values-ru/strings.xml`)
- Implement database re-seeding with temporary tables:
  1. Create temporary mirror tables (`temp_truth_events`, `temp_impact`, `temp_progress_metrics`)
  2. Copy data with remapped localization fields
  3. Perform atomic swap to maintain relational integrity
  4. Remove temp tables after migration

### Algorithm Implementation

- **Context Field Display**: Implement entity name resolution with `remember()`-like caching, fallback to ID
- **Date Normalization**: Normalize timestamps to start of day (00:00:00) for accurate comparison
- **Corrected Flag Calculation**: Track initial End Timestamp, auto-set Corrected when End Timestamp changes
- **Template Selection**: Flag-based navigation with state management for context field passing

## Constraints

- **Desktop-Specific Logic Preservation**: Must not modify Desktop-only interaction flows or underlying Desktop logic layer
- **No Functional Refactoring**: Only UI structure, rendering, and navigation behavior are modified
- **Desktop Documentation Reference**: Use Desktop documentation/specifications to verify which parts are UI-only
- **Android Documentation Authority**: Use Android documentation as authoritative reference for UI structure and behavior

## Dependencies

- Android UI specification documentation (`docs/ANDROID_UI_SPECIFICATION.md`)
- Android implementation report (`docs/ANDROID_UI_IMPLEMENTATION_REPORT.md`)
- Android functional specification (`spec/24-function_mobile_android.md`)
- Localization implementation documentation (`specs/014-android-localization/LOCALIZATION_IMPLEMENTATION.md`)
- Desktop UI current implementation (for reference on Desktop-specific features to preserve)

## Risks

- **Risk**: Modifying Desktop-specific functional logic while rebuilding UI
  - **Mitigation**: Strict separation of UI layer from logic layer, use Desktop documentation to identify UI-only components

- **Risk**: Breaking Desktop-only workflows during navigation synchronization
  - **Mitigation**: Preserve Desktop-only command-line and developer tools, test all Desktop-specific features after changes

- **Risk**: Data loss during database re-seeding if temporary tables solution not implemented correctly
  - **Mitigation**: Use transaction-based approach, test thoroughly with existing event data, implement rollback mechanism

- **Risk**: Performance degradation from database re-seeding during language change
  - **Mitigation**: Optimize temporary tables operations, use efficient SQL queries, test with large datasets

## Related Documents

- [`docs/ANDROID_UI_SPECIFICATION.md`](../../docs/ANDROID_UI_SPECIFICATION.md) - Complete Android UI specification
- [`docs/ANDROID_UI_IMPLEMENTATION_REPORT.md`](../../docs/ANDROID_UI_IMPLEMENTATION_REPORT.md) - Implementation report and synchronization guide
- [`spec/24-function_mobile_android.md`](../../spec/24-function_mobile_android.md) - Android functional specification
- [`specs/014-android-localization/LOCALIZATION_IMPLEMENTATION.md`](../014-android-localization/LOCALIZATION_IMPLEMENTATION.md) - Localization implementation details
- [`docs/UI_Desktop.md`](../../docs/UI_Desktop.md) - Current Desktop UI documentation
- [`spec/23-function_desktop.md`](../../spec/23-function_desktop.md) - Desktop functional specification
