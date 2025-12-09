/**
 * Dashboard Screen
 * Matches Android Dashboard screen layout and behavior.
 * Route: dashboard (home)
 */

import React, { useEffect } from 'react';
import { Screen } from '@/components/layout/TopMenuBar';
import { useSyncStore } from '@/stores/sync';
import { useNavigationStore } from '@/stores/navigation';
import { ApiService } from '@/services/api';
import { getEmoji } from '@/utils/emojiMapping';

interface NavigationState {
  eventId?: number;
  [key: string]: any;
}

interface DashboardProps {
  onNavigate: (screen: Screen, state?: NavigationState) => void;
}

export const Dashboard: React.FC<DashboardProps> = ({ onNavigate }) => {
  const { 
    isOnline, 
    lastSync, 
    pendingOperations, 
    syncInProgress,
    fetchSyncStatus,
    startSync 
  } = useSyncStore();
  const { setViewJudgments } = useNavigationStore();
  const [totalEvents, setTotalEvents] = React.useState<number>(0);
  const [loading, setLoading] = React.useState(true);

  useEffect(() => {
    // Load sync status and event count on mount
    fetchSyncStatus();
    loadEventCount();
  }, []);

  const loadEventCount = async () => {
    try {
      setLoading(true);
      const response = await ApiService.getEvents(1, 1);
      setTotalEvents(response.pagination?.total || 0);
    } catch (error) {
      console.error('Failed to load event count:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleViewEvents = () => {
    onNavigate('events');
  };

  const handleViewJudgments = () => {
    setViewJudgments(true);
    onNavigate('events');
  };

  const handleNewEvent = () => {
    onNavigate('new-event');
  };

  const handleManageTemplates = () => {
    onNavigate('context-editor');
  };

  const handleOverallSummary = () => {
    onNavigate('overall-summary');
  };

  const handleTrainingResults = () => {
    onNavigate('training-results');
  };

  const handleSettings = () => {
    onNavigate('settings');
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-6">
        {getEmoji('screens', 'dashboard')} Dashboard
      </h1>

      {/* Sync Status Card */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 mb-6">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Sync Status</h2>
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <div className={`w-3 h-3 rounded-full ${isOnline ? 'bg-green-500' : 'bg-red-500'}`} />
            <span className="text-sm text-gray-700 dark:text-gray-300">
              {isOnline ? 'Online' : 'Offline'}
            </span>
            {lastSync && (
              <span className="text-sm text-gray-500 dark:text-gray-400">
                Last sync: {new Date(lastSync).toLocaleString()}
              </span>
            )}
            {pendingOperations > 0 && (
              <span className="text-sm text-yellow-600 dark:text-yellow-400">
                {pendingOperations} pending operations
              </span>
            )}
          </div>
          <button
            onClick={startSync}
            disabled={syncInProgress || !isOnline}
            className="px-4 py-2 bg-blue-600 dark:bg-blue-500 text-white rounded hover:bg-blue-700 dark:hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {getEmoji('actions', 'sync')} {syncInProgress ? 'Syncing...' : 'Sync'}
          </button>
        </div>
      </div>

      {/* Quick Stats */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 mb-6">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Quick Stats</h2>
        <div className="flex items-center space-x-4">
          <button
            onClick={handleViewEvents}
            className="text-2xl font-bold text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 cursor-pointer"
          >
            {loading ? '...' : totalEvents} Events
          </button>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <button
          onClick={handleViewEvents}
          className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 text-left hover:shadow-md dark:hover:shadow-gray-600 transition-shadow"
        >
          <div className="text-2xl mb-2">{getEmoji('navigation', 'events')}</div>
          <div className="font-semibold text-gray-900 dark:text-gray-100">View Events</div>
          <div className="text-sm text-gray-500 dark:text-gray-400 mt-1">View all events</div>
        </button>

        <button
          onClick={handleViewJudgments}
          className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 text-left hover:shadow-md dark:hover:shadow-gray-600 transition-shadow"
        >
          <div className="text-2xl mb-2">{getEmoji('navigation', 'judgments')}</div>
          <div className="font-semibold text-gray-900 dark:text-gray-100">View Judgments</div>
          <div className="text-sm text-gray-500 dark:text-gray-400 mt-1">View event judgments</div>
        </button>

        <button
          onClick={handleNewEvent}
          className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 text-left hover:shadow-md dark:hover:shadow-gray-600 transition-shadow"
        >
          <div className="text-2xl mb-2">{getEmoji('actions', 'create')}</div>
          <div className="font-semibold text-gray-900 dark:text-gray-100">New Event</div>
          <div className="text-sm text-gray-500 dark:text-gray-400 mt-1">Create a new event</div>
        </button>

        <button
          onClick={handleManageTemplates}
          className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 text-left hover:shadow-md dark:hover:shadow-gray-600 transition-shadow"
        >
          <div className="text-2xl mb-2">{getEmoji('navigation', 'templates')}</div>
          <div className="font-semibold text-gray-900 dark:text-gray-100">Manage Context Templates</div>
          <div className="text-sm text-gray-500 dark:text-gray-400 mt-1">Create and edit templates</div>
        </button>

        <button
          onClick={handleOverallSummary}
          className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 text-left hover:shadow-md dark:hover:shadow-gray-600 transition-shadow"
        >
          <div className="text-2xl mb-2">{getEmoji('navigation', 'summary')}</div>
          <div className="font-semibold text-gray-900 dark:text-gray-100">Overall Summary</div>
          <div className="text-sm text-gray-500 dark:text-gray-400 mt-1">View overall statistics</div>
        </button>

        <button
          onClick={handleTrainingResults}
          className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 text-left hover:shadow-md dark:hover:shadow-gray-600 transition-shadow"
        >
          <div className="text-2xl mb-2">{getEmoji('navigation', 'training')}</div>
          <div className="font-semibold text-gray-900 dark:text-gray-100">Training Results</div>
          <div className="text-sm text-gray-500 dark:text-gray-400 mt-1">View training metrics</div>
        </button>

        <button
          onClick={handleSettings}
          className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 text-left hover:shadow-md dark:hover:shadow-gray-600 transition-shadow"
        >
          <div className="text-2xl mb-2">{getEmoji('navigation', 'settings')}</div>
          <div className="font-semibold text-gray-900 dark:text-gray-100">Settings</div>
          <div className="text-sm text-gray-500 dark:text-gray-400 mt-1">Configure application</div>
        </button>
      </div>
    </div>
  );
};

