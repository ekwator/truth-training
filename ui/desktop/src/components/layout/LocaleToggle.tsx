import React, { useState, useEffect, useRef } from 'react';
import { supportedLocales, getCurrentLocale, setLocale, t } from '@/i18n';
import { useToast } from '@/components/system/Toaster';

interface LocaleToggleProps {
  variant?: 'dropdown' | 'button';
  className?: string;
}

export const LocaleToggle: React.FC<LocaleToggleProps> = ({ 
  variant = 'dropdown',
  className = '' 
}) => {
  const [currentLocale, setCurrentLocale] = useState<string>(getCurrentLocale());
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const { addToast } = useToast();

  // Only show EN and RU for now (as per contract)
  const availableLocales = supportedLocales.filter(l => l.code === 'en' || l.code === 'ru');

  useEffect(() => {
    // Update locale from localStorage on mount
    const stored = localStorage.getItem('truth-locale');
    if (stored && availableLocales.some(l => l.code === stored)) {
      setCurrentLocale(stored);
    }
  }, []);

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [isOpen]);

  const handleLocaleChange = async (localeCode: string) => {
    if (localeCode === currentLocale) {
      setIsOpen(false);
      return;
    }

    try {
      await setLocale(localeCode, true);
      setCurrentLocale(localeCode);
      setIsOpen(false);
      
      // UI updates instantly without reload
      // Force re-render by updating document
      document.documentElement.lang = localeCode;
    } catch (error) {
      console.error('Failed to change locale:', error);
      addToast({
        type: 'error',
        title: t('errors.unableToSaveLocale'),
        message: t('errors.retryMessage'),
      });
    }
  };

  const currentLocaleInfo = availableLocales.find(l => l.code === currentLocale) || availableLocales[0];

  if (variant === 'button') {
    return (
      <div className={`relative ${className}`} ref={containerRef}>
        <button
          onClick={() => setIsOpen(!isOpen)}
          className="px-3 py-2 text-sm font-medium text-gray-700 hover:text-gray-900 flex items-center space-x-1"
          aria-label={t('locale.change')}
          aria-expanded={isOpen}
        >
          <span>{currentLocaleInfo.nativeName}</span>
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </button>

        {isOpen && (
          <div className="absolute right-0 mt-2 w-48 bg-white border border-gray-200 rounded shadow-lg z-50">
            {availableLocales.map((locale) => (
              <button
                key={locale.code}
                onClick={() => handleLocaleChange(locale.code)}
                className={`w-full text-left px-4 py-2 text-sm hover:bg-gray-100 ${
                  currentLocale === locale.code ? 'bg-blue-50 text-blue-600' : 'text-gray-700'
                }`}
              >
                {locale.nativeName}
              </button>
            ))}
          </div>
        )}
      </div>
    );
  }

  // Dropdown variant (default)
  return (
    <div className={`relative ${className}`} ref={containerRef}>
      <select
        value={currentLocale}
        onChange={(e) => handleLocaleChange(e.target.value)}
        className="px-3 py-2 text-sm border border-gray-300 rounded bg-white text-gray-700 hover:bg-gray-50"
        aria-label={t('locale.change')}
      >
        {availableLocales.map((locale) => (
          <option key={locale.code} value={locale.code}>
            {locale.nativeName}
          </option>
        ))}
      </select>
    </div>
  );
};

