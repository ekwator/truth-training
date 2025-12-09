# Research: Full Desktop UI Reconstruction and Synchronization

**Feature**: Full Desktop UI Reconstruction and Synchronization  
**Date**: 2025-12-09  
**Phase**: 0 - Research & Technology Decisions

## Research Areas

### 1. Android UI Specification Analysis

**Decision**: Use Android UI Specification (`docs/ANDROID_UI_SPECIFICATION.md`) as the authoritative source for Desktop UI synchronization.

**Key Findings**:
- Android UI has 13 screens, Desktop UI has 7 screens that need synchronization
- Android uses Jetpack Compose with Material Design 3
- Android uses Jetpack Navigation Compose with flag-based routing
- Android uses ViewModel + StateFlow for state management
- Android uses Room (SQLite) for local database
- Android supports RU/EN localization (to be removed in Desktop)

**Navigation Patterns Identified**:
1. **Template Selection Flow**: Flag `selectTemplateForEvent` in `savedStateHandle` of "contexts" entry
2. **View Judgments Flow**: Flag `viewJudgments` in "events" entry's `savedStateHandle`
3. **Template Creation Flow**: Template data stored in "context/create" entry's `savedStateHandle`

**Screen Mapping**:
- Dashboard → Dashboard (Desktop)
- New Event → NewEvent (Desktop)
- Event List → Events (Desktop)
- Event Detail → EventDetail (Desktop, may need new screen)
- Event Edit → EventEdit (Desktop, may need new screen)
- Context Templates → ContextEditor (Desktop)
- New Template → ContextEditor (Desktop, same screen)
- Judgments → Judgments (Desktop)
- Judgment Submission → Judgments (Desktop, modal/form)
- Overall Summary → OverallSummary (Desktop)
- Training Results → TrainingResults (Desktop)
- Settings → Settings (Desktop)
- Nodes → Dashboard (Desktop, NodesPanel component)

**Rationale**: Android UI Specification provides complete documentation of all screens, navigation flows, algorithms, and behavioral patterns. This is the source of truth for Desktop UI synchronization.

**Alternatives Considered**:
- Using only Desktop UI current implementation: Rejected - would not achieve synchronization goal
- Creating new specification from scratch: Rejected - Android implementation is complete and tested

### 2. Desktop UI Current Structure Review

**Decision**: Preserve existing Desktop UI structure while rebuilding visual layer and UI behavior.

**Current Structure**:
- **Frontend**: React 18.2.0, TypeScript 5.2.2, Vite 6.4.1, Tauri 2.9.0
- **State Management**: Zustand 4.4.7
- **Styling**: Tailwind CSS, Headless UI
- **Backend**: Tauri (Rust), SQLite (rusqlite 0.31)
- **Screens**: 8 screens (Dashboard, NewEvent, ContextEditor, EventSummary, Events, Judgments, OverallSummary, TrainingResults, Settings)

**Key Components**:
- `ContextPicker`: Searchable combobox with validation (already matches Android pattern)
- `DatePickerField`: Date picker component (needs Android algorithm synchronization)
- `TopMenuBar`: Navigation bar with screen routing
- `NodesPanel`: Network nodes display (Desktop-specific)

**Current Navigation**: Screen state management via `useNavigationStore` (Zustand), keyboard shortcuts (Alt+1 through Alt+8)

**Desktop-Specific Functionality to Preserve**:
- Keyboard shortcuts (Alt+1 through Alt+8)
- NodesPanel component (network nodes display)
- Tauri-specific features (system integration, file system access)
- CLI integration (if any)
- Any Desktop-only screens or features not present in Android

**Rationale**: Existing structure is well-organized and follows React/TypeScript best practices. Only visual layer and UI behavior need reconstruction, not the entire architecture.

**Alternatives Considered**:
- Complete rewrite: Rejected - too risky, would lose Desktop-specific functionality
- Minimal changes: Rejected - would not achieve synchronization goal

### 3. Flag-Based Navigation Patterns in React/TypeScript

**Decision**: Implement flag-based navigation using Zustand store with navigation state flags, equivalent to Android's `savedStateHandle`.

**Android Pattern**:
- `savedStateHandle` stores navigation flags and data
- Flags persist across navigation (e.g., `viewJudgments` flag persists across event selections)
- Flags are cleared only when explicitly reset
- `LaunchedEffect` observes flag changes and updates ViewModel

**React/TypeScript Equivalent**:
- Use Zustand store (`stores/navigation.ts`) to manage navigation flags
- Flags: `selectTemplateForEvent`, `viewJudgments`, `selectedTemplateContext`
- Use React `useEffect` to observe flag changes (equivalent to `LaunchedEffect`)
- Flags persist in Zustand store across navigation
- Flags are cleared only when explicitly reset

**Implementation Pattern**:
```typescript
// stores/navigation.ts
interface NavigationState {
  selectTemplateForEvent: boolean;
  viewJudgments: boolean;
  selectedTemplateContext: {
    categoryId?: number;
    formaId?: number;
    causeId?: number;
    developId?: number;
    effectId?: number;
  };
  // Actions
  setSelectTemplateForEvent: (value: boolean) => void;
  setViewJudgments: (value: boolean) => void;
  setSelectedTemplateContext: (context: {...}) => void;
  clearTemplateSelection: () => void;
}
```

**Rationale**: Zustand provides lightweight state management with persistence capabilities. Navigation flags can be stored in Zustand store and observed via React hooks, matching Android's `savedStateHandle` pattern.

**Alternatives Considered**:
- React Context API: Rejected - performance concerns with frequent updates
- URL query parameters: Rejected - not suitable for internal navigation flags
- Local storage: Rejected - too slow, not needed for in-memory navigation state

### 4. Safe Database Reseeding with Temporary Tables

**Decision**: Implement safe database reseeding using temporary tables with atomic swap, matching Android's approach.

**Android Pattern** (from `specs/014-android-localization/LOCALIZATION_IMPLEMENTATION.md`):
1. Create temporary tables with `temp_` prefix
2. Fill temporary tables with new data (English-only)
3. Atomically swap temporary tables with main schema tables
4. Drop temporary tables
5. Refresh UI with updated data

**Desktop Implementation**:
- Implement in `src-tauri/src/commands/knowledge_base.rs`
- Use SQLite transactions for atomicity
- Create temporary tables: `temp_category`, `temp_forma`, `temp_cause`, `temp_develop`, `temp_effect`, `temp_context`
- Fill temporary tables with English-only data
- Use `ALTER TABLE ... RENAME TO` for atomic swap (SQLite supports this)
- Drop temporary tables after successful swap
- Emit event to refresh UI

**Algorithm**:
```rust
// Pseudo-code
1. Begin transaction
2. Create temp_category, temp_forma, temp_cause, temp_develop, temp_effect, temp_context
3. Insert English-only data into temp tables
4. Rename main tables to old_* (backup)
5. Rename temp tables to main table names (atomic swap)
6. Drop old_* tables
7. Commit transaction
8. Emit UI refresh event
```

**Rationale**: Temporary tables approach ensures FK → PK integrity is maintained throughout the process. Atomic swap prevents data corruption. This matches Android's proven approach.

**Alternatives Considered**:
- DELETE + INSERT: Rejected - risks FK constraint violations during process
- TRUNCATE + INSERT: Rejected - SQLite doesn't support TRUNCATE, and risks FK violations
- Separate database: Rejected - too complex, requires data migration

### 5. Emoji Implementation Patterns for Accessibility

**Decision**: Add emojis to all UI elements following constitutional requirement Rule 8.

**Requirements** (from Constitution Rule 8):
- All Desktop UI interface elements MUST include appropriate emojis
- Emojis must be semantically meaningful and directly related to function
- Emoji selection must be consistent across the application
- Emojis are enhancement, not replacement for text (graceful degradation)

**Implementation Pattern**:
- Create emoji mapping system in `src/utils/emojiMapping.ts`
- Map UI elements to emojis: buttons, menu items, navigation links, form labels, status indicators
- Use consistent emojis for similar functionality
- Example mappings:
  - Dashboard: 🏠
  - New Event: ➕
  - Context Editor: 📝
  - Events: 📋
  - Judgments: ⚖️
  - Settings: ⚙️
  - Save: 💾
  - Cancel: ❌
  - Delete: 🗑️
  - Edit: ✏️
  - Sync: 🔄
  - Online: 🟢
  - Offline: 🔴

**Component Integration**:
- Update all button components to include emoji prefix
- Update form labels to include emoji prefix
- Update navigation items to include emoji prefix
- Update status indicators to include emoji prefix

**Rationale**: Emojis provide universal visual cues that transcend language barriers. Consistent emoji usage improves accessibility for users with limited interface language comprehension.

**Alternatives Considered**:
- Icons library: Rejected - adds dependency, emojis are built-in and universal
- Images: Rejected - adds complexity, emojis are simpler and more accessible
- No visual enhancement: Rejected - violates constitutional requirement Rule 8

### 6. Algorithm Synchronization Requirements

**Decision**: Desktop UI algorithms must match Android algorithms exactly.

**Algorithms to Synchronize**:

1. **Context Field Visibility Rules**:
   - Android: Context fields are always visible in New Event screen
   - Desktop: Must match - all context fields always visible

2. **Date Normalization Algorithm**:
   - Android: Normalize timestamps to start of day (00:00:00) for validation
   - Desktop: Must implement identical algorithm in `utils/dateNormalization.ts`
   ```typescript
   function normalizeToStartOfDay(timestamp: number): number {
     const date = new Date(timestamp);
     date.setHours(0, 0, 0, 0);
     return date.getTime();
   }
   ```

3. **Date Validation Algorithm**:
   - Android: End timestamp cannot be less than Start timestamp (after normalization)
   - Desktop: Must match - validate after normalization

4. **Template Selection Algorithm**:
   - Android: Flag-based navigation with `savedStateHandle`
   - Desktop: Must match - Zustand store with navigation flags

5. **Corrected Flag Algorithm**:
   - Android: Auto-calculated based on End Timestamp change
   - Desktop: Must match - same calculation logic

6. **Context Field Display Algorithm**:
   - Android: Entity names resolved from knowledge base flows, fallback to ID
   - Desktop: Must match - entity resolution in `utils/entityResolution.ts`

**Rationale**: Algorithm synchronization ensures behavioral parity between Desktop and Android. Users should experience identical behavior across platforms.

**Alternatives Considered**:
- Different algorithms: Rejected - would break synchronization goal
- Simplified algorithms: Rejected - would break synchronization goal

### 7. Desktop-Specific Functionality Identification

**Decision**: Preserve all Desktop-specific functionality during UI reconstruction.

**Identified Desktop-Specific Features**:
1. **Keyboard Shortcuts**: Alt+1 through Alt+8 for screen navigation
2. **NodesPanel Component**: Network nodes display (not in Android)
3. **Tauri Integration**: System-level features (file system, notifications, etc.)
4. **CLI Integration**: If any CLI-specific features exist
5. **Desktop-Only Screens**: Any screens not present in Android

**Preservation Strategy**:
- Identify all Desktop-specific code paths
- Mark them as "DO NOT MODIFY" during reconstruction
- Test Desktop-specific features after each reconstruction phase
- Document Desktop-specific features in code comments

**Rationale**: Desktop-specific functionality provides unique value. Preserving it maintains Desktop's competitive advantage while achieving UI synchronization.

**Alternatives Considered**:
- Remove Desktop-specific features: Rejected - would reduce Desktop value
- Modify Desktop-specific features: Rejected - risks breaking functionality

### 8. Component Synchronization Patterns

**Decision**: Desktop components must match Android component patterns in appearance, behavior, and validation feedback.

**Components to Synchronize**:

1. **ContextPicker**:
   - Android: ExposedDropdownMenuBox with human-readable labels, manual entry, validation
   - Desktop: Already implemented as searchable combobox - verify it matches Android behavior exactly

2. **DatePickerField**:
   - Android: DatePicker with validation, clear button for optional fields
   - Desktop: Must match - implement same validation and clear button behavior

3. **Event Card**:
   - Android: Card with description, timestamps, vector indicator, context fields
   - Desktop: Must match - same layout and information display

4. **Template Card**:
   - Android: Card with name, context fields (FlowRow with AssistChips), description
   - Desktop: Must match - same layout and information display

5. **Judgment Card**:
   - Android: Card with assessment, confidence level, reasoning, timestamp
   - Desktop: Must match - same layout and information display

**Rationale**: Component synchronization ensures visual and behavioral parity. Users should see and interact with identical components across platforms.

**Alternatives Considered**:
- Different component designs: Rejected - would break synchronization goal
- Simplified components: Rejected - would break synchronization goal

## Technology Decisions Summary

| Decision Area | Technology/Approach | Rationale |
|---------------|---------------------|-----------|
| Navigation State | Zustand store with flags | Equivalent to Android's savedStateHandle, lightweight, persistent |
| Database Reseeding | Temporary tables with atomic swap | Maintains FK integrity, matches Android approach |
| Emoji Implementation | Emoji mapping system with consistent patterns | Constitutional requirement, improves accessibility |
| Algorithm Synchronization | Exact algorithm matching | Ensures behavioral parity |
| Component Synchronization | Match Android component patterns | Ensures visual and behavioral parity |
| Desktop Feature Preservation | Identify and preserve Desktop-specific code | Maintains Desktop value while achieving synchronization |

## Next Steps

1. **Phase 1: Design**
   - Design flag-based navigation system (Zustand store)
   - Design safe database reseeding algorithm
   - Design emoji mapping system
   - Design component synchronization patterns
   - Create data-model.md
   - Create contracts/ directory
   - Create quickstart.md

2. **Phase 2: Task Breakdown**
   - Generate tasks.md via /speckit.tasks command
   - Organize tasks by user story priority
   - Define dependencies and execution order

3. **Implementation**
   - Rebuild Desktop UI visual layer
   - Implement flag-based navigation
   - Implement safe database reseeding
   - Add emojis to all UI elements
   - Synchronize algorithms
   - Preserve Desktop-specific functionality
   - Test synchronization with Android UI

