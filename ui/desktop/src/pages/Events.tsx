import React, { useEffect, useState } from 'react';
import { useEventsStore } from '@/stores/events';
import { EventCard } from '@/components/Dashboard/EventCard';
import { CreateEventButton } from '@/components/Dashboard/CreateEventButton';
import { Screen } from '@/components/layout/TopMenuBar';

interface EventsProps {
  onNavigate?: (screen: Screen) => void;
}

export const Events: React.FC<EventsProps> = ({ onNavigate }) => {
  const { events, loading, error, fetchEvents, filters, setFilters } = useEventsStore();
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    fetchEvents();
  }, [fetchEvents]);

  const handleSearch = (term: string) => {
    setSearchTerm(term);
    setFilters({ search: term || undefined });
  };

  const filteredEvents = events.filter(event => {
    // Search filter
    if (searchTerm) {
      const searchLower = searchTerm.toLowerCase();
      if (!event.description.toLowerCase().includes(searchLower)) {
        return false;
      }
    }
    // Recognition filter (heuristic: look for keywords in description)
    if (filters.recognition) {
      const text = event.description.toLowerCase();
      if (!text.includes(filters.recognition)) return false;
    }
    // Detected filter
    if (filters.detected !== undefined) {
      if (event.detected !== filters.detected) return false;
    }
    return true;
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
          <h2 className="text-2xl font-bold text-red-600 mb-4">Error Loading Events</h2>
          <p className="text-gray-600 mb-4">{error}</p>
          <button
            onClick={() => fetchEvents()}
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
              <h1 className="text-2xl font-bold text-gray-900">Events</h1>
              <p className="text-sm text-gray-600">Manage and view all events</p>
            </div>
            <CreateEventButton />
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Search and Filters */}
        <div className="mb-6">
          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex flex-col sm:flex-row gap-4">
              <div className="flex-1">
                <label htmlFor="search" className="block text-sm font-medium text-gray-700 mb-2">
                  Search Events
                </label>
                <input
                  type="text"
                  id="search"
                  value={searchTerm}
                  onChange={(e) => handleSearch(e.target.value)}
                  placeholder="Search by description..."
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                />
              </div>
              <div className="sm:w-48">
                <label htmlFor="detected" className="block text-sm font-medium text-gray-700 mb-2">
                  Detected
                </label>
                <select
                  id="detected"
                  value={filters.detected === undefined ? '' : filters.detected ? 'true' : 'false'}
                  onChange={(e) => {
                    const value = e.target.value;
                    setFilters({ detected: value === '' ? undefined : value === 'true' });
                  }}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value="">All</option>
                  <option value="true">Detected</option>
                  <option value="false">Not Detected</option>
                </select>
              </div>
              <div className="sm:w-48">
                <label htmlFor="recognition" className="block text-sm font-medium text-gray-700 mb-2">
                  Recognition
                </label>
                <select
                  id="recognition"
                  value={filters.recognition || ''}
                  onChange={(e) => setFilters({ recognition: (e.target.value as any) || undefined })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value="">All</option>
                  <option value="confirm">Confirm</option>
                  <option value="reject">Reject</option>
                  <option value="abstain">Abstain</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        {/* Events Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredEvents.length === 0 ? (
            <div className="col-span-full">
              <div className="bg-white rounded-lg shadow p-12 text-center">
                <div className="text-gray-400 mb-4 text-sm">No data</div>
                <h3 className="text-lg font-medium text-gray-900 mb-2">
                  {searchTerm ? 'No events match your search' : 'No events yet'}
                </h3>
                <p className="text-gray-500 mb-4">
                  {searchTerm ? 'Try adjusting your search terms.' : 'Get started by creating your first event.'}
                </p>
                {!searchTerm && <CreateEventButton />}
              </div>
            </div>
          ) : (
            filteredEvents.map((event) => (
              <EventCard key={event.id} event={event} onNavigate={onNavigate} />
            ))
          )}
        </div>

        {/* Pagination */}
        {filteredEvents.length > 0 && (
          <div className="mt-8 flex justify-center">
            <nav className="flex items-center space-x-2">
              <button className="px-3 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50">
                Previous
              </button>
              <span className="px-3 py-2 text-sm font-medium text-gray-700 bg-blue-50 border border-blue-300 rounded-md">
                1
              </span>
              <button className="px-3 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50">
                Next
              </button>
            </nav>
          </div>
        )}
      </main>
    </div>
  );
};
