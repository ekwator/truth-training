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
import { ApiService, Impact, Consensus } from '@/services/api';
import { Event } from '@/types/events';
import { Judgment, JudgmentAssessment } from '@/types/judgments';
import { getEmoji } from '@/utils/emojiMapping';
import { AddImpactModal } from '@/components/impacts/AddImpactModal';
import { SubmitJudgmentModal } from '@/components/judgments/SubmitJudgmentModal';
import { mapToBoolean, getDisplayText } from '@/utils/impactLevelMapper';

interface NavigationState {
  eventId?: number;
  [key: string]: any;
}

interface EventSummaryProps {
  eventId?: number;
  onNavigate?: (screen: Screen, state?: NavigationState) => void;
}

export const EventSummary: React.FC<EventSummaryProps> = ({ eventId, onNavigate }) => {
  const [event, setEvent] = useState<Event | null>(null);
  const [impacts, setImpacts] = useState<Impact[]>([]);
  const [judgments, setJudgments] = useState<Judgment[]>([]);
  const [consensus, setConsensus] = useState<Consensus | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadingRelated, setLoadingRelated] = useState(false);
  const [isAddImpactModalOpen, setIsAddImpactModalOpen] = useState(false);
  const [isSubmitJudgmentModalOpen, setIsSubmitJudgmentModalOpen] = useState(false);

  useEffect(() => {
    if (eventId) {
      loadEvent();
      loadRelatedData();
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

  const loadRelatedData = async () => {
    if (!eventId) return;
    setLoadingRelated(true);
    try {
      // Load judgments
      const judgmentsResponse = await ApiService.getJudgments(eventId);
      setJudgments(judgmentsResponse.data || []);

      // Load consensus if available
      try {
        const consensusData = await ApiService.getConsensus(eventId);
        setConsensus(consensusData);
      } catch (error) {
        // Consensus may not be available for all events
        console.debug('Consensus not available for event:', eventId);
      }

      // Load impacts for event
      // TODO: Implement getImpactsForEvent API method or filter from all impacts
      // For now, impacts will be loaded when added via modal
      // In a real implementation, we would call: ApiService.getImpactsForEvent(eventId)
    } catch (error) {
      console.error('Failed to load related data:', error);
    } finally {
      setLoadingRelated(false);
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

  const formatDateString = (timestamp: string) => {
    return new Date(timestamp).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const handleImpactAdded = (impact: Impact) => {
    // Add new impact to list
    setImpacts((prev) => [...prev, impact]);
    // Reload related data to ensure consistency
    loadRelatedData();
  };

  const handleJudgmentSubmitted = (judgment: Judgment) => {
    // Add new judgment to list
    setJudgments((prev) => [...prev, judgment]);
    // Reload related data to ensure consistency
    loadRelatedData();
  };


  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-6">
          {getEmoji('screens', 'events')} Event Summary
        </h1>
        <div className="text-center py-8 text-gray-700 dark:text-gray-300">
          {getEmoji('status', 'syncing')} Loading event...
        </div>
      </div>
    );
  }

  if (!event) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-6">
          {getEmoji('screens', 'events')} Event Summary
        </h1>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-8 text-center">
          <div className="text-6xl mb-4">{getEmoji('status', 'warning')}</div>
          <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100 mb-2">
            Event Not Found
          </h2>
          <p className="text-gray-600 dark:text-gray-400 mb-4">
            The requested event could not be loaded. Please check the event ID and try again.
          </p>
          {onNavigate && (
            <button
              onClick={() => onNavigate('events')}
              className="px-4 py-2 bg-blue-600 dark:bg-blue-500 text-white rounded-lg hover:bg-blue-700 dark:hover:bg-blue-600 transition-colors"
            >
              {getEmoji('navigation', 'events')} Back to Events
            </button>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-6">
        {getEmoji('screens', 'events')} Event Summary
      </h1>

      <div className="space-y-6">
        {/* Event Details Card */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
              {getEmoji('fields', 'description')} Event Details
            </h2>
            {onNavigate && (
              <button
                onClick={() => {
                  // Navigate to Events screen and trigger edit modal
                  // We'll use a custom handler in Events.tsx to open the modal
                  onNavigate('events', { eventId, editMode: true });
                }}
                className="px-4 py-2 bg-blue-600 dark:bg-blue-500 text-white rounded-lg hover:bg-blue-700 dark:hover:bg-blue-600 transition-colors flex items-center space-x-2"
              >
                <span>{getEmoji('actions', 'edit')}</span>
                <span>Edit Event</span>
              </button>
            )}
          </div>
          <div className="space-y-4">
            <div>
              <p className="text-lg text-gray-900 dark:text-gray-100 mb-2">{event.description}</p>
            </div>

            {/* Event Metadata Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
              {/* Context Fields */}
              {event.category_name && (
                <div className="flex items-center space-x-2">
                  <span className="text-gray-500 dark:text-gray-400">{getEmoji('fields', 'category')} Category:</span>
                  <span className="text-gray-900 dark:text-gray-100 font-medium">{event.category_name}</span>
                </div>
              )}
              {event.forma_name && (
                <div className="flex items-center space-x-2">
                  <span className="text-gray-500 dark:text-gray-400">{getEmoji('fields', 'forma')} Forma:</span>
                  <span className="text-gray-900 dark:text-gray-100 font-medium">{event.forma_name}</span>
                </div>
              )}
              {event.cause_name && (
                <div className="flex items-center space-x-2">
                  <span className="text-gray-500 dark:text-gray-400">{getEmoji('fields', 'cause')} Cause:</span>
                  <span className="text-gray-900 dark:text-gray-100 font-medium">{event.cause_name}</span>
                </div>
              )}
              {event.develop_name && (
                <div className="flex items-center space-x-2">
                  <span className="text-gray-500 dark:text-gray-400">{getEmoji('fields', 'develop')} Develop:</span>
                  <span className="text-gray-900 dark:text-gray-100 font-medium">{event.develop_name}</span>
                </div>
              )}
              {event.effect_name && (
                <div className="flex items-center space-x-2">
                  <span className="text-gray-500 dark:text-gray-400">{getEmoji('fields', 'effect')} Effect:</span>
                  <span className="text-gray-900 dark:text-gray-100 font-medium">{event.effect_name}</span>
                </div>
              )}

              {/* Timestamps */}
              <div className="flex items-center space-x-2">
                <span className="text-gray-500 dark:text-gray-400">{getEmoji('fields', 'startDate')} Start:</span>
                <span className="text-gray-900 dark:text-gray-100">{formatDate(event.timestamp_start)}</span>
              </div>
              {event.timestamp_end && (
                <div className="flex items-center space-x-2">
                  <span className="text-gray-500 dark:text-gray-400">{getEmoji('fields', 'endDate')} End:</span>
                  <span className="text-gray-900 dark:text-gray-100">{formatDate(event.timestamp_end)}</span>
                </div>
              )}

              {/* Vector */}
              <div className="flex items-center space-x-2">
                <span className="text-gray-500 dark:text-gray-400">Vector:</span>
                <span className={`px-2 py-1 rounded text-sm font-medium ${
                  event.vector 
                    ? 'bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200' 
                    : 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200'
                }`}>
                  {event.vector ? 'Outgoing' : 'Incoming'}
                </span>
              </div>

              {/* Status Flags */}
              {event.detected !== null && (
                <div className="flex items-center space-x-2">
                  <span className="text-gray-500 dark:text-gray-400">Detected:</span>
                  <span className={`px-2 py-1 rounded text-sm font-medium ${
                    event.detected 
                      ? 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200' 
                      : 'bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200'
                  }`}>
                    {event.detected ? 'True' : 'False'}
                  </span>
                </div>
              )}
              {event.corrected && (
                <div className="flex items-center space-x-2">
                  <span className="text-gray-500 dark:text-gray-400">Corrected:</span>
                  <span className="px-2 py-1 rounded text-sm font-medium bg-yellow-100 dark:bg-yellow-900 text-yellow-800 dark:text-yellow-200">
                    Yes
                  </span>
                </div>
              )}
            </div>

            {/* Collective Score */}
            {event.collective_score !== null && event.collective_score !== undefined && (
              <div className="mt-4 p-4 bg-gray-50 dark:bg-gray-700 rounded-lg">
                <div className="flex items-center justify-between">
                  <span className="text-gray-700 dark:text-gray-300 font-medium">
                    {getEmoji('status', 'success')} Collective Score:
                  </span>
                  <span className="text-2xl font-bold text-gray-900 dark:text-gray-100">
                    {(event.collective_score * 100).toFixed(1)}%
                  </span>
                </div>
                <div className="mt-2 w-full bg-gray-200 dark:bg-gray-600 rounded-full h-2">
                  <div
                    className="bg-blue-600 dark:bg-blue-500 h-2 rounded-full transition-all"
                    style={{ width: `${event.collective_score * 100}%` }}
                  />
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Consensus Card */}
        {consensus && (
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6">
            <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100 mb-4">
              {getEmoji('status', 'success')} Consensus
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Value</p>
                <p className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                  {consensus.consensus_value || 'Pending'}
                </p>
              </div>
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Confidence</p>
                <p className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                  {(consensus.confidence_score * 100).toFixed(1)}%
                </p>
              </div>
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Participants</p>
                <p className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                  {consensus.participant_count}
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Judgments Section */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
              {getEmoji('navigation', 'judgments')} Judgments ({judgments.length})
            </h2>
            <button
              onClick={() => setIsSubmitJudgmentModalOpen(true)}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 dark:bg-blue-500 rounded-md hover:bg-blue-700 dark:hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              {getEmoji('actions', 'submit')} Submit Judgment
            </button>
          </div>
          
          {judgments.length > 0 ? (
            <div className="space-y-3">
              {judgments.map((judgment) => {
                // Map assessment from API format to display format
                const assessmentMap: Record<string, JudgmentAssessment> = {
                  'true': 'confirm',
                  'false': 'reject',
                  'uncertain': 'abstain'
                };
                const displayAssessment = assessmentMap[judgment.assessment as string] || judgment.assessment as JudgmentAssessment;
                
                return (
                  <div
                    key={judgment.id}
                    className="p-4 bg-gray-50 dark:bg-gray-700 rounded-lg border border-gray-200 dark:border-gray-600"
                  >
                    <div className="flex items-start justify-between mb-2">
                      <div className="flex items-center space-x-3">
                        <span className={`px-2 py-1 rounded text-sm font-medium capitalize ${
                          displayAssessment === 'confirm'
                            ? 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200'
                            : displayAssessment === 'reject'
                            ? 'bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200'
                            : 'bg-yellow-100 dark:bg-yellow-900 text-yellow-800 dark:text-yellow-200'
                        }`}>
                          {displayAssessment}
                        </span>
                        <span className="text-sm text-gray-500 dark:text-gray-400">
                          Confidence: {(judgment.confidence_level * 100).toFixed(0)}%
                        </span>
                      </div>
                      <span className="text-xs text-gray-500 dark:text-gray-400">
                        {formatDateString(judgment.submitted_at)}
                      </span>
                    </div>
                    {judgment.reasoning && (
                      <p className="text-sm text-gray-700 dark:text-gray-300 mt-2">{judgment.reasoning}</p>
                    )}
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="text-center py-8">
              <p className="text-gray-500 dark:text-gray-400">
                {getEmoji('status', 'warning')} No judgments submitted yet
              </p>
              <button
                onClick={() => setIsSubmitJudgmentModalOpen(true)}
                className="mt-4 px-4 py-2 text-sm font-medium text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300"
              >
                {getEmoji('actions', 'submit')} Submit First Judgment
              </button>
            </div>
          )}
        </div>

        {/* Impacts Section */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
              {getEmoji('status', 'success')} Impacts ({impacts.length})
            </h2>
            <button
              onClick={() => setIsAddImpactModalOpen(true)}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 dark:bg-blue-500 rounded-md hover:bg-blue-700 dark:hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              {getEmoji('actions', 'create')} Add Impact
            </button>
          </div>
          
          {impacts.length > 0 ? (
            <div className="space-y-3">
              {impacts.map((impact) => {
                // Map impact_level to boolean value for display
                const isPositive = mapToBoolean(impact.impact_level);
                const displayText = getDisplayText(isPositive);
                
                return (
                  <div
                    key={impact.id}
                    className="p-4 bg-gray-50 dark:bg-gray-700 rounded-lg border border-gray-200 dark:border-gray-600"
                  >
                    <div className="flex items-start justify-between mb-2">
                      <div>
                        <span className={`text-sm font-medium ${
                          isPositive 
                            ? 'text-green-600 dark:text-green-400' 
                            : 'text-red-600 dark:text-red-400'
                        }`}>
                          {displayText}
                        </span>
                        <span className="ml-2 text-sm text-gray-500 dark:text-gray-400">
                          (Level {impact.impact_level})
                        </span>
                      </div>
                      <span className="text-xs text-gray-500 dark:text-gray-400">
                        {formatDateString(impact.created_at)}
                      </span>
                    </div>
                    {impact.notes && (
                      <p className="text-sm text-gray-700 dark:text-gray-300 mt-2">{impact.notes}</p>
                    )}
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="text-center py-8">
              <p className="text-gray-500 dark:text-gray-400">
                {getEmoji('status', 'warning')} No impacts recorded yet
              </p>
              <button
                onClick={() => setIsAddImpactModalOpen(true)}
                className="mt-4 px-4 py-2 text-sm font-medium text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300"
              >
                {getEmoji('actions', 'create')} Add First Impact
              </button>
            </div>
          )}
        </div>

        {/* Empty States */}
        {!loadingRelated && judgments.length === 0 && impacts.length === 0 && !consensus && (
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 text-center">
            <p className="text-gray-500 dark:text-gray-400">
              {getEmoji('status', 'warning')} No additional data available for this event
            </p>
          </div>
        )}
      </div>

      {/* Add Impact Modal */}
      {eventId && (
        <AddImpactModal
          isOpen={isAddImpactModalOpen}
          onClose={() => setIsAddImpactModalOpen(false)}
          eventId={eventId}
          onImpactAdded={handleImpactAdded}
        />
      )}

      {/* Submit Judgment Modal */}
      {eventId && (
        <SubmitJudgmentModal
          isOpen={isSubmitJudgmentModalOpen}
          onClose={() => setIsSubmitJudgmentModalOpen(false)}
          eventId={eventId}
          onJudgmentSubmitted={handleJudgmentSubmitted}
        />
      )}
    </div>
  );
};

