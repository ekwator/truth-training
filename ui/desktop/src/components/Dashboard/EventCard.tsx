import React, { useState } from 'react';
import { Event } from '@/types/events';
import { Modal } from '@/components/system/Modal';
import { ApiService } from '@/services/api';

interface EventCardProps {
  event: Event;
}

export const EventCard: React.FC<EventCardProps> = ({ event }) => {
  const [openView, setOpenView] = useState(false);
  const [openJudge, setOpenJudge] = useState(false);
  const [judgeAssessment, setJudgeAssessment] = useState<'true'|'false'|'uncertain'>('true');
  const [judgeConfidence, setJudgeConfidence] = useState<number>(0.7);
  const [judgeReasoning, setJudgeReasoning] = useState<string>('');
  const [submitting, setSubmitting] = useState(false);
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'active':
        return 'bg-green-100 text-green-800';
      case 'inactive':
        return 'bg-gray-100 text-gray-800';
      case 'archived':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className="bg-white rounded-lg shadow hover:shadow-md transition-shadow duration-200">
      <div className="p-6">
        <div className="flex items-start justify-between mb-4">
          <div className="flex-1">
            <h3 className="text-lg font-semibold text-gray-900 mb-2 line-clamp-2">
              {event.title}
            </h3>
            <p className="text-sm text-gray-600 line-clamp-3">
              {event.description}
            </p>
          </div>
          <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(event.status)}`}>
            {event.status}
          </span>
        </div>

        <div className="flex items-center justify-between text-sm text-gray-500 mb-4">
          <span>Created {formatDate(event.created_at)}</span>
          {event.updated_at && (
            <span>Updated {formatDate(event.updated_at)}</span>
          )}
        </div>

        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4 text-sm text-gray-500">
            <span className="flex items-center">
              <svg className="w-4 h-4 mr-1 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
              - participants
            </span>
            <span className="flex items-center">
              <svg className="w-4 h-4 mr-1 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              - consensus
            </span>
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
          <div className="text-base font-semibold">{event.title}</div>
          <div className="text-sm text-gray-700">{event.description}</div>
          <div className="text-xs text-gray-500">Status: {event.status}</div>
          <div className="text-xs text-gray-500">Created: {new Date(event.created_at).toLocaleString()}</div>
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
              <option value="true">True</option>
              <option value="false">False</option>
              <option value="uncertain">Uncertain</option>
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
