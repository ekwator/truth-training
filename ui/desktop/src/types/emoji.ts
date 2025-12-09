/**
 * Emoji mapping types.
 * Supports constitutional requirement Rule 8 - UI Desktop Emoji Accessibility.
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

