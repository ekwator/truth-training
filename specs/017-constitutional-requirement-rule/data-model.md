# Data Model: Android UI Emoji Mapping

**Feature**: Android UI Emoji Accessibility Implementation (Constitutional Requirement Rule 8)  
**Date**: 2025-12-10  
**Phase**: 1 - Design

## Entity: Emoji Mapping Utility

### Structure

The emoji mapping utility is a Kotlin object providing centralized emoji access matching Desktop `emojiMapping.ts` structure.

### Categories

#### 1. Screens Category

Maps screen names to emoji characters for screen titles.

| Key | Emoji | Desktop Match | Usage |
|-----|-------|---------------|-------|
| dashboard | 🏠 | ✅ | DashboardScreen title |
| newEvent | ➕ | ✅ | EventCreateScreen title |
| contextEditor | 📝 | ✅ | ContextTemplateEditorScreen title |
| events | 📋 | ✅ | EventListScreen title |
| judgments | ⚖️ | ✅ | JudgmentListScreen title |
| overallSummary | 📊 | ✅ | OverallSummaryScreen title |
| trainingResults | 📈 | ✅ | TrainingResultsScreen title |
| settings | ⚙️ | ✅ | SettingsScreen title |

#### 2. Actions Category

Maps action names to emoji characters for action buttons.

| Key | Emoji | Desktop Match | Usage |
|-----|-------|---------------|-------|
| save | 💾 | ✅ | Save button |
| cancel | ❌ | ✅ | Cancel button |
| delete | 🗑️ | ✅ | Delete button |
| edit | ✏️ | ✅ | Edit button |
| create | ➕ | ✅ | Create button |
| submit | ✅ | ✅ | Submit button |
| refresh | 🔄 | ✅ | Refresh button |
| sync | 🔄 | ✅ | Sync button |
| back | ⬅️ | ✅ | Back navigation button |
| next | ➡️ | ✅ | Next navigation button |

#### 3. Fields Category

Maps form field names to emoji characters for field labels.

| Key | Emoji | Desktop Match | Usage |
|-----|-------|---------------|-------|
| name | 📝 | ✅ | Name field label |
| description | 📄 | ✅ | Description field label |
| category | 🏷️ | ✅ | Category field label |
| forma | 📐 | ✅ | Forma field label |
| cause | 🔍 | ✅ | Cause field label |
| develop | 📈 | ✅ | Develop field label |
| effect | 💥 | ✅ | Effect field label |
| startDate | 📅 | ✅ | Start Date field label |
| endDate | 📅 | ✅ | End Date field label |
| assessment | ⚖️ | ✅ | Assessment field label |
| confidence | 📊 | ✅ | Confidence field label |
| reasoning | 💭 | ✅ | Reasoning field label |

#### 4. Status Category

Maps status indicator names to emoji characters for status displays.

| Key | Emoji | Desktop Match | Usage |
|-----|-------|---------------|-------|
| online | 🟢 | ✅ | Online status indicator |
| offline | 🔴 | ✅ | Offline status indicator |
| syncing | 🔄 | ✅ | Syncing status indicator |
| error | ❌ | ✅ | Error message indicator |
| success | ✅ | ✅ | Success message indicator |
| warning | ⚠️ | ✅ | Warning message indicator |

#### 5. Navigation Category

Maps navigation item names to emoji characters for navigation menus.

| Key | Emoji | Desktop Match | Usage |
|-----|-------|---------------|-------|
| home | 🏠 | ✅ | Home/Dashboard navigation |
| events | 📋 | ✅ | Events navigation |
| judgments | ⚖️ | ✅ | Judgments navigation |
| templates | 📝 | ✅ | Context Templates navigation |
| summary | 📊 | ✅ | Overall Summary navigation |
| training | 📈 | ✅ | Training Results navigation |
| settings | ⚙️ | ✅ | Settings navigation |

### Lookup Function

**Function Signature**:
```kotlin
fun getEmoji(category: String, key: String): String
```

**Parameters**:
- `category`: One of "screens", "actions", "fields", "status", "navigation"
- `key`: Key within the category (e.g., "dashboard", "save", "name")

**Return Value**:
- Emoji string if category and key exist
- Empty string ("") if category or key not found (graceful degradation)

**Behavior**:
- O(1) lookup time (constant time)
- Type-safe access through nested data classes
- Returns empty string for invalid inputs (ensures UI remains functional)

## Entity: Android UI Element

### Structure

Represents any UI element in Android that requires emoji enhancement.

### Types

1. **Screen Title**: Top-level screen heading (e.g., "🏠 Dashboard")
2. **Action Button**: Interactive button (e.g., "💾 Save")
3. **Form Field Label**: Input field label (e.g., "📝 Name")
4. **Status Indicator**: Status display (e.g., "🟢 Online")
5. **Navigation Item**: Navigation menu item (e.g., "🏠 Home")

### Attributes

- **Element Type**: Screen title, button, label, status, navigation
- **Category**: Emoji mapping category (screens, actions, fields, status, navigation)
- **Key**: Emoji mapping key within category
- **Text Content**: Text label accompanying emoji
- **Desktop Equivalent**: Corresponding Desktop UI element (if exists)

### Emoji Assignment Rules

1. **Screen Titles**: Use `screens` category, key matching screen name
2. **Action Buttons**: Use `actions` category, key matching action name
3. **Form Field Labels**: Use `fields` category, key matching field name
4. **Status Indicators**: Use `status` category, key matching status type
5. **Navigation Items**: Use `navigation` category, key matching navigation route

## Validation Rules

### Emoji Mapping Validation

1. **Structure Validation**:
   - All categories must exist: screens, actions, fields, status, navigation
   - All keys within categories must match Desktop `emojiMapping.ts` exactly
   - Emoji values must match Desktop emoji values exactly

2. **Consistency Validation**:
   - Same functionality must use same emoji across all screens
   - Screen titles must use screen category emoji
   - Action buttons must use action category emoji matching button function

3. **Coverage Validation**:
   - 100% of interactive elements must have emojis
   - 100% of form field labels must have emojis
   - 100% of screen titles must have emojis
   - 100% of status indicators must have emojis

### Desktop Parity Validation

1. **Emoji Value Parity**:
   - Android emoji values must match Desktop emoji values exactly
   - No emoji substitution or variation allowed
   - Category structure must match Desktop structure

2. **Key Name Parity**:
   - Android key names must match Desktop key names exactly
   - Category names must match Desktop category names exactly
   - Case-sensitive matching required

## Implementation Constraints

1. **Unicode Support**: Emojis must use Unicode 12.0+ characters (widely supported)
2. **Text Preservation**: Text labels must remain present alongside emojis
3. **Accessibility**: Emojis must not replace semantic content in accessibility labels
4. **Material Design**: Layout and styling remain Material Design 3 compliant
5. **Performance**: Emoji lookup must be O(1) constant time
6. **Graceful Degradation**: UI must remain functional if emoji rendering fails
7. **Language Independence**: Emoji mapping utility is language-independent - same emoji returned regardless of selected language (English/Russian). UI components combine emoji with localized text from Android string resources.

## Example Usage

```kotlin
// Screen title (with localization)
Text("${EmojiMapping.getEmoji("screens", "dashboard")} ${context.getString(R.string.dashboard)}")
// English: "🏠 Dashboard"
// Russian: "🏠 Панель управления"

// Action button (with localization)
Button(onClick = { /* ... */ }) {
    Text("${EmojiMapping.getEmoji("actions", "save")} ${context.getString(R.string.save)}")
}
// English: "💾 Save"
// Russian: "💾 Сохранить"

// Form field label (with localization)
OutlinedTextField(
    label = { Text("${EmojiMapping.getEmoji("fields", "name")} ${context.getString(R.string.name)}") },
    // ...
)
// English: "📝 Name"
// Russian: "📝 Имя"

// Status indicator (with localization)
Text("${EmojiMapping.getEmoji("status", "online")} ${context.getString(R.string.online)}")
// English: "🟢 Online"
// Russian: "🟢 В сети"

// Navigation item (with localization)
NavigationBarItem(
    label = { Text("${EmojiMapping.getEmoji("navigation", "home")} ${context.getString(R.string.home)}") },
    // ...
)
// English: "🏠 Home"
// Russian: "🏠 Главная"
```

