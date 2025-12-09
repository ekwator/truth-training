# Data Model: Full Desktop UI Reconstruction and Synchronization

**Feature**: Full Desktop UI Reconstruction and Synchronization  
**Date**: 2025-12-09  
**Phase**: 1 - Design & Architecture

## Overview

This document defines the data models required for Desktop UI reconstruction and synchronization with Android UI. The models cover navigation state management, emoji mapping, temporary table structures, and component state synchronization.

## Core Data Models

### 1. Navigation State Model

**Purpose**: Manage flag-based navigation state equivalent to Android's `savedStateHandle`.

**Location**: `ui/desktop/src/stores/navigation.ts`

**TypeScript Interface**:
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

**Key Attributes**:
- `selectTemplateForEvent`: Boolean flag indicating template selection mode for event creation
- `selectedTemplateContext`: Context fields from selected template (category, forma, cause, develop, effect)
- `viewJudgments`: Boolean flag indicating judgments view mode
- `selectedEventIdForJudgments`: Event ID for judgments view
- `selectedTemplateForEdit`: Template data for editing

**Relationships**:
- Used by: NewEvent screen, ContextEditor screen, Events screen, Judgments screen
- Persists: Across navigation until explicitly cleared
- Equivalent: Android's `savedStateHandle` in Navigation Compose

### 2. Emoji Mapping Model

**Purpose**: Map UI elements to appropriate emojis for accessibility (constitutional requirement Rule 8).

**Location**: `ui/desktop/src/utils/emojiMapping.ts`

**TypeScript Interface**:
```typescript
interface EmojiMapping {
  // Screens
  screens: {
    dashboard: string;
    newEvent: string;
    contextEditor: string;
    events: string;
    judgments: string;
    overallSummary: string;
    trainingResults: string;
    settings: string;
  };

  // Actions
  actions: {
    save: string;
    cancel: string;
    delete: string;
    edit: string;
    create: string;
    submit: string;
    refresh: string;
    sync: string;
    back: string;
    next: string;
  };

  // Form Fields
  fields: {
    name: string;
    description: string;
    category: string;
    forma: string;
    cause: string;
    develop: string;
    effect: string;
    startDate: string;
    endDate: string;
    assessment: string;
    confidence: string;
    reasoning: string;
  };

  // Status Indicators
  status: {
    online: string;
    offline: string;
    syncing: string;
    error: string;
    success: string;
    warning: string;
  };

  // Navigation
  navigation: {
    home: string;
    events: string;
    judgments: string;
    templates: string;
    summary: string;
    training: string;
    settings: string;
  };
}

// Default emoji mapping
const defaultEmojiMapping: EmojiMapping = {
  screens: {
    dashboard: '🏠',
    newEvent: '➕',
    contextEditor: '📝',
    events: '📋',
    judgments: '⚖️',
    overallSummary: '📊',
    trainingResults: '📈',
    settings: '⚙️',
  },
  actions: {
    save: '💾',
    cancel: '❌',
    delete: '🗑️',
    edit: '✏️',
    create: '➕',
    submit: '✅',
    refresh: '🔄',
    sync: '🔄',
    back: '⬅️',
    next: '➡️',
  },
  fields: {
    name: '📝',
    description: '📄',
    category: '🏷️',
    forma: '📐',
    cause: '🔍',
    develop: '📈',
    effect: '💥',
    startDate: '📅',
    endDate: '📅',
    assessment: '⚖️',
    confidence: '📊',
    reasoning: '💭',
  },
  status: {
    online: '🟢',
    offline: '🔴',
    syncing: '🔄',
    error: '❌',
    success: '✅',
    warning: '⚠️',
  },
  navigation: {
    home: '🏠',
    events: '📋',
    judgments: '⚖️',
    templates: '📝',
    summary: '📊',
    training: '📈',
    settings: '⚙️',
  },
};
```

**Key Attributes**:
- `screens`: Emojis for screen titles and navigation
- `actions`: Emojis for action buttons
- `fields`: Emojis for form field labels
- `status`: Emojis for status indicators
- `navigation`: Emojis for navigation items

**Relationships**:
- Used by: All UI components (buttons, labels, navigation items, status indicators)
- Consistency: Same emoji for same function across application
- Requirement: Constitutional requirement Rule 8

### 3. Temporary Table Model

**Purpose**: Structure for temporary tables used in safe database reseeding.

**Location**: `ui/desktop/src-tauri/src/commands/knowledge_base.rs`

**Rust Struct** (conceptual):
```rust
// Temporary table names
const TEMP_CATEGORY: &str = "temp_category";
const TEMP_FORMA: &str = "temp_forma";
const TEMP_CAUSE: &str = "temp_cause";
const TEMP_DEVELOP: &str = "temp_develop";
const TEMP_EFFECT: &str = "temp_effect";
const TEMP_CONTEXT: &str = "temp_context";

// Temporary table schema (matches main tables)
struct TempCategory {
    id: i64,
    name: String,
    description: Option<String>,
}

struct TempForma {
    id: i64,
    name: String,
    quality: i64,
    description: Option<String>,
}

struct TempCause {
    id: i64,
    name: String,
    quality: i64,
    description: Option<String>,
}

struct TempDevelop {
    id: i64,
    name: String,
    quality: i64,
    description: Option<String>,
}

struct TempEffect {
    id: i64,
    name: String,
    quality: i64,
    description: Option<String>,
}

struct TempContext {
    id: i64,
    name: String,
    category_id: Option<i64>,
    forma_id: Option<i64>,
    cause_id: Option<i64>,
    develop_id: Option<i64>,
    effect_id: Option<i64>,
    description: Option<String>,
}
```

**Key Attributes**:
- Table names: `temp_` prefix for all temporary tables
- Schema: Matches main table schema exactly
- Data: English-only data for knowledge base reseeding
- FK relationships: Maintained in temporary tables

**Relationships**:
- Created: Before reseeding process
- Filled: With English-only data
- Swapped: Atomically with main tables
- Dropped: After successful swap

### 4. Component State Model

**Purpose**: Synchronize component state between Desktop and Android.

**Location**: Component-specific stores in `ui/desktop/src/stores/`

**TypeScript Interfaces**:

#### Event Form State
```typescript
interface EventFormState {
  name: string;
  description: string;
  categoryId: number | null;
  formaId: number | null;
  causeId: number | null;
  developId: number | null;
  effectId: number | null;
  timestampStart: number; // Required, defaults to current date
  timestampEnd: number | null; // Optional
  vector: number; // 0 = Outgoing, 1 = Incoming
  detected: boolean | null;
  corrected: boolean; // Auto-calculated
  errors: {
    name?: string;
    description?: string;
    categoryId?: string;
    formaId?: string;
    causeId?: string;
    developId?: string;
    effectId?: string;
    timestampEnd?: string;
  };
}
```

#### Template Form State
```typescript
interface TemplateFormState {
  name: string;
  description: string;
  categoryId: number | null;
  formaId: number | null;
  causeId: number | null;
  developId: number | null;
  effectId: number | null;
  errors: {
    name?: string;
    categoryId?: string;
    formaId?: string;
    causeId?: string;
    developId?: string;
    effectId?: string;
    duplicate?: string;
  };
}
```

#### Judgment Form State
```typescript
interface JudgmentFormState {
  assessment: 'true' | 'false' | 'uncertain';
  confidenceLevel: number; // 0.0 - 1.0
  reasoning: string;
  errors: {
    assessment?: string;
    confidenceLevel?: string;
  };
}
```

**Key Attributes**:
- Form fields: Match Android form fields exactly
- Validation errors: Match Android validation error messages
- State management: Zustand stores for reactive updates

**Relationships**:
- Used by: NewEvent screen, ContextEditor screen, Judgments screen
- Synchronized: With Android form state patterns
- Validation: Matches Android validation rules exactly

### 5. Date Normalization Model

**Purpose**: Normalize timestamps to start of day for validation (matches Android algorithm).

**Location**: `ui/desktop/src/utils/dateNormalization.ts`

**TypeScript Functions**:
```typescript
/**
 * Normalize timestamp to start of day (00:00:00)
 * Matches Android algorithm exactly
 */
function normalizeToStartOfDay(timestamp: number): number {
  const date = new Date(timestamp);
  date.setHours(0, 0, 0, 0);
  return date.getTime();
}

/**
 * Validate date range (End >= Start after normalization)
 * Matches Android validation exactly
 */
function validateDateRange(
  startTimestamp: number,
  endTimestamp: number | null
): { valid: boolean; error?: string } {
  const normalizedStart = normalizeToStartOfDay(startTimestamp);
  const normalizedEnd = endTimestamp
    ? normalizeToStartOfDay(endTimestamp)
    : null;

  if (normalizedEnd !== null && normalizedEnd < normalizedStart) {
    return {
      valid: false,
      error: 'End Timestamp cannot be less than Start Timestamp',
    };
  }

  return { valid: true };
}
```

**Key Attributes**:
- Normalization: Timestamps normalized to start of day (00:00:00)
- Validation: End timestamp cannot be less than start timestamp (after normalization)
- Algorithm: Matches Android algorithm exactly

**Relationships**:
- Used by: NewEvent screen, EventEdit screen
- Synchronized: With Android date normalization algorithm
- Validation: Matches Android validation rules

### 6. Entity Resolution Model

**Purpose**: Resolve context field IDs to human-readable names (matches Android algorithm).

**Location**: `ui/desktop/src/utils/entityResolution.ts`

**TypeScript Interface**:
```typescript
interface EntityResolution {
  resolveCategoryName(id: number): string | null;
  resolveFormaName(id: number): string | null;
  resolveCauseName(id: number): string | null;
  resolveDevelopName(id: number): string | null;
  resolveEffectName(id: number): string | null;
  resolveContextField(
    fieldType: 'category' | 'forma' | 'cause' | 'develop' | 'effect',
    id: number
  ): string;
}

/**
 * Resolve context field ID to name, fallback to ID if not found
 * Matches Android algorithm exactly
 */
function resolveContextFieldName(
  fieldType: 'category' | 'forma' | 'cause' | 'develop' | 'effect',
  id: number,
  entities: Array<{ id: number; name: string }>
): string {
  const entity = entities.find((e) => e.id === id);
  return entity ? entity.name : `${id}`;
}
```

**Key Attributes**:
- Resolution: Context field IDs resolved to names from knowledge base
- Fallback: ID displayed if name not found (matches Android)
- Reactive: Updates when knowledge base changes (after reseeding)

**Relationships**:
- Used by: EventDetail screen, EventCard component, TemplateCard component
- Data source: Knowledge base entities (category, forma, cause, develop, effect)
- Synchronized: With Android entity resolution algorithm

## Database Schema

### Knowledge Base Tables (for Reseeding)

**Tables**:
- `category`: id (PK), name, description
- `forma`: id (PK), name, quality, description
- `cause`: id (PK), name, quality, description
- `develop`: id (PK), name, quality, description
- `effect`: id (PK), name, quality, description
- `context`: id (PK), name, category_id (FK), forma_id (FK), cause_id (FK), develop_id (FK), effect_id (FK), description

**Temporary Tables** (during reseeding):
- `temp_category`: Same schema as `category`
- `temp_forma`: Same schema as `forma`
- `temp_cause`: Same schema as `cause`
- `temp_develop`: Same schema as `develop`
- `temp_effect`: Same schema as `effect`
- `temp_context`: Same schema as `context`

**Reseeding Process**:
1. Create temporary tables with `temp_` prefix
2. Fill temporary tables with English-only data
3. Atomically swap temporary tables with main tables
4. Drop temporary tables
5. Refresh UI with updated data

## Data Flow

### Navigation Flag Flow

```
User Action → Navigation Store (Zustand) → Screen Component → useEffect Hook → ViewModel Update → UI Update
```

**Example: Template Selection Flow**:
1. User clicks "Select Template" button in NewEvent screen
2. `setSelectTemplateForEvent(true)` called in navigation store
3. Navigation to ContextEditor screen
4. ContextEditor observes `selectTemplateForEvent` flag
5. User selects template
6. `setSelectedTemplateContext({...})` called in navigation store
7. Navigation back to NewEvent screen
8. NewEvent observes `selectedTemplateContext` in useEffect
9. Form fields updated with template context
10. `clearTemplateSelection()` called after use

### Database Reseeding Flow

```
Reseed Request → Create Temp Tables → Fill Temp Tables → Atomic Swap → Drop Temp Tables → UI Refresh Event → UI Update
```

**Example: Knowledge Base Reseeding**:
1. User triggers reseed (or automatic on app init)
2. Create `temp_category`, `temp_forma`, etc.
3. Insert English-only data into temp tables
4. Begin transaction
5. Rename main tables to `old_*`
6. Rename temp tables to main table names (atomic swap)
7. Drop `old_*` tables
8. Commit transaction
9. Emit UI refresh event
10. UI components reload knowledge base data

## Validation Rules

### Event Form Validation
- **Name**: Required, cannot be empty
- **Description**: Required, cannot be empty
- **All Context Fields**: Required, cannot be NULL
- **Start Timestamp**: Required, defaults to current date, cannot be cleared
- **End Timestamp**: Optional, but if provided, cannot be less than Start Timestamp (after normalization)

### Template Form Validation
- **Name**: Required, cannot be empty
- **All Context Fields**: Required, cannot be NULL
- **Duplicate Detection**: Templates with identical non-NULL context fields cannot be created

### Judgment Form Validation
- **Assessment**: Required, must be "true", "false", or "uncertain"
- **Confidence Level**: Required, must be between 0.0 and 1.0

**Note**: All validation rules match Android validation rules exactly.

