/**
 * Overall Summary Screen
 * Matches Android Overall Summary screen layout and behavior.
 * Route: overall-summary
 */

import React, { useEffect, useState } from 'react';
import { ApiService } from '@/services/api';
import { getEmoji } from '@/utils/emojiMapping';

export const OverallSummary: React.FC = () => {
  const [metrics, setMetrics] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadMetrics();
  }, []);

  const loadMetrics = async () => {
    setLoading(true);
    try {
      const response = await ApiService.getOverallMetrics();
      setMetrics(response);
    } catch (error) {
      console.error('Failed to load metrics:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-6">
        {getEmoji('screens', 'overallSummary')} Overall Summary
      </h1>

      {loading ? (
        <div className="text-center py-8 text-gray-700 dark:text-gray-300">{getEmoji('status', 'syncing')} Loading metrics...</div>
      ) : metrics ? (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Total Events</p>
              <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{metrics.total_events || 0}</p>
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Average Impact</p>
              <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{metrics.avg_impact || 0}</p>
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Last Updated</p>
              <p className="text-sm text-gray-900 dark:text-gray-100">{metrics.last_updated || 'N/A'}</p>
            </div>
          </div>
        </div>
      ) : (
        <div className="text-center py-8 text-gray-500 dark:text-gray-400">{getEmoji('status', 'warning')} No metrics available</div>
      )}
    </div>
  );
};

