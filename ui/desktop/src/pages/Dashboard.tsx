/**
 * Dashboard Screen
 * Matches Android Dashboard screen layout and behavior.
 * Route: dashboard (home)
 */

import React, { useEffect } from 'react';
import { Screen } from '@/components/layout/TopMenuBar';
import { useSyncStore } from '@/stores/sync';
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
  const [totalEvents, setTotalEvents] = React.useState<number>(0);
  const [averageImpact, setAverageImpact] = React.useState<number>(0);
  const [loading, setLoading] = React.useState(true);

  useEffect(() => {
    // Load sync status and quick stats on mount
    fetchSyncStatus();
    loadQuickStats();
  }, []);

  const loadQuickStats = async () => {
    try {
      setLoading(true);
      const metrics = await ApiService.getOverallMetrics();
      setTotalEvents(metrics.total_events || 0);
      setAverageImpact(metrics.average_impact_level || 0);
    } catch (error) {
      console.error('Failed to load quick stats:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleViewEvents = () => {
    onNavigate('events');
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
        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
          {getEmoji('status', 'success')} Quick Stats
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <button
            onClick={handleViewEvents}
            className="text-left p-4 bg-blue-50 dark:bg-blue-900 rounded-lg hover:bg-blue-100 dark:hover:bg-blue-800 transition-colors"
          >
            <div className="text-3xl font-bold text-blue-600 dark:text-blue-400">
              {loading ? '...' : totalEvents}
            </div>
            <div className="text-sm text-gray-600 dark:text-gray-400 mt-1">
              {getEmoji('navigation', 'events')} Total Events
            </div>
          </button>
          <div className="text-left p-4 bg-green-50 dark:bg-green-900 rounded-lg">
            <div className="text-3xl font-bold text-green-600 dark:text-green-400">
              {loading ? '...' : averageImpact.toFixed(2)}
            </div>
            <div className="text-sm text-gray-600 dark:text-gray-400 mt-1">
              {getEmoji('status', 'success')} Average Impact Level
            </div>
          </div>
        </div>
      </div>

    </div>
  );
};

