# Component Contracts: Desktop UI Synchronization

**Feature**: 015-request-desktop-ui  
**Date**: 2025-01-XX  
**Status**: Design Complete

## Overview

This document defines the contracts for UI components that must be synchronized with Android implementation. Components must match Android behavior exactly while preserving Desktop-specific functional features.

## ContextPicker Component

### Contract

**File**: `ui/desktop/src/components/context/ContextPicker.tsx`

**Purpose**: Reusable component for selecting knowledge base entities (Category, Forma, Cause, Develop, Effect).

### Props Interface

```typescript
interface ContextPickerProps {
  label: string;
  value: number | null;
  options: Array<{ id: number; name: string }>;
  onChange: (value: number | null) => void;
  error?: string;
  required?: boolean;
  disabled?: boolean;
}
```

### Behavior Requirements

1. **Display**: Shows human-readable entity names (not IDs)
2. **Search**: Searchable combobox (Desktop) matching Android ExposedDropdownMenuBox behavior
3. **Validation**: Validates selected ID exists in options list
4. **Error State**: Displays error message if validation fails
5. **Required Field**: Shows required indicator if `required = true`
6. **Disabled State**: Prevents interaction if `disabled = true`

### Validation Rules

- Selected value must exist in options list
- If `required = true`, value cannot be null
- Error message displayed if validation fails

### Android Parity

- **Visual**: Matches Android ExposedDropdownMenuBox appearance
- **Behavior**: Same validation and error handling
- **Interaction**: Same user interaction patterns

## DatePickerField Component

### Contract

**File**: `ui/desktop/src/components/DatePickerField.tsx` (NEW)

**Purpose**: Reusable component for date selection with normalization and validation.

### Props Interface

```typescript
interface DatePickerFieldProps {
  label: string;
  value: Date | null;
  onChange: (value: Date | null) => void;
  minDate?: Date | null;
  maxDate?: Date | null;
  required?: boolean;
  allowClear?: boolean;
  onDateCleared?: () => void;
  error?: string;
  disabled?: boolean;
}
```

### Behavior Requirements

1. **Display**: Shows formatted date according to locale
2. **Picker**: Opens date picker on click
3. **Normalization**: Normalizes dates to start of day (00:00:00) for comparison
4. **Validation**: Validates date range (End >= Start, can be equal)
5. **Clear**: Supports clearing if `allowClear = true`
6. **Error State**: Displays error message if validation fails
7. **Required Field**: Shows required indicator if `required = true`
8. **Disabled State**: Prevents interaction if `disabled = true`

### Date Normalization Algorithm

```typescript
function normalizeToStartOfDay(date: Date): Date {
  const normalized = new Date(date);
  normalized.setHours(0, 0, 0, 0);
  return normalized;
}
```

### Validation Rules

- If `required = true`, value cannot be null
- If `minDate` provided, value must be >= minDate (normalized)
- If `maxDate` provided, value must be <= maxDate (normalized)
- Error message displayed if validation fails

### Android Parity

- **Visual**: Matches Android Material Date Picker appearance
- **Behavior**: Same normalization and validation rules
- **Interaction**: Same user interaction patterns

## Template Selection UI

### Contract

**File**: `ui/desktop/src/pages/NewEvent.tsx` (UPDATE)

**Purpose**: Template selection flow matching Android implementation.

### Navigation Flow

1. User clicks "Select Template" button
2. Flag `selectTemplateForEvent = true` set in Zustand navigation store
3. Navigate to Context Templates screen
4. User selects template
5. Template context stored in Zustand template context store
6. Navigate back to New Event screen
7. Form fields updated from template context

### State Management

**Navigation Store**:
```typescript
interface NavigationState {
  selectTemplateForEvent: boolean;
  setSelectTemplateForEvent: (value: boolean) => void;
}
```

**Template Context Store**:
```typescript
interface TemplateContextState {
  selectedTemplateContext: {
    categoryId: number | null;
    formaId: number | null;
    causeId: number | null;
    developId: number | null;
    effectId: number | null;
  } | null;
  setSelectedTemplateContext: (context: TemplateContext | null) => void;
}
```

### Android Parity

- **Flow**: Same navigation pattern with flags
- **State**: Similar state management (Zustand vs StateFlow)
- **Pre-filling**: Same form field pre-filling behavior

## Context Field Display

### Contract

**File**: Multiple screens (EventDetail, EventEdit, ContextTemplateList, etc.)

**Purpose**: Display human-readable entity names for context field IDs.

### Display Algorithm

```typescript
function getEntityNameById<T>(
  id: number | null,
  entities: T[],
  getId: (entity: T) => number,
  getName: (entity: T) => string
): string | null {
  if (id === null) return null;
  const entity = entities.find(e => getId(e) === id);
  return entity ? getName(entity) : null;
}

// Usage
const categoryDisplay = useMemo(() => {
  if (!event.categoryId) return null;
  const name = getEntityNameById(
    event.categoryId,
    categories,
    (c) => c.id,
    (c) => c.name
  );
  return name || event.categoryId.toString(); // Fallback to ID
}, [event.categoryId, categories]);
```

### Display Format

**Event Detail Screen**:
- FlowRow with AssistChip components
- Format: "Category: Social", "Forma: Truth", etc.
- Fallback to ID if name not found

**Event Edit Screen**:
- Same display format as Event Detail
- Read-only display (not editable)

**Context Template List**:
- Same display format as Event Detail
- Shows context fields for each template

### Reactive Updates

- Uses `useMemo` with dependencies: [fieldId, entities]
- Updates automatically when knowledge base changes
- Ensures immediate update after language change and re-seeding

### Android Parity

- **Algorithm**: Same entity name resolution logic
- **Display**: Same FlowRow with AssistChip format
- **Reactivity**: Same reactive update behavior

## Event Edit Screen Fields

### Contract

**File**: `ui/desktop/src/pages/EventEdit.tsx` (UPDATE)

**Purpose**: Edit event with read-only fields and editable flags/timestamps.

### Field States

**Read-Only Fields**:
- Name (display only)
- Description (display only)
- Context Fields (display only, using context field display algorithm)
- Start Timestamp (display only, if already set)
- Vector (display only)

**Editable Fields**:
- Flags: detected, corrected (auto-calculated)
- End Timestamp (always editable, if Start Timestamp exists)

### Corrected Flag Algorithm

```typescript
// Track initial End Timestamp
const initialEndTimestamp = useRef(event.timestampEnd);

// Auto-set Corrected when End Timestamp changes
useEffect(() => {
  if (event.timestampEnd === null) {
    // If End Timestamp was null, don't set Corrected
    return;
  }
  
  if (endTimestamp !== initialEndTimestamp.current) {
    // End Timestamp changed, auto-set Corrected
    setCorrected(true);
  }
}, [endTimestamp, event.timestampEnd]);
```

### Android Parity

- **Read-Only**: Same fields read-only
- **Editable**: Same fields editable
- **Corrected Flag**: Same auto-calculation algorithm

## Validation Contracts

### Event Validation

**Contract**: All event forms must validate according to Android rules.

**Rules**:
1. Name: required, non-empty string
2. Description: required, non-empty string
3. All context fields: required, non-null
4. Start Timestamp: required, defaults to current date
5. End Timestamp: optional, but if provided:
   - Must be >= Start Timestamp (normalized)
   - Can be equal to Start Timestamp

**Error Display**:
- Inline error messages per field
- Disabled save button when validation fails
- Clear error states on field change

### Template Validation

**Contract**: All template forms must validate according to Android rules.

**Rules**:
1. Name: required, non-empty string
2. All context fields: required, non-null
3. Duplicate detection: compare non-NULL fields only

**Error Display**:
- Inline error messages per field
- Duplicate error: 409 Conflict message
- Disabled save button when validation fails

## Localization Contracts

### String Resource Keys

**Contract**: All UI strings must use i18n keys matching Android structure.

**Key Structure**: `{screen}.{element}` or `{category}.{element}`

**Examples**:
- `dashboard.title` → "Dashboard"
- `events.createEvent` → "Create Event"
- `contextFields.title` → "Context Fields"
- `validation.fieldRequired` → "Required field"

### Locale Application

**Contract**: Locale must be applied at application startup and on change.

**Implementation**:
1. Read locale from config file
2. Apply locale to document.documentElement.lang
3. Update all UI strings via i18n function
4. Trigger knowledge base re-seeding if locale changed

## Navigation Contracts

### Flag-Based Routing

**Contract**: Navigation must support flag-based conditional routing.

**Flags**:
- `selectTemplateForEvent`: true when selecting template for event
- `viewJudgments`: true when viewing judgments for events

**Implementation**:
- Flags stored in Zustand navigation store
- Navigation logic checks flags before routing
- Flags persist across navigation until explicitly cleared

### Route Structure

**Contract**: Navigation must support all Android routes.

**Routes**:
- `/dashboard` → Dashboard
- `/events` → Event List
- `/event/:id` → Event Detail
- `/event/:id/edit` → Event Edit
- `/event/create` → New Event
- `/contexts` → Context Templates
- `/context/create` → New Template
- `/judgments/:eventId` → Judgments
- `/judgment/submit/:eventId` → Judgment Submission
- `/summary` → Overall Summary
- `/training` → Training Results
- `/settings` → Settings
- `/nodes` → Nodes

## Performance Contracts

### Response Times

**Contract**: UI interactions must respond within performance targets.

**Targets**:
- Form field updates: <100ms
- Navigation transitions: <200ms
- Language switching: <5 seconds (including database re-seeding)
- Context field display: <50ms (with cached entity names)

### Data Loading

**Contract**: Data must load efficiently with proper loading states.

**Requirements**:
- Show loading indicators during async operations
- Cache knowledge base entities
- Paginate large lists
- Lazy load when possible

## Error Handling Contracts

### User-Facing Errors

**Contract**: Errors must be displayed clearly and actionably.

**Display Methods**:
- Inline error messages for form fields
- Toast notifications for general errors
- Error cards for critical errors
- Retry options where applicable

### Error States

**Contract**: All components must handle error states gracefully.

**States**:
- Loading: show spinner/loading indicator
- Empty: show message with action button
- Error: show error message with retry option
- Success: show success message (if applicable)

## Testing Contracts

### Component Testing

**Contract**: All components must have unit tests.

**Coverage**:
- Props validation
- User interactions
- Error states
- Edge cases

### Integration Testing

**Contract**: Navigation flows must have integration tests.

**Coverage**:
- Template selection flow
- View judgments flow
- Language change flow
- Form validation flows

## References

- [Android UI Specification](../../../docs/ANDROID_UI_SPECIFICATION.md)
- [Component Specifications](../../../docs/ANDROID_UI_SPECIFICATION.md#component-specifications)

---

**Status**: Design complete, ready for implementation

