/**
 * Desktop-specific screen: Event Summary
 * This screen is Desktop-only and not present in Android UI.
 * Preserved during UI reconstruction to maintain Desktop-specific functionality.
 * 
 * Route: event-summary
 * 
 * Note: This screen should NOT be synchronized with Android UI.
 * It is a Desktop-specific feature that must be preserved.
 */

import React, { useEffect, useState } from 'react';
import { Screen } from '@/components/layout/TopMenuBar';
import { ApiService } from '@/services/api';
import { getEmoji } from '@/utils/emojiMapping';

interface NavigationState {
  eventId?: number;
  [key: string]: any;
}

interface EventSummaryProps {
  eventId?: number;
  onNavigate?: (screen: Screen, state?: NavigationState) => void;
}

export const EventSummary: React.FC<EventSummaryProps> = ({ eventId, onNavigate: _onNavigate }) => {
  const [event, setEvent] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (eventId) {
      loadEvent();
    }
  }, [eventId]);

  const loadEvent = async () => {
    if (!eventId) return;
    setLoading(true);
    try {
      const response = await ApiService.getEvent(eventId);
      setEvent(response);
    } catch (error) {
      console.error('Failed to load event:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-6">
        {getEmoji('screens', 'events')} Event Summary
      </h1>

      {loading ? (
        <div className="text-center py-8 text-gray-700 dark:text-gray-300">{getEmoji('status', 'syncing')} Loading event...</div>
      ) : event ? (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100 mb-4">{event.description}</h2>
          <div className="space-y-2">
            <p className="text-sm text-gray-600 dark:text-gray-400">
              Start: {new Date(event.timestamp_start * 1000).toLocaleString()}
            </p>
            {event.timestamp_end && (
              <p className="text-sm text-gray-600 dark:text-gray-400">
                End: {new Date(event.timestamp_end * 1000).toLocaleString()}
              </p>
            )}
          </div>
        </div>
      ) : (
        <div className="text-center py-8 text-gray-500 dark:text-gray-400">{getEmoji('status', 'warning')} No event data available</div>
      )}
    </div>
  );
};

