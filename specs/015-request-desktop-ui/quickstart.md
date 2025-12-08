# Quickstart: Desktop UI Synchronization

**Feature**: 015-request-desktop-ui  
**Date**: 2025-01-XX  
**Status**: Design Complete

## Overview

This quickstart guide provides integration scenarios and examples for synchronizing Desktop UI with Android implementation. It covers common use cases, code examples, and integration patterns.

## Prerequisites

- Desktop UI codebase (`ui/desktop/`)
- Android UI specification (`docs/ANDROID_UI_SPECIFICATION.md`)
- Tauri backend setup
- React/TypeScript frontend setup
- Zustand state management

## Integration Scenarios

### Scenario 1: Template Selection Flow

**Goal**: Implement template selection for event creation matching Android behavior.

**Steps**:

1. **Add Navigation Store**:
```typescript
// src/stores/navigation.ts
import { create } from 'zustand';

interface NavigationState {
  selectTemplateForEvent: boolean;
  viewJudgments: boolean;
  setSelectTemplateForEvent: (value: boolean) => void;
  setViewJudgments: (value: boolean) => void;
}

export const useNavigationStore = create<NavigationState>((set) => ({
  selectTemplateForEvent: false,
  viewJudgments: false,
  setSelectTemplateForEvent: (value) => set({ selectTemplateForEvent: value }),
  setViewJudgments: (value) => set({ viewJudgments: value }),
}));
```

2. **Add Template Context Store**:
```typescript
// src/stores/templateContext.ts
import { create } from 'zustand';

interface TemplateContext {
  categoryId: number | null;
  formaId: number | null;
  causeId: number | null;
  developId: number | null;
  effectId: number | null;
}

interface TemplateContextState {
  selectedTemplateContext: TemplateContext | null;
  setSelectedTemplateContext: (context: TemplateContext | null) => void;
}

export const useTemplateContextStore = create<TemplateContextState>((set) => ({
  selectedTemplateContext: null,
  setSelectedTemplateContext: (context) => set({ selectedTemplateContext: context }),
}));
```

3. **Update NewEvent Screen**:
```typescript
// src/pages/NewEvent.tsx
import { useNavigationStore } from '@/stores/navigation';
import { useTemplateContextStore } from '@/stores/templateContext';

export const NewEvent: React.FC = () => {
  const { setSelectTemplateForEvent } = useNavigationStore();
  const { selectedTemplateContext, setSelectedTemplateContext } = useTemplateContextStore();
  
  const handleSelectTemplate = () => {
    setSelectTemplateForEvent(true);
    // Navigate to Context Templates screen
    setCurrentScreen('context-editor');
  };
  
  // Use selectedTemplateContext to pre-fill form fields
  useEffect(() => {
    if (selectedTemplateContext) {
      setCategoryId(selectedTemplateContext.categoryId);
      setFormaId(selectedTemplateContext.formaId);
      setCauseId(selectedTemplateContext.causeId);
      setDevelopId(selectedTemplateContext.developId);
      setEffectId(selectedTemplateContext.effectId);
      // Clear template context after use
      setSelectedTemplateContext(null);
    }
  }, [selectedTemplateContext]);
  
  // ... rest of component
};
```

4. **Update ContextEditor Screen**:
```typescript
// src/pages/ContextEditor.tsx
import { useNavigationStore } from '@/stores/navigation';
import { useTemplateContextStore } from '@/stores/templateContext';

export const ContextEditor: React.FC = () => {
  const { selectTemplateForEvent, setSelectTemplateForEvent } = useNavigationStore();
  const { setSelectedTemplateContext } = useTemplateContextStore();
  
  const handleTemplateClick = (template: ContextTemplate) => {
    if (selectTemplateForEvent) {
      // Store template context for event
      setSelectedTemplateContext({
        categoryId: template.categoryId,
        formaId: template.formaId,
        causeId: template.causeId,
        developId: template.developId,
        effectId: template.effectId,
      });
      // Clear flag
      setSelectTemplateForEvent(false);
      // Navigate back to New Event
      setCurrentScreen('new-event');
    } else {
      // Normal flow: pre-fill new template
      // ... existing logic
    }
  };
  
  // ... rest of component
};
```

### Scenario 2: Context Field Display

**Goal**: Display entity names instead of IDs for context fields.

**Steps**:

1. **Create Entity Name Resolution Helper**:
```typescript
// src/utils/entityResolution.ts
export function getEntityNameById<T>(
  id: number | null,
  entities: T[],
  getId: (entity: T) => number,
  getName: (entity: T) => string
): string | null {
  if (id === null) return null;
  const entity = entities.find(e => getId(e) === id);
  return entity ? getName(entity) : null;
}
```

2. **Update EventDetail Screen**:
```typescript
// src/pages/EventDetail.tsx
import { getEntityNameById } from '@/utils/entityResolution';

export const EventDetail: React.FC<{ eventId: number }> = ({ eventId }) => {
  const [categories, setCategories] = useState<Category[]>([]);
  const [formas, setFormas] = useState<Forma[]>([]);
  // ... other entity lists
  
  // Load knowledge base entities
  useEffect(() => {
    const loadEntities = async () => {
      const [cats, forms, causes, develops, effects] = await Promise.all([
        invoke('knowledge_base_list', { entityType: 'category' }),
        invoke('knowledge_base_list', { entityType: 'forma' }),
        invoke('knowledge_base_list', { entityType: 'cause' }),
        invoke('knowledge_base_list', { entityType: 'develop' }),
        invoke('knowledge_base_list', { entityType: 'effect' }),
      ]);
      setCategories(cats);
      setFormas(forms);
      // ... set other entities
    };
    loadEntities();
  }, []);
  
  // Resolve entity names
  const categoryDisplay = useMemo(() => {
    if (!event.categoryId) return null;
    const name = getEntityNameById(
      event.categoryId,
      categories,
      (c) => c.id,
      (c) => c.name
    );
    return name || event.categoryId.toString();
  }, [event.categoryId, categories]);
  
  // ... similar for other context fields
  
  return (
    <div>
      {categoryDisplay && (
        <AssistChip label={`Category: ${categoryDisplay}`} />
      )}
      {/* ... other context fields */}
    </div>
  );
};
```

### Scenario 3: DatePickerField Component

**Goal**: Create DatePickerField component matching Android behavior.

**Steps**:

1. **Create DatePickerField Component**:
```typescript
// src/components/DatePickerField.tsx
import { useState } from 'react';
import { format } from 'date-fns';

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

export const DatePickerField: React.FC<DatePickerFieldProps> = ({
  label,
  value,
  onChange,
  minDate,
  maxDate,
  required,
  allowClear,
  onDateCleared,
  error,
  disabled,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  
  const normalizeToStartOfDay = (date: Date): Date => {
    const normalized = new Date(date);
    normalized.setHours(0, 0, 0, 0);
    return normalized;
  };
  
  const handleDateChange = (newDate: Date | null) => {
    if (newDate) {
      const normalized = normalizeToStartOfDay(newDate);
      
      // Validate minDate
      if (minDate) {
        const normalizedMin = normalizeToStartOfDay(minDate);
        if (normalized < normalizedMin) {
          // Show error
          return;
        }
      }
      
      // Validate maxDate
      if (maxDate) {
        const normalizedMax = normalizeToStartOfDay(maxDate);
        if (normalized > normalizedMax) {
          // Show error
          return;
        }
      }
      
      onChange(normalized);
    } else {
      onChange(null);
    }
  };
  
  return (
    <div>
      <label>{label} {required && '*'}</label>
      <input
        type="date"
        value={value ? format(value, 'yyyy-MM-dd') : ''}
        onChange={(e) => {
          const date = e.target.value ? new Date(e.target.value) : null;
          handleDateChange(date);
        }}
        min={minDate ? format(minDate, 'yyyy-MM-dd') : undefined}
        max={maxDate ? format(maxDate, 'yyyy-MM-dd') : undefined}
        disabled={disabled}
      />
      {allowClear && value && (
        <button onClick={() => {
          onChange(null);
          onDateCleared?.();
        }}>
          Clear
        </button>
      )}
      {error && <span className="error">{error}</span>}
    </div>
  );
};
```

2. **Use in NewEvent Screen**:
```typescript
// src/pages/NewEvent.tsx
import { DatePickerField } from '@/components/DatePickerField';

export const NewEvent: React.FC = () => {
  const [startTimestamp, setStartTimestamp] = useState<Date | null>(new Date());
  const [endTimestamp, setEndTimestamp] = useState<Date | null>(null);
  
  return (
    <div>
      <DatePickerField
        label="Start Timestamp"
        value={startTimestamp}
        onChange={setStartTimestamp}
        required
        disabled={false}
      />
      <DatePickerField
        label="End Timestamp"
        value={endTimestamp}
        onChange={setEndTimestamp}
        minDate={startTimestamp}
        allowClear
        onDateCleared={() => setEndTimestamp(null)}
      />
    </div>
  );
};
```

### Scenario 4: Language Change with Database Re-seeding

**Goal**: Implement language change with temporary tables solution.

**Steps**:

1. **Update Tauri Backend**:
```rust
// src-tauri/src/commands/knowledge_base.rs
#[tauri::command]
pub async fn reseed_knowledge_base(
    locale: String,
    force_reseed: bool,
    db: State<'_, Db>,
) -> Result<(), String> {
    let mut conn = db.0.lock();
    
    conn.execute("BEGIN TRANSACTION", [])
        .map_err(|e| format!("Failed to begin transaction: {}", e))?;
    
    // Create temporary tables
    conn.execute(
        "CREATE TABLE temp_truth_events AS SELECT * FROM truth_events",
        [],
    )?;
    conn.execute(
        "CREATE TABLE temp_impact AS SELECT * FROM impact",
        [],
    )?;
    conn.execute(
        "CREATE TABLE temp_progress_metrics AS SELECT * FROM progress_metrics",
        [],
    )?;
    
    // Clear knowledge base
    conn.execute("DELETE FROM context", [])?;
    conn.execute("DELETE FROM impact_type", [])?;
    conn.execute("DELETE FROM effect", [])?;
    conn.execute("DELETE FROM develop", [])?;
    conn.execute("DELETE FROM cause", [])?;
    conn.execute("DELETE FROM forma", [])?;
    conn.execute("DELETE FROM category", [])?;
    
    // Seed knowledge base with new locale
    seed_knowledge_base(&mut conn, &locale)?;
    
    // Restore data
    conn.execute(
        "INSERT INTO truth_events SELECT * FROM temp_truth_events",
        [],
    )?;
    conn.execute(
        "INSERT INTO impact SELECT * FROM temp_impact",
        [],
    )?;
    conn.execute(
        "INSERT INTO progress_metrics SELECT * FROM temp_progress_metrics",
        [],
    )?;
    
    // Drop temporary tables
    conn.execute("DROP TABLE temp_truth_events", [])?;
    conn.execute("DROP TABLE temp_impact", [])?;
    conn.execute("DROP TABLE temp_progress_metrics", [])?;
    
    conn.execute("COMMIT", [])
        .map_err(|e| format!("Failed to commit transaction: {}", e))?;
    
    Ok(())
}
```

2. **Update Settings Screen**:
```typescript
// src/pages/Settings.tsx
import { invoke } from '@tauri-apps/api/core';
import { setLocale } from '@/i18n';

export const Settings: React.FC = () => {
  const handleLanguageChange = async (newLocale: 'en' | 'ru') => {
    try {
      // Save locale to config
      const config = await invoke('get_app_config');
      await invoke('save_app_config', {
        config: { ...config, locale: newLocale },
      });
      
      // Re-seed knowledge base
      await invoke('reseed_knowledge_base', {
        locale: newLocale,
        forceReseed: true,
      });
      
      // Clear context templates
      await invoke('clear_context_templates');
      
      // Update UI locale
      await setLocale(newLocale, false);
      
      // Reload page to apply changes
      window.location.reload();
    } catch (error) {
      console.error('Failed to change language:', error);
      // Show error message
    }
  };
  
  return (
    <div>
      <button onClick={() => handleLanguageChange('en')}>English</button>
      <button onClick={() => handleLanguageChange('ru')}>Russian</button>
    </div>
  );
};
```

### Scenario 5: Validation Rules

**Goal**: Implement validation matching Android rules.

**Steps**:

1. **Create Validation Utilities**:
```typescript
// src/utils/validation.ts
export interface ValidationError {
  field: string;
  message: string;
}

export function validateEvent(event: Partial<Event>): ValidationError[] {
  const errors: ValidationError[] = [];
  
  if (!event.name || event.name.trim() === '') {
    errors.push({ field: 'name', message: 'Name is required' });
  }
  
  if (!event.description || event.description.trim() === '') {
    errors.push({ field: 'description', message: 'Description is required' });
  }
  
  if (event.categoryId === null || event.categoryId === undefined) {
    errors.push({ field: 'categoryId', message: 'Category is required' });
  }
  
  // ... similar for other context fields
  
  if (!event.timestampStart) {
    errors.push({ field: 'timestampStart', message: 'Start timestamp is required' });
  }
  
  if (event.timestampEnd && event.timestampStart) {
    const normalizedStart = normalizeToStartOfDay(new Date(event.timestampStart));
    const normalizedEnd = normalizeToStartOfDay(new Date(event.timestampEnd));
    
    if (normalizedEnd < normalizedStart) {
      errors.push({
        field: 'timestampEnd',
        message: 'End timestamp cannot be less than start timestamp',
      });
    }
  }
  
  return errors;
}
```

2. **Use in NewEvent Screen**:
```typescript
// src/pages/NewEvent.tsx
import { validateEvent } from '@/utils/validation';

export const NewEvent: React.FC = () => {
  const [errors, setErrors] = useState<ValidationError[]>([]);
  
  const handleSave = async () => {
    const eventData = {
      name,
      description,
      categoryId,
      formaId,
      causeId,
      developId,
      effectId,
      timestampStart: startTimestamp?.getTime() || null,
      timestampEnd: endTimestamp?.getTime() || null,
    };
    
    const validationErrors = validateEvent(eventData);
    if (validationErrors.length > 0) {
      setErrors(validationErrors);
      return;
    }
    
    // Save event
    // ...
  };
  
  return (
    <div>
      {/* Form fields */}
      {errors.map((error) => (
        <div key={error.field} className="error">
          {error.message}
        </div>
      ))}
      <button onClick={handleSave} disabled={errors.length > 0}>
        Save
      </button>
    </div>
  );
};
```

## Testing Scenarios

### Test 1: Template Selection Flow

```typescript
// tests/integration/templateSelection.test.tsx
describe('Template Selection Flow', () => {
  it('should pre-fill event form from selected template', async () => {
    // 1. Navigate to New Event screen
    // 2. Click "Select Template" button
    // 3. Navigate to Context Templates screen
    // 4. Select a template
    // 5. Verify navigation back to New Event
    // 6. Verify form fields are pre-filled
  });
});
```

### Test 2: Context Field Display

```typescript
// tests/integration/contextFieldDisplay.test.tsx
describe('Context Field Display', () => {
  it('should display entity names instead of IDs', async () => {
    // 1. Load event with context fields
    // 2. Load knowledge base entities
    // 3. Verify context fields display names
    // 4. Change language
    // 5. Verify context fields update with new names
  });
});
```

### Test 3: Language Change

```typescript
// tests/integration/languageChange.test.tsx
describe('Language Change', () => {
  it('should preserve event data during language change', async () => {
    // 1. Create event with context fields
    // 2. Change language
    // 3. Verify event data is preserved
    // 4. Verify context fields display new names
    // 5. Verify context templates are cleared
  });
});
```

## Common Patterns

### Pattern 1: Reactive Entity Name Resolution

```typescript
const categoryDisplay = useMemo(() => {
  if (!event.categoryId) return null;
  const name = getEntityNameById(
    event.categoryId,
    categories,
    (c) => c.id,
    (c) => c.name
  );
  return name || event.categoryId.toString();
}, [event.categoryId, categories]);
```

### Pattern 2: Date Normalization

```typescript
function normalizeToStartOfDay(date: Date): Date {
  const normalized = new Date(date);
  normalized.setHours(0, 0, 0, 0);
  return normalized;
}
```

### Pattern 3: Flag-Based Navigation

```typescript
const { selectTemplateForEvent, setSelectTemplateForEvent } = useNavigationStore();

if (selectTemplateForEvent) {
  // Handle template selection for event
  setSelectTemplateForEvent(false);
} else {
  // Handle normal flow
}
```

## Troubleshooting

### Issue 1: Context Fields Not Displaying After Language Change

**Solution**: Ensure entity name resolution uses `useMemo` with proper dependencies and knowledge base entities are reloaded after language change.

### Issue 2: Template Selection Not Working

**Solution**: Verify navigation flags are set correctly and template context is stored in Zustand store before navigation.

### Issue 3: Date Validation Failing

**Solution**: Ensure dates are normalized to start of day before comparison.

## References

- [Android UI Specification](../../../docs/ANDROID_UI_SPECIFICATION.md)
- [Component Contracts](contracts/component-contracts.md)
- [API Contracts](contracts/api-contracts.md)
- [Data Model](data-model.md)

---

**Status**: Design complete, ready for implementation

