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

export const supportedLocales: Locale[] = [
  {
    code: 'en',
    name: 'English',
    nativeName: 'English',
    direction: 'ltr'
  },
  {
    code: 'ru',
    name: 'Russian',
    nativeName: 'Русский',
    direction: 'ltr'
  },
  // ES/FR/DE/AR remain in code but are not selectable until strings exist
  {
    code: 'es',
    name: 'Spanish',
    nativeName: 'Español',
    direction: 'ltr'
  },
  {
    code: 'fr',
    name: 'French',
    nativeName: 'Français',
    direction: 'ltr'
  },
  {
    code: 'de',
    name: 'German',
    nativeName: 'Deutsch',
    direction: 'ltr'
  },
  {
    code: 'ar',
    name: 'Arabic',
    nativeName: 'العربية',
    direction: 'rtl'
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
    refresh: 'Refresh'
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
    withConsensus: 'With Consensus',
    participants: 'Participants',
    recentEvents: 'Recent Events',
    noEvents: 'No events yet',
    noEventsDescription: 'Get started by creating your first event.'
  },
  events: {
    title: 'Events',
    subtitle: 'Manage and view all events',
    createEvent: 'Create Event',
    searchPlaceholder: 'Search by title or description...',
    status: 'Status',
    allStatus: 'All Status',
    active: 'Active',
    inactive: 'Inactive',
    archived: 'Archived',
    noEventsFound: 'No events match your search',
    noEventsDescription: 'Try adjusting your search terms.',
    created: 'Created',
    updated: 'Updated'
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
    confidenceLevel: 'Confidence Level',
    allLevels: 'All Levels',
    high: 'High (0.8+)',
    medium: 'Medium (0.5+)',
    low: 'Low (0.2+)',
    noJudgments: 'No judgments found',
    noJudgmentsDescription: 'No judgments have been submitted yet.',
    avgConfidence: 'Avg Confidence'
  },
  settings: {
    title: 'Settings',
    subtitle: 'Configure application preferences',
    apiConfig: 'API Configuration',
    apiBaseUrl: 'API Base URL',
    apiBaseUrlDescription: 'The base URL for the Truth Training API',
    syncConfig: 'Sync Configuration',
    enableAutoSync: 'Enable automatic sync',
    syncInterval: 'Sync Interval (seconds)',
    syncIntervalDescription: 'How often to automatically sync offline operations',
    syncStatus: 'Sync Status',
    connectionStatus: 'Connection Status',
    online: 'Online',
    offline: 'Offline',
    pendingOperations: 'Pending Operations',
    lastSync: 'Last Sync',
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
    saveSettings: 'Save Settings'
  },
  errors: {
    networkError: 'Network error occurred',
    serverError: 'Server error occurred',
    validationError: 'Validation error',
    unknownError: 'An unknown error occurred',
    retryMessage: 'Please try again or contact support if the problem persists.'
  }
};

// Get translations for current locale
export const getTranslations = (locale: string = getCurrentLocale()): Translation => {
  if (locale === 'ru') {
    // Dynamic import to avoid circular dependencies
    try {
      const { ruTranslations } = require('./ru');
      return ruTranslations as Translation;
    } catch (e) {
      console.warn('translation.missing', { locale, key: 'ru' });
      return defaultTranslations;
    }
  }
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

// Locale detection
export const detectLocale = (): string => {
  // Check localStorage first
  const stored = localStorage.getItem('truth-locale');
  if (stored && supportedLocales.some(l => l.code === stored)) {
    return stored;
  }
  
  // Check browser language
  const browserLang = navigator.language.split('-')[0];
  const supported = supportedLocales.find(l => l.code === browserLang);
  if (supported) {
    return supported.code;
  }
  
  // Default to English
  return 'en';
};

// Set locale with telemetry
export const setLocale = async (locale: string, persistToBackend: boolean = true): Promise<void> => {
  const previousLocale = getCurrentLocale();
  
  if (supportedLocales.some(l => l.code === locale)) {
    localStorage.setItem('truth-locale', locale);
    document.documentElement.lang = locale;
    
    // Set text direction
    const localeInfo = supportedLocales.find(l => l.code === locale);
    if (localeInfo) {
      document.documentElement.dir = localeInfo.direction;
    }

    // Persist to backend if in Tauri
    if (persistToBackend && typeof window !== 'undefined') {
      try {
        const isTauri = (window as any).__TAURI__ !== undefined;
        if (isTauri) {
          const { invoke } = await import('@tauri-apps/api/core');
          const config = await invoke('get_app_config') as Record<string, any>;
          if (config && typeof config === 'object') {
            const updatedConfig = { ...config, locale };
            await invoke('save_app_config', { config: updatedConfig });
          }
        }
      } catch (error) {
        console.error('Failed to persist locale to backend:', error);
        // Log telemetry: locale.change failure
        console.warn('locale.change', { from: previousLocale, to: locale, success: false, error });
        throw error;
      }
    }

    // Log telemetry: locale.change success
    console.log('locale.change', { from: previousLocale, to: locale, success: true });
  }
};

// Get current locale
export const getCurrentLocale = (): string => {
  return detectLocale();
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
