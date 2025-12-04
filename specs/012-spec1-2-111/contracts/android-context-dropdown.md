# Contract: Android Context Dropdowns

**Feature**: Android context dropdowns match Desktop UX  
**User Story**: User Story 3 (Priority: P2)  
**Status**: Draft

## Preconditions

- Android app is launched and `TruthDatabase` is initialized
- Contexts are seeded in embedded Room database during initialization
- `ContextTemplateRepository` is available and can load contexts

## Contract

### Input
- User opens `EventCreateScreen` to create a new event
- Context fields (`category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`) need to be populated

### Output
- Context fields render as dropdown/combo components instead of numeric inputs
- Dropdowns are populated with human-readable labels from embedded Room database
- Invalid context IDs are blocked with inline error states
- Submission is prevented if any context ID is not in the lookup list

### Behavior

1. **Dropdown UI**:
   - `EventCreateScreen` MUST render dropdown/combo components for context fields (not `OutlinedTextField` with numeric input)
   - Dropdowns MUST display human-readable labels (e.g., context `name` field)
   - Dropdowns MUST allow selection from list of available contexts
   - Dropdowns MUST be populated from `ContextTemplateRepository.getAllTemplatesFlow()` or `listTemplates()`

2. **Data Loading**:
   - Contexts MUST be loaded from embedded Room database (not API)
   - Loading MUST happen during screen initialization (via ViewModel or Composable state)
   - If context data is unavailable (database not initialized, migration failed), screen MUST show error state and allow retry

3. **Validation**:
   - Before submission, validation MUST check that all provided context IDs exist in lookup tables
   - Invalid IDs MUST block submission with inline error states
   - Error messages MUST explain which field has invalid ID and why submission is blocked

4. **Manual Entry (Optional)**:
   - If manual entry is allowed, validation MUST cross-check typed ID against lookup list
   - Invalid manual entries MUST be highlighted and blocked

5. **Telemetry (Optional)**:
   - If telemetry infrastructure exists, log `context_picker.validation.failure` events
   - Otherwise, use local logging (Logcat) for observability

## Success Criteria

- **SC-003**: 100% of Android event submissions use context IDs sourced from validated dropdown lists; attempted invalid submissions are prevented with inline error states.

## Test Cases

### TC-001: Dropdown Population
1. Open `EventCreateScreen`
2. Observe context fields
3. **Expected**: Dropdowns are displayed with human-readable labels, populated from embedded database

### TC-002: Invalid ID Validation
1. Open `EventCreateScreen`
2. Manually enter invalid context ID (e.g., `99999` for `category_id`)
3. Attempt to submit event
4. **Expected**: Field is highlighted, submission is blocked, error message explains mismatch

### TC-003: Valid Submission
1. Open `EventCreateScreen`
2. Select valid contexts from dropdowns
3. Fill required fields (description, timestamp)
4. Submit event
5. **Expected**: Event is created successfully with valid context IDs

### TC-004: Context Data Unavailable
1. Simulate database initialization failure
2. Open `EventCreateScreen`
3. **Expected**: Error state is shown, retry option is available, submission is disabled

## Observability

- Log context dropdown load: `android.context.dropdown.load.success`, `android.context.dropdown.load.failure`
- Log validation failures: `android.context.validation.failure` (if telemetry exists) or Logcat
- Log submission attempts: `android.event.submit.attempt`, `android.event.submit.success`, `android.event.submit.failure`

## References

- `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventCreateScreen.kt`
- `truth-android-client/app/src/main/java/com/truth/training/client/data/repository/ContextTemplateRepository.kt`
- `truth-android-client/app/src/main/java/com/truth/training/client/data/database/daos/ContextTemplateDao.kt`
- `ui/desktop/src/pages/NewEvent.tsx` (Desktop reference for UX parity)

