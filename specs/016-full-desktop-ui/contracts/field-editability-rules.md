# Contract: Field Editability Rules

**Feature**: 016-full-desktop-ui  
**User Story**: Field display and validation rules alignment  
**Status**: Draft

## Preconditions

- Desktop UI is running
- User is authenticated (if required)
- Event or Template data is available

## Contract

### Input

- User opens View Event, Edit Event, Create Event, or Context Templates screen
- Event or Template data is loaded

### Output

- Fields displayed according to editability rules
- Editable fields allow user input
- Read-only fields display values but prevent editing
- Validation rules enforced on save

### Behavior

#### View Event Screen

1. **Field Display**:
   - All fields displayed as read-only
   - Context fields displayed with resolved entity names
   - No inline editing capability
   - Edit action available (navigates to Edit Event screen)

2. **Validation**:
   - No validation required (read-only display)

#### Edit Event Screen

1. **Editable Fields**:
   - `detected: boolean` - Can be toggled
   - `timestamp_end: number | null` - Can be modified, defaults to current date if empty

2. **Read-only Fields**:
   - `description: string` - Displayed but not editable
   - `timestamp_start: number` - Displayed but not editable
   - Context fields (category, forma, cause, develop, effect) - Hidden or disabled (NOT editable)

3. **Validation**:
   - `timestamp_end >= timestamp_start` (if both provided)
   - Save operation validates only editable fields

#### Create Event Screen

1. **Editable Fields**:
   - All fields editable (description, timestamps, context fields, vector, detected)

2. **Validation**:
   - `description` required
   - `timestamp_end >= timestamp_start` (if both provided)
   - Context field IDs must exist in lookup tables

#### Context Templates Screen

1. **Template Selection Flow**:
   - Clicking template in list navigates to form
   - All fields prefilled from selected template
   - User can modify prefilled values
   - Save creates new template (not edit of existing)

2. **Validation**:
   - `name` required
   - All context fields required (cannot be NULL)
   - Duplicate detection prevents save if identical non-NULL fields exist

## Error Handling

- **409 Conflict**: Duplicate template detected - display error message, prevent save
- **Validation Errors**: Display inline error messages for invalid fields
- **API Errors**: Display user-friendly error messages

## References

- Android implementation: `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventEditScreen.kt`
- Android implementation: `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/contexts/ContextTemplateEditorScreen.kt`

