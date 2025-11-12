import React, { useState, useEffect } from 'react';
import { Event } from '@/types/events';
import { Modal } from '@/components/system/Modal';
import { ApiService } from '@/services/api';
import { ContextTemplate } from '@/types/contexts';
import { Screen } from '@/components/layout/TopMenuBar';
import { useContextEditorStore } from '@/stores/contextEditor';

interface EventCardProps {
  event: Event;
  onNavigate?: (screen: Screen) => void;
}

export const EventCard: React.FC<EventCardProps> = ({ event, onNavigate }) => {
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
    <div className="bg-white rounded-lg shadow hover:shadow-md transition-shadow duration-200">
      <div className="p-6">
        <div className="flex items-start justify-between mb-4">
          <div className="flex-1">
            <h3 className="text-lg font-semibold text-gray-900 mb-2 line-clamp-2">
              {event.description}
            </h3>
            <p className="text-sm text-gray-600 line-clamp-3">
              {event.description}
            </p>
          </div>
          <div className="flex items-center space-x-2">
            {isConfession && (
              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
                Confession
              </span>
            )}
            {event.detected !== undefined && (
              <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${event.detected ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                {event.detected ? 'Detected' : 'Not Detected'}
              </span>
            )}
          </div>
        </div>

        <div className="flex items-center justify-between text-sm text-gray-500 mb-4">
          <span>Created {formatDate(event.timestamp_start)}</span>
          {event.timestamp_end && (
            <span>Ended {formatDate(event.timestamp_end)}</span>
          )}
        </div>

        {/* Template matching display */}
        {matchingTemplate ? (
          <div className="mb-4 text-xs text-gray-500">Matching template...</div>
        ) : matchedTemplate ? (
          <div className="mb-4 px-3 py-2 bg-green-50 border border-green-200 rounded text-sm">
            <span className="text-green-800">
              <strong>Template:</strong> {matchedTemplate.name}
            </span>
          </div>
        ) : (event.category_id || event.forma_id || event.cause_id || event.develop_id || event.effect_id) ? (
          <div className="mb-4">
            <button
              onClick={handleCreateTemplate}
              className="px-3 py-1 text-xs bg-blue-100 text-blue-700 rounded hover:bg-blue-200 transition-colors"
            >
              [Create Template]
            </button>
          </div>
        ) : null}

        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4 text-sm text-gray-500">
            <span className="flex items-center">participants: -</span>
            <span className="flex items-center">consensus: -</span>
          </div>
          
          <div className="flex space-x-2">
            <button className="px-3 py-1 text-sm bg-blue-100 text-blue-700 rounded hover:bg-blue-200 transition-colors" onClick={() => setOpenView(true)}>
              View
            </button>
            <button className="px-3 py-1 text-sm bg-gray-100 text-gray-700 rounded hover:bg-gray-200 transition-colors" onClick={() => setOpenJudge(true)}>
              Judge
            </button>
          </div>
        </div>
      </div>
      <Modal open={openView} onClose={() => setOpenView(false)} title="Event Details">
        <div className="space-y-2">
          <div className="text-sm text-gray-500">ID: {event.id}</div>
          <div className="text-base font-semibold">{event.description}</div>
          <div className="text-xs text-gray-500">Vector: {event.vector ? 'Outgoing' : 'Incoming'}</div>
          <div className="text-xs text-gray-500">Created: {new Date(event.timestamp_start * 1000).toLocaleString()}</div>
        </div>
      </Modal>
      <Modal open={openJudge} onClose={() => setOpenJudge(false)} title="Submit Judgment" footer={
        <>
          <button className="px-3 py-2 text-sm bg-gray-100 text-gray-700 rounded hover:bg-gray-200" onClick={() => setOpenJudge(false)}>Cancel</button>
          <button disabled={submitting} className="px-3 py-2 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 disabled:bg-gray-300" onClick={async () => {
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
          }}>Submit</button>
        </>
      }>
        <div className="space-y-3">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Assessment</label>
            <select className="w-full px-3 py-2 border border-gray-300 rounded-md" value={judgeAssessment} onChange={(e) => setJudgeAssessment(e.target.value as any)}>
              <option value="confirm">Confirm</option>
              <option value="reject">Reject</option>
              <option value="abstain">Abstain</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Confidence (0..1)</label>
            <input type="number" min={0} max={1} step={0.05} value={judgeConfidence} onChange={(e)=> setJudgeConfidence(Number(e.target.value))} className="w-full px-3 py-2 border border-gray-300 rounded-md" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Reasoning</label>
            <textarea rows={3} className="w-full px-3 py-2 border border-gray-300 rounded-md" placeholder="Explain your judgment..." value={judgeReasoning} onChange={(e)=> setJudgeReasoning(e.target.value)} />
          </div>
        </div>
      </Modal>
    </div>
  );
};
