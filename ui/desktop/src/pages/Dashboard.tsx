import React, { useEffect } from 'react';
import { useEventsStore } from '@/stores/events';
import { useSyncStore } from '@/stores/sync';
import { useToast } from '@/components/system/Toaster';
import { SyncStatus } from '@/components/system/SyncStatus';
import { EventCard } from '@/components/Dashboard/EventCard';
import { CreateEventButton } from '@/components/Dashboard/CreateEventButton';
import { Screen } from '@/components/layout/TopMenuBar';

interface DashboardProps {
  onNavigate?: (screen: Screen) => void;
}

export const Dashboard: React.FC<DashboardProps> = ({ onNavigate }) => {
  const { events, loading, error, fetchEvents } = useEventsStore();
  const { syncStatus, isOnline, pendingOperations, fetchSyncStatus } = useSyncStore();
  const { addToast } = useToast();

  useEffect(() => {
    const loadData = async () => {
      try {
        await Promise.all([fetchEvents(), fetchSyncStatus()]);
      } catch (err) {
        addToast({
          type: 'error',
          title: 'Failed to load dashboard',
          message: 'Please check your connection and try again.'
        });
      }
    };
    
    loadData();
  }, [fetchEvents, fetchSyncStatus, addToast]);

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
          <h2 className="text-2xl font-bold text-red-600 mb-4">Error Loading Dashboard</h2>
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
              <h1 className="text-2xl font-bold text-gray-900">Truth Training</h1>
              <p className="text-sm text-gray-600">Collective Intelligence Dashboard • UI Desktop v0.2.0</p>
            </div>
            <div className="flex items-center space-x-4">
              <SyncStatus 
                isOnline={isOnline}
                pendingOperations={pendingOperations}
                lastSync={syncStatus?.lastSync ?? null}
              />
              <CreateEventButton />
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Stats Overview (text-only) */}
        <div className="bg-white rounded-lg shadow p-6 mb-8">
          <div className="space-y-2 text-sm text-gray-800">
            <div>• Total Events: {events.length}</div>
            <div>• Active Events: {events.filter(e => e.status === 'active').length}</div>
            <div>• With Consensus: {events.filter(e => e.status === 'active').length}</div>
            <div>• Participants: -</div>
          </div>
        </div>

        {/* Events List */}
        <div className="bg-white shadow rounded-lg">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-medium text-gray-900">Recent Events</h2>
          </div>
          <div className="divide-y divide-gray-200">
            {events.length === 0 ? (
              <div className="px-6 py-12 text-center">
                <div className="text-gray-400 mb-4 text-sm">No data</div>
                <h3 className="text-lg font-medium text-gray-900 mb-2">No events yet</h3>
                <p className="text-gray-500 mb-4">Get started by creating your first event.</p>
                <CreateEventButton />
              </div>
            ) : (
              events.map((event) => (
                <EventCard key={event.id} event={event} onNavigate={onNavigate} />
              ))
            )}
          </div>
        </div>
      </main>
    </div>
  );
};
