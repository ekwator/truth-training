/**
 * Events Screen
 * Matches Android Event List screen layout and behavior.
 * Implements view judgments flag handling.
 * Route: events
 */

import React, { useEffect } from 'react';
import { Screen } from '@/components/layout/TopMenuBar';
import { useNavigationStore } from '@/stores/navigation';
import { useEventsStore } from '@/stores/events';
import { EventCard } from '@/components/Dashboard/EventCard';
import { getEmoji } from '@/utils/emojiMapping';

interface NavigationState {
  eventId?: number;
  [key: string]: any;
}

interface EventsProps {
  onNavigate: (screen: Screen, state?: NavigationState) => void;
}

export const Events: React.FC<EventsProps> = ({ onNavigate }) => {
  const { viewJudgments, setSelectedEventIdForJudgments } = useNavigationStore();
  const { events, loading, fetchEvents } = useEventsStore();

  useEffect(() => {
    fetchEvents();
  }, [fetchEvents]);

  const handleEventClick = (eventId: number) => {
    if (viewJudgments) {
      // Android algorithm: If viewJudgments flag is true, navigate to judgments
      setSelectedEventIdForJudgments(eventId.toString());
      onNavigate('judgments', { eventId });
    } else {
      // Otherwise, navigate to event detail (or summary)
      onNavigate('event-summary', { eventId });
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">
          {getEmoji('screens', 'events')} Events
        </h1>
        {viewJudgments && (
          <div className="text-sm text-gray-600 dark:text-gray-400">
            View Judgments Mode - Click an event to view its judgments
          </div>
        )}
        <button
          onClick={() => onNavigate('new-event')}
          className="px-4 py-2 bg-blue-600 dark:bg-blue-500 text-white rounded hover:bg-blue-700 dark:hover:bg-blue-600"
        >
          {getEmoji('actions', 'create')} New Event
        </button>
      </div>

      {loading ? (
        <div className="text-center py-8 text-gray-700 dark:text-gray-300">{getEmoji('status', 'syncing')} Loading events...</div>
      ) : events.length === 0 ? (
        <div className="text-center py-8 text-gray-500 dark:text-gray-400">{getEmoji('status', 'warning')} No events found</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {events.map((event) => (
            <div key={event.id} onClick={() => handleEventClick(event.id)}>
              <EventCard event={event} onNavigate={onNavigate} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

