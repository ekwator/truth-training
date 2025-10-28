import React, { useState, useEffect } from 'react';
import { ErrorBoundary } from '@/components/system/ErrorBoundary';
import { ToastProvider } from '@/components/system/Toaster';
import { ThemeProvider } from '@/components/system/ThemeProvider';
import { TopMenuBar, Screen } from '@/components/layout/TopMenuBar';
import { Dashboard } from '@/pages/Dashboard';
import { NewEvent } from '@/pages/NewEvent';
import { EventSummary } from '@/pages/EventSummary';
import { OverallSummary } from '@/pages/OverallSummary';
import { TrainingResults } from '@/pages/TrainingResults';
import { Logs } from '@/pages/Logs';
import { Settings } from '@/pages/Settings';

export const App: React.FC = () => {
  const [currentScreen, setCurrentScreen] = useState<Screen>('home');

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
            setCurrentScreen('event-summary');
            break;
          case '4':
            event.preventDefault();
            setCurrentScreen('overall-summary');
            break;
          case '5':
            event.preventDefault();
            setCurrentScreen('training-results');
            break;
          case '6':
            event.preventDefault();
            setCurrentScreen('logs');
            break;
          case '7':
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
        return <Dashboard />;
      case 'new-event':
        return <NewEvent />;
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
        return <Dashboard />;
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
