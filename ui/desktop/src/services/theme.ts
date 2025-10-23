// Advanced theming system for Truth Training Desktop

export interface ColorPalette {
  primary: string;
  secondary: string;
  accent: string;
  background: string;
  surface: string;
  text: string;
  textSecondary: string;
  border: string;
  success: string;
  warning: string;
  error: string;
  info: string;
}

export interface Theme {
  name: string;
  colors: ColorPalette;
  typography: {
    fontFamily: string;
    fontSize: {
      xs: string;
      sm: string;
      base: string;
      lg: string;
      xl: string;
      '2xl': string;
      '3xl': string;
    };
    fontWeight: {
      normal: number;
      medium: number;
      semibold: number;
      bold: number;
    };
  };
  spacing: {
    xs: string;
    sm: string;
    md: string;
    lg: string;
    xl: string;
    '2xl': string;
  };
  borderRadius: {
    sm: string;
    md: string;
    lg: string;
    full: string;
  };
  shadows: {
    sm: string;
    md: string;
    lg: string;
    xl: string;
  };
}

export const lightTheme: Theme = {
  name: 'Light',
  colors: {
    primary: '#3B82F6',
    secondary: '#6B7280',
    accent: '#F59E0B',
    background: '#FFFFFF',
    surface: '#F9FAFB',
    text: '#111827',
    textSecondary: '#6B7280',
    border: '#E5E7EB',
    success: '#10B981',
    warning: '#F59E0B',
    error: '#EF4444',
    info: '#3B82F6'
  },
  typography: {
    fontFamily: 'Inter, system-ui, sans-serif',
    fontSize: {
      xs: '0.75rem',
      sm: '0.875rem',
      base: '1rem',
      lg: '1.125rem',
      xl: '1.25rem',
      '2xl': '1.5rem',
      '3xl': '1.875rem'
    },
    fontWeight: {
      normal: 400,
      medium: 500,
      semibold: 600,
      bold: 700
    }
  },
  spacing: {
    xs: '0.25rem',
    sm: '0.5rem',
    md: '1rem',
    lg: '1.5rem',
    xl: '2rem',
    '2xl': '3rem'
  },
  borderRadius: {
    sm: '0.25rem',
    md: '0.375rem',
    lg: '0.5rem',
    full: '9999px'
  },
  shadows: {
    sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
    md: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
    lg: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',
    xl: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)'
  }
};

export const darkTheme: Theme = {
  name: 'Dark',
  colors: {
    primary: '#60A5FA',
    secondary: '#9CA3AF',
    accent: '#FBBF24',
    background: '#111827',
    surface: '#1F2937',
    text: '#F9FAFB',
    textSecondary: '#D1D5DB',
    border: '#374151',
    success: '#34D399',
    warning: '#FBBF24',
    error: '#F87171',
    info: '#60A5FA'
  },
  typography: {
    fontFamily: 'Inter, system-ui, sans-serif',
    fontSize: {
      xs: '0.75rem',
      sm: '0.875rem',
      base: '1rem',
      lg: '1.125rem',
      xl: '1.25rem',
      '2xl': '1.5rem',
      '3xl': '1.875rem'
    },
    fontWeight: {
      normal: 400,
      medium: 500,
      semibold: 600,
      bold: 700
    }
  },
  spacing: {
    xs: '0.25rem',
    sm: '0.5rem',
    md: '1rem',
    lg: '1.5rem',
    xl: '2rem',
    '2xl': '3rem'
  },
  borderRadius: {
    sm: '0.25rem',
    md: '0.375rem',
    lg: '0.5rem',
    full: '9999px'
  },
  shadows: {
    sm: '0 1px 2px 0 rgba(0, 0, 0, 0.3)',
    md: '0 4px 6px -1px rgba(0, 0, 0, 0.4), 0 2px 4px -1px rgba(0, 0, 0, 0.3)',
    lg: '0 10px 15px -3px rgba(0, 0, 0, 0.4), 0 4px 6px -2px rgba(0, 0, 0, 0.3)',
    xl: '0 20px 25px -5px rgba(0, 0, 0, 0.4), 0 10px 10px -5px rgba(0, 0, 0, 0.3)'
  }
};

export const highContrastTheme: Theme = {
  name: 'High Contrast',
  colors: {
    primary: '#0000FF',
    secondary: '#808080',
    accent: '#FFA500',
    background: '#FFFFFF',
    surface: '#F0F0F0',
    text: '#000000',
    textSecondary: '#333333',
    border: '#000000',
    success: '#008000',
    warning: '#FFA500',
    error: '#FF0000',
    info: '#0000FF'
  },
  typography: {
    fontFamily: 'Arial, sans-serif',
    fontSize: {
      xs: '0.75rem',
      sm: '0.875rem',
      base: '1rem',
      lg: '1.125rem',
      xl: '1.25rem',
      '2xl': '1.5rem',
      '3xl': '1.875rem'
    },
    fontWeight: {
      normal: 400,
      medium: 500,
      semibold: 600,
      bold: 700
    }
  },
  spacing: {
    xs: '0.25rem',
    sm: '0.5rem',
    md: '1rem',
    lg: '1.5rem',
    xl: '2rem',
    '2xl': '3rem'
  },
  borderRadius: {
    sm: '0.25rem',
    md: '0.375rem',
    lg: '0.5rem',
    full: '9999px'
  },
  shadows: {
    sm: '0 1px 2px 0 rgba(0, 0, 0, 0.5)',
    md: '0 4px 6px -1px rgba(0, 0, 0, 0.6), 0 2px 4px -1px rgba(0, 0, 0, 0.5)',
    lg: '0 10px 15px -3px rgba(0, 0, 0, 0.6), 0 4px 6px -2px rgba(0, 0, 0, 0.5)',
    xl: '0 20px 25px -5px rgba(0, 0, 0, 0.6), 0 10px 10px -5px rgba(0, 0, 0, 0.5)'
  }
};

export class ThemeManager {
  private static instance: ThemeManager;
  private currentTheme: Theme;
  private listeners: Set<(theme: Theme) => void> = new Set();

  constructor() {
    this.currentTheme = this.loadTheme();
    this.applyTheme(this.currentTheme);
  }

  static getInstance(): ThemeManager {
    if (!ThemeManager.instance) {
      ThemeManager.instance = new ThemeManager();
    }
    return ThemeManager.instance;
  }

  // Get available themes
  getAvailableThemes(): Theme[] {
    return [lightTheme, darkTheme, highContrastTheme];
  }

  // Get current theme
  getCurrentTheme(): Theme {
    return this.currentTheme;
  }

  // Set theme
  setTheme(theme: Theme): void {
    this.currentTheme = theme;
    this.saveTheme(theme);
    this.applyTheme(theme);
    this.notifyListeners();
  }

  // Set theme by name
  setThemeByName(name: string): boolean {
    const theme = this.getAvailableThemes().find(t => t.name === name);
    if (theme) {
      this.setTheme(theme);
      return true;
    }
    return false;
  }

  // Apply theme to document
  private applyTheme(theme: Theme): void {
    const root = document.documentElement;
    
    // Apply CSS custom properties
    Object.entries(theme.colors).forEach(([key, value]) => {
      root.style.setProperty(`--color-${key}`, value);
    });
    
    Object.entries(theme.typography.fontSize).forEach(([key, value]) => {
      root.style.setProperty(`--font-size-${key}`, value);
    });
    
    Object.entries(theme.spacing).forEach(([key, value]) => {
      root.style.setProperty(`--spacing-${key}`, value);
    });
    
    Object.entries(theme.borderRadius).forEach(([key, value]) => {
      root.style.setProperty(`--border-radius-${key}`, value);
    });
    
    Object.entries(theme.shadows).forEach(([key, value]) => {
      root.style.setProperty(`--shadow-${key}`, value);
    });
    
    // Apply font family
    root.style.setProperty('--font-family', theme.typography.fontFamily);
  }

  // Load theme from storage
  private loadTheme(): Theme {
    try {
      const stored = localStorage.getItem('truth-theme');
      if (stored) {
        const parsed = JSON.parse(stored);
        const theme = this.getAvailableThemes().find(t => t.name === parsed.name);
        if (theme) return theme;
      }
    } catch (error) {
      console.warn('Failed to load theme from storage:', error);
    }
    
    // Default to system preference
    if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return darkTheme;
    }
    
    return lightTheme;
  }

  // Save theme to storage
  private saveTheme(theme: Theme): void {
    try {
      localStorage.setItem('truth-theme', JSON.stringify({ name: theme.name }));
    } catch (error) {
      console.warn('Failed to save theme to storage:', error);
    }
  }

  // Subscribe to theme changes
  subscribe(listener: (theme: Theme) => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  // Notify listeners
  private notifyListeners(): void {
    this.listeners.forEach(listener => listener(this.currentTheme));
  }

  // Create custom theme
  createCustomTheme(baseTheme: Theme, overrides: Partial<Theme>): Theme {
    return {
      ...baseTheme,
      ...overrides,
      colors: { ...baseTheme.colors, ...overrides.colors },
      typography: { ...baseTheme.typography, ...overrides.typography },
      spacing: { ...baseTheme.spacing, ...overrides.spacing },
      borderRadius: { ...baseTheme.borderRadius, ...overrides.borderRadius },
      shadows: { ...baseTheme.shadows, ...overrides.shadows }
    };
  }
}

// Export singleton instance
export const themeManager = ThemeManager.getInstance();

// React hook for theme
export const useTheme = () => {
  const [theme, setTheme] = React.useState(themeManager.getCurrentTheme());
  
  React.useEffect(() => {
    const unsubscribe = themeManager.subscribe(setTheme);
    return unsubscribe;
  }, []);
  
  return {
    theme,
    setTheme: (newTheme: Theme) => themeManager.setTheme(newTheme),
    setThemeByName: (name: string) => themeManager.setThemeByName(name),
    availableThemes: themeManager.getAvailableThemes()
  };
};

// Import React for hooks
import React from 'react';
