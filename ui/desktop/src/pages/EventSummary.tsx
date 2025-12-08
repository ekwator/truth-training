import React, { useState, useEffect, useMemo } from 'react';
import { ApiService } from '@/services/api';
import { EventDetails } from '@/types/events';
import { Screen } from '@/components/layout/TopMenuBar';
import { getEntityNameById } from '@/utils/entityResolution';
import { t } from '@/i18n';

interface NavigationState {
  eventId?: number;
  [key: string]: any;
}

interface EventSummaryProps {
  eventId?: number;
  onNavigate?: (screen: Screen, state?: NavigationState) => void;
}

interface KnowledgeBaseEntity {
  id: number;
  name: string;
}

export const EventSummary: React.FC<EventSummaryProps> = ({ eventId, onNavigate }) => {
  const [event, setEvent] = useState<EventDetails | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // Form state for editing
  const [detected, setDetected] = useState<boolean | undefined>(undefined);
  const [timestampEnd, setTimestampEnd] = useState<number | null>(null);
  const [initialTimestampEnd, setInitialTimestampEnd] = useState<number | null>(null);
  
  // Knowledge base entities (TODO: Fetch from Tauri commands when available)
  // For now, using empty arrays - will be populated when Tauri commands are added
  const categories: KnowledgeBaseEntity[] = [];
  const formas: KnowledgeBaseEntity[] = [];
  const causes: KnowledgeBaseEntity[] = [];
  const develops: KnowledgeBaseEntity[] = [];
  const effects: KnowledgeBaseEntity[] = [];

  useEffect(() => {
    if (eventId) {
      fetchEventDetails(eventId);
    }
  }, [eventId]);

  const fetchEventDetails = async (id: number) => {
    setLoading(true);
    setError(null);
    try {
      const eventData = await ApiService.getEvent(id);
      // Convert Event to EventDetails (add missing fields)
      const eventDetails: EventDetails = {
        ...eventData,
        judgments: [],
        impacts: [],
        consensus: null,
        summary: undefined,
        participant_count: undefined,
        last_activity: undefined
      };
      setEvent(eventDetails);
      setDetected(eventData.detected);
      setTimestampEnd(eventData.timestamp_end || null);
      setInitialTimestampEnd(eventData.timestamp_end || null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load event');
    } finally {
      setLoading(false);
    }
  };

  // Context field display values using entityResolution
  const categoryDisplay = useMemo(() => {
    if (!event?.category_id) return null;
    // First try to use category_name from event (if backend joined it)
    if (event.category_name) return event.category_name;
    // Otherwise use entityResolution utility
    return getEntityNameById(
      event.category_id,
      categories,
      (e) => e.id,
      (e) => e.name
    );
  }, [event?.category_id, event?.category_name, categories]);

  const formaDisplay = useMemo(() => {
    if (!event?.forma_id) return null;
    if (event.forma_name) return event.forma_name;
    return getEntityNameById(
      event.forma_id,
      formas,
      (e) => e.id,
      (e) => e.name
    );
  }, [event?.forma_id, event?.forma_name, formas]);

  const causeDisplay = useMemo(() => {
    if (!event?.cause_id) return null;
    if (event.cause_name) return event.cause_name;
    return getEntityNameById(
      event.cause_id,
      causes,
      (e) => e.id,
      (e) => e.name
    );
  }, [event?.cause_id, event?.cause_name, causes]);

  const developDisplay = useMemo(() => {
    if (!event?.develop_id) return null;
    if (event.develop_name) return event.develop_name;
    return getEntityNameById(
      event.develop_id,
      develops,
      (e) => e.id,
      (e) => e.name
    );
  }, [event?.develop_id, event?.develop_name, develops]);

  const effectDisplay = useMemo(() => {
    if (!event?.effect_id) return null;
    if (event.effect_name) return event.effect_name;
    return getEntityNameById(
      event.effect_id,
      effects,
      (e) => e.id,
      (e) => e.name
    );
  }, [event?.effect_id, event?.effect_name, effects]);

  // Corrected flag calculation (matches Android algorithm)
  const corrected = useMemo(() => {
    if (!event) return false;
    if (initialTimestampEnd === null) {
      // If End Timestamp was initially empty, Corrected is not set
      return event.corrected;
    } else {
      // If End Timestamp was set and changed, Corrected is automatically set
      if (timestampEnd !== null && timestampEnd !== initialTimestampEnd) {
        return true;
      } else {
        return event.corrected;
      }
    }
  }, [event, timestampEnd, initialTimestampEnd]);

  const formatDate = (timestamp: number) => {
    return new Date(timestamp * 1000).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const handleSave = async () => {
    if (!event) return;
    try {
      // TODO: Implement update event Tauri command
      // For now, just refresh the event data
      // await ApiService.updateEvent(event.id, {
      //   detected,
      //   timestamp_end: timestampEnd || undefined,
      //   corrected
      // });
      setIsEditing(false);
      // Refresh event data
      await fetchEventDetails(event.id);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save event');
    }
  };

  const handleDelete = async () => {
    if (!event) return;
    if (!confirm(t('events.confirmDelete'))) return;
    try {
      // TODO: Implement delete event Tauri command
      onNavigate?.('home');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete event');
    }
  };

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
          <h2 className="text-2xl font-bold text-red-600 mb-4">{t('events.errorLoading')}</h2>
          <p className="text-gray-600 mb-4">{error}</p>
          <button
            onClick={() => event && fetchEventDetails(event.id)}
            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            {t('common.retry')}
          </button>
        </div>
      </div>
    );
  }

  if (!event) {
    return (
      <div className="min-h-screen bg-gray-50 py-8">
        <div className="max-w-3xl mx-auto px-4">
          <div className="bg-white rounded-lg shadow p-12 text-center">
            <p className="text-gray-600">{t('events.noEventSelected')}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Top App Bar */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">
                {isEditing ? t('events.editEvent') : t('events.eventDetails')}
              </h1>
            </div>
            <div className="flex items-center space-x-2">
              {!isEditing && (
                <>
                  <button
                    onClick={() => setIsEditing(true)}
                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                  >
                    {t('common.edit')}
                  </button>
                  <button
                    onClick={handleDelete}
                    className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700"
                  >
                    {t('common.delete')}
                  </button>
                </>
              )}
              {isEditing && (
                <>
                  <button
                    onClick={handleSave}
                    className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
                  >
                    {t('common.save')}
                  </button>
                  <button
                    onClick={() => {
                      setIsEditing(false);
                      setDetected(event.detected);
                      setTimestampEnd(event.timestamp_end || null);
                    }}
                    className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300"
                  >
                    {t('common.cancel')}
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-white rounded-lg shadow p-6 space-y-6">
          {/* Description (headline) */}
          <div>
            <h2 className="text-xl font-semibold text-gray-900 mb-2">{event.description}</h2>
          </div>

          {/* Vector chip */}
          <div>
            <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${
              event.vector ? 'bg-blue-100 text-blue-800' : 'bg-purple-100 text-purple-800'
            }`}>
              {event.vector ? t('events.outgoing') : t('events.incoming')}
            </span>
          </div>

          {/* Context Fields Display (FlowRow with AssistChips equivalent) */}
          {(categoryDisplay || formaDisplay || causeDisplay || developDisplay || effectDisplay) && (
            <div>
              <h3 className="text-sm font-medium text-gray-700 mb-2">{t('events.contextFields')}</h3>
              <div className="flex flex-wrap gap-2">
                {categoryDisplay && (
                  <span className="inline-flex items-center px-3 py-1 rounded-full text-sm bg-gray-100 text-gray-800">
                    {t('events.category')}: {categoryDisplay}
                  </span>
                )}
                {formaDisplay && (
                  <span className="inline-flex items-center px-3 py-1 rounded-full text-sm bg-gray-100 text-gray-800">
                    {t('events.forma')}: {formaDisplay}
                  </span>
                )}
                {causeDisplay && (
                  <span className="inline-flex items-center px-3 py-1 rounded-full text-sm bg-gray-100 text-gray-800">
                    {t('events.cause')}: {causeDisplay}
                  </span>
                )}
                {developDisplay && (
                  <span className="inline-flex items-center px-3 py-1 rounded-full text-sm bg-gray-100 text-gray-800">
                    {t('events.develop')}: {developDisplay}
                  </span>
                )}
                {effectDisplay && (
                  <span className="inline-flex items-center px-3 py-1 rounded-full text-sm bg-gray-100 text-gray-800">
                    {t('events.effect')}: {effectDisplay}
                  </span>
                )}
              </div>
            </div>
          )}

          {/* Timestamps */}
          <div className="space-y-2">
            <div>
              <span className="text-sm font-medium text-gray-700">{t('events.startTimestamp')}: </span>
              <span className="text-sm text-gray-600">{formatDate(event.timestamp_start)}</span>
            </div>
            {event.timestamp_end && (
              <div>
                <span className="text-sm font-medium text-gray-700">{t('events.endTimestamp')}: </span>
                <span className="text-sm text-gray-600">{formatDate(event.timestamp_end)}</span>
              </div>
            )}
          </div>

          {/* Flags */}
          <div className="space-y-2">
            <div>
              <span className="text-sm font-medium text-gray-700">{t('events.detected')}: </span>
              <span className={`text-sm ${event.detected ? 'text-green-600' : event.detected === false ? 'text-red-600' : 'text-gray-500'}`}>
                {event.detected === true ? t('events.detected') : event.detected === false ? t('events.notDetected') : t('events.unknown')}
              </span>
            </div>
            <div>
              <span className="text-sm font-medium text-gray-700">{t('events.corrected')}: </span>
              <span className={`text-sm ${event.corrected ? 'text-green-600' : 'text-gray-500'}`}>
                {event.corrected ? t('common.yes') : t('common.no')}
              </span>
            </div>
          </div>

          {/* Edit Mode Fields */}
          {isEditing && (
            <div className="border-t pt-6 space-y-4">
              {/* Read-only fields */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">{t('events.description')}</label>
                <p className="text-sm text-gray-600 bg-gray-50 p-2 rounded">{event.description}</p>
              </div>

              {/* Editable Flags */}
              <div className="space-y-4">
                <div>
                  <label className="flex items-center">
                    <input
                      type="checkbox"
                      checked={detected === true}
                      onChange={(e) => setDetected(e.target.checked ? true : undefined)}
                      className="mr-2"
                    />
                    <span className="text-sm font-medium text-gray-700">{t('events.detected')}</span>
                  </label>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('events.corrected')}</label>
                  <p className="text-sm text-gray-600 bg-gray-50 p-2 rounded">
                    {corrected ? t('common.yes') : t('common.no')} {t('events.autoCalculated')}
                  </p>
                </div>
              </div>

              {/* Editable Timestamps */}
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('events.startTimestamp')}</label>
                  <p className="text-sm text-gray-600 bg-gray-50 p-2 rounded">{formatDate(event.timestamp_start)}</p>
                  <p className="text-xs text-gray-500 mt-1">{t('events.readOnly')}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('events.endTimestamp')}</label>
                  <input
                    type="datetime-local"
                    value={timestampEnd ? new Date(timestampEnd * 1000).toISOString().slice(0, 16) : ''}
                    onChange={(e) => {
                      const value = e.target.value;
                      if (value) {
                        const date = new Date(value);
                        const timestamp = Math.floor(date.getTime() / 1000);
                        setTimestampEnd(timestamp);
                        // Corrected flag is auto-calculated via useMemo when timestampEnd changes
                      } else {
                        setTimestampEnd(null);
                      }
                    }}
                    min={event.timestamp_start ? new Date(event.timestamp_start * 1000).toISOString().slice(0, 16) : undefined}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md"
                  />
                  <p className="text-xs text-gray-500 mt-1">{t('events.endTimestampHint')}</p>
                </div>
              </div>
            </div>
          )}

          {/* View Judgments button */}
          {!isEditing && event && (
            <div>
              <button
                onClick={() => {
                  // Navigate to Judgments screen with eventId
                  onNavigate?.('events', { eventId: event.id });
                }}
                className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
              >
                {t('events.viewJudgments')}
              </button>
            </div>
          )}
        </div>
      </main>
    </div>
  );
};
