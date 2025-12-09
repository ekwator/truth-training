import React, { useState, useEffect } from 'react';
import { Event } from '@/types/events';
import { Modal } from '@/components/system/Modal';
import { ApiService } from '@/services/api';
import { ContextTemplate } from '@/types/contexts';
import { Screen } from '@/components/layout/TopMenuBar';
import { useContextEditorStore } from '@/stores/contextEditor';
import { getEmoji } from '@/utils/emojiMapping';

interface NavigationState {
  eventId?: number;
  [key: string]: any;
}

interface EventCardProps {
  event: Event;
  onNavigate?: (screen: Screen, state?: NavigationState) => void;
  onViewEvent?: () => void;
  onViewJudgments?: () => void;
  onEditEvent?: () => void;
  onClick?: () => void;
}

export const EventCard: React.FC<EventCardProps> = ({ 
  event, 
  onNavigate,
  onViewEvent,
  onViewJudgments,
  onEditEvent,
  onClick
}) => {
  const { setPrefilledData } = useContextEditorStore();
  const [openView, setOpenView] = useState(false);
  const [openJudge, setOpenJudge] = useState(false);
  const [judgeAssessment, setJudgeAssessment] = useState<'confirm'|'reject'|'abstain'>('confirm');
  const [judgeConfidence, setJudgeConfidence] = useState<number>(0.7);
  const [judgeReasoning, setJudgeReasoning] = useState<string>('');
  const [submitting, setSubmitting] = useState(false);
  const [matchedTemplate, setMatchedTemplate] = useState<ContextTemplate | null>(null);
  const [matchingTemplate, setMatchingTemplate] = useState(false);

  const formatDate = (timestamp: number) => {
    return new Date(timestamp * 1000).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const isConfession = React.useMemo(() => {
    if (!event.description) return false;
    const d = event.description.trim();
    // Check if description suggests a confession pattern
    return d.length > 0;
  }, [event.description]);

  // Match event to context template on mount
  useEffect(() => {
    const matchTemplate = async () => {
      // Only match if event has at least one non-NULL context field
      if (!event.category_id && !event.forma_id && !event.cause_id && !event.develop_id && !event.effect_id) {
        return;
      }

      setMatchingTemplate(true);
      try {
        const response = await ApiService.matchContext({
          category_id: event.category_id,
          forma_id: event.forma_id,
          cause_id: event.cause_id,
          develop_id: event.develop_id,
          effect_id: event.effect_id,
        });
        if (response.matched && response.template) {
          setMatchedTemplate(response.template);
        }
      } catch (error) {
        console.error('Failed to match template:', error);
        // Silently fail - template matching is optional
      } finally {
        setMatchingTemplate(false);
      }
    };

    matchTemplate();
  }, [event.category_id, event.forma_id, event.cause_id, event.develop_id, event.effect_id]);

  const handleCreateTemplate = () => {
    // Set prefilled data in store and navigate to ContextEditor
    setPrefilledData({
      category_id: event.category_id,
      forma_id: event.forma_id,
      cause_id: event.cause_id,
      develop_id: event.develop_id,
      effect_id: event.effect_id,
    });
    if (onNavigate) {
      onNavigate('context-editor');
    }
  };

  return (
    <div 
      className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 hover:shadow-md dark:hover:shadow-gray-600 transition-shadow duration-200 cursor-pointer"
      onClick={onClick}
    >
      <div className="p-6">
        <div className="flex items-start justify-between mb-4">
          <div className="flex-1">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2 line-clamp-2">
              {event.description}
            </h3>
            <p className="text-sm text-gray-600 dark:text-gray-400 line-clamp-3">
              {event.description}
            </p>
          </div>
          <div className="flex items-center space-x-2">
            {isConfession && (
              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-yellow-100 dark:bg-yellow-900 text-yellow-800 dark:text-yellow-200">
                {getEmoji('status', 'warning')} Confession
              </span>
            )}
            {event.detected !== undefined && (
              <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${event.detected ? 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200' : 'bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200'}`}>
                {event.detected ? `${getEmoji('status', 'success')} Detected` : `${getEmoji('status', 'error')} Not Detected`}
              </span>
            )}
          </div>
        </div>

        <div className="flex items-center justify-between text-sm text-gray-500 dark:text-gray-400 mb-4">
          <span>Created {formatDate(event.timestamp_start)}</span>
          {event.timestamp_end && (
            <span>Ended {formatDate(event.timestamp_end)}</span>
          )}
        </div>

        {/* Template matching display */}
        {matchingTemplate ? (
          <div className="mb-4 text-xs text-gray-500 dark:text-gray-400">Matching template...</div>
        ) : matchedTemplate ? (
          <div className="mb-4 px-3 py-2 bg-green-50 dark:bg-green-900 border border-green-200 dark:border-green-700 rounded text-sm">
            <span className="text-green-800 dark:text-green-200">
              <strong>Template:</strong> {matchedTemplate.name}
            </span>
          </div>
        ) : (event.category_id || event.forma_id || event.cause_id || event.develop_id || event.effect_id) ? (
          <div className="mb-4">
            <button
              onClick={handleCreateTemplate}
              className="px-3 py-1 text-xs bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-200 rounded hover:bg-blue-200 dark:hover:bg-blue-800 transition-colors"
            >
              {getEmoji('actions', 'create')} [Create Template]
            </button>
          </div>
        ) : null}

        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4 text-sm text-gray-500 dark:text-gray-400">
            <span className="flex items-center">participants: -</span>
            <span className="flex items-center">consensus: -</span>
          </div>
          
          <div className="flex space-x-2">
            {onViewEvent ? (
              <button 
                className="px-3 py-1 text-sm bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-200 rounded hover:bg-blue-200 dark:hover:bg-blue-800 transition-colors"
                onClick={(e) => {
                  e.stopPropagation();
                  onViewEvent();
                }}
              >
                {getEmoji('screens', 'events')} View Event
              </button>
            ) : (
              <button 
                className="px-3 py-1 text-sm bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-200 rounded hover:bg-blue-200 dark:hover:bg-blue-800 transition-colors" 
                onClick={(e) => {
                  e.stopPropagation();
                  setOpenView(true);
                }}
              >
                {getEmoji('actions', 'edit')} View
              </button>
            )}
            {onEditEvent && (
              <button 
                className="px-3 py-1 text-sm bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-200 rounded hover:bg-green-200 dark:hover:bg-green-800 transition-colors"
                onClick={(e) => {
                  e.stopPropagation();
                  onEditEvent();
                }}
              >
                {getEmoji('actions', 'edit')} Edit
              </button>
            )}
            {onViewJudgments ? (
              <button 
                className="px-3 py-1 text-sm bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
                onClick={(e) => {
                  e.stopPropagation();
                  onViewJudgments();
                }}
              >
                {getEmoji('navigation', 'judgments')} View Judgments
              </button>
            ) : (
              <button 
                className="px-3 py-1 text-sm bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors" 
                onClick={(e) => {
                  e.stopPropagation();
                  setOpenJudge(true);
                }}
              >
                {getEmoji('navigation', 'judgments')} Judge
              </button>
            )}
          </div>
        </div>
      </div>
      <Modal open={openView} onClose={() => setOpenView(false)} title="Event Details">
        <div className="space-y-2">
          <div className="text-sm text-gray-500 dark:text-gray-400">ID: {event.id}</div>
          <div className="text-base font-semibold text-gray-900 dark:text-gray-100">{event.description}</div>
          <div className="text-xs text-gray-500 dark:text-gray-400">Vector: {event.vector ? 'Outgoing' : 'Incoming'}</div>
          <div className="text-xs text-gray-500 dark:text-gray-400">Created: {new Date(event.timestamp_start * 1000).toLocaleString()}</div>
        </div>
      </Modal>
      <Modal open={openJudge} onClose={() => setOpenJudge(false)} title={`${getEmoji('screens', 'judgments')} Submit Judgment`} footer={
        <>
          <button className="px-3 py-2 text-sm bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded hover:bg-gray-200 dark:hover:bg-gray-600" onClick={() => setOpenJudge(false)}>{getEmoji('actions', 'cancel')} Cancel</button>
          <button disabled={submitting} className="px-3 py-2 text-sm bg-blue-600 dark:bg-blue-500 text-white rounded hover:bg-blue-700 dark:hover:bg-blue-600 disabled:bg-gray-300 dark:disabled:bg-gray-600" onClick={async () => {
            try {
              setSubmitting(true);
              await ApiService.createJudgment({
                event_id: event.id,
                assessment: judgeAssessment,
                confidence_level: judgeConfidence,
                reasoning: judgeReasoning || undefined,
                signature: 'local'
              });
              setOpenJudge(false);
              setJudgeReasoning('');
            } catch (e) {
              console.error(e);
            } finally {
              setSubmitting(false);
            }
          }}>{getEmoji('actions', 'submit')} Submit</button>
        </>
      }>
        <div className="space-y-3">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{getEmoji('fields', 'assessment')} Assessment</label>
            <select className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-gray-100" value={judgeAssessment} onChange={(e) => setJudgeAssessment(e.target.value as any)}>
              <option value="confirm">Confirm</option>
              <option value="reject">Reject</option>
              <option value="abstain">Abstain</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{getEmoji('fields', 'confidence')} Confidence (0..1)</label>
            <input type="number" min={0} max={1} step={0.05} value={judgeConfidence} onChange={(e)=> setJudgeConfidence(Number(e.target.value))} className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-gray-100" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{getEmoji('fields', 'reasoning')} Reasoning</label>
            <textarea rows={3} className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-gray-100" placeholder="Explain your judgment..." value={judgeReasoning} onChange={(e)=> setJudgeReasoning(e.target.value)} />
          </div>
        </div>
      </Modal>
    </div>
  );
};
