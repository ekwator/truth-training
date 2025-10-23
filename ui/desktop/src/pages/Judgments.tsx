import React, { useEffect, useState } from 'react';
import { useJudgmentsStore } from '@/stores/judgments';
import { JudgmentCard } from '@/components/JudgmentPanel/JudgmentCard';

export const Judgments: React.FC = () => {
  const { judgments, loading, error, fetchJudgments, filters, setFilters } = useJudgmentsStore();
  const [selectedEventId, setSelectedEventId] = useState<string>('');

  useEffect(() => {
    fetchJudgments();
  }, [fetchJudgments]);

  const handleEventFilter = (eventId: string) => {
    setSelectedEventId(eventId);
    setFilters({ event_id: eventId || undefined });
  };

  const filteredJudgments = judgments.filter(judgment => {
    if (!selectedEventId) return true;
    return judgment.event_id === selectedEventId;
  });

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-red-600 mb-4">Error Loading Judgments</h2>
          <p className="text-gray-600 mb-4">{error}</p>
          <button
            onClick={() => fetchJudgments()}
            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Judgments</h1>
              <p className="text-sm text-gray-600">View and manage all judgments</p>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Filters */}
        <div className="mb-6">
          <div className="bg-white rounded-lg shadow p-6">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label htmlFor="event-filter" className="block text-sm font-medium text-gray-700 mb-2">
                  Filter by Event
                </label>
                <select
                  id="event-filter"
                  value={selectedEventId}
                  onChange={(e) => handleEventFilter(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value="">All Events</option>
                  {/* This would be populated with actual events */}
                  <option value="event-1">Sample Event 1</option>
                  <option value="event-2">Sample Event 2</option>
                </select>
              </div>
              <div>
                <label htmlFor="assessment-filter" className="block text-sm font-medium text-gray-700 mb-2">
                  Assessment
                </label>
                <select
                  id="assessment-filter"
                  value={filters.assessment || ''}
                  onChange={(e) => setFilters({ assessment: e.target.value as any || undefined })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value="">All Assessments</option>
                  <option value="true">True</option>
                  <option value="false">False</option>
                  <option value="uncertain">Uncertain</option>
                </select>
              </div>
              <div>
                <label htmlFor="confidence-filter" className="block text-sm font-medium text-gray-700 mb-2">
                  Confidence Level
                </label>
                <select
                  id="confidence-filter"
                  value={filters.confidence_min || ''}
                  onChange={(e) => setFilters({ confidence_min: e.target.value ? parseFloat(e.target.value) : undefined })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value="">All Levels</option>
                  <option value="0.8">High (0.8+)</option>
                  <option value="0.5">Medium (0.5+)</option>
                  <option value="0.2">Low (0.2+)</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        {/* Judgments List */}
        <div className="bg-white shadow rounded-lg">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-medium text-gray-900">
              Judgments ({filteredJudgments.length})
            </h2>
          </div>
          <div className="divide-y divide-gray-200">
            {filteredJudgments.length === 0 ? (
              <div className="px-6 py-12 text-center">
                <div className="text-gray-400 mb-4">
                  <svg className="mx-auto h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <h3 className="text-lg font-medium text-gray-900 mb-2">No judgments found</h3>
                <p className="text-gray-500">
                  {selectedEventId ? 'No judgments for the selected event.' : 'No judgments have been submitted yet.'}
                </p>
              </div>
            ) : (
              filteredJudgments.map((judgment) => (
                <JudgmentCard key={judgment.id} judgment={judgment} />
              ))
            )}
          </div>
        </div>

        {/* Statistics */}
        {filteredJudgments.length > 0 && (
          <div className="mt-8 grid grid-cols-1 md:grid-cols-4 gap-6">
            <div className="bg-white rounded-lg shadow p-6">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <div className="w-8 h-8 bg-green-500 rounded-full flex items-center justify-center">
                    <span className="text-white text-sm font-medium">T</span>
                  </div>
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-500">True</p>
                  <p className="text-2xl font-semibold text-gray-900">
                    {filteredJudgments.filter(j => j.assessment === 'true').length}
                  </p>
                </div>
              </div>
            </div>

            <div className="bg-white rounded-lg shadow p-6">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <div className="w-8 h-8 bg-red-500 rounded-full flex items-center justify-center">
                    <span className="text-white text-sm font-medium">F</span>
                  </div>
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-500">False</p>
                  <p className="text-2xl font-semibold text-gray-900">
                    {filteredJudgments.filter(j => j.assessment === 'false').length}
                  </p>
                </div>
              </div>
            </div>

            <div className="bg-white rounded-lg shadow p-6">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <div className="w-8 h-8 bg-yellow-500 rounded-full flex items-center justify-center">
                    <span className="text-white text-sm font-medium">U</span>
                  </div>
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-500">Uncertain</p>
                  <p className="text-2xl font-semibold text-gray-900">
                    {filteredJudgments.filter(j => j.assessment === 'uncertain').length}
                  </p>
                </div>
              </div>
            </div>

            <div className="bg-white rounded-lg shadow p-6">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center">
                    <span className="text-white text-sm font-medium">A</span>
                  </div>
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-500">Avg Confidence</p>
                  <p className="text-2xl font-semibold text-gray-900">
                    {filteredJudgments.length > 0 
                      ? (filteredJudgments.reduce((sum, j) => sum + j.confidence_level, 0) / filteredJudgments.length).toFixed(2)
                      : '0.00'
                    }
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};
