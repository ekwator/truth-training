# Contract: Template Duplicate Detection

**Feature**: 016-full-desktop-ui  
**User Story**: Field display and validation rules alignment  
**Status**: Draft

## Preconditions

- Desktop UI is running
- Context Templates screen is open
- User is creating or editing a template
- Template has at least one non-NULL context field

## Contract

### Input

- User fills template form with context fields
- User clicks Save button
- Template data: `{ name, description, category_id, forma_id, cause_id, develop_id, effect_id }`

### Output

- Duplicate check performed before save
- If duplicate found: 409 Conflict error returned, save prevented
- If no duplicate: Template saved successfully

### Behavior

1. **Duplicate Detection Algorithm**:
   - Check if template has any non-NULL context fields
   - If yes, query for existing templates with identical non-NULL fields
   - NULL values are ignored in comparison
   - Comparison logic:
     - `category_id`: Compare only if both non-NULL
     - `forma_id`: Compare only if both non-NULL
     - `cause_id`: Compare only if both non-NULL
     - `develop_id`: Compare only if both non-NULL
     - `effect_id`: Compare only if both non-NULL
   - If all non-NULL fields match existing template, duplicate detected

2. **Error Response**:
   - Status: 409 Conflict
   - Message: "Template with identical fields already exists (409 Conflict)"
   - Save operation prevented
   - Error message displayed to user

3. **Success Response**:
   - Status: 200 OK or 201 Created
   - Template saved successfully
   - Form cleared or navigated back to list

## SQL Logic Reference (Android)

```sql
SELECT COUNT(*) FROM context_templates 
WHERE (category_id IS NULL OR category_id = ?) 
  AND (category_id IS NOT NULL OR ? IS NULL)
  AND (forma_id IS NULL OR forma_id = ?) 
  AND (forma_id IS NOT NULL OR ? IS NULL)
  AND (cause_id IS NULL OR cause_id = ?) 
  AND (cause_id IS NOT NULL OR ? IS NULL)
  AND (develop_id IS NULL OR develop_id = ?) 
  AND (develop_id IS NOT NULL OR ? IS NULL)
  AND (effect_id IS NULL OR effect_id = ?) 
  AND (effect_id IS NOT NULL OR ? IS NULL)
  AND id != ?
```

## Test Cases

### Test Case 1: Duplicate Detection - All Fields Match

**Input**:
- Template 1: `{ category_id: 1, forma_id: 2, cause_id: 3, develop_id: 4, effect_id: 5 }`
- Template 2: `{ category_id: 1, forma_id: 2, cause_id: 3, develop_id: 4, effect_id: 5 }`

**Expected**: 409 Conflict error, save prevented

### Test Case 2: Duplicate Detection - NULL Values Ignored

**Input**:
- Template 1: `{ category_id: 1, forma_id: 2, cause_id: null, develop_id: 4, effect_id: 5 }`
- Template 2: `{ category_id: 1, forma_id: 2, cause_id: 3, develop_id: 4, effect_id: 5 }`

**Expected**: No duplicate (different cause_id values, one is NULL)

### Test Case 3: No Duplicate - Different Fields

**Input**:
- Template 1: `{ category_id: 1, forma_id: 2, cause_id: 3, develop_id: 4, effect_id: 5 }`
- Template 2: `{ category_id: 1, forma_id: 2, cause_id: 3, develop_id: 4, effect_id: 6 }`

**Expected**: No duplicate, save succeeds

## References

- Android implementation: `truth-android-client/app/src/main/java/com/truth/training/client/data/repository/ContextTemplateRepository.kt`
- Android implementation: `truth-android-client/app/src/main/java/com/truth/training/client/data/database/daos/ContextTemplateDao.kt`

