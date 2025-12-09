/**
 * Emoji mapping system for UI elements.
 * Provides consistent emoji assignment for accessibility (constitutional requirement Rule 8).
 * All UI elements must include appropriate emojis for better understanding.
 */

export interface EmojiMapping {
  // Screens
  screens: {
    dashboard: string;
    newEvent: string;
    contextEditor: string;
    events: string;
    judgments: string;
    overallSummary: string;
    trainingResults: string;
    settings: string;
  };

  // Actions
  actions: {
    save: string;
    cancel: string;
    delete: string;
    edit: string;
    create: string;
    submit: string;
    refresh: string;
    sync: string;
    back: string;
    next: string;
  };

  // Form Fields
  fields: {
    name: string;
    description: string;
    category: string;
    forma: string;
    cause: string;
    develop: string;
    effect: string;
    startDate: string;
    endDate: string;
    assessment: string;
    confidence: string;
    reasoning: string;
  };

  // Status Indicators
  status: {
    online: string;
    offline: string;
    syncing: string;
    error: string;
    success: string;
    warning: string;
  };

  // Navigation
  navigation: {
    home: string;
    events: string;
    judgments: string;
    templates: string;
    summary: string;
    training: string;
    settings: string;
  };
}

/**
 * Default emoji mapping.
 * Emojis are semantically meaningful and directly related to function.
 * Same function = same emoji across the application (consistency requirement).
 */
export const defaultEmojiMapping: EmojiMapping = {
  screens: {
    dashboard: '🏠',
    newEvent: '➕',
    contextEditor: '📝',
    events: '📋',
    judgments: '⚖️',
    overallSummary: '📊',
    trainingResults: '📈',
    settings: '⚙️',
  },
  actions: {
    save: '💾',
    cancel: '❌',
    delete: '🗑️',
    edit: '✏️',
    create: '➕',
    submit: '✅',
    refresh: '🔄',
    sync: '🔄',
    back: '⬅️',
    next: '➡️',
  },
  fields: {
    name: '📝',
    description: '📄',
    category: '🏷️',
    forma: '📐',
    cause: '🔍',
    develop: '📈',
    effect: '💥',
    startDate: '📅',
    endDate: '📅',
    assessment: '⚖️',
    confidence: '📊',
    reasoning: '💭',
  },
  status: {
    online: '🟢',
    offline: '🔴',
    syncing: '🔄',
    error: '❌',
    success: '✅',
    warning: '⚠️',
  },
  navigation: {
    home: '🏠',
    events: '📋',
    judgments: '⚖️',
    templates: '📝',
    summary: '📊',
    training: '📈',
    settings: '⚙️',
  },
};

/**
 * Get emoji for a UI element.
 * Returns appropriate emoji based on element type and key.
 * 
 * @param category - Category of UI element (screens, actions, fields, status, navigation)
 * @param key - Key within the category
 * @returns Emoji string, or empty string if not found
 * 
 * @example
 * ```typescript
 * const saveEmoji = getEmoji('actions', 'save'); // Returns '💾'
 * const dashboardEmoji = getEmoji('screens', 'dashboard'); // Returns '🏠'
 * ```
 */
export function getEmoji(
  category: keyof EmojiMapping,
  key: string
): string {
  const categoryMap = defaultEmojiMapping[category];
  if (categoryMap && key in categoryMap) {
    return (categoryMap as Record<string, string>)[key];
  }
  return '';
}

