import React, { useState, useEffect, useRef } from 'react';
import { ErrorBoundary } from '@/components/system/ErrorBoundary';
import { ToastProvider } from '@/components/system/Toaster';
import { ThemeProvider } from '@/components/system/ThemeProvider';
import { TopMenuBar, Screen } from '@/components/layout/TopMenuBar';
import { Dashboard } from '@/pages/Dashboard';
import { NewEvent } from '@/pages/NewEvent';
import { ContextEditor } from '@/pages/ContextEditor';
import { EventSummary } from '@/pages/EventSummary';
import { Events } from '@/pages/Events';
import { Judgments } from '@/pages/Judgments';
import { OverallSummary } from '@/pages/OverallSummary';
import { TrainingResults } from '@/pages/TrainingResults';
import { Settings } from '@/pages/Settings';
import { initializeLocale } from '@/i18n';
import { useNavigationStore } from '@/stores/navigation';

// Navigation state for passing parameters between screens
interface NavigationState {
  eventId?: number;
  [key: string]: any;
}

export const App: React.FC = () => {
  const [currentScreen, setCurrentScreen] = useState<Screen>('home');
  const [navigationState, setNavigationState] = useState<NavigationState>({});
  const navigationStack = useRef<Screen[]>(['home']);
  const { viewJudgments, selectedEventIdForJudgments } = useNavigationStore();

  // Initialize locale on app start
  useEffect(() => {
    initializeLocale().catch((error) => {
      console.error('Failed to initialize locale:', error);
    });
  }, []);

  // Navigation handler with back stack support
  const handleNavigate = (screen: Screen, state?: NavigationState) => {
    // Add current screen to back stack (unless it's the same screen)
    if (currentScreen !== screen) {
      navigationStack.current.push(currentScreen);
      // Limit stack size to prevent memory issues
      if (navigationStack.current.length > 10) {
        navigationStack.current.shift();
      }
    }
    
    setCurrentScreen(screen);
    if (state) {
      setNavigationState(state);
    }
  };

  // Back navigation handler
  const handleBack = () => {
    if (navigationStack.current.length > 1) {
      navigationStack.current.pop(); // Remove current screen
      const previousScreen = navigationStack.current[navigationStack.current.length - 1];
      setCurrentScreen(previousScreen);
      setNavigationState({});
    } else {
      // If no back stack, go to home
      setCurrentScreen('home');
      navigationStack.current = ['home'];
      setNavigationState({});
    }
  };

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // Back navigation with Escape key
      if (event.key === 'Escape' && currentScreen !== 'home') {
        event.preventDefault();
        handleBack();
        return;
      }

      // Desktop-specific feature: Keyboard shortcuts (Alt+1 through Alt+8)
      // This is a Desktop-only feature, not present in Android UI
      // Preserved during UI reconstruction to maintain Desktop-specific functionality
      if (event.altKey) {
        switch (event.key) {
          case '1':
            event.preventDefault();
            handleNavigate('home');
            break;
          case '2':
            event.preventDefault();
            handleNavigate('new-event');
            break;
          case '3':
            event.preventDefault();
            handleNavigate('context-editor');
            break;
          case '4':
            event.preventDefault();
            handleNavigate('event-summary');
            break;
          case '5':
            event.preventDefault();
            handleNavigate('overall-summary');
            break;
          case '6':
            event.preventDefault();
            handleNavigate('training-results');
            break;
          case '8':
            event.preventDefault();
            handleNavigate('settings');
            break;
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [currentScreen]);

  const renderScreen = () => {
    // Flag-based conditional routing (Android savedStateHandle equivalent)
    // If viewJudgments is true and we're navigating to events, show judgments instead
    // But only if we have an eventId - otherwise show events list for selection
    if (viewJudgments && currentScreen === 'events' && (navigationState.eventId || selectedEventIdForJudgments)) {
      const eventId = navigationState.eventId || (selectedEventIdForJudgments ? parseInt(selectedEventIdForJudgments) : undefined);
      return <Judgments eventId={eventId} onNavigate={handleNavigate} />;
    }

    // If selectTemplateForEvent is true and we're in context-editor, handle template selection mode
    // (This is already handled in ContextEditor component, but we ensure navigation works)

    switch (currentScreen) {
      case 'home':
        return <Dashboard onNavigate={handleNavigate} />;
      case 'new-event':
        return <NewEvent onNavigate={handleNavigate} />;
      case 'context-editor':
        return <ContextEditor onNavigate={handleNavigate} />;
      case 'event-summary':
        return <EventSummary eventId={navigationState.eventId} onNavigate={handleNavigate} />;
      case 'events':
        // Show events list - if viewJudgments is true, clicking an event will navigate to judgments
        return <Events onNavigate={handleNavigate} navigationState={navigationState} />;
      case 'judgments': {
        // Explicit judgments screen (can be navigated to directly)
        // Use eventId from navigation state or from selectedEventIdForJudgments
        const eventId = navigationState.eventId || (selectedEventIdForJudgments ? parseInt(selectedEventIdForJudgments) : undefined);
        return <Judgments eventId={eventId} onNavigate={handleNavigate} />;
      }
      case 'overall-summary':
        return <OverallSummary />;
      case 'training-results':
        return <TrainingResults />;
      case 'settings':
        return <Settings />;
      default:
        return <Dashboard onNavigate={handleNavigate} />;
    }
  };

  return (
    <ErrorBoundary>
      <ThemeProvider>
        <ToastProvider>
          <TopMenuBar currentScreen={currentScreen} onNavigate={setCurrentScreen} />
          <div className="bg-gray-900 dark:bg-gray-900">
            {renderScreen()}
          </div>
        </ToastProvider>
      </ThemeProvider>
    </ErrorBoundary>
  );
};
