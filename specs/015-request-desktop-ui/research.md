# Research: Desktop UI Synchronization Based on Android Client Implementation

**Feature**: 015-request-desktop-ui  
**Date**: 2025-01-XX  
**Status**: Complete

## Research Objectives

1. Analyze Android UI implementation to understand patterns, algorithms, and behaviors
2. Review Desktop UI current state to identify gaps and differences
3. Identify synchronization points and requirements
4. Research localization system and database re-seeding solutions
5. Document technical approach for Desktop UI synchronization

## Android UI Implementation Analysis

### Reference Documentation

1. **Main UI Specification**: `docs/ANDROID_UI_SPECIFICATION.md`
   - Complete specification of all 13 screens
   - Navigation structure with route graph
   - Component specifications
   - Algorithms and behaviors with code examples
   - Data flow diagrams
   - Localization implementation

2. **Implementation Report**: `docs/ANDROID_UI_IMPLEMENTATION_REPORT.md`
   - Executive summary
   - Synchronization points
   - Recommendations for Desktop UI

3. **Functional Specification**: `spec/24-function_mobile_android.md`
   - Updated to reflect current implementation
   - Technology stack and design principles

4. **Localization Documentation**: `specs/014-android-localization/LOCALIZATION_IMPLEMENTATION.md`
   - Language switching flow
   - Knowledge base re-seeding with temporary tables
   - Event data preservation

### Key Android Implementation Patterns

#### 1. Navigation Structure

**Android Approach**:
- Single `NavHost` with route-based navigation
- Flag-based conditional routing using `savedStateHandle`
- Navigation graph with nested routes

**Key Routes**:
```
dashboard (start)
├── events
│   ├── event/{eventId}
│   │   ├── event/{eventId}/edit
│   │   └── judgments/{eventId}
│   └── event/create
├── contexts
│   └── context/create
├── summary
├── training
├── settings
└── nodes
```

**Desktop Equivalent**:
- Screen-based navigation via `setCurrentScreen` state
- No route-based navigation currently
- Keyboard shortcuts (Alt+1 through Alt+8)

**Synchronization Requirement**: Implement flag-based conditional routing while preserving keyboard shortcuts.

#### 2. Template Selection Flow

**Android Implementation**:
1. User clicks "Select Template" in New Event screen
2. Flag `selectTemplateForEvent = true` set in "contexts" entry's `savedStateHandle`
3. Navigation to "contexts" screen
4. User selects template
5. Template context stored in "event/create" entry's `savedStateHandle`
6. Navigation returns via `popBackStack()`
7. `LaunchedEffect` observes `savedStateHandle` changes
8. ViewModel's `setSelectedTemplateContext()` called
9. Form fields updated via `selectedTemplateContext` StateFlow

**Desktop Current State**:
- Template selection via dropdown in NewEvent screen
- No navigation to Context Templates screen for selection
- Template data prefills fields directly

**Synchronization Requirement**: Implement flag-based navigation to Context Templates screen, with state management for template context passing.

#### 3. Context Field Display Algorithm

**Android Implementation**:
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

// Usage with remember() for reactive updates
val categoryDisplay = remember(event.categoryId, categories.size, categories) {
    event.categoryId?.let { id ->
        val name = getEntityNameById(id, categories, { it.id }, { it.name })
        if (name != null) name else id.toString()
    }
}
```

**Key Points**:
- Uses `remember()` with keys (ID, list size, list) to force recomputation
- Falls back to ID if name not found
- Ensures immediate update after knowledge base re-seeding

**Desktop Current State**:
- May display IDs instead of names
- May not update reactively after knowledge base changes

**Synchronization Requirement**: Implement same algorithm with React `useMemo` or `useEffect` for reactive updates.

#### 4. Date Normalization Algorithm

**Android Implementation**:
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

**Usage**: Both Start and End timestamps normalized before comparison.

**Desktop Current State**: May not normalize dates, causing validation issues.

**Synchronization Requirement**: Implement same normalization algorithm in TypeScript/JavaScript.

#### 5. Localization System

**Android Implementation**:
- String resources: `values/strings.xml` (English), `values-ru/strings.xml` (Russian)
- Locale application at Application and Activity levels via `attachBaseContext()`
- Knowledge base re-seeding with temporary tables solution
- Context templates cleared on language change
- Event data preserved during language change

**Desktop Current State**:
- Localization system exists (`src/i18n/index.ts`, `src/i18n/ru.ts`)
- Hardcoded strings in UI components
- Database re-seeding may not use temporary tables solution
- Context templates may not be cleared on language change

**Synchronization Requirement**:
1. Replace all hardcoded strings with i18n keys
2. Implement temporary tables solution for database re-seeding
3. Clear context templates on language change
4. Ensure event data preservation

#### 6. Validation Rules

**Android Validation**:
- **Events**: Name required, Description required, All context fields required (cannot be NULL), Start Timestamp required (defaults to current date), End Timestamp optional (cannot be less than Start, can be equal)
- **Templates**: Name required, All context fields required (cannot be NULL), Duplicate detection based on context fields only (name and description not compared)
- **Judgments**: Assessment required (true/false/uncertain), Confidence Level required (0.0-1.0)

**Desktop Current State**: Validation rules may differ or be incomplete.

**Synchronization Requirement**: Match Android validation rules exactly.

## Desktop UI Current State Analysis

### Technology Stack

**Frontend**:
- React 18.2, TypeScript 5.2+
- Zustand for state management
- Tailwind CSS for styling
- Vite for build system

**Backend**:
- Tauri 2.5.1 (Rust)
- rusqlite 0.31 (bundled SQLite)
- Core library integration

### Current Screen Implementation

1. **Dashboard** (`src/pages/Dashboard.tsx`)
   - Exists, may need visual updates
   - Uses `useEventsStore`, `useSyncStore`

2. **NewEvent** (`src/pages/NewEvent.tsx`)
   - Exists, has ContextPicker
   - May need template selection flow update
   - May need DatePickerField component
   - May need validation updates

3. **ContextEditor** (`src/pages/ContextEditor.tsx`)
   - Exists, may need visual updates
   - May need duplicate detection updates

4. **EventSummary** (`src/pages/EventSummary.tsx`)
   - Exists, may need context field display updates
   - May need edit functionality

5. **OverallSummary** (`src/pages/OverallSummary.tsx`)
   - Exists, may need visual updates

6. **TrainingResults** (`src/pages/TrainingResults.tsx`)
   - Exists, may need visual updates

7. **Settings** (`src/pages/Settings.tsx`)
   - Exists, has localization toggle
   - May need database re-seeding updates

### Current Component Implementation

1. **ContextPicker** (`src/components/context/ContextPicker.tsx`)
   - Exists, searchable combobox
   - May need validation updates
   - May need error handling updates

2. **DatePickerField**: Not found, needs to be created

3. **LocaleToggle** (`src/components/layout/LocaleToggle.tsx`)
   - Exists, may need updates for database re-seeding

### Current Localization System

**Implementation**: `src/i18n/index.ts`

**Features**:
- Translation function `t(key: string)`
- Locale detection and persistence
- Russian translations in `ru.ts`
- Locale change via `setLocale()`

**Issues**:
- Hardcoded strings in UI components
- Database re-seeding may not use temporary tables
- Context templates may not be cleared on language change

### Current Navigation System

**Implementation**: `src/App.tsx`

**Features**:
- Screen-based navigation via `setCurrentScreen` state
- Keyboard shortcuts (Alt+1 through Alt+8)
- No route-based navigation
- No flag-based conditional routing

**Gaps**:
- No flag-based navigation for template selection
- No flag-based navigation for view judgments
- No state management for navigation flags

## Synchronization Requirements

### 1. Visual Structure Parity

**Requirement**: All 13 Desktop screens must visually match Android screens.

**Screens to Synchronize**:
1. Dashboard
2. New Event
3. Event List
4. Event Detail
5. Event Edit
6. Context Templates
7. New Template
8. Judgments
9. Judgment Submission
10. Overall Summary
11. Training Results
12. Settings
13. Nodes (if implemented)

### 2. Navigation Synchronization

**Requirement**: Implement flag-based conditional routing matching Android patterns.

**Key Flows**:
1. Template Selection Flow (New Event → Context Templates → New Event)
2. View Judgments Flow (Dashboard → Events List → Judgments)
3. Template Creation Flow (Context Templates → New Template)

### 3. Component Parity

**Requirement**: Components must behave identically to Android equivalents.

**Components**:
1. ContextPicker: Update validation, error handling, entity resolution
2. DatePickerField: Create new component with normalization and validation
3. Template Selection UI: Implement flag-based navigation
4. Context Field Display: Implement name resolution algorithm

### 4. Localization System Fix

**Requirement**: Fix localization system to work reliably with temporary tables solution.

**Tasks**:
1. Replace hardcoded strings with i18n keys
2. Implement temporary tables solution in Tauri backend
3. Clear context templates on language change
4. Preserve event data during language change

### 5. Algorithm Implementation

**Requirement**: Implement algorithms matching Android exactly.

**Algorithms**:
1. Context Field Display: Entity name resolution with reactive updates
2. Date Normalization: Normalize to start of day for comparison
3. Corrected Flag Calculation: Track initial End Timestamp, auto-set when changed
4. Template Selection: Flag-based navigation with state management

### 6. Validation Rules Parity

**Requirement**: Match Android validation rules exactly.

**Validation Areas**:
1. Event validation (name, description, context fields, dates)
2. Template validation (name, context fields, duplicate detection)
3. Judgment validation (assessment, confidence level)

## Technical Decisions

### 1. Navigation State Management

**Decision**: Use Zustand store for navigation flags instead of Android's `savedStateHandle`.

**Rationale**:
- Desktop uses Zustand for state management
- Zustand provides similar reactivity to Android's StateFlow
- No need to introduce new state management solution

**Implementation**:
```typescript
// Navigation store
interface NavigationState {
  selectTemplateForEvent: boolean;
  viewJudgments: boolean;
  setSelectTemplateForEvent: (value: boolean) => void;
  setViewJudgments: (value: boolean) => void;
}
```

### 2. Component Updates

**Decision**: Update existing ContextPicker, create new DatePickerField.

**Rationale**:
- ContextPicker exists and works, needs updates
- DatePickerField doesn't exist, needs to be created
- Maintains existing component architecture

### 3. Localization String Keys

**Decision**: Use same key structure as Android (`values/strings.xml` structure).

**Rationale**:
- Ensures consistency across platforms
- Makes translation maintenance easier
- Matches Android implementation

### 4. Database Re-seeding

**Decision**: Implement temporary tables solution in Tauri backend (Rust).

**Rationale**:
- Matches Android implementation exactly
- Preserves event data integrity
- Uses same algorithm for consistency

**Implementation Location**: `ui/desktop/src-tauri/src/storage.rs` or new command handler

### 5. State Preservation

**Decision**: Use React state management (Zustand) for template context passing.

**Rationale**:
- Similar to Android's StateFlow approach
- Integrates with existing Desktop architecture
- Provides reactivity for UI updates

## Risks and Mitigations

### Risk 1: Breaking Desktop-Specific Features

**Risk**: Modifying UI layer may accidentally break Desktop-only functionality.

**Mitigation**:
- Strict separation of UI layer from logic layer
- Use Desktop documentation to identify UI-only components
- Comprehensive testing of Desktop-specific features

### Risk 2: Performance Degradation

**Risk**: Database re-seeding during language change may be slow.

**Mitigation**:
- Optimize temporary tables operations
- Use efficient SQL queries
- Test with large datasets
- Target: <5 seconds for language switch

### Risk 3: Data Loss During Re-seeding

**Risk**: Temporary tables solution may not preserve all data correctly.

**Mitigation**:
- Use transaction-based approach
- Test thoroughly with existing event data
- Implement rollback mechanism
- Verify FK relationships after re-seeding

### Risk 4: Navigation Complexity

**Risk**: Flag-based navigation may be complex to implement in Desktop.

**Mitigation**:
- Use Zustand for simple flag management
- Keep navigation logic clear and documented
- Test all navigation flows thoroughly

## Research Conclusions

1. **Android UI is fully documented** with complete specifications for all screens, components, and algorithms.

2. **Desktop UI has foundation** but needs synchronization with Android patterns:
   - Navigation needs flag-based routing
   - Components need updates/creation
   - Localization needs fixes
   - Validation needs updates

3. **Temporary tables solution is critical** for database re-seeding to preserve event data.

4. **Visual parity is achievable** while preserving Desktop-specific functionality.

5. **Implementation is feasible** using existing Desktop architecture (React, Zustand, Tauri).

## Next Steps

1. Create data-model.md documenting data flow and entity relationships
2. Create quickstart.md with integration scenarios
3. Create contracts/ directory with API/component contracts
4. Generate tasks.md for implementation breakdown

---

**Status**: Research complete, ready for design phase

