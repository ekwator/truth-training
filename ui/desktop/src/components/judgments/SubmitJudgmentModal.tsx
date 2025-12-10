/**
 * SubmitJudgmentModal Component
 * Modal dialog for submitting judgments for events
 * 
 * Features:
 * - Assessment selection (confirm/reject/abstain)
 * - Confidence level slider (0.0-1.0)
 * - Optional reasoning field
 * - Form validation
 * - Emoji support (Rule 8)
 * - Localization (EN/RU)
 */

import React, { useState } from 'react';
import { Dialog } from '@headlessui/react';
import { XMarkIcon } from '@heroicons/react/24/outline';
import { ApiService } from '@/services/api';
import { Judgment, JudgmentAssessment } from '@/types/judgments';
import { getEmoji } from '@/utils/emojiMapping';
import { useToast } from '@/components/system/Toaster';

interface SubmitJudgmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  eventId: number;
  onJudgmentSubmitted?: (judgment: Judgment) => void;
}

export const SubmitJudgmentModal: React.FC<SubmitJudgmentModalProps> = ({
  isOpen,
  onClose,
  eventId,
  onJudgmentSubmitted
}) => {
  const [assessment, setAssessment] = useState<JudgmentAssessment | ''>('');
  const [confidenceLevel, setConfidenceLevel] = useState<number>(0.5);
  const [reasoning, setReasoning] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const { addToast } = useToast();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // Validate assessment
    if (!assessment || (assessment !== 'confirm' && assessment !== 'reject' && assessment !== 'abstain')) {
      setError('Please select an assessment');
      return;
    }

    // Validate confidence level
    if (confidenceLevel < 0.0 || confidenceLevel > 1.0) {
      setError('Confidence level must be between 0.0 and 1.0');
      return;
    }

    setLoading(true);
    try {
      // Map assessment to API format: 'confirm' → "true", 'reject' → "false", 'abstain' → "uncertain"
      const assessmentMap: Record<JudgmentAssessment, string> = {
        confirm: 'true',
        reject: 'false',
        abstain: 'uncertain'
      };

      // Create judgment with mapped assessment for API
      const apiRequest = {
        event_id: eventId,
        assessment: assessmentMap[assessment as JudgmentAssessment],
        confidence_level: confidenceLevel,
        reasoning: reasoning.trim() || undefined,
        signature: '' // TODO: Generate signature if required by backend
      };

      const judgment = await ApiService.createJudgment(apiRequest as any);
      
      // Map API response back to frontend format
      const reverseMap: Record<string, JudgmentAssessment> = {
        'true': 'confirm',
        'false': 'reject',
        'uncertain': 'abstain'
      };
      
      // Notify parent component
      if (onJudgmentSubmitted) {
        const frontendJudgment: Judgment = {
          ...judgment,
          assessment: reverseMap[judgment.assessment as string] || judgment.assessment as JudgmentAssessment
        };
        onJudgmentSubmitted(frontendJudgment);
      }

      // Show success message
      addToast({
        type: 'success',
        title: 'Judgment Submitted',
        message: `Judgment submitted successfully: ${assessment} (${(confidenceLevel * 100).toFixed(0)}% confidence)`
      });

      // Reset form and close
      setAssessment('');
      setConfidenceLevel(0.5);
      setReasoning('');
      onClose();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to submit judgment';
      setError(errorMessage);
      addToast({
        type: 'error',
        title: 'Error',
        message: errorMessage
      });
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    setAssessment('');
    setConfidenceLevel(0.5);
    setReasoning('');
    setError(null);
    onClose();
  };

  const getAssessmentColor = (assess: JudgmentAssessment) => {
    switch (assess) {
      case 'confirm':
        return 'text-green-600 dark:text-green-400 bg-green-50 dark:bg-green-900/20';
      case 'reject':
        return 'text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20';
      case 'abstain':
        return 'text-yellow-600 dark:text-yellow-400 bg-yellow-50 dark:bg-yellow-900/20';
      default:
        return 'text-gray-600 dark:text-gray-400 bg-gray-50 dark:bg-gray-700';
    }
  };

  return (
    <Dialog open={isOpen} onClose={handleCancel} className="relative z-50">
      {/* Backdrop */}
      <div className="fixed inset-0 bg-gray-600 bg-opacity-50 dark:bg-gray-900 dark:bg-opacity-75" aria-hidden="true" />

      {/* Modal */}
      <div className="fixed inset-0 flex items-center justify-center p-4">
        <Dialog.Panel className="w-full max-w-md bg-white dark:bg-gray-800 rounded-lg shadow-xl">
          <div className="p-6">
            {/* Header */}
            <div className="flex items-center justify-between mb-4">
              <Dialog.Title className="text-xl font-semibold text-gray-900 dark:text-gray-100">
                {getEmoji('actions', 'submit')} Submit Judgment
              </Dialog.Title>
              <button
                onClick={handleCancel}
                className="text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300"
                aria-label="Close"
              >
                <XMarkIcon className="h-6 w-6" />
              </button>
            </div>

            {/* Form */}
            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Assessment Selection */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  {getEmoji('fields', 'assessment')} Assessment *
                </label>
                <div className="space-y-2">
                  {(['confirm', 'reject', 'abstain'] as JudgmentAssessment[]).map((option) => (
                    <label
                      key={option}
                      className={`flex items-center p-3 border-2 rounded-md cursor-pointer transition-colors ${
                        assessment === option
                          ? getAssessmentColor(option) + ' border-current'
                          : 'border-gray-300 dark:border-gray-600 hover:border-gray-400 dark:hover:border-gray-500'
                      }`}
                    >
                      <input
                        type="radio"
                        name="assessment"
                        value={option}
                        checked={assessment === option}
                        onChange={(e) => setAssessment(e.target.value as JudgmentAssessment)}
                        className="mr-3"
                      />
                      <span className="font-medium capitalize">{option}</span>
                    </label>
                  ))}
                </div>
              </div>

              {/* Confidence Level Slider */}
              <div>
                <label htmlFor="confidence-level" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  {getEmoji('fields', 'confidence')} Confidence Level: {(confidenceLevel * 100).toFixed(0)}%
                </label>
                <input
                  type="range"
                  id="confidence-level"
                  min="0"
                  max="1"
                  step="0.01"
                  value={confidenceLevel}
                  onChange={(e) => setConfidenceLevel(Number(e.target.value))}
                  className="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-blue-600"
                />
                <div className="flex justify-between text-xs text-gray-500 dark:text-gray-400 mt-1">
                  <span>0% (No confidence)</span>
                  <span>50%</span>
                  <span>100% (Full confidence)</span>
                </div>
              </div>

              {/* Reasoning */}
              <div>
                <label htmlFor="reasoning" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  {getEmoji('fields', 'reasoning')} Reasoning (Optional)
                </label>
                <textarea
                  id="reasoning"
                  value={reasoning}
                  onChange={(e) => setReasoning(e.target.value)}
                  rows={4}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm dark:bg-gray-700 dark:text-gray-100 focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  placeholder="Add optional reasoning for your judgment..."
                  maxLength={2000}
                />
                <div className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                  {reasoning.length}/2000 characters
                </div>
              </div>

              {/* Error Message */}
              {error && (
                <div className="p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-md">
                  <p className="text-sm text-red-800 dark:text-red-200">{error}</p>
                </div>
              )}

              {/* Actions */}
              <div className="flex justify-end space-x-3 pt-4 border-t dark:border-gray-700">
                <button
                  type="button"
                  onClick={handleCancel}
                  disabled={loading}
                  className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {getEmoji('actions', 'cancel')} Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading || !assessment || confidenceLevel < 0.0 || confidenceLevel > 1.0}
                  className="px-4 py-2 text-sm font-medium text-white bg-blue-600 dark:bg-blue-500 rounded-md hover:bg-blue-700 dark:hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {loading ? (
                    <>
                      {getEmoji('status', 'loading')} Submitting...
                    </>
                  ) : (
                    <>
                      {getEmoji('actions', 'submit')} Submit Judgment
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </Dialog.Panel>
      </div>
    </Dialog>
  );
};

