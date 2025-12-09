/**
 * Training Results Screen
 * Matches Android Training Results screen layout and behavior.
 * Displays training progress and results with metrics visualization.
 * Route: training-results
 * Reference: spec/23-function_desktop.md lines 243-261
 */

import React, { useEffect, useState } from 'react';
import { ApiService } from '@/services/api';
import { getEmoji } from '@/utils/emojiMapping';

export const TrainingResults: React.FC = () => {
  const [metrics, setMetrics] = useState<{
    total_events: number;
    average_impact_level: number;
    last_updated?: string;
  } | null>(null);
  const [eventRows, setEventRows] = useState<Array<{
    event: string;
    summary: string;
    impact?: number;
    date: string;
  }>>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadTrainingData();
  }, []);

  const loadTrainingData = async () => {
    setLoading(true);
    try {
      const [metricsData, rowsData] = await Promise.all([
        ApiService.getOverallMetrics(),
        ApiService.getEventRows(),
      ]);
      setMetrics(metricsData);
      setEventRows(rowsData);
    } catch (error) {
      console.error('Failed to load training data:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="text-center py-8 text-gray-700 dark:text-gray-300">
          {getEmoji('status', 'syncing')} Loading training results...
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-6">
        {getEmoji('screens', 'trainingResults')} Training Results
      </h1>

      {/* Progress Metrics */}
      {metrics && (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 mb-6">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
            {getEmoji('status', 'success')} Progress Metrics
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="text-center">
              <div className="text-3xl font-bold text-blue-600 dark:text-blue-400">
                {metrics.total_events}
              </div>
              <div className="text-sm text-gray-600 dark:text-gray-400 mt-1">Total Events</div>
            </div>
            <div className="text-center">
              <div className="text-3xl font-bold text-green-600 dark:text-green-400">
                {metrics.average_impact_level.toFixed(2)}
              </div>
              <div className="text-sm text-gray-600 dark:text-gray-400 mt-1">Average Impact Level</div>
            </div>
            {metrics.last_updated && (
              <div className="text-center">
                <div className="text-sm font-semibold text-gray-700 dark:text-gray-300">
                  {new Date(metrics.last_updated).toLocaleString()}
                </div>
                <div className="text-sm text-gray-600 dark:text-gray-400 mt-1">Last Updated</div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Results Table */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
          {getEmoji('navigation', 'events')} Training Results
        </h2>
        {eventRows.length === 0 ? (
          <div className="text-center py-8 text-gray-500 dark:text-gray-400">
            {getEmoji('status', 'warning')} No training results available
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                    Event
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                    Summary
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                    Impact
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                    Date
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                {eventRows.map((row, index) => (
                  <tr key={index} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-gray-100">
                      {row.event}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600 dark:text-gray-400">
                      {row.summary}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-400">
                      {row.impact !== undefined ? row.impact.toFixed(2) : '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-400">
                      {row.date}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

