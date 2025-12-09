import React from 'react';
import { Judgment } from '@/types/judgments';
import { getEmoji } from '@/utils/emojiMapping';

interface JudgmentCardProps {
  judgment: Judgment;
}

export const JudgmentCard: React.FC<JudgmentCardProps> = ({ judgment }) => {
  const getAssessmentColor = (assessment: string) => {
    switch (assessment) {
      case 'confirm':
        return 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200';
      case 'reject':
        return 'bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200';
      case 'abstain':
        return 'bg-yellow-100 dark:bg-yellow-900 text-yellow-800 dark:text-yellow-200';
      default:
        return 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200';
    }
  };

  const getConfidenceColor = (confidence: number) => {
    if (confidence >= 0.8) return 'text-green-600';
    if (confidence >= 0.5) return 'text-yellow-600';
    return 'text-red-600';
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

  const truncateId = (id: string) => {
    return `${id.substring(0, 8)}...${id.substring(id.length - 8)}`;
  };

  return (
    <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-6 hover:shadow-md dark:hover:shadow-gray-700 transition-shadow">
      <div className="flex items-start justify-between mb-4">
        <div className="flex-1">
          <div className="flex items-center space-x-3 mb-2">
            <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getAssessmentColor(judgment.assessment)}`}>
              {judgment.assessment.toUpperCase()}
            </span>
            <span className={`text-sm font-medium ${getConfidenceColor(judgment.confidence_level)}`}>
              {(judgment.confidence_level * 100).toFixed(0)}% confidence
            </span>
          </div>
          
          {judgment.reasoning && (
            <p className="text-sm text-gray-700 dark:text-gray-300 mb-3 line-clamp-3">
              {judgment.reasoning}
            </p>
          )}
        </div>
      </div>

      <div className="flex items-center justify-between text-sm text-gray-500 dark:text-gray-400">
        <div className="flex items-center space-x-4">
          <span className="flex items-center">{truncateId(judgment.participant_id)}</span>
          <span className="flex items-center">{formatDate(judgment.submitted_at)}</span>
        </div>
        
        <div className="flex items-center space-x-2">
          <button className="px-2 py-1 text-xs bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-200 rounded hover:bg-blue-200 dark:hover:bg-blue-800 transition-colors">
            {getEmoji('actions', 'edit')} View Details
          </button>
          <button className="px-2 py-1 text-xs bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors">
            {getEmoji('navigation', 'events')} Event
          </button>
        </div>
      </div>

      {/* Confidence Bar */}
      <div className="mt-3">
        <div className="flex items-center justify-between text-xs text-gray-500 dark:text-gray-400 mb-1">
          <span>{getEmoji('fields', 'confidence')} Confidence Level</span>
          <span>{(judgment.confidence_level * 100).toFixed(0)}%</span>
        </div>
        <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
          <div
            className={`h-2 rounded-full transition-all duration-300 ${
              judgment.confidence_level >= 0.8 ? 'bg-green-500' :
              judgment.confidence_level >= 0.5 ? 'bg-yellow-500' : 'bg-red-500'
            }`}
            style={{ width: `${judgment.confidence_level * 100}%` }}
          />
        </div>
      </div>
    </div>
  );
};
