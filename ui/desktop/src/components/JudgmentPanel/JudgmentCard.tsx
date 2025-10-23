import React from 'react';
import { Judgment } from '@/types/judgments';

interface JudgmentCardProps {
  judgment: Judgment;
}

export const JudgmentCard: React.FC<JudgmentCardProps> = ({ judgment }) => {
  const getAssessmentColor = (assessment: string) => {
    switch (assessment) {
      case 'true':
        return 'bg-green-100 text-green-800';
      case 'false':
        return 'bg-red-100 text-red-800';
      case 'uncertain':
        return 'bg-yellow-100 text-yellow-800';
      default:
        return 'bg-gray-100 text-gray-800';
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
    <div className="bg-white border border-gray-200 rounded-lg p-6 hover:shadow-md transition-shadow">
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
            <p className="text-sm text-gray-700 mb-3 line-clamp-3">
              {judgment.reasoning}
            </p>
          )}
        </div>
      </div>

      <div className="flex items-center justify-between text-sm text-gray-500">
        <div className="flex items-center space-x-4">
          <span className="flex items-center">
            <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            {truncateId(judgment.participant_id)}
          </span>
          <span className="flex items-center">
            <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {formatDate(judgment.submitted_at)}
          </span>
        </div>
        
        <div className="flex items-center space-x-2">
          <button className="px-2 py-1 text-xs bg-blue-100 text-blue-700 rounded hover:bg-blue-200 transition-colors">
            View Details
          </button>
          <button className="px-2 py-1 text-xs bg-gray-100 text-gray-700 rounded hover:bg-gray-200 transition-colors">
            Event
          </button>
        </div>
      </div>

      {/* Confidence Bar */}
      <div className="mt-3">
        <div className="flex items-center justify-between text-xs text-gray-500 mb-1">
          <span>Confidence Level</span>
          <span>{(judgment.confidence_level * 100).toFixed(0)}%</span>
        </div>
        <div className="w-full bg-gray-200 rounded-full h-2">
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
