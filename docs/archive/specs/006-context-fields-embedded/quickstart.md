<!-- Archived from [specs/006-context-fields-embedded/quickstart.md](specs/006-context-fields-embedded/quickstart.md) -->

# Quickstart: Context Fields Embedded in Events

**Feature**: 006-context-fields-embedded  
**Date**: 2025-01-27

## Prerequisites

- Database initialized with knowledge base (category, forma, cause, develop, effect tables populated)
- Server running with updated API endpoints
- Desktop UI updated with Context Editor screen

## Quickstart Steps

### 1. Create Event with Embedded Context Fields

**Via API**:
```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Test event with embedded context",
    "category_id": 1,
    "forma_id": 2,
    "cause_id": 5,
    "develop_id": 3,
    "effect_id": 2,
    "vector": true
  }'
```

**Expected Response**:
```json
{
  "id": 123
}
```

**Verification**: Query database to confirm event has embedded fields (no context_id):
```sql
SELECT id, description, category_id, forma_id, cause_id, develop_id, effect_id 
FROM truth_events WHERE id = 123;
```

### 2. Create Context Template

**Via API**:
```bash
curl -X POST http://localhost:8080/contexts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Interpersonal Openness",
    "category_id": 1,
    "forma_id": 2,
    "cause_id": 5,
    "develop_id": 3,
    "effect_id": 2,
    "description": "Honest dialogue template"
  }'
```

**Expected Response**:
```json
{
  "id": 10,
  "name": "Interpersonal Openness",
  "category_id": 1,
  "forma_id": 2,
  "cause_id": 5,
  "develop_id": 3,
  "effect_id": 2,
  "description": "Honest dialogue template"
}
```

### 3. Select Template When Creating Event (UI)

**Steps**:
1. Open Desktop UI → "New Event" page
2. Select "Interpersonal Openness" from context template dropdown
3. Verify five context fields auto-populate: category_id=1, forma_id=2, cause_id=5, develop_id=3, effect_id=2
4. Enter event description
5. Submit event

**Verification**: Event created with embedded fields matching template values.

### 4. Match Event to Template

**Via API**:
```bash
curl -X POST http://localhost:8080/contexts/match \
  -H "Content-Type: application/json" \
  -d '{
    "category_id": 1,
    "forma_id": 2,
    "cause_id": 5,
    "develop_id": 3,
    "effect_id": 2
  }'
```

**Expected Response** (if match found):
```json
{
  "matched": true,
  "template": {
    "id": 10,
    "name": "Interpersonal Openness",
    ...
  }
}
```

**Expected Response** (if no match):
```json
{
  "matched": false,
  "template": null
}
```

### 5. Display Context Name in Event List (UI)

**Steps**:
1. Open Desktop UI → "Events" page
2. View event list
3. For events matching a template: Display template name (e.g., "Interpersonal Openness")
4. For events without match: Display "[Create Template]" button

**Verification**: Correct display based on template matching.

### 6. Create Template from Event (UI)

**Steps**:
1. Open event in list that doesn't match any template
2. Click "[Create Template]" button
3. Context Editor opens with fields prefilled from event
4. Enter template name: "Custom Template"
5. Modify fields if needed (optional)
6. Submit template

**Expected**: Template created; event now displays "Custom Template" name.

### 7. Duplicate Detection

**Via API**:
```bash
# Try to create duplicate template (same field combination)
curl -X POST http://localhost:8080/contexts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Duplicate Attempt",
    "category_id": 1,
    "forma_id": 2,
    "cause_id": 5,
    "develop_id": 3,
    "effect_id": 2
  }'
```

**Expected Response**: 409 Conflict with error message "Template already exists"

### 8. Foreign Key Validation

**Via API**:
```bash
# Try to create event with invalid category_id
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Invalid FK test",
    "category_id": 99999,  # Non-existent ID
    "vector": true
  }'
```

**Expected Response**: 400 Bad Request with error message about invalid foreign key reference

## Success Criteria

✅ Events can be created with embedded context fields (no context_id)  
✅ Context templates can be created and listed  
✅ Template selection auto-populates event form fields  
✅ Template matching displays context name in event list  
✅ "[Create Template]" option appears for unmatched events  
✅ Duplicate template detection prevents creation  
✅ Foreign key validation rejects invalid references  
✅ All API endpoints return expected response formats  
✅ UI flows complete without errors

## Rollback (if needed)

**Note**: This is a breaking change. Rollback requires:
1. Revert code changes across all crates
2. Restore context_id column in database (manual migration)
3. Migrate embedded fields back to context_id (manual script)

**No automatic rollback path** — manual database restoration required.

_Version: v1.0.0_

