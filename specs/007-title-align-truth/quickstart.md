# Quickstart: Android Client v1.0.0 Testing

**Feature**: Align Truth Training Android Client with Desktop v1.0.0 Features  
**Branch**: 007-title-align-truth  
**Date**: 2025-11-02

---

## Prerequisites

1. **Android Development Environment**:
   - Android Studio (Giraffe+)
   - JDK 17
   - Android SDK 24+ (target SDK 35)
   - Gradle 8.0+

2. **Server Setup**:
   - Truth Training Core/Server v1.0.0 running
   - Accessible at `http://localhost:8080` (local) or configured server URL
   - JWT authentication enabled

3. **Build Configuration**:
   - Product flavor: `local` (for local server) or `remote` (for production)
   - Build variant: `localDebug` or `remoteDebug`

---

## Test Scenarios

### Scenario 1: Event Creation with Context Template

**Objective**: Verify event creation with embedded context fields and template selection.

**Steps**:
1. Launch Android app
2. Authenticate with valid credentials
3. Navigate to "New Event" screen
4. Select a context template from dropdown
5. Verify context fields (category_id, forma_id, cause_id, develop_id, effect_id) are prefilled
6. Modify prefilled fields (optional)
7. Fill in event title, description, dates
8. Save event
9. Verify event appears in event list with matching template name

**Expected Results**:
- ✅ Template selection prefills context fields
- ✅ Event created with embedded context fields (not context_id)
- ✅ Event saved to Room database locally
- ✅ Event synced to server (if online)
- ✅ Event appears in Desktop UI with identical data

**API Calls**:
- `GET /api/v1/contexts` - Load templates
- `POST /api/v1/events` - Create event with embedded fields

---

### Scenario 2: Context Template Creation and Duplicate Detection

**Objective**: Verify context template creation and duplicate prevention.

**Steps**:
1. Navigate to "Context Editor" screen
2. Fill in template form (name, optional context fields, description)
3. Save template
4. Verify template appears in template list
5. Attempt to create another template with identical non-NULL fields
6. Verify duplicate error (409 Conflict) is shown

**Expected Results**:
- ✅ Template created successfully
- ✅ Template saved to Room database
- ✅ Template synced to server
- ✅ Duplicate detection prevents creation of identical templates
- ✅ Error message clearly indicates conflict

**API Calls**:
- `POST /api/v1/contexts` - Create template
- `POST /api/v1/contexts` - Attempt duplicate (should return 409)

---

### Scenario 3: Judgment Submission and Consensus

**Objective**: Verify judgment submission and consensus calculation.

**Steps**:
1. Navigate to event details screen
2. Submit a judgment (assessment: true/false/uncertain, confidence_level: 0.0-1.0, reasoning)
3. Verify judgment appears in judgment list
4. Submit additional judgments from same/different users
5. View judgment statistics (true_count, false_count, uncertain_count, avg_confidence)
6. Verify consensus information is displayed

**Expected Results**:
- ✅ Judgment saved to Room database
- ✅ Judgment synced to server
- ✅ Statistics calculated correctly
- ✅ Consensus matches Desktop calculation
- ✅ All judgments appear in both Android and Desktop

**API Calls**:
- `POST /api/v1/judgments` - Submit judgment
- `GET /api/v1/judgments?event_id={id}` - List judgments
- `GET /api/v1/judgments/stats/{event_id}` - Get statistics

---

### Scenario 4: Offline-First Operation

**Objective**: Verify offline-first architecture with local queue and background sync.

**Steps**:
1. Ensure app is online and synced
2. Disable network connectivity (airplane mode or disconnect Wi-Fi)
3. Create a new event offline
4. Submit a judgment offline
5. Create a context template offline
6. Verify all operations are saved locally (Room database)
7. Verify sync queue shows pending operations
8. Re-enable network connectivity
9. Verify background sync processes queue automatically
10. Verify data appears on Desktop after sync

**Expected Results**:
- ✅ All operations saved locally when offline
- ✅ Sync queue tracks pending operations
- ✅ Sync status indicator shows offline state and pending count
- ✅ Background sync processes queue when online
- ✅ No data loss during sync
- ✅ Conflict resolution: local-wins strategy applied

**API Calls** (after sync):
- `POST /api/v1/events` - Sync event
- `POST /api/v1/judgments` - Sync judgment
- `POST /api/v1/contexts` - Sync template

---

### Scenario 5: Template Matching

**Objective**: Verify template matching functionality.

**Steps**:
1. Create an event with specific context fields (category_id=1, forma_id=2, etc.)
2. Navigate to event list
3. Verify event shows matching template name (if template exists with matching fields)
4. If no matching template, verify "[Create Template]" button appears
5. Click "[Create Template]" button
6. Verify Context Editor opens with event's fields prefilled
7. Save template
8. Verify template matches original event's fields

**Expected Results**:
- ✅ Template matching identifies existing templates
- ✅ Template name displayed for matching events
- ✅ "[Create Template]" option available for unmatched events
- ✅ Template creation from event prefills correctly

**API Calls**:
- `POST /api/v1/contexts/match` - Match event fields to template
- `POST /api/v1/contexts/from-event` - Create template from event

---

### Scenario 6: Cross-Platform Data Consistency

**Objective**: Verify data created on Desktop appears in Android and vice versa.

**Steps**:
1. Create an event on Desktop UI
2. Wait for sync (or trigger manual sync)
3. Open Android app
4. Verify event appears in Android event list
5. Submit a judgment on Android
6. Open Desktop UI
7. Verify judgment appears in Desktop
8. Modify event on Desktop
9. Verify modification appears in Android

**Expected Results**:
- ✅ Events created on Desktop appear in Android
- ✅ Judgments created on Android appear in Desktop
- ✅ Event modifications sync bidirectionally
- ✅ All data structures match (embedded fields, timestamps, etc.)

**API Calls**:
- `GET /api/v1/events` - Sync events from server
- `GET /api/v1/judgments?event_id={id}` - Sync judgments
- `PUT /api/v1/events/{id}` - Update event

---

### Scenario 7: Performance and Response Times

**Objective**: Verify mobile-optimized performance targets.

**Steps**:
1. Measure UI response time for navigation (< 200ms)
2. Measure data loading time for event list (< 500ms first paint)
3. Test pagination performance (35 items per page)
4. Test search/filter performance
5. Monitor battery usage during background sync
6. Test app behavior during low memory conditions

**Expected Results**:
- ✅ UI response < 200ms for user actions
- ✅ Initial screen load < 500ms
- ✅ Pagination smooth and responsive
- ✅ Background sync doesn't block UI
- ✅ Battery usage acceptable

---

### Scenario 8: Error Handling and Validation

**Objective**: Verify error handling and input validation.

**Steps**:
1. Attempt to create event without title (required field)
2. Attempt to submit judgment with confidence_level > 1.0
3. Attempt to create template with duplicate fields (409 Conflict)
4. Attempt to sync when server is unreachable
5. Test with invalid JWT token
6. Test with expired JWT token

**Expected Results**:
- ✅ Validation errors shown clearly
- ✅ Network errors handled gracefully
- ✅ Authentication errors trigger re-login flow
- ✅ Sync failures queued for retry
- ✅ User-friendly error messages

---

## Validation Checklist

After completing all scenarios, verify:

- [ ] All events use embedded context fields (category_id, forma_id, etc.) instead of context_id
- [ ] Context templates can be created, matched, and selected
- [ ] Judgments can be submitted with correct validation
- [ ] Offline operations are queued and synced when online
- [ ] Data created on Android appears identically on Desktop
- [ ] Data created on Desktop appears identically on Android
- [ ] Performance targets met (UI < 200ms, load < 500ms)
- [ ] Error handling works correctly
- [ ] Sync status indicator shows accurate state
- [ ] Room database schema matches Desktop SQLite schema
- [ ] All API endpoints match Desktop v1.0.0 contracts

---

## Troubleshooting

### Issue: Events not syncing
- Check sync queue status
- Verify network connectivity
- Check JWT token validity
- Review server logs for errors

### Issue: Template matching not working
- Verify context fields are non-NULL
- Check duplicate detection logic
- Verify template exists in database

### Issue: Performance issues
- Check Room database indices
- Monitor background sync frequency
- Review query optimization

### Issue: Data inconsistency between platforms
- Verify API version compatibility (must be v1.0.0)
- Check embedded fields vs context_id mismatch
- Review sync conflict resolution logic

---

## Next Steps

After quickstart validation:
1. Run full test suite (unit, integration, UI tests)
2. Perform load testing
3. Test on multiple Android devices/versions
4. Validate CI/CD pipeline updates
5. Update documentation (Truth-training.md, ANDROID_MIGRATION.md)

