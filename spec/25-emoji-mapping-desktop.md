# Emoji Mapping Specification: Desktop UI

**Version:** v1.0.0  
**Spec ID:** 25  
**Updated:** 2025-12-09  
**Status:** Active

**Purpose:** This document defines the complete emoji mapping for all UI elements in the Desktop UI application. This mapping is used for synchronization with Android client and ensures compliance with constitutional requirement Rule 8 (UI Desktop Emoji Accessibility Requirement).

**Related Documents:**
- [Constitution](../.specify/memory/constitution.md) - Rule 8: UI Desktop Emoji Accessibility Requirement
- [Desktop UI Functional Specification](23-function_desktop.md)
- [UX Guidelines](09-ux-guidelines.md)
- [Android UI Specification](../docs/ANDROID_UI_SPECIFICATION.md)

## Overview

All Desktop UI interface elements MUST be accompanied by appropriate emojis to improve understanding of element purpose for users who have difficulty understanding the interface language. Emojis are semantically meaningful and directly related to element function, providing universal visual cues that transcend language barriers.

## Emoji Mapping Categories

### 1. Screens (Экраны)

| Screen Name | Screen ID | Emoji | Description | Usage Location |
|------------|-----------|-------|-------------|----------------|
| Dashboard | `dashboard` | 🏠 | Home screen with overview | `src/pages/Dashboard.tsx`, TopMenuBar navigation |
| New Event | `newEvent` | ➕ | Create new event screen | `src/pages/NewEvent.tsx`, TopMenuBar navigation |
| Context Editor | `contextEditor` | 📝 | Context template editor screen | `src/pages/ContextEditor.tsx`, TopMenuBar navigation |
| Events | `events` | 📋 | Events list screen | `src/pages/Events.tsx`, TopMenuBar navigation |
| Judgments | `judgments` | ⚖️ | Judgments screen | `src/pages/Judgments.tsx`, TopMenuBar navigation |
| Overall Summary | `overallSummary` | 📊 | Overall summary screen | `src/pages/OverallSummary.tsx`, TopMenuBar navigation |
| Training Results | `trainingResults` | 📈 | Training results screen | `src/pages/TrainingResults.tsx`, TopMenuBar navigation |
| Settings | `settings` | ⚙️ | Settings screen | `src/pages/Settings.tsx`, TopMenuBar navigation |
| Event Summary | `event-summary` | 📋 | Event detail summary (Desktop-specific) | `src/pages/EventSummary.tsx` |

### 2. Navigation Elements (Элементы навигации)

| Element | Navigation ID | Emoji | Description | Usage Location |
|---------|---------------|-------|-------------|----------------|
| Home | `home` | 🏠 | Navigate to dashboard | TopMenuBar, navigation links |
| Events | `events` | 📋 | Navigate to events list | TopMenuBar, navigation links |
| Judgments | `judgments` | ⚖️ | Navigate to judgments | TopMenuBar, navigation links |
| Templates | `templates` | 📝 | Navigate to context templates | TopMenuBar, navigation links |
| Summary | `summary` | 📊 | Navigate to overall summary | TopMenuBar, navigation links |
| Training | `training` | 📈 | Navigate to training results | TopMenuBar, navigation links |
| Settings | `settings` | ⚙️ | Navigate to settings | TopMenuBar, navigation links |

### 3. Actions (Действия)

| Action | Action ID | Emoji | Description | Usage Location |
|--------|-----------|-------|-------------|----------------|
| Save | `save` | 💾 | Save changes | Form submit buttons, save actions |
| Cancel | `cancel` | ❌ | Cancel operation | Cancel buttons, close dialogs |
| Delete | `delete` | 🗑️ | Delete item | Delete buttons, remove actions |
| Edit | `edit` | ✏️ | Edit item | Edit buttons, modify actions |
| Create | `create` | ➕ | Create new item | Create buttons, new item actions |
| Submit | `submit` | ✅ | Submit form | Submit buttons, form submission |
| Refresh | `refresh` | 🔄 | Refresh data | Refresh buttons, reload actions |
| Sync | `sync` | 🔄 | Synchronize data | Sync buttons, synchronization actions |
| Back | `back` | ⬅️ | Navigate back | Back buttons, navigation back |
| Next | `next` | ➡️ | Navigate forward | Next buttons, navigation forward |

### 4. Form Fields (Поля форм)

| Field | Field ID | Emoji | Description | Usage Location |
|-------|----------|-------|-------------|----------------|
| Name | `name` | 📝 | Name input field | NewEvent form, ContextEditor form |
| Description | `description` | 📄 | Description textarea | NewEvent form, ContextEditor form |
| Category | `category` | 🏷️ | Category selector | NewEvent form, ContextEditor form, ContextPicker |
| Forma | `forma` | 📐 | Forma selector | NewEvent form, ContextEditor form, ContextPicker |
| Cause | `cause` | 🔍 | Cause selector | NewEvent form, ContextEditor form, ContextPicker |
| Develop | `develop` | 📈 | Develop selector | NewEvent form, ContextEditor form, ContextPicker |
| Effect | `effect` | 💥 | Effect selector | NewEvent form, ContextEditor form, ContextPicker |
| Start Date | `startDate` | 📅 | Start date picker | NewEvent form, DatePickerField |
| End Date | `endDate` | 📅 | End date picker | NewEvent form, DatePickerField |
| Assessment | `assessment` | ⚖️ | Judgment assessment | Judgment form, judgment submission |
| Confidence | `confidence` | 📊 | Confidence level | Judgment form, judgment submission |
| Reasoning | `reasoning` | 💭 | Reasoning text | Judgment form, judgment submission |

### 5. Status Indicators (Индикаторы статуса)

| Status | Status ID | Emoji | Description | Usage Location |
|--------|-----------|-------|-------------|----------------|
| Online | `online` | 🟢 | Online status | SyncStatus component, connection indicators |
| Offline | `offline` | 🔴 | Offline status | SyncStatus component, connection indicators |
| Syncing | `syncing` | 🔄 | Synchronizing | SyncStatus component, loading states |
| Error | `error` | ❌ | Error state | Error messages, error boundaries |
| Success | `success` | ✅ | Success state | Success messages, completion indicators |
| Warning | `warning` | ⚠️ | Warning state | Warning messages, caution indicators |
| Info | `info` | ℹ️ | Information | Info messages, help text |

## Screen-by-Screen Emoji Mapping

### Dashboard Screen (`src/pages/Dashboard.tsx`)

| UI Element | Element Type | Emoji | Mapping Key |
|------------|--------------|-------|-------------|
| Screen Title | Screen | 🏠 | `screens.dashboard` |
| Create Event Button | Action | ➕ | `actions.create` |
| Total Events Label | Field | 📋 | `screens.events` |
| Detected Events Label | Field | 📋 | `screens.events` |
| Events with Consensus Label | Field | ⚖️ | `screens.judgments` |
| Participants Label | Field | 👥 | (custom) |
| Sync Status Indicator | Status | 🟢/🔴/🔄 | `status.online/offline/syncing` |
| Loading State | Status | 🔄 | `status.syncing` |
| Error State | Status | ❌ | `status.error` |

### New Event Screen (`src/pages/NewEvent.tsx`)

| UI Element | Element Type | Emoji | Mapping Key |
|------------|--------------|-------|-------------|
| Screen Title | Screen | ➕ | `screens.newEvent` |
| Name Field Label | Field | 📝 | `fields.name` |
| Description Field Label | Field | 📄 | `fields.description` |
| Select Template Button | Action | ➕ | `actions.create` |
| Category Field Label | Field | 🏷️ | `fields.category` |
| Forma Field Label | Field | 📐 | `fields.forma` |
| Cause Field Label | Field | 🔍 | `fields.cause` |
| Develop Field Label | Field | 📈 | `fields.develop` |
| Effect Field Label | Field | 💥 | `fields.effect` |
| Start Date Field Label | Field | 📅 | `fields.startDate` |
| End Date Field Label | Field | 📅 | `fields.endDate` |
| Cancel Button | Action | ❌ | `actions.cancel` |
| Save Button | Action | 💾 | `actions.save` |
| Loading State | Status | 🔄 | `status.syncing` |
| Error State | Status | ❌ | `status.error` |
| Success State | Status | ✅ | `status.success` |

### Context Editor Screen (`src/pages/ContextEditor.tsx`)

| UI Element | Element Type | Emoji | Mapping Key |
|------------|--------------|-------|-------------|
| Screen Title | Screen | 📝 | `screens.contextEditor` |
| Name Field Label | Field | 📝 | `fields.name` |
| Description Field Label | Field | 📄 | `fields.description` |
| Category Field Label | Field | 🏷️ | `fields.category` |
| Forma Field Label | Field | 📐 | `fields.forma` |
| Cause Field Label | Field | 🔍 | `fields.cause` |
| Develop Field Label | Field | 📈 | `fields.develop` |
| Effect Field Label | Field | 💥 | `fields.effect` |
| Save Button | Action | 💾 | `actions.save` |
| Cancel Button | Action | ❌ | `actions.cancel` |
| Delete Button | Action | 🗑️ | `actions.delete` |
| Edit Button | Action | ✏️ | `actions.edit` |
| Create Button | Action | ➕ | `actions.create` |
| Loading State | Status | 🔄 | `status.syncing` |
| Error State | Status | ❌ | `status.error` |
| Duplicate Error | Status | ⚠️ | `status.warning` |

### Events Screen (`src/pages/Events.tsx`)

| UI Element | Element Type | Emoji | Mapping Key |
|------------|--------------|-------|-------------|
| Screen Title | Screen | 📋 | `screens.events` |
| New Event Button | Action | ➕ | `actions.create` |
| Event Card | Component | 📋 | `screens.events` |
| View Judgments Link | Navigation | ⚖️ | `navigation.judgments` |
| Loading State | Status | 🔄 | `status.syncing` |
| Empty State | Status | ⚠️ | `status.warning` |
| Error State | Status | ❌ | `status.error` |

### Judgments Screen (`src/pages/Judgments.tsx`)

| UI Element | Element Type | Emoji | Mapping Key |
|------------|--------------|-------|-------------|
| Screen Title | Screen | ⚖️ | `screens.judgments` |
| Assessment Field | Field | ⚖️ | `fields.assessment` |
| Confidence Field | Field | 📊 | `fields.confidence` |
| Reasoning Field | Field | 💭 | `fields.reasoning` |
| Submit Button | Action | ✅ | `actions.submit` |
| Loading State | Status | 🔄 | `status.syncing` |
| Empty State | Status | ⚠️ | `status.warning` |
| Error State | Status | ❌ | `status.error` |

### Overall Summary Screen (`src/pages/OverallSummary.tsx`)

| UI Element | Element Type | Emoji | Mapping Key |
|------------|--------------|-------|-------------|
| Screen Title | Screen | 📊 | `screens.overallSummary` |
| Refresh Button | Action | 🔄 | `actions.refresh` |
| Export Button | Action | 💾 | `actions.save` |
| Loading State | Status | 🔄 | `status.syncing` |
| Error State | Status | ❌ | `status.error` |

### Training Results Screen (`src/pages/TrainingResults.tsx`)

| UI Element | Element Type | Emoji | Mapping Key |
|------------|--------------|-------|-------------|
| Screen Title | Screen | 📈 | `screens.trainingResults` |
| Refresh Button | Action | 🔄 | `actions.refresh` |
| Loading State | Status | 🔄 | `status.syncing` |
| Info Message | Status | ℹ️ | `status.info` |

### Settings Screen (`src/pages/Settings.tsx`)

| UI Element | Element Type | Emoji | Mapping Key |
|------------|--------------|-------|-------------|
| Screen Title | Screen | ⚙️ | `screens.settings` |
| Save Button | Action | 💾 | `actions.save` |
| Cancel Button | Action | ❌ | `actions.cancel` |
| Info Message | Status | ℹ️ | `status.info` |

### Event Summary Screen (`src/pages/EventSummary.tsx`) - Desktop-specific

| UI Element | Element Type | Emoji | Mapping Key |
|------------|--------------|-------|-------------|
| Screen Title | Screen | 📋 | `screens.events` |
| Edit Button | Action | ✏️ | `actions.edit` |
| Loading State | Status | 🔄 | `status.syncing` |
| Error State | Status | ❌ | `status.error` |
| Empty State | Status | ⚠️ | `status.warning` |

## Component-Level Emoji Mapping

### TopMenuBar Component (`src/components/layout/TopMenuBar.tsx`)

| UI Element | Element Type | Emoji | Mapping Key |
|------------|--------------|-------|-------------|
| Dashboard Link | Navigation | 🏠 | `navigation.home` |
| Create Event Link | Navigation | ➕ | `actions.create` |
| Context Editor Link | Navigation | 📝 | `navigation.templates` |
| Overall Summary Link | Navigation | 📊 | `navigation.summary` |
| Training Results Link | Navigation | 📈 | `navigation.training` |
| Settings Link | Navigation | ⚙️ | `navigation.settings` |

### ContextPicker Component (`src/components/context/ContextPicker.tsx`)

| UI Element | Element Type | Emoji | Mapping Key |
|------------|--------------|-------|-------------|
| Category Selector | Field | 🏷️ | `fields.category` |
| Forma Selector | Field | 📐 | `fields.forma` |
| Cause Selector | Field | 🔍 | `fields.cause` |
| Develop Selector | Field | 📈 | `fields.develop` |
| Effect Selector | Field | 💥 | `fields.effect` |
| Loading State | Status | 🔄 | `status.syncing` |
| Error State | Status | ❌ | `status.error` |

### DatePickerField Component (`src/components/DatePickerField.tsx`)

| UI Element | Element Type | Emoji | Mapping Key |
|------------|--------------|-------|-------------|
| Start Date Label | Field | 📅 | `fields.startDate` |
| End Date Label | Field | 📅 | `fields.endDate` |
| Clear Button | Action | ❌ | `actions.cancel` |
| Validation Error | Status | ❌ | `status.error` |

### EventCard Component (`src/components/Dashboard/EventCard.tsx`)

| UI Element | Element Type | Emoji | Mapping Key |
|------------|--------------|-------|-------------|
| Event Icon | Screen | 📋 | `screens.events` |
| View Button | Action | 👁️ | (custom) |
| Edit Button | Action | ✏️ | `actions.edit` |
| Delete Button | Action | 🗑️ | `actions.delete` |

### NodesPanel Component (`src/components/NodesPanel.tsx`) - Desktop-specific

| UI Element | Element Type | Emoji | Mapping Key |
|------------|--------------|-------|-------------|
| Panel Title | Component | 🌐 | (custom) |
| Refresh Button | Action | 🔄 | `actions.refresh` |
| Online Node | Status | 🟢 | `status.online` |
| Offline Node | Status | 🔴 | `status.offline` |

## Implementation Reference

### Code Location
- **Emoji Mapping Definition**: `ui/desktop/src/utils/emojiMapping.ts`
- **Usage Function**: `getEmoji(category: string, key: string): string`

### Usage Example
```typescript
import { getEmoji } from '@/utils/emojiMapping';

// Get emoji for screen
const dashboardEmoji = getEmoji('screens', 'dashboard'); // Returns '🏠'

// Get emoji for action
const saveEmoji = getEmoji('actions', 'save'); // Returns '💾'

// Get emoji for field
const categoryEmoji = getEmoji('fields', 'category'); // Returns '🏷️'

// Usage in component
<button>
  {getEmoji('actions', 'save')} Save
</button>
```

## Android Client Synchronization

This emoji mapping specification serves as the source of truth for synchronizing emoji usage between Desktop UI and Android client. The Android client MUST use the same emojis for equivalent UI elements to ensure consistency across platforms.

### Synchronization Rules:
1. **Same Function = Same Emoji**: UI elements with the same function across platforms must use the same emoji
2. **Category Consistency**: Emoji categories (screens, actions, fields, status, navigation) must be consistent
3. **Semantic Meaning**: Emojis must be semantically meaningful and directly related to element function
4. **Accessibility**: All UI elements must include emojis (constitutional requirement Rule 8)

## Maintenance

When adding new UI elements:
1. Add the element to the appropriate category in `ui/desktop/src/utils/emojiMapping.ts`
2. Update this specification document with the new element
3. Ensure Android client is updated with the same emoji mapping
4. Verify emoji consistency across all screens

## Version History

- **v1.0.0** (2025-12-09): Initial specification created with complete emoji mapping for all Desktop UI screens and components.

