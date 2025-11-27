# Research: Context Fields Embedded in Events

**Feature**: 006-context-fields-embedded  
**Date**: 2025-01-27

## Research Questions

### 1. Field Embedding vs. Foreign Key Lookup

**Question**: What are the performance and simplicity trade-offs between embedding context fields directly in events vs. using context_id FK?

**Decision**: Embed the five context fields (category_id, forma_id, cause_id, develop_id, effect_id) directly into truth_events table.

**Rationale**:
- **Simplicity**: Eliminates FK lookup join when querying events. Direct field access is faster for read operations.
- **Data Integrity**: Still maintains FK constraints to reference tables (category, forma, cause, develop, effect) for validation.
- **Flexibility**: Events can have context values that don't match any template, enabling ad-hoc categorization.
- **Query Performance**: No JOIN required when fetching events with context information. Indexes can be placed directly on embedded fields.

**Alternatives Considered**:
- Keep context_id with JOIN: Maintains normalization but adds complexity and query overhead.
- Hybrid approach (context_id + embedded fields): Redundant and violates single source of truth.

### 2. Template Duplicate Detection

**Question**: How to efficiently detect duplicate context templates when checking exact match of 5 nullable FK fields?

**Decision**: Compare only non-NULL fields for duplicate detection. NULL values are ignored in the comparison.

**Rationale**:
- Matches template matching behavior (consistent UX). If two templates differ only in NULL positions, they are considered distinct.
- SQL query filters out NULL fields in WHERE clause: `SELECT COUNT(*) FROM context WHERE (category_id IS NULL OR category_id = ?) AND (category_id IS NOT NULL OR ? IS NULL) AND ...`
- Alternative: Compare only fields where both sides are non-NULL, allowing templates with partial fields to coexist.
- Rust code performs pre-check before INSERT with NULL-aware comparison logic.
- Index on non-NULL composite key fields improves lookup performance.

**Alternatives Considered**:
- Treat NULL as equivalent (COALESCE approach): Rejected because it would consider templates with different NULL patterns as duplicates, reducing flexibility.
- Fetch all templates and compare in application code: Less efficient, requires loading all data.
- UNIQUE constraint on composite: Too strict (allows only one exact match, but we want to allow creation if user explicitly wants).

### 3. Foreign Key Validation

**Question**: Should the system reject invalid FK references at creation time, or allow orphaned references?

**Decision**: Reject invalid FK references at creation time with clear error messages.

**Rationale**:
- SQLite foreign key constraints (PRAGMA foreign_keys = ON) enforce referential integrity at database level.
- Application-level validation provides better error messages before database constraint violation.
- Rust `rusqlite` returns specific error types for FK violations, enabling structured error handling.
- Prevents data corruption and maintains data consistency.

**Alternatives Considered**:
- Allow orphaned references with warnings: Creates data quality issues and breaks query assumptions.
- Soft validation (log warnings only): Doesn't prevent issues, just hides them.

### 4. UI Template Matching

**Question**: How should the UI match event fields to templates and display context name vs. [Create Template] option?

**Decision**: Backend provides template matching API using non-NULL field comparison (same as duplicate detection). UI displays context name if match found, otherwise shows [Create Template] button.

**Rationale**:
- Consistent behavior: Template matching uses same NULL-aware logic as duplicate detection.
- Backend query compares only non-NULL fields: `SELECT name FROM context WHERE (category_id IS NULL OR category_id = ?) AND (category_id IS NOT NULL OR ? IS NULL) AND ...` for all five fields.
- Efficient single query per event (can be batched for list views).
- UI displays matched template name, or "[Create Template]" button if no match.
- Button opens Context Editor with fields prefilled from event.

**Alternatives Considered**:
- Exact match including NULL positions: Rejected for consistency with duplicate detection behavior.
- Client-side matching: Requires fetching all templates to frontend, inefficient for large datasets.
- Lazy matching (on-demand): Adds latency; better to compute during event fetch.

### 5. Version Bump Strategy

**Question**: How to coordinate v1.0.0 version bump across multi-crate workspace?

**Decision**: Bump all crates (core_lib, truth_core, app) and desktop UI package.json to v1.0.0 simultaneously.

**Rationale**:
- v1.0.0 marks first stable baseline with breaking changes (context_id removal).
- Coordinated version bump ensures consistency and clear migration path.
- CHANGELOG entries in each crate document breaking changes.
- Version registry ([docs/VERSION_REGISTRY.md](../../docs/VERSION_REGISTRY.md)(d[ocs/VERSION_REGISTRY.md](ocs/VERSION_REGISTRY.md) updated with baseline marker.

**Alternatives Considered**:
- Incremental versioning per crate: Creates confusion about compatibility; breaking change affects all layers.
- Pre-release versions (1.0.0-rc): Unnecessary overhead; changes are well-defined.

## Summary

All research questions resolved. Key decisions:
1. Embed five context fields directly in events (no context_id FK).
2. Duplicate detection compares only non-NULL fields (NULL values ignored).
3. Reject invalid FK references at creation time (immediate rejection with error).
4. Template matching compares only non-NULL fields (consistent with duplicate detection).
5. Coordinated v1.0.0 version bump across all crates.

