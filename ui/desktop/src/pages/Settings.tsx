/**
 * Settings Screen
 * Matches Android Settings screen layout and behavior.
 * Localization toggle removed (English-only interface).
 * Route: settings
 */

import React from 'react';
import { getEmoji } from '@/utils/emojiMapping';

export const Settings: React.FC = () => {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-6">
        {getEmoji('screens', 'settings')} Settings
      </h1>
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6">
        <p className="text-gray-600 dark:text-gray-400">{getEmoji('status', 'info')} Settings configuration will be displayed here</p>
        <p className="text-sm text-gray-500 dark:text-gray-400 mt-2">{getEmoji('status', 'info')} Note: Localization toggle removed (English-only interface)</p>
      </div>
    </div>
  );
};

