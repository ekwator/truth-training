import React from 'react';
import { getEmoji } from '@/utils/emojiMapping';
import { LocaleToggle } from './LocaleToggle';
import { useNavigationStore } from '@/stores/navigation';

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
  const { setViewJudgments } = useNavigationStore();
  return (
    <nav className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex space-x-1">
            <button
              onClick={() => onNavigate('home')}
              className={`px-3 py-2 text-sm font-medium ${
                currentScreen === 'home' ? 'border-b-2 border-blue-600 dark:border-blue-400 text-blue-600 dark:text-blue-400' : 'text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100'
              }`}
            >
              {getEmoji('navigation', 'home')} Dashboard
            </button>
            <button
              onClick={() => onNavigate('events')}
              className={`px-3 py-2 text-sm font-medium ${
                currentScreen === 'events' ? 'border-b-2 border-blue-600 dark:border-blue-400 text-blue-600 dark:text-blue-400' : 'text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100'
              }`}
            >
              {getEmoji('navigation', 'events')} View Events
            </button>
            <button
              onClick={() => {
                setViewJudgments(true);
                onNavigate('events');
              }}
              className={`px-3 py-2 text-sm font-medium ${
                currentScreen === 'judgments' ? 'border-b-2 border-blue-600 dark:border-blue-400 text-blue-600 dark:text-blue-400' : 'text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100'
              }`}
            >
              {getEmoji('navigation', 'judgments')} View Judgments
            </button>
            <button
              onClick={() => onNavigate('new-event')}
              className={`px-3 py-2 text-sm font-medium ${
                currentScreen === 'new-event' ? 'border-b-2 border-blue-600 dark:border-blue-400 text-blue-600 dark:text-blue-400' : 'text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100'
              }`}
            >
              {getEmoji('actions', 'create')} New Event
            </button>
            <button
              onClick={() => onNavigate('context-editor')}
              className={`px-3 py-2 text-sm font-medium ${
                currentScreen === 'context-editor' ? 'border-b-2 border-blue-600 dark:border-blue-400 text-blue-600 dark:text-blue-400' : 'text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100'
              }`}
            >
              {getEmoji('navigation', 'templates')} Manage Context Templates
            </button>
            <button
              onClick={() => onNavigate('overall-summary')}
              className={`px-3 py-2 text-sm font-medium ${
                currentScreen === 'overall-summary' ? 'border-b-2 border-blue-600 dark:border-blue-400 text-blue-600 dark:text-blue-400' : 'text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100'
              }`}
            >
              {getEmoji('navigation', 'summary')} Overall Summary
            </button>
            <button
              onClick={() => onNavigate('training-results')}
              className={`px-3 py-2 text-sm font-medium ${
                currentScreen === 'training-results' ? 'border-b-2 border-blue-600 dark:border-blue-400 text-blue-600 dark:text-blue-400' : 'text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100'
              }`}
            >
              {getEmoji('navigation', 'training')} Training Results
            </button>
            <button
              onClick={() => onNavigate('settings')}
              className={`px-3 py-2 text-sm font-medium ${
                currentScreen === 'settings' ? 'border-b-2 border-blue-600 dark:border-blue-400 text-blue-600 dark:text-blue-400' : 'text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100'
              }`}
            >
              {getEmoji('navigation', 'settings')} Settings
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
