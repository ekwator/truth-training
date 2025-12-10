# Contract: Emoji Mapping Utility

**Feature**: Android UI Emoji Accessibility Implementation  
**User Story**: User Story 2 (Priority: P1)  
**Status**: Draft

## Preconditions

- Android app codebase exists with Jetpack Compose UI
- Desktop UI emoji mapping exists at `ui/desktop/src/utils/emojiMapping.ts`

## Contract

### Input

- Category name: One of "screens", "actions", "fields", "status", "navigation"
- Key name: Key within the category (e.g., "dashboard", "save", "name")

### Output

- Emoji string: Unicode emoji character matching Desktop implementation
- Empty string: If category or key not found (graceful degradation)

### Behavior

1. **Lookup Function**:
   - Function signature: `fun getEmoji(category: String, key: String): String`
   - O(1) constant time lookup
   - Returns emoji string if category and key exist
   - Returns empty string ("") if category or key not found

2. **Structure Matching**:
   - Mapping structure MUST match Desktop `emojiMapping.ts` exactly
   - Categories MUST match: screens, actions, fields, status, navigation
   - Key names MUST match Desktop key names exactly (case-sensitive)
   - Emoji values MUST match Desktop emoji values exactly

3. **Category Coverage**:
   - Screens category: dashboard, newEvent, contextEditor, events, judgments, overallSummary, trainingResults, settings
   - Actions category: save, cancel, delete, edit, create, submit, refresh, sync, back, next
   - Fields category: name, description, category, forma, cause, develop, effect, startDate, endDate, assessment, confidence, reasoning
   - Status category: online, offline, syncing, error, success, warning
   - Navigation category: home, events, judgments, templates, summary, training, settings

### Test Cases

**TC-001**: Valid category and key returns correct emoji
- Input: category = "screens", key = "dashboard"
- Expected: "🏠"
- Matches Desktop: Yes

**TC-002**: Valid category and key returns correct emoji (actions)
- Input: category = "actions", key = "save"
- Expected: "💾"
- Matches Desktop: Yes

**TC-003**: Invalid category returns empty string
- Input: category = "invalid", key = "dashboard"
- Expected: ""
- Graceful degradation: Yes

**TC-004**: Invalid key returns empty string
- Input: category = "screens", key = "invalid"
- Expected: ""
- Graceful degradation: Yes

**TC-005**: All Desktop emojis present and matching
- Verify: All emoji values match Desktop `emojiMapping.ts` exactly
- Verification: Automated comparison test

## Success Criteria

- **SC-005**: Android emoji mapping utility structure achieves 100% parity with Desktop `emojiMapping.ts` structure (categories, key names, emoji values match exactly).

