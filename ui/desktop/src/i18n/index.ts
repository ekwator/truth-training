// Internationalization setup for Truth Training Desktop

export interface Locale {
  code: string;
  name: string;
  nativeName: string;
  direction: 'ltr' | 'rtl';
}

export interface Translation {
  [key: string]: string | Translation;
}

// English-only interface (localization removed)
export const supportedLocales: Locale[] = [
  {
    code: 'en',
    name: 'English',
    nativeName: 'English',
    direction: 'ltr'
  }
];

// Default translations (English)
export const defaultTranslations: Translation = {
  common: {
    loading: 'Loading...',
    error: 'Error',
    success: 'Success',
    cancel: 'Cancel',
    save: 'Save',
    delete: 'Delete',
    edit: 'Edit',
    view: 'View',
    close: 'Close',
    retry: 'Retry',
    refresh: 'Refresh',
    yes: 'Yes',
    no: 'No',
    saving: 'Saving...',
    note: 'Note',
    confirm: 'Confirm'
  },
  navigation: {
    dashboard: 'Dashboard',
    events: 'Events',
    judgments: 'Judgments',
    settings: 'Settings'
  },
  dashboard: {
    title: 'Truth Training',
    subtitle: 'Collective Intelligence Dashboard',
    totalEvents: 'Total Events',
    activeEvents: 'Active Events',
    detectedEvents: 'Detected Events',
    withConsensus: 'With Consensus',
    participants: 'Participants',
    recentEvents: 'Recent Events',
    noEvents: 'No events yet',
    noEventsDescription: 'Get started by creating your first event.',
    createFirstEvent: 'Create First Event',
    noData: 'No data',
    errorLoading: 'Error Loading Dashboard',
    quickStats: 'Quick Stats',
    actions: 'Actions',
    viewEvents: 'View Events',
    viewJudgments: 'View Judgments',
    viewSummary: 'View Summary',
    newEvent: 'New Event',
    manageContextTemplates: 'Manage Context Templates',
    overallSummary: 'Overall Summary',
    trainingResults: 'Training Results',
    settings: 'Settings',
    lastSync: 'Last sync: %1$s'
  },
  events: {
    title: 'Events',
    subtitle: 'Manage and view all events',
    createEvent: 'Create Event',
    search: 'Search Events',
    searchPlaceholder: 'Search by title or description...',
    status: 'Status',
    allStatus: 'All Status',
    active: 'Active',
    inactive: 'Inactive',
    archived: 'Archived',
    detected: 'Detected',
    notDetected: 'Not Detected',
    noEventsFound: 'No events match your search',
    noEvents: 'No events yet',
    noEventsDescription: 'Try adjusting your search terms.',
    errorLoading: 'Error Loading Events',
    created: 'Created',
    updated: 'Updated',
    eventDetails: 'Event Details',
    editEvent: 'Edit Event',
    contextFields: 'Context Fields',
    contextFieldsTitle: 'Context Fields',
    category: 'Category',
    forma: 'Forma',
    cause: 'Cause',
    develop: 'Develop',
    effect: 'Effect',
    startTimestamp: 'Start Timestamp',
    endTimestamp: 'End Timestamp',
    corrected: 'Corrected',
    outgoing: 'Outgoing',
    incoming: 'Incoming',
    unknown: 'Unknown',
    viewJudgments: 'View Judgments',
    confirmDelete: 'Are you sure you want to delete this event?',
    noEventSelected: 'No event selected',
    description: 'Description',
    autoCalculated: '(auto-calculated)',
    readOnly: '(read-only)',
    endTimestampHint: 'Cannot be less than Start Timestamp, can be equal',
    contextTemplate: 'Context Template',
    selectTemplate: 'Select Template',
    templateSelected: 'Template selected - fields prefilled',
    templateSelectionHint: 'Click to select a template and prefill context fields',
    selectTemplateToFillFields: 'Select a template to fill context fields',
    contextFieldsOptional: 'Context Fields (optional, NULL values ignored in duplicate detection)',
    timestamps: 'Timestamps',
    start: 'Start',
    end: 'End',
    vector: 'Vector',
    templateSelection: 'Template Selection',
    noTemplatesAvailable: 'No templates available',
    createTemplateFirst: 'Create a template first',
    timestampCannotBeEmpty: 'Timestamp cannot be empty',
    endTimestampCannotBeLessThanStart: 'End Timestamp cannot be less than Start Timestamp (can be equal)',
    requiredField: 'Required field',
    timeline: 'Timeline',
    direction: 'Direction',
    knowledgeBaseUnavailable: 'Knowledge base data unavailable',
    knowledgeBaseUnavailableMessage: 'Please ensure database connection is available. Context fields may be empty.',
    contextFieldsPrefilled: 'Context fields prefilled from template',
    flags: 'Flags',
    name: 'Name'
  },
  contexts: {
    title: 'Context Templates',
    newTemplate: 'New Template',
    addTemplate: 'Add Template',
    addFirstTemplate: 'Add First Template',
    noTemplates: 'No templates available',
    templateName: 'Template Name',
    templateNameRequired: 'Template Name *',
    templateNamePlaceholder: 'Enter template name (e.g., Interpersonal Conflict)',
    description: 'Description',
    templateDescription: 'Description',
    descriptionPlaceholder: 'Enter template description...',
    contextFields: 'Context Fields',
    contextFieldsTitle: 'Context Fields',
    category: 'Category',
    forma: 'Forma',
    cause: 'Cause',
    develop: 'Develop',
    effect: 'Effect',
    fieldRequired: 'This field is required',
    templateCreated: 'Template Created',
    templateCreatedMessage: 'Context template "{name}" has been created successfully.',
    duplicateTemplate: 'Duplicate Template',
    duplicateTemplateMessage: 'A template with identical non-NULL fields already exists.',
    duplicateError: 'Template already exists. A template with identical non-NULL fields already exists.',
    templateDuplicateError: 'Template with identical fields already exists',
    errorSavingTemplate: 'Error saving template',
    templateDuplicateNote: 'Note: Templates with identical non-NULL fields cannot be created (409 Conflict)',
    duplicateNote: 'Duplicate detection compares only non-NULL fields. Templates with identical non-NULL field values cannot be created.',
    errorLoading: 'Error Loading Templates',
    errorLoadingMessage: 'Failed to load context templates.',
    errorCreating: 'Failed to Create Template',
    errorCreatingMessage: 'Please check your connection and try again.',
    noTemplatesYet: 'No templates yet',
    createFirstTemplate: 'Create First Template',
    noContextFields: 'No context fields',
    emptyTemplate: 'Empty template (no context fields)',
    selectTemplatePrefill: 'Select a template to prefill context fields:',
    editTemplate: 'Edit Template'
  },
  judgments: {
    title: 'Judgments',
    subtitle: 'View and manage all judgments',
    filterByEvent: 'Filter by Event',
    allEvents: 'All Events',
    assessment: 'Assessment',
    allAssessments: 'All Assessments',
    true: 'True',
    false: 'False',
    uncertain: 'Uncertain',
    assessmentTrue: 'True',
    assessmentFalse: 'False',
    confidenceLevel: 'Confidence Level',
    allLevels: 'All Levels',
    high: 'High (0.8+)',
    medium: 'Medium (0.5+)',
    low: 'Low (0.2+)',
    noJudgments: 'No judgments found',
    noJudgmentsYet: 'No judgments yet',
    noJudgmentsDescription: 'No judgments have been submitted yet.',
    noJudgmentsForEvent: 'No judgments for the selected event.',
    avgConfidence: 'Avg Confidence',
    averageConfidence: 'Average Confidence',
    totalJudgments: 'Total Judgments',
    addJudgment: 'Add Judgment',
    newJudgment: 'New Judgment',
    submitJudgment: 'Submit Judgment',
    submit: 'Submit',
    reasoning: 'Reasoning',
    reasoningOptional: 'Reasoning (optional)',
    reasoningPlaceholder: 'Explain your assessment...',
    assessmentRequired: 'Assessment is required',
    confidenceRange: 'Confidence level must be between 0.0 and 1.0',
    mustBeBetween: 'Must be between 0.0 and 1.0',
    confidencePercent: '%1$d%% confidence',
    judgmentSubmitted: 'Judgment Submitted',
    judgmentSubmittedMessage: 'Your judgment has been submitted successfully.',
    errorSubmitting: 'Error Submitting Judgment',
    errorSubmittingMessage: 'Failed to submit judgment. Please try again.',
    errorLoading: 'Error Loading Judgments',
    consensusPercentage: 'Consensus Percentage',
    consensusStatistics: 'Consensus Statistics',
    collectiveIntelligenceMessage: 'Collective Intelligence: Your judgment contributes to truth convergence and consensus.'
  },
  summary: {
    title: 'Overall Summary',
    refresh: 'Refresh',
    metrics: 'Metrics',
    totalEvents: 'Total Events',
    totalEventsCount: 'Total events: %1$d',
    detectedEvents: 'Detected Events',
    detectedEventsCount: 'Detected events: %1$d',
    eventsWithConsensus: 'Events with Consensus',
    eventsWithConsensusCount: 'Events with consensus: %1$d',
    averageCollectiveScore: 'Average Collective Score',
    lastUpdated: 'Last Updated',
    lastUpdatedTime: 'Last updated: %1$s',
    never: 'Never',
    networkStatistics: 'Network Statistics',
    nodeCount: 'Node Count',
    activeConnections: 'Active Connections',
    syncStatus: 'Sync Status',
    exportReport: 'Export Report (.txt)',
    eventSummary: 'Event Summary',
    peers: 'Peers',
    edges: 'Edges',
    averageTrust: 'Average Trust',
    updated: 'Updated: %1$s'
  },
  training: {
    title: 'Training Results',
    refresh: 'Refresh',
    progressMetrics: 'Progress Metrics',
    totalEvents: 'Total Events',
    totalPositiveImpact: 'Total Positive Impact',
    totalNegativeImpact: 'Total Negative Impact',
    averageScore: 'Average Score',
    trendIndicator: 'Trend Indicator',
    trend: 'Trend',
    impactProgress: 'Impact Progress',
    positiveImpactProgress: 'Positive Impact Progress',
    progressPercentage: 'Progress Percentage',
    resultsTable: 'Results Table',
    noData: 'No data available',
    historicalResults: 'Historical Results',
    eventsCount: 'Events: %1$d',
    trendValue: 'Trend: %1$s'
  },
  nodes: {
    title: 'Nodes',
    description: 'LAN/Wi-Fi/Global nodes with TTL countdown and reachability status',
    refresh: 'Refresh',
    discover: 'Discover',
    cleanup: 'Cleanup',
    healthCheck: 'Health Check',
    loading: 'Loading nodes…',
    noNodes: 'No nodes discovered yet.',
    address: 'Address',
    type: 'Type',
    status: 'Status',
    ttl: 'TTL (s)',
    expiresIn: 'Expires In',
    source: 'Source',
    lastSeen: 'Last Seen',
    online: 'Online',
    offline: 'Offline',
    lastUpdated: 'Last updated'
  },
  settings: {
    title: 'Settings',
    subtitle: 'Configure application preferences',
    language: 'Language',
    languageSelection: 'Language Selection',
    languageDescription: 'Select application language',
    languageChanged: 'Language Changed',
    languageChangedMessage: 'Application language has been changed. The page will reload.',
    changeLanguage: 'Change Language',
    changeLanguageMessage: 'Changing language will clear all context templates and update knowledge base data. The application will restart. Continue?',
    confirmLanguageChange: 'Confirm Language Change',
    confirmLanguageChangeMessage: 'Changing the language will clear context templates and re-seed the knowledge base. Continue?',
    errorChangingLanguage: 'Error Changing Language',
    errorChangingLanguageMessage: 'Failed to change language. Please try again.',
    actions: 'Actions',
    clearEvents: 'Clear Events',
    clearEventsTitle: 'Clear Events',
    clearEventsMessage: 'This will delete all events from the database. This action cannot be undone. Are you sure?',
    confirmClearEvents: 'Confirm Clear Events',
    confirmClearEventsMessage: 'Are you sure you want to delete all events? This action cannot be undone.',
    clearEventsNotImplemented: 'Clear Events functionality will be implemented via Tauri command.',
    errorClearingEvents: 'Error Clearing Events',
    errorClearingEventsMessage: 'Failed to clear events. Please try again.',
    apiConfig: 'API Configuration',
    apiBaseUrl: 'API Base URL',
    apiBaseUrlDescription: 'The base URL for the Truth Training API',
    syncConfig: 'Sync Configuration',
    enableAutoSync: 'Enable automatic sync',
    syncInterval: 'Sync Interval (seconds)',
    syncIntervalDescription: 'How often to automatically sync offline operations',
    syncStatus: 'Sync Status',
    syncNow: 'Sync Now',
    connectionStatus: 'Connection Status',
    connectionMode: 'Connection Mode',
    mode: 'Mode',
    coreLocal: 'Core (Local)',
    httpApi: 'HTTP API',
    serverConfiguration: 'Server Configuration',
    ipAddress: 'IP Address',
    invalidIpFormat: 'Invalid IP address format',
    port: 'Port',
    portRangeError: 'Port must be between 1 and 65535',
    nearbySync: 'Nearby Sync',
    enableUdpBroadcast: 'Enable UDP Broadcast Discovery',
    intervalMs: 'Interval (ms)',
    intervalRangeError: 'Interval must be between 500 and 60000 ms',
    discoveryWorkerSettings: 'Discovery Worker Settings',
    enableBackgroundDiscovery: 'Enable Background Discovery',
    lanIntervalMs: 'LAN Interval (ms)',
    wifiIntervalMs: 'Wi-Fi Interval (ms)',
    globalIntervalMs: 'Global Interval (ms)',
    lanTtlSeconds: 'LAN TTL (seconds)',
    wifiTtlSeconds: 'Wi-Fi TTL (seconds)',
    globalTtlSeconds: 'Global TTL (seconds)',
    online: 'Online',
    offline: 'Offline',
    pendingOperations: 'Pending Operations',
    pendingOperationsCount: 'Pending operations: %1$d',
    lastSync: 'Last Sync',
    lastSyncTime: 'Last sync: %1$s',
    never: 'Never',
    syncInProgress: 'Sync in Progress',
    yes: 'Yes',
    no: 'No',
    forceSync: 'Force Sync',
    clearQueue: 'Clear Queue',
    appInfo: 'Application Information',
    version: 'Version',
    build: 'Build',
    platform: 'Platform',
    saveSettings: 'Save Settings',
    saveConnectionSettings: 'Save Connection Settings',
    saveDiscoverySettings: 'Save Discovery Settings',
    testConnection: 'Test Connection',
    testResult: 'Test result: %1$s',
    testTime: 'Test time: %1$s',
    back: 'Back',
    english: 'English',
    russian: 'Russian'
  },
  errors: {
    networkError: 'Network error occurred',
    serverError: 'Server error occurred',
    validationError: 'Validation error',
    dateRequired: 'Date is required',
    // Event validation messages
    eventNameRequired: 'Event name is required',
    eventDescriptionRequired: 'Event description is required',
    startTimestampRequired: 'Start timestamp is required',
    endTimestampBeforeStart: 'End timestamp cannot be less than start timestamp',
    // Template validation messages
    templateNameRequired: 'Template name is required',
    // Context field validation messages
    categoryRequired: 'Category is required',
    formaRequired: 'Forma is required',
    causeRequired: 'Cause is required',
    developRequired: 'Develop is required',
    effectRequired: 'Effect is required',
    // Date validation messages
    invalidDate: 'Invalid date format',
    dateTooEarly: 'Date is too early',
    dateTooLate: 'Date is too late',
    clear: 'Clear',
    unknownError: 'An unknown error occurred',
    retryMessage: 'Please try again or contact support if the problem persists.'
  }
};

// Get translations for current locale (English-only)
export const getTranslations = (_locale: string = getCurrentLocale()): Translation => {
  // English-only interface - always return English translations
  return defaultTranslations;
};

// Translation function with locale support
export const t = (key: string, locale?: string): string => {
  const translations = getTranslations(locale);
  const keys = key.split('.');
  let value: any = translations;
  
  for (const k of keys) {
    if (value && typeof value === 'object' && k in value) {
      value = value[k];
    } else {
      // Missing translation - log warning and fallback to English
      if (locale && locale !== 'en') {
        console.warn('translation.missing', { locale, key });
      }
      // Fallback to English
      const enValue = getTranslations('en');
      let enResult: any = enValue;
      for (const enK of keys) {
        if (enResult && typeof enResult === 'object' && enK in enResult) {
          enResult = enResult[enK];
        } else {
          return key; // Return key if not found in English either
        }
      }
      return typeof enResult === 'string' ? enResult : key;
    }
  }
  
  return typeof value === 'string' ? value : key;
};

// Locale detection (English-only)
export const detectLocale = async (): Promise<string> => {
  // English-only interface - always return 'en'
  return 'en';
};

// Set locale (English-only, no-op for locale switching)
export const setLocale = async (_locale: string, _persistToBackend: boolean = true): Promise<void> => {
  // English-only interface - always use English
  const englishLocale = 'en';
  localStorage.setItem('truth-locale', englishLocale);
  document.documentElement.lang = englishLocale;
  document.documentElement.dir = 'ltr';

  // Persist to backend if in Tauri (always save as 'en')
  if (_persistToBackend && typeof window !== 'undefined') {
      try {
        const isTauri = (window as any).__TAURI__ !== undefined;
        if (isTauri) {
          const { invoke } = await import('@tauri-apps/api/core');
          const currentConfig = await invoke('get_app_config') as Record<string, any>;
          
          const updatedConfig = {
            mode: currentConfig?.mode || 'core',
            server_ip: currentConfig?.server_ip || '127.0.0.1',
            server_port: currentConfig?.server_port || 8080,
            nearby_sync: currentConfig?.nearby_sync ?? false,
            nearby_interval_ms: currentConfig?.nearby_interval_ms || 3000,
          locale: englishLocale, // Always English
          };
          
          await invoke('save_app_config', { config: updatedConfig });
      }
    } catch (error) {
      console.warn('Failed to persist locale to backend:', error);
    }
  }
};

// Get current locale (English-only)
export const getCurrentLocale = (): string => {
  // English-only interface - always return 'en'
  return 'en';
};

// Initialize locale (English-only)
export const initializeLocale = async (): Promise<void> => {
  // English-only interface - always initialize to English
  const englishLocale = 'en';
  localStorage.setItem('truth-locale', englishLocale);
  document.documentElement.lang = englishLocale;
  document.documentElement.dir = 'ltr';
};

// Format numbers based on locale
export const formatNumber = (value: number, locale: string = getCurrentLocale()): string => {
  return new Intl.NumberFormat(locale).format(value);
};

// Format dates based on locale
export const formatDate = (date: Date, locale: string = getCurrentLocale()): string => {
  return new Intl.DateTimeFormat(locale, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
};

// Format relative time
export const formatRelativeTime = (date: Date, locale: string = getCurrentLocale()): string => {
  const rtf = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' });
  const now = new Date();
  const diffInSeconds = Math.floor((date.getTime() - now.getTime()) / 1000);
  
  if (Math.abs(diffInSeconds) < 60) {
    return rtf.format(diffInSeconds, 'second');
  } else if (Math.abs(diffInSeconds) < 3600) {
    return rtf.format(Math.floor(diffInSeconds / 60), 'minute');
  } else if (Math.abs(diffInSeconds) < 86400) {
    return rtf.format(Math.floor(diffInSeconds / 3600), 'hour');
  } else {
    return rtf.format(Math.floor(diffInSeconds / 86400), 'day');
  }
};
