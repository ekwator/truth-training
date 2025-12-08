import React from 'react';
import { LocaleToggle } from './LocaleToggle';
import { t } from '@/i18n';

export type Screen = 'home' | 'new-event' | 'context-editor' | 'event-summary' | 'events' | 'judgments' | 'overall-summary' | 'training-results' | 'settings';

interface NavigationState {
  eventId?: number;
  [key: string]: any;
}

interface TopMenuBarProps {
  currentScreen: Screen;
  onNavigate: (screen: Screen, state?: NavigationState) => void;
}

export const TopMenuBar: React.FC<TopMenuBarProps> = ({ currentScreen, onNavigate }) => {
  // Back navigation handler
  // @ts-ignore - Reserved for future use
  const handleBack = () => {
    // Navigate to previous screen (App.tsx will handle back stack)
    // For now, just go to home if not already there
    if (currentScreen !== 'home') {
      onNavigate('home');
    }
  };
  return (
    <nav className="bg-white border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex space-x-1">
            <button
              onClick={() => onNavigate('home')}
              className={`px-3 py-2 text-sm font-medium ${
                currentScreen === 'home' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-700 hover:text-gray-900'
              }`}
            >
              {t('navigation.dashboard')}
            </button>
            <button
              onClick={() => onNavigate('new-event')}
              className={`px-3 py-2 text-sm font-medium ${
                currentScreen === 'new-event' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-700 hover:text-gray-900'
              }`}
            >
              {t('events.createEvent')}
            </button>
            <button
              onClick={() => onNavigate('context-editor')}
              className={`px-3 py-2 text-sm font-medium ${
                currentScreen === 'context-editor' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-700 hover:text-gray-900'
              }`}
            >
              Context Editor
            </button>
            <button
              onClick={() => onNavigate('event-summary')}
              className={`px-3 py-2 text-sm font-medium ${
                currentScreen === 'event-summary' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-700 hover:text-gray-900'
              }`}
            >
              Event Summary
            </button>
            <button
              onClick={() => onNavigate('overall-summary')}
              className={`px-3 py-2 text-sm font-medium ${
                currentScreen === 'overall-summary' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-700 hover:text-gray-900'
              }`}
            >
              Overall Summary
            </button>
            <button
              onClick={() => onNavigate('training-results')}
              className={`px-3 py-2 text-sm font-medium ${
                currentScreen === 'training-results' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-700 hover:text-gray-900'
              }`}
            >
              Training Results
            </button>
            <button
              onClick={() => onNavigate('settings')}
              className={`px-3 py-2 text-sm font-medium ${
                currentScreen === 'settings' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-700 hover:text-gray-900'
              }`}
            >
              {t('navigation.settings')}
            </button>
          </div>
          <div className="flex items-center">
            <LocaleToggle variant="button" />
          </div>
        </div>
      </div>
    </nav>
  );
};
