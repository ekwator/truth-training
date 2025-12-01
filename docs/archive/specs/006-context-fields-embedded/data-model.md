<!-- Archived from [specs/006-context-fields-embedded/data-model.md](specs/006-context-fields-embedded/data-model.md) -->

# Data Model: Context Fields Embedded in Events

**Feature**: 006-context-fields-embedded  
**Date**: 2025-01-27

## Entity Changes

### TruthEvent (Modified)

**Table**: `truth_events`

**Changes**:
- **REMOVED**: `context_id` (INTEGER, FK → context.id)
- **ADDED**: `category_id` (INTEGER, FK → category.id, nullable)
- **ADDED**: `forma_id` (INTEGER, FK → forma.id, nullable)
- **ADDED**: `cause_id` (INTEGER, FK → cause.id, nullable)
- **ADDED**: `develop_id` (INTEGER, FK → develop.id, nullable)
- **ADDED**: `effect_id` (INTEGER, FK → effect.id, nullable)

**Fields (Complete)**:
- `id` (INTEGER, PK, AUTOINCREMENT)
- `description` (TEXT, NOT NULL)
- `category_id` (INTEGER, FK → category.id, nullable)
- `forma_id` (INTEGER, FK → forma.id, nullable)
- `cause_id` (INTEGER, FK → cause.id, nullable)
- `develop_id` (INTEGER, FK → develop.id, nullable)
- `effect_id` (INTEGER, FK → effect.id, nullable)
- `vector` (INTEGER, NOT NULL) — event direction (0/1)
- `detected` (INTEGER, nullable) — truth/lie detection (NULL/0/1)
- `corrected` (INTEGER, NOT NULL, DEFAULT 0) — correction indicator
- `timestamp_start` (INTEGER, NOT NULL) — UNIX timestamp
- `timestamp_end` (INTEGER, nullable) — UNIX timestamp
- `code` (INTEGER, NOT NULL, DEFAULT 1) — event classification code
- `collective_score` (REAL, nullable) — collective truth score (0-1)

**Rust Struct**:
```rust
pub struct TruthEvent {
    pub id: i64,
    pub description: String,
    pub category_id: Option<i64>,
    pub forma_id: Option<i64>,
    pub cause_id: Option<i64>,
    pub develop_id: Option<i64>,
    pub effect_id: Option<i64>,
    pub vector: bool,
    pub detected: Option<bool>,
    pub corrected: bool,
    pub timestamp_start: i64,
    pub timestamp_end: Option<i64>,
    pub code: u8,
    pub collective_score: Option<f64>,
}
```

**Rust Struct for Creation**:
```rust
pub struct NewTruthEvent {
    pub description: String,
    pub category_id: Option<i64>,
    pub forma_id: Option<i64>,
    pub cause_id: Option<i64>,
    pub develop_id: Option<i64>,
    pub effect_id: Option<i64>,
    pub vector: bool,
    pub timestamp_start: i64,
    pub code: u8,
}
```

### Context Template (Unchanged Structure, Enhanced Usage)

**Table**: `context`

**Fields**: (no schema changes, but usage changes)
- `id` (INTEGER, PK)
- `name` (TEXT, NOT NULL) — template name
- `category_id` (INTEGER, FK → category.id, nullable)
- `forma_id` (INTEGER, FK → forma.id, nullable)
- `cause_id` (INTEGER, FK → cause.id, nullable)
- `develop_id` (INTEGER, FK → develop.id, nullable)
- `effect_id` (INTEGER, FK → effect.id, nullable)
- `description` (TEXT, nullable) — template description

**Rust Struct**: (unchanged)
```rust
pub struct Context {
    pub id: i64,
    pub name: String,
    pub category_id: Option<i64>,
    pub forma_id: Option<i64>,
    pub cause_id: Option<i64>,
    pub develop_id: Option<i64>,
    pub effect_id: Option<i64>,
    pub description: Option<String>,
}
```

**New Struct for Creation**:
```rust
pub struct NewContext {
    pub name: String,
    pub category_id: Option<i64>,
    pub forma_id: Option<i64>,
    pub cause_id: Option<i64>,
    pub develop_id: Option<i64>,
    pub effect_id: Option<i64>,
    pub description: Option<String>,
}
```

## Validation Rules

### Event Creation
1. `description` MUST NOT be empty (trimmed length > 0).
2. Foreign key references (category_id, forma_id, cause_id, develop_id, effect_id) MUST reference existing records OR be NULL.
3. If FK reference is provided (non-NULL), validation query MUST confirm existence before INSERT. Invalid FK references MUST be rejected immediately with an error message.
4. All five context fields are optional (nullable) to allow events without complete context classification.

### Context Template Creation
1. `name` MUST NOT be empty (trimmed length > 0).
2. `name` MUST be unique (enforced by application, not DB constraint to allow user flexibility).
3. Duplicate detection: Template with identical non-NULL field combination MUST be detected before creation.
4. Duplicate detection compares only non-NULL fields: NULL values are ignored in the comparison. If all non-NULL field values match an existing template, it is considered a duplicate.
5. Foreign key references MUST be validated (same as event creation). Invalid FKs MUST be rejected immediately with error message.

### Template Matching
1. Matching algorithm: Compare only non-NULL fields (consistent with duplicate detection).
2. Query compares non-NULL fields only: For each of the five fields, match if both sides are NULL OR both sides have the same non-NULL value. NULL values are ignored.
3. If match found: Return template name for display.
4. If no match: Return NULL (UI shows "[Create Template]" option).

## Relationships

### TruthEvent → Reference Tables (FK)
- `category_id` → `category.id` (optional)
- `forma_id` → `forma.id` (optional)
- `cause_id` → `cause.id` (optional)
- `develop_id` → `develop.id` (optional)
- `effect_id` → `effect.id` (optional)

### Context → Reference Tables (FK)
- `category_id` → `category.id` (optional)
- `forma_id` → `forma.id` (optional)
- `cause_id` → `cause.id` (optional)
- `develop_id` → `develop.id` (optional)
- `effect_id` → `effect.id` (optional)

**Note**: Context templates and events now share the same reference structure, enabling template-based event field prefilling.

## Migration Considerations

**No Automatic Migrations**: Manual data migration required.

**Migration Strategy** (manual):
1. For existing events with `context_id`:
   - Lookup context template by `context_id`.
   - Extract five field values from context.
   - Update event: SET category_id, forma_id, cause_id, develop_id, effect_id FROM context lookup.
   - Remove context_id column (ALTER TABLE DROP COLUMN, or recreate table).
2. Events without valid context_id: Set all five fields to NULL.

**Backward Compatibility**:
- Existing code expecting `context_id` will break (breaking change).
- API endpoints must be updated simultaneously (no phased rollout).
- Version bump to v1.0.0 marks this breaking change.

## Indexes

**Recommended Indexes**:
- `CREATE INDEX idx_truth_events_category ON truth_events(category_id)`
- `CREATE INDEX idx_truth_events_forma ON truth_events(forma_id)`
- `CREATE INDEX idx_context_fields ON context(category_id, forma_id, cause_id, develop_id, effect_id)` — for duplicate detection and matching


