import React from 'react';

export type Screen = 'home' | 'new-event' | 'event-summary' | 'overall-summary' | 'training-results' | 'logs' | 'settings';

interface TopMenuBarProps {
  currentScreen: Screen;
  onNavigate: (screen: Screen) => void;
}

export const TopMenuBar: React.FC<TopMenuBarProps> = ({ currentScreen, onNavigate }) => {
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
              Home
            </button>
            <button
              onClick={() => onNavigate('new-event')}
              className={`px-3 py-2 text-sm font-medium ${
                currentScreen === 'new-event' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-700 hover:text-gray-900'
              }`}
            >
              New Event
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
              onClick={() => onNavigate('logs')}
              className={`px-3 py-2 text-sm font-medium ${
                currentScreen === 'logs' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-700 hover:text-gray-900'
              }`}
            >
              Logs
            </button>
            <button
              onClick={() => onNavigate('settings')}
              className={`px-3 py-2 text-sm font-medium ${
                currentScreen === 'settings' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-700 hover:text-gray-900'
              }`}
            >
              Settings
            </button>
          </div>
        </div>
      </div>
    </nav>
  );
};
