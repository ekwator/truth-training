# UX Guidelines

Use /spec as the primary decision source before reading /docs.
Version: v1.0.0
Updated: 2025-01-XX
Spec ID: 09

## UX Guidelines

See [docs/ui_guidelines.md](../docs/ui_guidelines.md). This spec aligns endpoint names with current API.

Principles
- No business logic in UI; use API/FFI.
- Show expert wizard with questions and rationale.
- Visualize progress trends; sync status.

CLI UX (truthctl)
- Subcommands mirror domain objects: `peers add/list`, `sync` (with `--pull-only`).
- Consistent flags and defaults: `--db truth_db.sqlite`, `--peers peers.json`, `--verbose`.
- Human-first output by default; JSON output can be added as a follow-up.
- Avoid destructive actions; confirm before overwriting `peers.json`.
- Align names with REST endpoints where possible; avoid inventing new terms.

## Context Template UX Patterns (v1.0.0)

### Template Selection Pattern

**Use Case**: User wants to create an event with a preconfigured context template.

**Flow**:
1. On NewEvent page, user sees "Context Template" dropdown selector
2. User selects a template (e.g., "Interpersonal Conflict")
3. Five context fields are automatically prefilled (category, forma, cause, develop, effect)
4. User can modify prefilled fields if needed
5. User completes remaining event fields and submits form

**UX Principles**:
- **Non-destructive**: Prefilling does not lock fields; user can modify or clear values
- **Clear feedback**: Selected template name is displayed, and fields show source (e.g., "From template: Interpersonal Conflict")
- **Progressive disclosure**: Template selection is optional; users can manually enter fields
- **Validation feedback**: Invalid FK references show immediate error with field-level feedback

### Create Template from Event Pattern

**Use Case**: User creates an event with custom context fields and wants to save as a reusable template.

**Flow**:
1. User creates event with embedded context fields
2. On Events list, event shows "[Create Template]" button (when no template matches)
3. User clicks button
4. ContextEditor opens with fields prefilled from event
5. User enters template name and optional description
6. System checks for duplicate (non-NULL field comparison)
7. If duplicate exists, shows "Template already exists" error
8. If unique, template is created and event list updates to show template name

**UX Principles**:
- **Opportunistic creation**: Template creation is offered when context is already defined
- **Duplicate prevention**: Clear error message when attempting to create duplicate template
- **Feedback loop**: After creation, event list immediately reflects template name

### Template Matching Display Pattern

**Use Case**: User wants to see which events match existing context templates.

**Flow**:
1. Events list displays template name next to each event (when match found)
2. Matching uses non-NULL field comparison (NULL values ignored)
3. Events without matching templates show "[Create Template]" button
4. Clicking template name opens ContextEditor in view/edit mode

**UX Principles**:
- **Visual consistency**: Template name displayed consistently across event list
- **Scannable**: Template names help users quickly identify event patterns
- **Actionable**: "[Create Template]" button provides clear path to template creation
- **NULL-aware**: Matching logic ignores NULL fields, so partial matches are valid

### Duplicate Detection Feedback Pattern

**Use Case**: User attempts to create a context template that already exists.

**Flow**:
1. User fills ContextEditor form and submits
2. System validates FK references (rejects invalid with 400 error)
3. System checks for duplicate (non-NULL field comparison)
4. If duplicate found, displays error: "Template already exists. A template with identical non-NULL fields already exists."
5. Error highlights which fields matched
6. User can either modify fields or use existing template

**UX Principles**:
- **Clear error message**: Explicitly states what went wrong and why
- **Field-level feedback**: Highlights which fields caused the duplicate
- **Actionable guidance**: Suggests using existing template or modifying fields
- **Non-blocking**: Error does not prevent form editing; user can adjust and retry

- See [docs/README.md](../docs/README.md) for detailed explanations.

- See [spec/README.md](README.md) for detailed explanations.

## Emoji Accessibility Guidelines (Rule 8)

### Overview
All Desktop UI interface elements MUST be accompanied by appropriate emojis to improve understanding of element purpose for users who have difficulty understanding the interface language (constitutional requirement Rule 8).

### Emoji Requirements
1. **Semantic Meaning**: Emojis must be semantically meaningful and directly related to the function or purpose of the interface element (buttons, menu items, navigation links, form labels, status indicators, etc.).
2. **Consistency**: Emoji selection must be consistent across the application and follow established patterns for similar functionality.
3. **Universal Cues**: Emojis provide universal visual cues that transcend language barriers, making the interface more accessible to users regardless of their proficiency in the interface language.

### Emoji Categories
- **Screens**: Dashboard (🏠), New Event (➕), Context Editor (📝), Events (📋), Judgments (⚖️), Overall Summary (📊), Training Results (📈), Settings (⚙️)
- **Actions**: Save (💾), Cancel (❌), Delete (🗑️), Edit (✏️), Create (➕), Submit (✅), Refresh (🔄), Sync (🔄), Back (⬅️), Next (➡️)
- **Form Fields**: Name (📝), Description (📄), Category (🏷️), Forma (📐), Cause (🔍), Develop (📈), Effect (💥), Start Date (📅), End Date (📅), Assessment (⚖️), Confidence (📊), Reasoning (💭)
- **Status Indicators**: Online (🟢), Offline (🔴), Syncing (🔄), Error (❌), Success (✅), Warning (⚠️)
- **Navigation**: Home (🏠), Events (📋), Judgments (⚖️), Templates (📝), Summary (📊), Training (📈), Settings (⚙️)

### Implementation
- Emoji mapping is centralized in `ui/desktop/src/utils/emojiMapping.ts`
- Use `getEmoji(category, key)` function to retrieve emojis consistently
- All UI components should use emojis from the centralized mapping
- Emojis are enhancement, not replacement for text labels

### Graceful Degradation
If emoji rendering fails or is not supported on a platform, text labels must remain clear and functional. Emojis are enhancement, not replacement for text.

## Localization Status

### Desktop UI
- **RU/EN Language Switching**: Supported via `LocaleToggle` component in header and Settings → General
- **Supported Locales**: English (en), Russian (ru)
- **Locale Persistence**: Stored in `AppConfig.locale` and `localStorage` key `truth-locale`
- **Translation Files**: `ui/desktop/src/i18n/ru.ts` contains Russian translations
- **Fallback**: Missing translations fallback to English with console warning

### Android Client
- **Current Status**: **RU/EN Language Switching Supported**
- **Language Switching**: Implemented via Settings screen with language selection UI
- **Translation Files**: `app/src/main/res/values/strings.xml` (English) and `app/src/main/res/values-ru/strings.xml` (Russian)
- **Locale Persistence**: Stored in `AppConfig.locale` and SharedPreferences, persists across app restarts
- **Knowledge Base Re-seeding**: When language changes, context templates are cleared and knowledge base is re-seeded with data in the selected language
- **String Consistency**: Android strings are consistent with Desktop strings for key screens (navigation, event creation, error messages)

### Cross-Platform Parity
- **Current Status**: Both Desktop and Android support RU/EN language switching
- **Parity**: Android localization matches Desktop functionality
- **Documentation**: All quickstarts and UI guidelines document the current localization status
- **Consistency**: Strings are consistent across both platforms for key screens (navigation, event creation, error messages)
