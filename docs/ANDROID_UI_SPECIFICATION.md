# Android UI Specification

**Version:** v1.0.0  
**Last Updated:** 2025-01-XX  
**Status:** ✅ Fully Implemented

## Overview

This document provides a comprehensive specification of the Android client user interface, including all screens, navigation flows, algorithms, and behavioral patterns. This documentation is intended for synchronizing the Desktop UI implementation with the Android client.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Navigation Structure](#navigation-structure)
3. [Screen Specifications](#screen-specifications)
4. [Component Specifications](#component-specifications)
5. [Algorithms and Behaviors](#algorithms-and-behaviors)
6. [Data Flow](#data-flow)
7. [Localization](#localization)
8. [Validation Rules](#validation-rules)
9. [Error Handling](#error-handling)

## Architecture Overview

### Technology Stack

- **UI Framework:** Jetpack Compose
- **Navigation:** Jetpack Navigation Compose
- **State Management:** ViewModel + StateFlow
- **Database:** Room (SQLite)
- **Localization:** Android Resources (values/strings.xml, values-ru/strings.xml)

### Design Principles

- **Material Design 3:** All UI components follow Material Design 3 guidelines
- **Offline-First:** Local database with background sync
- **Reactive UI:** StateFlow-based reactive updates
- **Type Safety:** Strong typing with Kotlin

## Navigation Structure

### Navigation Graph

The application uses a single `NavHost` with the following routes:

```
dashboard (start destination)
├── events
│   ├── event/{eventId}
│   │   ├── event/{eventId}/edit
│   │   └── judgments/{eventId}
│   │       └── judgment/submit/{eventId}
│   └── event/create
├── contexts
│   └── context/create
├── summary
├── training
├── settings
└── nodes
```

### Navigation Patterns

#### 1. Template Selection Flow

**From New Event Screen:**
```
New Event → Select Template Button → Context Templates Screen
    ↓ (user selects template)
Context Templates Screen → (pop back) → New Event Screen (fields filled)
```

**Implementation:**
- Flag `selectTemplateForEvent` is set in `savedStateHandle` of "contexts" entry
- When template is clicked, context fields are stored in "event/create" entry's `savedStateHandle`
- Navigation returns to "event/create" via `popBackStack()`
- `LaunchedEffect` in "event/create" observes `savedStateHandle` changes and updates ViewModel

#### 2. View Judgments Flow

**From Dashboard:**
```
Dashboard → View Judgments Button → Events List (viewJudgments flag = true)
    ↓ (user selects event)
Events List → Judgments Screen (for selected event)
```

**Implementation:**
- Flag `viewJudgments` is set in "events" entry's `savedStateHandle`
- Flag persists across multiple event selections
- Flag is cleared only when navigating via "View Events" button

#### 3. Template Creation Flow

**From Context Templates Screen:**
```
Context Templates → Click Template → New Template Screen (fields pre-filled)
    OR
Context Templates → Add Button → New Template Screen (empty)
```

**Implementation:**
- Template data is stored in "context/create" entry's `savedStateHandle`
- `LaunchedEffect` in "context/create" observes initial values and updates form state

## Screen Specifications

### 1. Dashboard Screen

**Route:** `dashboard`  
**File:** `ui/compose/DashboardScreen.kt`  
**ViewModel:** `DashboardViewModel`

#### Visual Components

- **Top App Bar:** "Dashboard" title
- **Sync Status Card:**
  - Online/Offline indicator
  - Last sync time
  - Sync button
  - Pending operations count
- **Quick Stats:**
  - Total Events count (clickable, navigates to Events)
- **Action Buttons:**
  - "View Events" → navigates to Events List
  - "View Judgments" → navigates to Events List with `viewJudgments` flag
  - "New Event" → navigates to New Event Screen
  - "Manage Context Templates" → navigates to Context Templates Screen
  - "Overall Summary" → navigates to Overall Summary Screen
  - "Training Results" → navigates to Training Results Screen
  - "Settings" → navigates to Settings Screen

#### Behavior

- Loads sync status and event count on mount
- Updates sync status in real-time
- Handles navigation to all major screens

### 2. New Event Screen

**Route:** `event/create`  
**File:** `ui/compose/events/EventCreateScreen.kt`  
**ViewModel:** `EventCreateViewModel`

#### Visual Components

- **Top App Bar:** "New Event" title, back button, save button
- **Form Fields:**
  - **Name:** TextField (required)
  - **Description:** TextField (required)
  - **Context Fields Section:**
    - Title: "Context Fields"
    - "Select Template" button (OutlinedButton with icon)
    - Category picker (ContextPicker)
    - Forma picker (ContextPicker)
    - Cause picker (ContextPicker)
    - Develop picker (ContextPicker)
    - Effect picker (ContextPicker)
  - **Timestamps:**
    - Start Timestamp (DatePickerField, defaults to current date, cannot be empty)
    - End Timestamp (DatePickerField, can be empty, has clear button)
  - **Vector:** Toggle (Outgoing/Incoming)

#### Validation Rules

1. **Name:** Required, cannot be empty
2. **Description:** Required, cannot be empty
3. **All Context Fields:** Required, cannot be NULL
   - Validation errors shown per field
   - Save button disabled if any field is NULL
4. **Start Timestamp:** Required, defaults to current date, cannot be cleared
5. **End Timestamp:** Optional, but if provided:
   - Cannot be less than Start Timestamp
   - Can be equal to Start Timestamp
   - Can be empty

#### Template Selection Algorithm

1. User clicks "Select Template" button
2. Flag `selectTemplateForEvent = true` is set in "contexts" entry's `savedStateHandle`
3. Navigation to "contexts" screen
4. User selects a template
5. Template context fields are stored in "event/create" entry's `savedStateHandle`:
   - `selectedTemplateCategoryId`
   - `selectedTemplateFormaId`
   - `selectedTemplateCauseId`
   - `selectedTemplateDevelopId`
   - `selectedTemplateEffectId`
6. Navigation returns to "event/create" via `popBackStack()`
7. `LaunchedEffect` in "event/create" observes `savedStateHandle` changes
8. ViewModel's `setSelectedTemplateContext()` is called with template context
9. Form fields are updated via `selectedTemplateContext` StateFlow
10. `savedStateHandle` values are cleared after use

#### Date Validation Algorithm

```kotlin
// Normalize both timestamps to start of day (00:00:00)
val normalizedStart = normalizeToStartOfDay(timestampStart)
val normalizedEnd = timestampEnd?.let { normalizeToStartOfDay(it) }

// Validation: End cannot be less than Start
if (normalizedEnd != null && normalizedEnd < normalizedStart) {
    timestampEndError = "End Timestamp cannot be less than Start Timestamp"
} else {
    timestampEndError = null
}
```

### 3. Event List Screen

**Route:** `events`  
**File:** `ui/compose/events/EventListScreen.kt`  
**ViewModel:** `EventListViewModel`

#### Visual Components

- **Top App Bar:** "Events" title, "New Event" FAB
- **Events List:** LazyColumn with event cards
- **Event Card:**
  - Event description (truncated)
  - Timestamps
  - Vector indicator
  - Click handler

#### Behavior

- Loads events on mount
- Observes `viewJudgments` flag from `savedStateHandle`
- On event click:
  - If `viewJudgments == true`: Navigate to `judgments/{eventId}`
  - If `viewJudgments == false`: Navigate to `event/{eventId}`

### 4. Event Detail Screen

**Route:** `event/{eventId}`  
**File:** `ui/compose/events/EventDetailScreen.kt`  
**ViewModel:** `EventDetailViewModel`

#### Visual Components

- **Top App Bar:** "Event Details" title, Edit button, Delete button
- **Event Information:**
  - Description (headline)
  - Vector chip (Outgoing/Incoming)
  - **Context Fields Display:**
    - FlowRow with AssistChips
    - Format: "Category: {name}", "Forma: {name}", etc.
    - Entity names resolved from knowledge base flows
    - Fallback to ID if name not found
  - Timestamps (formatted)
  - Flags (Detected, Corrected)

#### Context Field Display Algorithm

1. Collect knowledge base entities from flows:
   - `categoriesFlow`, `formasFlow`, `causesFlow`, `developsFlow`, `effectsFlow`
2. For each context field ID in event:
   - Find entity by ID in corresponding list
   - Extract name from entity
   - If name found: Display "Field: {name}"
   - If name not found: Display "Field: {id}"
3. Use `remember()` with keys (ID, list size, list) to force recomputation when data changes
4. This ensures context fields update after language change and knowledge base re-seeding

#### Behavior

- Loads event on mount
- Observes knowledge base entity flows
- Updates context field display when flows emit new data
- Handles edit and delete actions
- Navigates to Judgments screen

### 5. Event Edit Screen

**Route:** `event/{eventId}/edit`  
**File:** `ui/compose/events/EventEditScreen.kt`  
**ViewModel:** `EventDetailViewModel`

#### Visual Components

- **Top App Bar:** "Edit Event" title, back button, save button
- **Read-Only Fields:**
  - Name (extracted from description)
  - Description (without name)
  - Context Fields (display only, same as Event Detail Screen)
- **Editable Fields:**
  - **Flags:**
    - Detected (Switch)
    - Corrected (read-only, auto-calculated)
  - **Timestamps:**
    - Start Timestamp (read-only, displays existing value)
    - End Timestamp (DatePickerField, always editable)

#### Corrected Flag Algorithm

```kotlin
val corrected = remember(timestampEnd, initialTimestampEnd) {
    if (initialTimestampEnd == null) {
        // If End Timestamp was initially empty, Corrected is not set
        event.corrected
    } else {
        // If End Timestamp was set and changed, Corrected is automatically set
        if (timestampEnd != null && timestampEnd != initialTimestampEnd) {
            true
        } else {
            event.corrected
        }
    }
}
```

#### Validation Rules

1. **End Timestamp:** Cannot be less than Start Timestamp (can be equal)
2. **End Timestamp:** Defaults to current date if not filled, cannot be empty

### 6. Context Templates Screen

**Route:** `contexts`  
**File:** `ui/compose/contexts/ContextTemplateListScreen.kt`  
**ViewModel:** `ContextTemplateListViewModel`

#### Visual Components

- **Top App Bar:** "Context Templates" title, Add button
- **FAB:** Add button (duplicate of top bar)
- **Template List:** LazyColumn with template cards
- **Template Card:**
  - Template name
  - Context fields (FlowRow with AssistChips, same format as Event Detail)
  - Description (if provided)
  - Click handler

#### Behavior

- Loads templates on mount
- Observes `selectTemplateForEvent` flag from `savedStateHandle`
- On template click:
  - If `selectTemplateForEvent == true`:
    - Store template context in "event/create" entry's `savedStateHandle`
    - Pop back to "event/create"
  - If `selectTemplateForEvent == false`:
    - Store template data in "context/create" entry's `savedStateHandle`
    - Navigate to "context/create"

### 7. New Template Screen

**Route:** `context/create`  
**File:** `ui/compose/contexts/ContextTemplateEditorScreen.kt`  
**ViewModel:** `ContextTemplateEditorViewModel`

#### Visual Components

- **Top App Bar:** "New Template" title, back button, save button
- **Form Fields:**
  - Name (TextField, required)
  - Description (TextField, optional)
  - **Context Fields:**
    - Category picker (ContextPicker, required)
    - Forma picker (ContextPicker, required)
    - Cause picker (ContextPicker, required)
    - Develop picker (ContextPicker, required)
    - Effect picker (ContextPicker, required)

#### Validation Rules

1. **Name:** Required, cannot be empty
2. **All Context Fields:** Required, cannot be NULL
   - Validation errors shown per field
   - Save button disabled if any field is NULL
3. **Duplicate Detection:** Templates with identical non-NULL context fields cannot be created
   - Name and description are NOT compared
   - Only context fields (category, forma, cause, develop, effect) are compared

#### Pre-filling Algorithm

1. When template is selected from Context Templates screen:
   - Template data is stored in "context/create" entry's `savedStateHandle`
2. `LaunchedEffect` observes initial values:
   - `initialName`, `initialCategoryId`, `initialFormaId`, etc.
3. When values change:
   - Form fields are updated
   - Validation errors are cleared

### 8. Judgments Screen

**Route:** `judgments/{eventId}`  
**File:** `ui/compose/judgments/JudgmentListScreen.kt`  
**ViewModel:** `JudgmentListViewModel`

#### Visual Components

- **Top App Bar:** "Judgments" title
- **FAB:** Add Judgment button
- **Event Title Card:** Displays event description
- **Consensus Statistics Card:**
  - True count
  - False count
  - Uncertain count
  - Consensus percentage
- **Judgments List:** LazyColumn with judgment cards
- **Judgment Card:**
  - Assessment (True/False/Uncertain)
  - Confidence level
  - Reasoning (if provided)
  - Submitted timestamp

#### Behavior

- Loads judgments and statistics on mount
- Updates when new judgments are submitted
- Navigates to Judgment Submission screen

### 9. Judgment Submission Screen

**Route:** `judgment/submit/{eventId}`  
**File:** `ui/compose/judgments/JudgmentSubmissionScreen.kt`

#### Visual Components

- **Top App Bar:** "Submit Judgment" title, back button, submit button
- **Event Card:** Displays event description
- **Form Fields:**
  - Assessment (RadioGroup: True/False/Uncertain)
  - Confidence Level (TextField, 0.0-1.0)
  - Reasoning (TextField, optional)

#### Validation Rules

1. **Assessment:** Required, must be "true", "false", or "uncertain"
2. **Confidence Level:** Required, must be between 0.0 and 1.0

### 10. Overall Summary Screen

**Route:** `summary`  
**File:** `ui/compose/summary/OverallSummaryScreen.kt`  
**ViewModel:** `OverallSummaryViewModel`

#### Visual Components

- **Top App Bar:** "Overall Summary" title, refresh button
- **Metrics Card:**
  - Total events count
  - Detected events count
  - Events with consensus count
  - Average collective score
  - Last updated timestamp
- **Network Statistics:**
  - Node count
  - Active connections
  - Sync status

#### Behavior

- Loads metrics on mount
- Refreshes on button click
- Updates periodically

### 11. Training Results Screen

**Route:** `training`  
**File:** `ui/compose/training/TrainingResultsScreen.kt`  
**ViewModel:** `TrainingResultsViewModel`

#### Visual Components

- **Top App Bar:** "Training Results" title, refresh button
- **Progress Metrics Card:**
  - Total events
  - Total positive impact
  - Total negative impact
  - Average score
  - Trend indicator
- **Impact Progress:**
  - Progress percentage
  - Progress bar
- **Results Table:** Training results data

#### Behavior

- Loads training data on mount
- Refreshes on button click
- Updates periodically

### 12. Settings Screen

**Route:** `settings`  
**File:** `ui/compose/settings/SettingsScreen.kt`  
**ViewModel:** `SettingsViewModel`

#### Visual Components

- **Top App Bar:** "Settings" title, back button
- **Language Selection:**
  - FilterChips for English and Russian
  - Confirmation dialog on change
- **Connection Mode Toggle:** Core (Local) vs HTTP API
- **Server Configuration:**
  - IP Address input
  - Port input
- **Nearby Sync Settings:**
  - Toggle + Interval input
- **Discovery Worker Settings:**
  - Enable toggle
  - LAN/Wi-Fi/Global intervals
  - LAN/Wi-Fi/Global TTLs
- **Test Connection Button**
- **Clear Events Button:** Deletes all events (with confirmation)
- **Connection Status Panel:**
  - Test result
  - Timestamp
  - Online/Offline status
  - Pending operations count

#### Language Change Algorithm

1. User selects language (English or Russian)
2. Confirmation dialog is shown
3. On confirmation:
   - `SettingsViewModel.changeLanguage()` is called
   - Locale is saved to `AppConfig`
   - Context templates are cleared
   - Knowledge base is re-seeded with temporary tables solution
   - Activity is recreated via `onLanguageChanged()` callback
4. `attachBaseContext()` applies new locale
5. UI updates with new language

### 13. Nodes Screen

**Route:** `nodes`  
**File:** `ui/compose/nodes/NodesScreen.kt`  
**ViewModel:** `NodesViewModel`

#### Visual Components

- **Top App Bar:** "Nodes" title
- **Discovery Status:** Online/Offline indicator
- **Nodes List:** LazyColumn with node items
- **Node Item:**
  - Node address
  - Node type (Hub/Leaf)
  - Status (reachable/unreachable)
  - Last seen timestamp
  - Source (LAN/Global/Manual)

#### Behavior

- Loads nodes on mount
- Updates when nodes change
- Supports manual refresh

## Component Specifications

### ContextPicker

**File:** `ui/compose/components/ContextPicker.kt`

#### Purpose

Reusable component for selecting knowledge base entities (Category, Forma, Cause, Develop, Effect).

#### Visual Components

- **Label:** Field name
- **Dropdown:** ExposedDropdownMenuBox with entity list
- **Error State:** Red border and error message (if validation fails)

#### Behavior

- Loads entities from Flow
- Displays human-readable names
- Validates selected ID exists in entity list
- Shows error state if validation fails

### DatePickerField

**File:** `ui/compose/components/DatePickerField.kt`

#### Purpose

Reusable component for date selection with Material Date Picker.

#### Visual Components

- **Label:** Field name
- **TextField:** Displays formatted date
- **Clear Button:** (optional) Clears date value

#### Behavior

- Opens Material Date Picker on click
- Formats date according to locale
- Validates date range
- Supports clearing (if `allowClear = true`)

## Algorithms and Behaviors

### 1. Context Field Display Algorithm

**Purpose:** Display human-readable names for context field IDs.

**Implementation:**

```kotlin
// Helper function
private fun <T> getEntityNameById(
    id: Int?,
    entities: List<T>,
    getId: (T) -> Int,
    getName: (T) -> String
): String? {
    if (id == null) return null
    return entities.find { getId(it) == id }?.let { getName(it) }
}

// Usage in screen
val categoryDisplay = remember(event.categoryId, categories.size, categories) {
    event.categoryId?.let { id ->
        val name = getEntityNameById(id, categories, { it.id }, { it.name })
        if (name != null) name else id.toString()
    }
}
```

**Key Points:**
- Uses `remember()` with keys to force recomputation when data changes
- Keys include: field ID, list size, and list itself
- Falls back to ID if name not found
- Ensures immediate update after knowledge base re-seeding

### 2. Template Selection for Event Algorithm

**Purpose:** Fill event form fields from selected template.

**Flow:**

1. User clicks "Select Template" in New Event screen
2. Flag `selectTemplateForEvent = true` is set in "contexts" entry
3. Navigation to "contexts" screen
4. User selects template
5. Template context stored in "event/create" entry's `savedStateHandle`
6. Navigation returns via `popBackStack()`
7. `LaunchedEffect` observes `savedStateHandle` changes
8. ViewModel's `setSelectedTemplateContext()` is called
9. Form fields update via `selectedTemplateContext` StateFlow

### 3. Date Normalization Algorithm

**Purpose:** Normalize timestamps to start of day for accurate comparison.

**Implementation:**

```kotlin
private fun normalizeToStartOfDay(timestamp: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}
```

**Usage:** Both Start and End timestamps are normalized before comparison.

### 4. Knowledge Base Re-seeding with Temporary Tables

**Purpose:** Preserve event data during language change.

**Flow:**

1. Create temporary tables: `temp_truth_events`, `temp_impact`, `temp_progress_metrics`
2. Move data from main tables to temporary tables
3. Clear knowledge base tables
4. Insert new knowledge base records (same IDs, different names)
5. Restore data from temporary tables
6. Drop temporary tables

**Transaction Safety:** All operations in single transaction.

## Data Flow

### Event Creation Flow

```
User Input → EventCreateScreen
    ↓
Form Validation
    ↓
EventCreateViewModel.createEvent()
    ↓
EventRepository.createEvent()
    ↓
Room Insert
    ↓
Success → Navigation to Dashboard
```

### Template Selection Flow

```
New Event Screen → Select Template Button
    ↓
Set selectTemplateForEvent flag
    ↓
Navigate to Context Templates
    ↓
User selects template
    ↓
Store template context in savedStateHandle
    ↓
Pop back to New Event
    ↓
LaunchedEffect observes savedStateHandle
    ↓
Update ViewModel
    ↓
Form fields update
```

### Language Change Flow

```
Settings Screen → Language Selection
    ↓
Confirmation Dialog
    ↓
SettingsViewModel.changeLanguage()
    ↓
1. Save locale to AppConfig
2. Clear context templates
3. Re-seed knowledge base (temporary tables)
    ↓
Activity recreation
    ↓
attachBaseContext() applies locale
    ↓
UI updates
```

## Localization

### Supported Languages

- **English (en):** Default
- **Russian (ru):** Full support

### String Resources

- **English:** `app/src/main/res/values/strings.xml`
- **Russian:** `app/src/main/res/values-ru/strings.xml`

### Locale Application

1. **Application Level:** `TruthTrainingApplication.attachBaseContext()`
2. **Activity Level:** `MainActivity.attachBaseContext()`
3. **Resource Resolution:** Android automatically resolves based on locale

### Knowledge Base Re-seeding

When language changes:
1. Context templates are cleared
2. Knowledge base is re-seeded with temporary tables solution
3. Event data is preserved
4. Context field IDs remain unchanged (FK relationships preserved)

## Validation Rules

### Event Validation

1. **Name:** Required, cannot be empty
2. **Description:** Required, cannot be empty
3. **All Context Fields:** Required, cannot be NULL
4. **Start Timestamp:** Required, defaults to current date
5. **End Timestamp:** Optional, but if provided:
   - Cannot be less than Start Timestamp
   - Can be equal to Start Timestamp

### Template Validation

1. **Name:** Required, cannot be empty
2. **All Context Fields:** Required, cannot be NULL
3. **Duplicate Detection:** Templates with identical non-NULL context fields cannot be created

### Judgment Validation

1. **Assessment:** Required, must be "true", "false", or "uncertain"
2. **Confidence Level:** Required, must be between 0.0 and 1.0

## Error Handling

### User-Facing Errors

- **Snackbar:** Displays error messages at bottom of screen
- **Inline Errors:** Field-level validation errors
- **Error Cards:** Full-width error cards for critical errors

### Error States

- **Loading State:** CircularProgressIndicator
- **Empty State:** Message with action button
- **Error State:** Error card with retry option

## Emoji Implementation

### Overview

The Android client implements emoji support for all UI elements in accordance with **Constitutional Requirement Rule 8**, ensuring accessibility and improved user experience. Emojis are language-independent and remain constant across English and Russian localizations.

### Emoji Mapping System

The emoji mapping is centralized in `EmojiMapping.kt` utility object, which provides consistent emoji assignment for:

- **Screens**: Dashboard 🏠, New Event ➕, Context Editor 📝, Events 📋, Judgments ⚖️, Overall Summary 📊, Training Results 📈, Settings ⚙️
- **Actions**: Save 💾, Cancel ❌, Delete 🗑️, Edit ✏️, Create ➕, Submit ✅, Refresh 🔄, Sync 🔄, Back ⬅️, Next ➡️
- **Form Fields**: Name 📝, Description 📄, Category 🏷️, Forma 📐, Cause 🔍, Develop 📈, Effect 💥, Start Date 📅, End Date 📅, Assessment ⚖️, Confidence 📊, Reasoning 💭
- **Status Indicators**: Online 🟢, Offline 🔴, Syncing 🔄, Error ❌, Success ✅, Warning ⚠️
- **Navigation**: Home 🏠, Events 📋, Judgments ⚖️, Templates 📝, Summary 📊, Training 📈, Settings ⚙️

### Usage Pattern

All UI elements follow the consistent pattern:

```kotlin
"${EmojiMapping.getEmoji("category", "key")} ${context.getString(R.string.localized_text)}"
```

Example:
```kotlin
Text("${EmojiMapping.getEmoji("screens", "dashboard")} ${context.getString(R.string.dashboard)}")
// Displays: "🏠 Dashboard" (English) or "🏠 Панель управления" (Russian)
```

### Localization

Emojis are **language-independent** - the same emoji is used regardless of the selected language (English/Russian). Only the text portion changes based on locale:

- English: "🏠 Dashboard"
- Russian: "🏠 Панель управления"

### Implementation Coverage

All screens, action buttons, form field labels, status indicators, and navigation items have been updated to include appropriate emojis matching the Desktop UI implementation.

### Testing

Comprehensive test coverage includes:

- **EmojiMappingTest.kt**: Unit tests for emoji mapping utility
- **EmojiCoverageTest.kt**: Integration tests verifying emoji presence across all UI elements
- **EmojiLocalizationTest.kt**: Tests for language-independent emoji behavior
- **DesktopParityTest.kt**: Contract tests ensuring emoji values match Desktop exactly
- **EmojiAccessibilityTest.kt**: Tests for TalkBack support and graceful degradation
- **EmojiEdgeCasesTest.kt**: Tests for edge cases (rendering failures, unsupported devices, theme compatibility)

### Performance

Emoji lookup is O(1) and does not impact UI frame time (< 1ms average). The implementation uses efficient map lookups with no performance overhead.

### Graceful Degradation

If emoji lookup fails (invalid category/key), an empty string is returned, and the UI falls back to text-only display:

```kotlin
val emoji = EmojiMapping.getEmoji("screens", "dashboard")
val label = if (emoji.isNotEmpty()) {
    "$emoji ${context.getString(R.string.dashboard)}"
} else {
    context.getString(R.string.dashboard) // Fallback to text only
}
```

This ensures the UI remains functional even on devices with limited emoji support.

### Theme Compatibility

All emojis are valid Unicode emoji characters (Unicode 12.0+) that render correctly in both light and dark Material Design 3 themes.

## Related Documents

- [Android Implementation Summary](ANDROID_IMPLEMENTATION_SUMMARY.md)
- [Localization Implementation](specs/014-android-localization/LOCALIZATION_IMPLEMENTATION.md)
- [Desktop UI Specification](UI_Desktop.md)
- [Functional Specification](../spec/24-function_mobile_android.md)

---

**Version:** v1.0.0  
**Last Updated:** 2025-01-XX

