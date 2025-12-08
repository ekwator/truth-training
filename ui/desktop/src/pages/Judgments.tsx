import React, { useEffect, useState } from 'react';
import { useJudgmentsStore } from '@/stores/judgments';
import { useEventsStore } from '@/stores/events';
import { useToast } from '@/components/system/Toaster';
import { JudgmentCard } from '@/components/JudgmentPanel/JudgmentCard';
import { ApiService } from '@/services/api';
import { Screen } from '@/components/layout/TopMenuBar';
import type { JudgmentAssessment } from '@/types/judgments';
import { t } from '@/i18n';

interface NavigationState {
  eventId?: number;
  [key: string]: any;
}

interface JudgmentsProps {
  eventId?: number;
  onNavigate?: (screen: Screen, state?: NavigationState) => void;
}

export const Judgments: React.FC<JudgmentsProps> = ({ eventId, onNavigate }) => {
  const { judgments, loading, error, fetchJudgments, filters, setFilters } = useJudgmentsStore();
  const { events } = useEventsStore();
  void events; // Suppress unused warning
  const [selectedEventId, setSelectedEventId] = useState<number | null>(eventId || null);
  const [event, setEvent] = useState<any | null>(null);
  const [showSubmission, setShowSubmission] = useState(false);

  useEffect(() => {
    if (selectedEventId) {
      fetchJudgments(selectedEventId);
      // Fetch event details
      ApiService.getEvent(selectedEventId)
        .then(setEvent)
        .catch(console.error);
    } else {
      fetchJudgments();
    }
  }, [fetchJudgments, selectedEventId]);

  const handleEventFilter = (eventId: number | null) => {
    setSelectedEventId(eventId);
    setFilters({ event_id: eventId ?? undefined });
  };

  const filteredJudgments = judgments.filter(judgment => {
    if (selectedEventId === null) return true;
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
          <h2 className="text-2xl font-bold text-red-600 mb-4">{t('judgments.errorLoading')}</h2>
          <p className="text-gray-600 mb-4">{error}</p>
          <button
            onClick={() => selectedEventId ? fetchJudgments(selectedEventId) : fetchJudgments()}
            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            {t('common.retry')}
          </button>
        </div>
      </div>
    );
  }

  // Judgment Submission Mode
  if (showSubmission && selectedEventId) {
    return <JudgmentSubmission eventId={selectedEventId} onBack={() => setShowSubmission(false)} onNavigate={onNavigate} />;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">{t('judgments.title')}</h1>
              <p className="text-sm text-gray-600">{t('judgments.subtitle')}</p>
            </div>
            {selectedEventId && (
              <button
                onClick={() => setShowSubmission(true)}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
              >
                {t('judgments.addJudgment')}
              </button>
            )}
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Event Title Card */}
        {event && (
          <div className="mb-6 bg-white rounded-lg shadow p-6">
            <h2 className="text-lg font-semibold text-gray-900">{event.description}</h2>
          </div>
        )}

        {/* Consensus Statistics Card */}
        {filteredJudgments.length > 0 && (
          <div className="mb-6 grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="bg-white rounded-lg shadow p-4">
              <p className="text-sm font-medium text-gray-500">{t('judgments.true')}</p>
              <p className="text-2xl font-semibold text-gray-900">
                {filteredJudgments.filter(j => j.assessment === 'confirm').length}
              </p>
            </div>
            <div className="bg-white rounded-lg shadow p-4">
              <p className="text-sm font-medium text-gray-500">{t('judgments.false')}</p>
              <p className="text-2xl font-semibold text-gray-900">
                {filteredJudgments.filter(j => j.assessment === 'reject').length}
              </p>
            </div>
            <div className="bg-white rounded-lg shadow p-4">
              <p className="text-sm font-medium text-gray-500">{t('judgments.uncertain')}</p>
              <p className="text-2xl font-semibold text-gray-900">
                {filteredJudgments.filter(j => j.assessment === 'abstain').length}
              </p>
            </div>
            <div className="bg-white rounded-lg shadow p-4">
              <p className="text-sm font-medium text-gray-500">{t('judgments.consensusPercentage')}</p>
              <p className="text-2xl font-semibold text-gray-900">
                {filteredJudgments.length > 0 
                  ? ((filteredJudgments.filter(j => j.assessment === 'confirm').length / filteredJudgments.length) * 100).toFixed(1)
                  : '0.0'
                }%
              </p>
            </div>
          </div>
        )}

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
                  value={selectedEventId !== null ? selectedEventId.toString() : ''}
                  onChange={(e) => {
                    const value = e.target.value;
                    handleEventFilter(value === '' ? null : Number(value));
                  }}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value="">All Events</option>
                  {/* TODO: Populate with actual events from store */}
                </select>
              </div>
              <div>
                <label htmlFor="assessment-filter" className="block text-sm font-medium text-gray-700 mb-2">
                  Assessment
                </label>
                <select
                  id="assessment-filter"
                  value={filters.assessment || ''}
                  onChange={(e) => {
                    const value = e.target.value as JudgmentAssessment | '';
                    setFilters({ assessment: value === '' ? undefined : value });
                  }}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value="">All Assessments</option>
                  <option value="confirm">Confirm</option>
                  <option value="reject">Reject</option>
                  <option value="abstain">Abstain</option>
                </select>
              </div>
              <div>
                <label htmlFor="confidence-filter" className="block text-sm font-medium text-gray-700 mb-2">
                  Confidence Level
                </label>
                <select
                  id="confidence-filter"
                  value={filters.confidence_min !== undefined ? filters.confidence_min.toString() : ''}
                  onChange={(e) => {
                    const value = e.target.value;
                    setFilters({ confidence_min: value ? parseFloat(value) : undefined });
                  }}
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
                <h3 className="text-lg font-medium text-gray-900 mb-2">{t('judgments.noJudgments')}</h3>
                <p className="text-gray-500">
                  {selectedEventId !== null ? t('judgments.noJudgmentsForEvent') : t('judgments.noJudgmentsDescription')}
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
                  <p className="text-sm font-medium text-gray-500">{t('judgments.true')}</p>
                  <p className="text-2xl font-semibold text-gray-900">
                    {filteredJudgments.filter(j => j.assessment === 'confirm').length}
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

// Judgment Submission Component
interface JudgmentSubmissionProps {
  eventId: number;
  onBack: () => void;
  onNavigate?: (screen: Screen, state?: NavigationState) => void;
}

const JudgmentSubmission: React.FC<JudgmentSubmissionProps> = ({ eventId, onBack, onNavigate }) => {
  const { addToast } = useToast();
  const [event, setEvent] = useState<any | null>(null);
  const [loading, setLoading] = useState(false);
  const [assessment, setAssessment] = useState<JudgmentAssessment>('confirm');
  const [confidenceLevel, setConfidenceLevel] = useState<number>(0.7);
  const [reasoning, setReasoning] = useState<string>('');
  const [assessmentError, setAssessmentError] = useState<string | null>(null);
  const [confidenceError, setConfidenceError] = useState<string | null>(null);

  useEffect(() => {
    ApiService.getEvent(eventId)
      .then(setEvent)
      .catch(console.error);
  }, [eventId]);

  const handleSubmit = async () => {
    // Clear errors
    setAssessmentError(null);
    setConfidenceError(null);
    let hasError = false;

    // Validate assessment
    if (!assessment || !['confirm', 'reject', 'abstain'].includes(assessment)) {
      setAssessmentError(t('judgments.assessmentRequired'));
      hasError = true;
    }

    // Validate confidence level
    if (confidenceLevel < 0.0 || confidenceLevel > 1.0) {
      setConfidenceError(t('judgments.confidenceRange'));
      hasError = true;
    }

    if (hasError) {
      return;
    }

    setLoading(true);
    try {
      await ApiService.createJudgment({
        event_id: eventId,
        assessment,
        confidence_level: confidenceLevel,
        reasoning: reasoning || undefined,
        signature: 'local'
      });
      addToast({
        type: 'success',
        title: t('judgments.judgmentSubmitted'),
        message: t('judgments.judgmentSubmittedMessage')
      });
      onBack();
      // Refresh judgments list
      if (onNavigate) {
        // Trigger refresh by navigating back and forth
        setTimeout(() => {
          window.location.reload(); // Simple refresh for now
        }, 500);
      }
    } catch (error: any) {
      addToast({
        type: 'error',
        title: t('judgments.errorSubmitting'),
        message: error?.message || t('judgments.errorSubmittingMessage')
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Top App Bar */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <h1 className="text-2xl font-bold text-gray-900">{t('judgments.submitJudgment')}</h1>
            <div className="flex items-center space-x-2">
              <button
                onClick={handleSubmit}
                disabled={loading}
                className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50"
              >
                {loading ? t('common.saving') : t('judgments.submit')}
              </button>
              <button
                onClick={onBack}
                className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300"
              >
                {t('common.cancel')}
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-white rounded-lg shadow p-6 space-y-6">
          {/* Event Card */}
          {event && (
            <div className="bg-gray-50 rounded-lg p-4">
              <h3 className="text-lg font-semibold text-gray-900">{event.description}</h3>
            </div>
          )}

          {/* Assessment Field */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              {t('judgments.assessment')} *
            </label>
            <div className="space-y-2">
              <label className="flex items-center">
                <input
                  type="radio"
                  value="confirm"
                  checked={assessment === 'confirm'}
                  onChange={(e) => {
                    setAssessment(e.target.value as JudgmentAssessment);
                    if (assessmentError) setAssessmentError(null);
                  }}
                  className="mr-2"
                />
                <span>{t('judgments.true')}</span>
              </label>
              <label className="flex items-center">
                <input
                  type="radio"
                  value="reject"
                  checked={assessment === 'reject'}
                  onChange={(e) => {
                    setAssessment(e.target.value as JudgmentAssessment);
                    if (assessmentError) setAssessmentError(null);
                  }}
                  className="mr-2"
                />
                <span>{t('judgments.false')}</span>
              </label>
              <label className="flex items-center">
                <input
                  type="radio"
                  value="abstain"
                  checked={assessment === 'abstain'}
                  onChange={(e) => {
                    setAssessment(e.target.value as JudgmentAssessment);
                    if (assessmentError) setAssessmentError(null);
                  }}
                  className="mr-2"
                />
                <span>{t('judgments.uncertain')}</span>
              </label>
            </div>
            {assessmentError && (
              <p className="mt-1 text-sm text-red-600">{assessmentError}</p>
            )}
          </div>

          {/* Confidence Level Field */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              {t('judgments.confidenceLevel')} (0.0 - 1.0) *
            </label>
            <input
              type="number"
              min="0"
              max="1"
              step="0.05"
              value={confidenceLevel}
              onChange={(e) => {
                setConfidenceLevel(parseFloat(e.target.value) || 0);
                if (confidenceError) setConfidenceError(null);
              }}
              className={`w-full px-3 py-2 border rounded ${
                confidenceError ? 'border-red-500' : 'border-gray-300'
              }`}
            />
            {confidenceError && (
              <p className="mt-1 text-sm text-red-600">{confidenceError}</p>
            )}
          </div>

          {/* Reasoning Field */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              {t('judgments.reasoning')}
            </label>
            <textarea
              value={reasoning}
              onChange={(e) => setReasoning(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded"
              rows={4}
              placeholder={t('judgments.reasoningPlaceholder')}
            />
          </div>
        </div>
      </main>
    </div>
  );
};
