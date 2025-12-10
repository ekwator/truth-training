/**
 * AddImpactModal Component
 * Modal dialog for adding impacts to events
 * 
 * Features:
 * - Impact level slider (1-5)
 * - Optional notes field
 * - Form validation
 * - Emoji support (Rule 8)
 * - Localization (EN/RU)
 */

import React, { useState } from 'react';
import { Dialog } from '@headlessui/react';
import { XMarkIcon } from '@heroicons/react/24/outline';
import { ApiService, AddImpactRequest, Impact } from '@/services/api';
import { mapToBoolean, isValid, getDisplayText } from '@/utils/impactLevelMapper';
import { getEmoji } from '@/utils/emojiMapping';
import { useToast } from '@/components/system/Toaster';

interface AddImpactModalProps {
  isOpen: boolean;
  onClose: () => void;
  eventId: number;
  onImpactAdded?: (impact: Impact) => void;
}

export const AddImpactModal: React.FC<AddImpactModalProps> = ({
  isOpen,
  onClose,
  eventId,
  onImpactAdded
}) => {
  const [impactLevel, setImpactLevel] = useState<number>(3);
  const [notes, setNotes] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const { addToast } = useToast();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // Validate impact level
    if (!isValid(impactLevel)) {
      setError('Impact level must be between 1 and 5');
      return;
    }

    setLoading(true);
    try {
      const request: AddImpactRequest = {
        event_id: eventId,
        impact_level: impactLevel,
        notes: notes.trim() || undefined
      };

      const impact = await ApiService.addImpact(request);
      
      // Notify parent component
      if (onImpactAdded) {
        onImpactAdded(impact);
      }

      // Show success message
      addToast({
        type: 'success',
        title: 'Impact Added',
        message: `Impact added successfully: ${getDisplayText(mapToBoolean(impactLevel))}`
      });

      // Reset form and close
      setImpactLevel(3);
      setNotes('');
      onClose();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to add impact';
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
    setImpactLevel(3);
    setNotes('');
    setError(null);
    onClose();
  };

  const isPositive = impactLevel > 3;
  const displayText = getDisplayText(mapToBoolean(impactLevel));

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
                {getEmoji('actions', 'create')} Add Impact
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
              {/* Impact Level Slider */}
              <div>
                <label htmlFor="impact-level" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  {getEmoji('fields', 'impact')} Impact Level: {impactLevel} ({displayText})
                </label>
                <input
                  type="range"
                  id="impact-level"
                  min="1"
                  max="5"
                  step="1"
                  value={impactLevel}
                  onChange={(e) => setImpactLevel(Number(e.target.value))}
                  className="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-blue-600"
                />
                <div className="flex justify-between text-xs text-gray-500 dark:text-gray-400 mt-1">
                  <span>1 (Negative)</span>
                  <span>3</span>
                  <span>5 (Positive)</span>
                </div>
                <div className={`mt-2 text-sm font-medium ${
                  isPositive 
                    ? 'text-green-600 dark:text-green-400' 
                    : 'text-red-600 dark:text-red-400'
                }`}>
                  {displayText}
                </div>
              </div>

              {/* Notes */}
              <div>
                <label htmlFor="notes" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  {getEmoji('fields', 'notes')} Notes (Optional)
                </label>
                <textarea
                  id="notes"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  rows={3}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm dark:bg-gray-700 dark:text-gray-100 focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  placeholder="Add optional notes about this impact..."
                  maxLength={1000}
                />
                <div className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                  {notes.length}/1000 characters
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
                  disabled={loading || !isValid(impactLevel)}
                  className="px-4 py-2 text-sm font-medium text-white bg-blue-600 dark:bg-blue-500 rounded-md hover:bg-blue-700 dark:hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {loading ? (
                    <>
                      {getEmoji('status', 'loading')} Saving...
                    </>
                  ) : (
                    <>
                      {getEmoji('actions', 'save')} Save Impact
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

