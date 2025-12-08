import React, { useState, useCallback, useEffect } from 'react';
import { t } from '@/i18n';

export const TrainingResults: React.FC = () => {
  const [dateRange, setDateRange] = useState({
    start: '',
    end: '',
  });
  const [contextFilter, setContextFilter] = useState('');
  
  // Suppress unused warnings - reserved for future use
  void setDateRange;
  void setContextFilter;
  const [impactProgress] = useState(50); // percentage
  const [averageScore] = useState(3.7);
  const [loading, setLoading] = useState(false);

  const fetchResults = useCallback(async () => {
    setLoading(true);
    try {
      // TODO: wire to Tauri command
      await new Promise(resolve => setTimeout(resolve, 500)); // mock delay
      console.log('Fetching results with filters:', { dateRange, contextFilter });
    } catch (error) {
      console.error('Failed to fetch results:', error);
    } finally {
      setLoading(false);
    }
  }, [dateRange, contextFilter]);

  useEffect(() => {
    fetchResults();
  }, [fetchResults]);

  const handleUpdate = () => {
    fetchResults();
  };

  // Reserved for future use
  // const handleResetFilters = () => {
  //   setDateRange({ start: '', end: ''});
  //   setContextFilter('');
  // };

  // const renderProgressBar = (percentage: number) => {
  //   const filled = Math.round(percentage / 10);
  //   const empty = 10 - filled;
  //   return `[${'#'.repeat(filled)}${'-'.repeat(empty)}] ${percentage}%`;
  // };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Top App Bar */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <h1 className="text-2xl font-bold text-gray-900">{t('training.title')}</h1>
            <button
              onClick={handleUpdate}
              disabled={loading}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              {t('training.refresh')}
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Progress Metrics Card */}
        <div className="bg-white shadow rounded-lg mb-6">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-medium text-gray-900">{t('training.progressMetrics')}</h2>
          </div>
          <div className="px-6 py-4">
            <div className="space-y-2 text-sm">
              <div>• {t('training.totalEvents')}: -</div>
              <div>• {t('training.totalPositiveImpact')}: -</div>
              <div>• {t('training.totalNegativeImpact')}: -</div>
              <div>• {t('training.averageScore')}: {averageScore.toFixed(1)}</div>
              <div>• {t('training.trendIndicator')}: -</div>
            </div>
          </div>
        </div>

        {/* Impact Progress */}
        <div className="bg-white shadow rounded-lg mb-6">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-medium text-gray-900">{t('training.impactProgress')}</h2>
          </div>
          <div className="px-6 py-4">
            <div className="text-sm mb-2">{t('training.progressPercentage')}: {impactProgress}%</div>
            <div className="w-full bg-gray-200 rounded-full h-4">
              <div
                className="bg-blue-600 h-4 rounded-full"
                style={{ width: `${impactProgress}%` }}
              ></div>
            </div>
          </div>
        </div>

        {/* Results Table */}
        <div className="bg-white shadow rounded-lg">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-medium text-gray-900">{t('training.resultsTable')}</h2>
          </div>
          <div className="px-6 py-4">
            <div className="text-sm text-gray-600">{t('training.noData')}</div>
          </div>
        </div>
      </main>
    </div>
  );
};
