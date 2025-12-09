/**
 * Events Screen
 * Matches Android Event List screen layout and behavior.
 * Implements view judgments flag handling.
 * Route: events
 */

import React, { useEffect, useState, useMemo } from 'react';
import { Screen } from '@/components/layout/TopMenuBar';
import { useNavigationStore } from '@/stores/navigation';
import { useEventsStore } from '@/stores/events';
import { EventCard } from '@/components/Dashboard/EventCard';
import { Modal } from '@/components/system/Modal';
import { DatePickerField } from '@/components/DatePickerField';
import { ApiService } from '@/services/api';
import { Event } from '@/types/events';
import { getEmoji } from '@/utils/emojiMapping';
import { validateDateRange } from '@/utils/dateNormalization';

interface NavigationState {
  eventId?: number;
  [key: string]: any;
}

interface EventsProps {
  onNavigate: (screen: Screen, state?: NavigationState) => void;
  navigationState?: NavigationState;
}

export const Events: React.FC<EventsProps> = ({ onNavigate, navigationState }) => {
  const { 
    viewJudgments, 
    setViewJudgments,
    setSelectedEventIdForJudgments,
    clearJudgmentsSelection
  } = useNavigationStore();
  const { events, loading, fetchEvents } = useEventsStore();

  // Edit Event modal state
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState<Event | null>(null);
  const [editDetected, setEditDetected] = useState<boolean>(false);
  const [editTimestampEnd, setEditTimestampEnd] = useState<number | null>(null);
  const [initialTimestampEnd, setInitialTimestampEnd] = useState<number | null>(null);
  const [editError, setEditError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    fetchEvents();
  }, [fetchEvents]);

  // Handle edit mode from navigation state (when navigating from EventSummary)
  useEffect(() => {
    if (navigationState?.editMode && navigationState?.eventId) {
      const event = events.find(e => e.id === navigationState.eventId);
      if (event) {
        handleOpenEditModal(event);
      }
    }
  }, [navigationState, events]);

  const handleOpenEditModal = (event: Event) => {
    setEditingEvent(event);
    setEditDetected(event.detected ?? false);
    // Track initial timestamp_end for corrected auto-set logic (Android rule)
    const initialEnd = event.timestamp_end ?? null;
    setInitialTimestampEnd(initialEnd);
    // End Timestamp: defaults to current date if not filled (Android rule)
    setEditTimestampEnd(initialEnd || Math.floor(Date.now() / 1000));
    setEditError(null);
    setEditModalOpen(true);
  };

  const handleCloseEditModal = () => {
    setEditModalOpen(false);
    setEditingEvent(null);
    setEditDetected(false);
    setEditTimestampEnd(null);
    setInitialTimestampEnd(null);
    setEditError(null);
  };

  // Calculate corrected value based on timestamp_end changes (Android auto-set logic)
  const corrected = useMemo(() => {
    if (!editingEvent) return false;
    
    if (initialTimestampEnd === null) {
      // If End Timestamp was initially empty, Corrected is not set (keep existing value)
      return editingEvent.corrected;
    } else {
      // If End Timestamp was set and changed, Corrected is automatically set to true
      if (editTimestampEnd !== null && editTimestampEnd !== initialTimestampEnd) {
        return true;
      } else {
        return editingEvent.corrected;
      }
    }
  }, [editingEvent, initialTimestampEnd, editTimestampEnd]);

  // Check if save should be enabled (Android save logic)
  const canSave = useMemo(() => {
    if (!editingEvent) return false;
    
    const detectedChanged = editDetected !== (editingEvent.detected ?? false);
    const correctedChanged = corrected !== editingEvent.corrected;
    const timestampEndChanged = editTimestampEnd !== null && editTimestampEnd !== initialTimestampEnd;
    
    // Validate timestamp_end if changed
    if (timestampEndChanged) {
      const validation = validateDateRange(editingEvent.timestamp_start, editTimestampEnd);
      if (!validation.valid) {
        return false; // Save disabled if validation fails
      }
    }
    
    return detectedChanged || correctedChanged || timestampEndChanged;
  }, [editingEvent, editDetected, corrected, editTimestampEnd, initialTimestampEnd]);

  const handleSaveEvent = async () => {
    if (!editingEvent) return;

    // Validation: timestamp_end >= timestamp_start (normalized to start of day, Android rule)
    const validation = validateDateRange(editingEvent.timestamp_start, editTimestampEnd);
    if (!validation.valid) {
      setEditError(validation.error || 'End timestamp cannot be less than start timestamp');
      return;
    }

    setSaving(true);
    setEditError(null);

    try {
      // Only include changed fields in update request (Android save logic)
      const updateData: { detected?: boolean; corrected?: boolean; timestamp_end?: number | null } = {};
      
      if (editDetected !== (editingEvent.detected ?? false)) {
        updateData.detected = editDetected;
      }
      
      if (corrected !== editingEvent.corrected) {
        updateData.corrected = corrected;
      }
      
      if (editTimestampEnd !== null && editTimestampEnd !== initialTimestampEnd) {
        updateData.timestamp_end = editTimestampEnd;
      }

      await ApiService.updateEvent(editingEvent.id, updateData);

      // Refresh events list
      await fetchEvents();
      handleCloseEditModal();
    } catch (error: any) {
      setEditError(error.message || 'Failed to update event');
    } finally {
      setSaving(false);
    }
  };

  const formatDate = (timestamp: number) => {
    return new Date(timestamp * 1000).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const handleEventClick = (eventId: number) => {
    if (viewJudgments) {
      // Android algorithm: If viewJudgments flag is true, navigate to judgments
      setSelectedEventIdForJudgments(eventId.toString());
      onNavigate('judgments', { eventId });
    } else {
      // Otherwise, navigate to event detail (EventSummary)
      onNavigate('event-summary', { eventId });
    }
  };

  const handleViewEventClick = (eventId: number) => {
    // Clear viewJudgments flag and navigate to event detail
    clearJudgmentsSelection();
    onNavigate('event-summary', { eventId });
  };

  const handleViewJudgmentsClick = () => {
    // Set viewJudgments flag and navigate to Events screen
    setViewJudgments(true);
    onNavigate('events');
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">
          {getEmoji('screens', 'events')} Events
        </h1>
        <div className="flex items-center space-x-3">
          {viewJudgments && (
            <div className="text-sm text-gray-600 dark:text-gray-400 bg-blue-50 dark:bg-blue-900 px-3 py-1 rounded">
              {getEmoji('status', 'info')} View Judgments Mode - Click an event to view its judgments
            </div>
          )}
          <button
            onClick={handleViewJudgmentsClick}
            className={`px-4 py-2 rounded hover:opacity-80 transition-opacity ${
              viewJudgments 
                ? 'bg-blue-600 dark:bg-blue-500 text-white' 
                : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300'
            }`}
          >
            {getEmoji('navigation', 'judgments')} View Judgments
          </button>
          <button
            onClick={() => onNavigate('new-event')}
            className="px-4 py-2 bg-blue-600 dark:bg-blue-500 text-white rounded hover:bg-blue-700 dark:hover:bg-blue-600"
          >
            {getEmoji('actions', 'create')} New Event
          </button>
        </div>
      </div>

      {loading ? (
        <div className="text-center py-8 text-gray-700 dark:text-gray-300">{getEmoji('status', 'syncing')} Loading events...</div>
      ) : events.length === 0 ? (
        <div className="text-center py-8 text-gray-500 dark:text-gray-400">{getEmoji('status', 'warning')} No events found</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {events.map((event) => (
            <div key={event.id}>
              <EventCard 
                event={event} 
                onNavigate={onNavigate}
                onViewEvent={() => handleViewEventClick(event.id)}
                onViewJudgments={() => {
                  setSelectedEventIdForJudgments(event.id.toString());
                  setViewJudgments(true);
                  onNavigate('judgments', { eventId: event.id });
                }}
                onEditEvent={() => handleOpenEditModal(event)}
                onClick={() => handleEventClick(event.id)}
              />
            </div>
          ))}
        </div>
      )}

      {/* Edit Event Modal */}
      <Modal
        open={editModalOpen}
        title={`${getEmoji('actions', 'edit')} Edit Event`}
        onClose={handleCloseEditModal}
        footer={
          <>
            <button
              onClick={handleCloseEditModal}
              className="px-4 py-2 text-sm bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
            >
              {getEmoji('actions', 'cancel')} Cancel
            </button>
            <button
              onClick={handleSaveEvent}
              disabled={saving || !canSave}
              className="px-4 py-2 text-sm bg-blue-600 dark:bg-blue-500 text-white rounded hover:bg-blue-700 dark:hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {getEmoji('actions', 'save')} {saving ? 'Saving...' : 'Save'}
            </button>
          </>
        }
      >
        {editingEvent && (
          <div className="space-y-4">
            {/* Read-only fields */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {getEmoji('fields', 'description')} Description
              </label>
              <p className="text-sm text-gray-900 dark:text-gray-100 bg-gray-50 dark:bg-gray-700 p-2 rounded border border-gray-200 dark:border-gray-600">
                {editingEvent.description}
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">Read-only</p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {getEmoji('fields', 'startDate')} Start Timestamp
              </label>
              <p className="text-sm text-gray-900 dark:text-gray-100 bg-gray-50 dark:bg-gray-700 p-2 rounded border border-gray-200 dark:border-gray-600">
                {formatDate(editingEvent.timestamp_start)}
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">Read-only</p>
            </div>

            {/* Editable fields */}
            <div>
              <label className="flex items-center space-x-2 mb-2">
                <input
                  type="checkbox"
                  checked={editDetected}
                  onChange={(e) => setEditDetected(e.target.checked)}
                  className="w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500 dark:focus:ring-blue-600 dark:ring-offset-gray-800 focus:ring-2 dark:bg-gray-700 dark:border-gray-600"
                />
                <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                  {getEmoji('status', 'success')} Detected
                </span>
              </label>
            </div>

            <div>
              <DatePickerField
                label={`${getEmoji('fields', 'endDate')} End Timestamp`}
                value={editTimestampEnd}
                onChange={(value) => {
                  setEditTimestampEnd(value);
                  // Clear error when user changes value
                  if (editError) {
                    setEditError(null);
                  }
                }}
                minDate={editingEvent.timestamp_start}
                allowClear={true}
                error={editError || undefined}
              />
            </div>

            {/* corrected Field - Read-only display (auto-set) */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {getEmoji('status', 'success')} Corrected
              </label>
              <div className="flex items-center space-x-2">
                <input
                  type="checkbox"
                  checked={corrected}
                  disabled={true}
                  readOnly={true}
                  className="w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500 dark:focus:ring-blue-600 dark:ring-offset-gray-800 focus:ring-2 dark:bg-gray-700 dark:border-gray-600 opacity-50 cursor-not-allowed"
                />
                <span className="text-sm text-gray-600 dark:text-gray-400">
                  {corrected ? 'Yes (auto-set)' : 'No'}
                </span>
                <span className="text-xs text-gray-500 dark:text-gray-400 italic">
                  (Set automatically when End Timestamp changes)
                </span>
              </div>
            </div>

            {/* Error message */}
            {editError && (
              <div className="p-3 bg-red-50 dark:bg-red-900 border border-red-200 dark:border-red-700 rounded text-sm text-red-800 dark:text-red-200">
                {getEmoji('status', 'error')} {editError}
              </div>
            )}

            {/* Note about context fields */}
            <div className="p-3 bg-blue-50 dark:bg-blue-900 border border-blue-200 dark:border-blue-700 rounded text-xs text-blue-800 dark:text-blue-200">
              {getEmoji('status', 'info')} Note: Context fields (category, forma, cause, develop, effect) cannot be edited. Only Detected flag and End Timestamp can be modified.
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

