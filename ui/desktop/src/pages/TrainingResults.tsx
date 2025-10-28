import React, { useState, useCallback, useEffect } from 'react';

export const TrainingResults: React.FC = () => {
  const [dateRange, setDateRange] = useState({
    start: '',
    end: '',
  });
  const [contextFilter, setContextFilter] = useState('');
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

  const handleResetFilters = () => {
    setDateRange({ start: '', end: ''});
    setContextFilter('');
  };

  const renderProgressBar = (percentage: number) => {
    const filled = Math.round(percentage / 10);
    const empty = 10 - filled;
    return `[${'#'.repeat(filled)}${'-'.repeat(empty)}] ${percentage}%`;
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-4xl mx-auto px-4">
        <h1 className="text-3xl font-bold mb-6">Training Results Overview</h1>

        {/* Filters */}
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h2 className="text-lg font-semibold mb-4">Filters</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
            <div>
              <label className="block text-sm font-medium mb-2">Date Range Start</label>
              <input
                type="date"
                value={dateRange.start}
                onChange={(e) => setDateRange({ ...dateRange, start: e.target.value })}
                className="w-full px-3 py-2 border rounded"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-2">Date Range End</label>
              <input
                type="date"
                value={dateRange.end}
                onChange={(e) => setDateRange({ ...dateRange, end: e.target.value })}
                className="w-full px-3 py-2 border rounded"
              />
            </div>
          </div>
          <div className="mb-4">
            <label className="block text-sm font-medium mb-2">Context Filter</label>
            <input
              type="text"
              value={contextFilter}
              onChange={(e) => setContextFilter(e.target.value)}
              placeholder="Filter by context..."
              className="w-full px-3 py-2 border rounded"
            />
          </div>
          <div className="flex gap-2">
            <button
              onClick={handleUpdate}
              disabled={loading}
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
            >
              Update
            </button>
            <button
              onClick={handleResetFilters}
              className="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300"
            >
              Reset Filters
            </button>
          </div>
        </div>

        {/* ASCII-style results */}
        {loading ? (
          <div className="bg-white rounded-lg shadow p-12 text-center">
            <p className="text-gray-600">Loading...</p>
          </div>
        ) : (
          <div className="bg-white rounded-lg shadow p-6 space-y-6">
            <div>
              <h3 className="text-md font-semibold mb-2">Impact Progress</h3>
              <pre className="bg-gray-50 p-4 rounded border font-mono text-sm">
                {renderProgressBar(impactProgress)}
              </pre>
            </div>
            <div>
              <h3 className="text-md font-semibold mb-2">Average Score</h3>
              <p className="text-2xl font-bold">{averageScore} / 5</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
