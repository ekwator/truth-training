# Navigation State Contract

**Feature**: Full Desktop UI Reconstruction and Synchronization  
**Date**: 2025-12-09  
**Type**: State Management Contract

## Overview

This contract defines the navigation state management API for flag-based routing, equivalent to Android's `savedStateHandle` pattern.

## Contract: Navigation Store (Zustand)

### Store Interface

```typescript
interface NavigationState {
  // Template Selection Flow
  selectTemplateForEvent: boolean;
  selectedTemplateContext: {
    categoryId?: number;
    formaId?: number;
    causeId?: number;
    developId?: number;
    effectId?: number;
  } | null;

  // View Judgments Flow
  viewJudgments: boolean;
  selectedEventIdForJudgments: string | null;

  // Template Creation Flow
  selectedTemplateForEdit: {
    id: number;
    name: string;
    categoryId?: number;
    formaId?: number;
    causeId?: number;
    developId?: number;
    effectId?: number;
    description?: string;
  } | null;

  // Actions
  setSelectTemplateForEvent: (value: boolean) => void;
  setSelectedTemplateContext: (context: {
    categoryId?: number;
    formaId?: number;
    causeId?: number;
    developId?: number;
    effectId?: number;
  } | null) => void;
  setViewJudgments: (value: boolean) => void;
  setSelectedEventIdForJudgments: (eventId: string | null) => void;
  setSelectedTemplateForEdit: (template: {...} | null) => void;
  clearTemplateSelection: () => void;
  clearJudgmentsSelection: () => void;
  clearTemplateEdit: () => void;
}
```

### Usage Pattern

#### Template Selection Flow

```typescript
// In NewEvent screen
const setSelectTemplateForEvent = useNavigationStore(
  (state) => state.setSelectTemplateForEvent
);

const handleSelectTemplate = () => {
  setSelectTemplateForEvent(true);
  navigateToContextEditor();
};

// In ContextEditor screen
const selectTemplateForEvent = useNavigationStore(
  (state) => state.selectTemplateForEvent
);
const selectedTemplateContext = useNavigationStore(
  (state) => state.selectedTemplateContext
);
const setSelectedTemplateContext = useNavigationStore(
  (state) => state.setSelectedTemplateContext
);
const clearTemplateSelection = useNavigationStore(
  (state) => state.clearTemplateSelection
);

useEffect(() => {
  if (selectTemplateForEvent) {
    // Show template selection UI
  }
}, [selectTemplateForEvent]);

const handleTemplateSelect = (template: Template) => {
  setSelectedTemplateContext({
    categoryId: template.categoryId,
    formaId: template.formaId,
    causeId: template.causeId,
    developId: template.developId,
    effectId: template.effectId,
  });
  clearTemplateSelection();
  navigateBackToNewEvent();
};

// Back in NewEvent screen
useEffect(() => {
  const context = selectedTemplateContext;
  if (context) {
    // Update form fields with template context
    updateFormFields(context);
    clearTemplateSelection();
  }
}, [selectedTemplateContext]);
```

#### View Judgments Flow

```typescript
// In Dashboard screen
const setViewJudgments = useNavigationStore(
  (state) => state.setViewJudgments
);

const handleViewJudgments = () => {
  setViewJudgments(true);
  navigateToEvents();
};

// In Events screen
const viewJudgments = useNavigationStore((state) => state.viewJudgments);
const setSelectedEventIdForJudgments = useNavigationStore(
  (state) => state.setSelectedEventIdForJudgments
);

useEffect(() => {
  if (viewJudgments) {
    // Show judgments view mode
  }
}, [viewJudgments]);

const handleEventClick = (eventId: string) => {
  if (viewJudgments) {
    setSelectedEventIdForJudgments(eventId);
    navigateToJudgments(eventId);
  } else {
    navigateToEventDetail(eventId);
  }
};
```

## Contract: Flag Persistence

### Requirements

1. **Persistence**: Flags persist across navigation until explicitly cleared
2. **Scope**: Flags are global (Zustand store is global)
3. **Lifecycle**: Flags cleared only when:
   - `clearTemplateSelection()` called
   - `clearJudgmentsSelection()` called
   - `clearTemplateEdit()` called
   - App restart (Zustand store resets)

### Equivalent Android Pattern

- Android: `savedStateHandle` in Navigation Compose
- Desktop: Zustand store with navigation flags
- Behavior: Identical flag persistence and lifecycle

## Contract: State Observation

### React Hook Pattern

```typescript
// Observe flag changes
useEffect(() => {
  const flag = useNavigationStore.getState().selectTemplateForEvent;
  if (flag) {
    // Handle flag change
  }
}, []);

// Or use Zustand selector
const flag = useNavigationStore((state) => state.selectTemplateForEvent);
```

### Equivalent Android Pattern

- Android: `LaunchedEffect` observing `savedStateHandle` changes
- Desktop: `useEffect` observing Zustand store changes
- Behavior: Identical observation pattern

## Contract: Error Handling

### Invalid State Handling

- If flag is set but context is missing: Clear flag and show error
- If navigation fails: Clear flag and show error
- If state is corrupted: Reset navigation store to default state

## Contract: Testing

### Unit Tests

- Test flag setting and clearing
- Test flag persistence across navigation
- Test state observation hooks
- Test error handling

### Integration Tests

- Test template selection flow end-to-end
- Test view judgments flow end-to-end
- Test template creation flow end-to-end

