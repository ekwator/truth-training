import React, { useEffect } from 'react';
import { useEventsStore } from '@/stores/events';
import { useSyncStore } from '@/stores/sync';
import { useToast } from '@/components/system/Toaster';
import { SyncStatus } from '@/components/system/SyncStatus';
import { EventCard } from '@/components/Dashboard/EventCard';
import { CreateEventButton } from '@/components/Dashboard/CreateEventButton';
import { Screen } from '@/components/layout/TopMenuBar';
import { NodesPanel } from '@/components/NodesPanel';
import { useNavigationStore } from '@/stores/navigation';
import { t } from '@/i18n';

interface NavigationState {
  eventId?: number;
  [key: string]: any;
}

interface DashboardProps {
  onNavigate?: (screen: Screen, state?: NavigationState) => void;
}

export const Dashboard: React.FC<DashboardProps> = ({ onNavigate }) => {
  const { events, loading, error, fetchEvents } = useEventsStore();
  const { syncStatus, isOnline, pendingOperations, fetchSyncStatus } = useSyncStore();
  const { addToast } = useToast();
  const { setViewJudgments } = useNavigationStore();

  useEffect(() => {
    const loadData = async () => {
      try {
        await Promise.all([fetchEvents(), fetchSyncStatus()]);
      } catch (err) {
        addToast({
          type: 'error',
          title: t('errors.networkError'),
          message: t('errors.retryMessage')
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
          <h2 className="text-2xl font-bold text-red-600 mb-4">{t('dashboard.errorLoading')}</h2>
          <p className="text-gray-600 mb-4">{error}</p>
          <button
            onClick={() => fetchEvents()}
            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            {t('common.retry')}
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
              <h1 className="text-2xl font-bold text-gray-900">{t('dashboard.title')}</h1>
              <p className="text-sm text-gray-600">{t('dashboard.subtitle')} • UI Desktop v1.0.0</p>
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
        {/* Quick Stats */}
        <div className="bg-white rounded-lg shadow p-6 mb-8">
          <h2 className="text-lg font-medium text-gray-900 mb-4">{t('dashboard.quickStats')}</h2>
          <div className="space-y-2 text-sm text-gray-800">
            <div>
              <button
                onClick={() => {
                  setViewJudgments(false);
                  onNavigate?.('events');
                }}
                className="text-blue-600 hover:text-blue-800 hover:underline"
              >
                {t('dashboard.totalEvents')}: {events.length}
              </button>
            </div>
            <div>• {t('dashboard.detectedEvents')}: {events.filter(e => e.detected === true).length}</div>
            <div>• {t('dashboard.withConsensus')}: {events.filter(e => e.collective_score !== null && e.collective_score !== undefined).length}</div>
            <div>• {t('dashboard.participants')}: -</div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="bg-white rounded-lg shadow p-6 mb-8">
          <h2 className="text-lg font-medium text-gray-900 mb-4">{t('dashboard.actions')}</h2>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            <button
              onClick={() => {
                setViewJudgments(false);
                onNavigate?.('events');
              }}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              {t('dashboard.viewEvents')}
            </button>
            <button
              onClick={() => {
                setViewJudgments(true);
                onNavigate?.('events');
              }}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              {t('dashboard.viewJudgments')}
            </button>
            <button
              onClick={() => onNavigate?.('new-event')}
              className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors"
            >
              {t('dashboard.newEvent')}
            </button>
            <button
              onClick={() => onNavigate?.('context-editor')}
              className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
            >
              {t('dashboard.manageContextTemplates')}
            </button>
            <button
              onClick={() => onNavigate?.('overall-summary')}
              className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
            >
              {t('dashboard.overallSummary')}
            </button>
            <button
              onClick={() => onNavigate?.('training-results')}
              className="px-4 py-2 bg-teal-600 text-white rounded-lg hover:bg-teal-700 transition-colors"
            >
              {t('dashboard.trainingResults')}
            </button>
            <button
              onClick={() => onNavigate?.('settings')}
              className="px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
            >
              {t('dashboard.settings')}
            </button>
          </div>
        </div>

        <div className="mb-8">
          <NodesPanel />
        </div>

        {/* Events List */}
        <div className="bg-white shadow rounded-lg">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-medium text-gray-900">{t('dashboard.recentEvents')}</h2>
          </div>
          <div className="divide-y divide-gray-200">
            {events.length === 0 ? (
              <div className="px-6 py-12 text-center">
                <div className="text-gray-400 mb-4 text-sm">{t('dashboard.noData')}</div>
                <h3 className="text-lg font-medium text-gray-900 mb-2">{t('dashboard.noEvents')}</h3>
                <p className="text-gray-500 mb-4">{t('dashboard.noEventsDescription')}</p>
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
