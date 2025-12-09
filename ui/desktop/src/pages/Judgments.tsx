/**
 * Judgments Screen
 * Matches Android Judgments screen layout and behavior.
 * Route: judgments
 */

import React, { useEffect, useState } from 'react';
import { Screen } from '@/components/layout/TopMenuBar';
import { useNavigationStore } from '@/stores/navigation';
import { ApiService } from '@/services/api';
import { getEmoji } from '@/utils/emojiMapping';

interface NavigationState {
  eventId?: number;
  [key: string]: any;
}

interface JudgmentsProps {
  eventId?: number;
  onNavigate: (screen: Screen, state?: NavigationState) => void;
}

export const Judgments: React.FC<JudgmentsProps> = ({ eventId, onNavigate: _onNavigate }) => {
  const { selectedEventIdForJudgments } = useNavigationStore();
  const [judgments, setJudgments] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  const effectiveEventId = eventId || (selectedEventIdForJudgments ? parseInt(selectedEventIdForJudgments) : undefined);

  useEffect(() => {
    if (effectiveEventId) {
      loadJudgments();
    }
  }, [effectiveEventId]);

  const loadJudgments = async () => {
    if (!effectiveEventId) return;
    setLoading(true);
    try {
      const response = await ApiService.getJudgments(effectiveEventId);
      setJudgments(response.data || []);
    } catch (error) {
      console.error('Failed to load judgments:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-6">
        {getEmoji('screens', 'judgments')} Judgments
      </h1>

      {loading ? (
        <div className="text-center py-8 text-gray-700 dark:text-gray-300">{getEmoji('status', 'syncing')} Loading judgments...</div>
      ) : judgments.length === 0 ? (
        <div className="text-center py-8 text-gray-500 dark:text-gray-400">{getEmoji('status', 'warning')} No judgments found</div>
      ) : (
        <div className="space-y-4">
          {judgments.map((judgment) => (
            <div key={judgment.id} className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-semibold text-gray-900 dark:text-gray-100">Assessment: {judgment.assessment}</p>
                  <p className="text-sm text-gray-600 dark:text-gray-400">Confidence: {judgment.confidence_level}</p>
                  {judgment.reasoning && (
                    <p className="text-sm text-gray-600 dark:text-gray-400 mt-2">{judgment.reasoning}</p>
                  )}
                </div>
                <div className="text-sm text-gray-500 dark:text-gray-400">
                  {new Date(judgment.submitted_at).toLocaleString()}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

