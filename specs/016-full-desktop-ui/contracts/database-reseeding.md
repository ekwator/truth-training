# Database Reseeding Contract

**Feature**: Full Desktop UI Reconstruction and Synchronization  
**Date**: 2025-12-09  
**Type**: Database Operation Contract

## Overview

This contract defines the safe database reseeding API using temporary tables, matching Android's approach for knowledge base updates.

## Contract: Safe Reseeding API

### Tauri Command Interface

```rust
#[command]
pub async fn reseed_knowledge_base(
    db: State<'_, crate::storage::Db>
) -> Result<ReseedResult, String> {
    // Implementation in knowledge_base.rs
}
```

### TypeScript Interface

```typescript
interface ReseedResult {
  success: boolean;
  message: string;
  tablesUpdated: string[];
}

async function reseedKnowledgeBase(): Promise<ReseedResult> {
  return await invoke<ReseedResult>('reseed_knowledge_base');
}
```

## Contract: Reseeding Algorithm

### Step 1: Create Temporary Tables

```sql
CREATE TABLE temp_category (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT
);

CREATE TABLE temp_forma (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    quality INTEGER NOT NULL,
    description TEXT
);

CREATE TABLE temp_cause (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    quality INTEGER NOT NULL,
    description TEXT
);

CREATE TABLE temp_develop (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    quality INTEGER NOT NULL,
    description TEXT
);

CREATE TABLE temp_effect (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    quality INTEGER NOT NULL,
    description TEXT
);

CREATE TABLE temp_context (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    category_id INTEGER,
    forma_id INTEGER,
    cause_id INTEGER,
    develop_id INTEGER,
    effect_id INTEGER,
    description TEXT,
    FOREIGN KEY(category_id) REFERENCES temp_category(id),
    FOREIGN KEY(forma_id) REFERENCES temp_forma(id),
    FOREIGN KEY(cause_id) REFERENCES temp_cause(id),
    FOREIGN KEY(develop_id) REFERENCES temp_develop(id),
    FOREIGN KEY(effect_id) REFERENCES temp_effect(id)
);
```

### Step 2: Fill Temporary Tables

```rust
// Insert English-only data into temp tables
insert_into_temp_category(&conn, english_categories)?;
insert_into_temp_forma(&conn, english_formas)?;
insert_into_temp_cause(&conn, english_causes)?;
insert_into_temp_develop(&conn, english_develops)?;
insert_into_temp_effect(&conn, english_effects)?;
insert_into_temp_context(&conn, english_contexts)?;
```

### Step 3: Atomic Swap

```sql
BEGIN TRANSACTION;

-- Backup main tables
ALTER TABLE category RENAME TO old_category;
ALTER TABLE forma RENAME TO old_forma;
ALTER TABLE cause RENAME TO old_cause;
ALTER TABLE develop RENAME TO old_develop;
ALTER TABLE effect RENAME TO old_effect;
ALTER TABLE context RENAME TO old_context;

-- Swap temp tables to main
ALTER TABLE temp_category RENAME TO category;
ALTER TABLE temp_forma RENAME TO forma;
ALTER TABLE temp_cause RENAME TO cause;
ALTER TABLE temp_develop RENAME TO develop;
ALTER TABLE temp_effect RENAME TO effect;
ALTER TABLE temp_context RENAME TO context;

-- Drop backup tables
DROP TABLE old_category;
DROP TABLE old_forma;
DROP TABLE old_cause;
DROP TABLE old_develop;
DROP TABLE old_effect;
DROP TABLE old_context;

COMMIT;
```

### Step 4: Cleanup and UI Refresh

```rust
// After successful swap
emit_ui_refresh_event()?;
Ok(ReseedResult {
    success: true,
    message: "Knowledge base reseeded successfully",
    tablesUpdated: vec![
        "category".to_string(),
        "forma".to_string(),
        "cause".to_string(),
        "develop".to_string(),
        "effect".to_string(),
        "context".to_string(),
    ],
})
```

## Contract: Error Handling

### Transaction Rollback

```rust
// If any step fails, rollback transaction
if let Err(e) = atomic_swap(&conn) {
    conn.rollback()?;
    // Clean up temp tables
    drop_temp_tables(&conn)?;
    return Err(format!("Reseeding failed: {}", e));
}
```

### Partial Failure Handling

- If temp table creation fails: Return error, no changes
- If data insertion fails: Rollback, drop temp tables, return error
- If atomic swap fails: Rollback, retain temp tables, return error (user can retry)
- If cleanup fails: Log error, but reseeding is successful

## Contract: FK Integrity

### Requirements

1. **Temporary tables maintain FK relationships**: All foreign keys in `temp_context` reference `temp_*` tables
2. **Atomic swap preserves FK integrity**: Main tables maintain FK relationships after swap
3. **No FK violations**: All FK constraints validated before swap

### Validation

```rust
// Validate FK integrity before swap
validate_temp_table_fks(&conn)?;
validate_main_table_fks(&conn)?;
```

## Contract: UI Refresh Event

### Event Emission

```rust
// Emit event to refresh UI
#[tauri::command]
pub async fn emit_knowledge_base_refreshed() -> Result<(), String> {
    // Emit event via Tauri event system
    app_handle.emit("knowledge-base-refreshed", ())?;
    Ok(())
}
```

### Event Handling (TypeScript)

```typescript
import { listen } from '@tauri-apps/api/event';

listen('knowledge-base-refreshed', () => {
  // Reload knowledge base data in all components
  reloadKnowledgeBaseData();
});
```

## Contract: Testing

### Unit Tests

- Test temp table creation
- Test data insertion into temp tables
- Test atomic swap
- Test FK integrity validation
- Test error handling and rollback

### Integration Tests

- Test full reseeding flow end-to-end
- Test UI refresh after reseeding
- Test FK integrity after reseeding
- Test error recovery

## Contract: Performance

### Requirements

- Reseeding completes in < 1 second for typical knowledge base size
- Atomic swap is instantaneous (SQLite RENAME is O(1))
- UI refresh is non-blocking

### Optimization

- Use batch inserts for temp table population
- Use single transaction for atomic swap
- Use event-driven UI refresh (non-blocking)

