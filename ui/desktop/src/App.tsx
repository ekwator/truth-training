import React, { useState, useEffect } from 'react';
import { ErrorBoundary } from '@/components/system/ErrorBoundary';
import { ToastProvider } from '@/components/system/Toaster';
import { ThemeProvider } from '@/components/system/ThemeProvider';
import { TopMenuBar, Screen } from '@/components/layout/TopMenuBar';
import { Dashboard } from '@/pages/Dashboard';
import { NewEvent } from '@/pages/NewEvent';
import { ContextEditor } from '@/pages/ContextEditor';
import { EventSummary } from '@/pages/EventSummary';
import { OverallSummary } from '@/pages/OverallSummary';
import { TrainingResults } from '@/pages/TrainingResults';
import { Logs } from '@/pages/Logs';
import { Settings } from '@/pages/Settings';
import { detectLocale, setLocale } from '@/i18n';
import { ApiService } from '@/services/api';

export const App: React.FC = () => {
  const [currentScreen, setCurrentScreen] = useState<Screen>('home');

  // Initialize locale on app start
  useEffect(() => {
    const initializeLocale = async () => {
      try {
        // Try to get locale from backend config
        if (typeof window !== 'undefined' && (window as any).__TAURI__ !== undefined) {
          const config = await ApiService.getAppConfig();
          if (config.locale) {
            await setLocale(config.locale, false); // Don't persist, just set
          } else {
            // Fallback to detected locale
            const detected = detectLocale();
            await setLocale(detected, false);
          }
        } else {
          // Web mode: use detected locale
          const detected = detectLocale();
          setLocale(detected, false);
        }
      } catch (error) {
        console.error('Failed to initialize locale:', error);
        // Fallback to detected locale
        const detected = detectLocale();
        setLocale(detected, false);
      }
    };

    initializeLocale();
  }, []);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.altKey) {
        switch (event.key) {
          case '1':
            event.preventDefault();
            setCurrentScreen('home');
            break;
          case '2':
            event.preventDefault();
            setCurrentScreen('new-event');
            break;
          case '3':
            event.preventDefault();
            setCurrentScreen('context-editor');
            break;
          case '4':
            event.preventDefault();
            setCurrentScreen('event-summary');
            break;
          case '5':
            event.preventDefault();
            setCurrentScreen('overall-summary');
            break;
          case '6':
            event.preventDefault();
            setCurrentScreen('training-results');
            break;
          case '7':
            event.preventDefault();
            setCurrentScreen('logs');
            break;
          case '8':
            event.preventDefault();
            setCurrentScreen('settings');
            break;
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const renderScreen = () => {
    switch (currentScreen) {
      case 'home':
        return <Dashboard onNavigate={setCurrentScreen} />;
      case 'new-event':
        return <NewEvent />;
      case 'context-editor':
        return <ContextEditor />;
      case 'event-summary':
        return <EventSummary />;
      case 'overall-summary':
        return <OverallSummary />;
      case 'training-results':
        return <TrainingResults />;
      case 'logs':
        return <Logs />;
      case 'settings':
        return <Settings />;
      default:
        return <Dashboard onNavigate={setCurrentScreen} />;
    }
  };

  return (
    <ErrorBoundary>
      <ThemeProvider>
        <ToastProvider>
          <TopMenuBar currentScreen={currentScreen} onNavigate={setCurrentScreen} />
          <div className="bg-gray-50">
            {renderScreen()}
          </div>
        </ToastProvider>
      </ThemeProvider>
    </ErrorBoundary>
  );
};
