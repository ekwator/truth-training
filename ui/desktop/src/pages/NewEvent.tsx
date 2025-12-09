/**
 * New Event Screen
 * Matches Android New Event screen layout and behavior.
 * Implements template selection flow matching Android algorithm.
 * Route: event/create (new-event)
 */

import React, { useEffect, useState } from 'react';
import { Screen } from '@/components/layout/TopMenuBar';
import { useNavigationStore } from '@/stores/navigation';
import { useEventsStore } from '@/stores/events';
import { ContextPicker } from '@/components/context/ContextPicker';
import { DatePickerField } from '@/components/DatePickerField';
import { validateDateRange } from '@/utils/dateNormalization';
import { getEmoji } from '@/utils/emojiMapping';

interface NavigationState {
  eventId?: number;
  [key: string]: any;
}

interface NewEventProps {
  onNavigate: (screen: Screen, state?: NavigationState) => void;
}

export const NewEvent: React.FC<NewEventProps> = ({ onNavigate }) => {
  const { 
    selectedTemplateContext,
    setSelectTemplateForEvent,
    clearTemplateSelection 
  } = useNavigationStore();
  const { createEvent } = useEventsStore();

  // Form state
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState<number | undefined>(undefined);
  const [formaId, setFormaId] = useState<number | undefined>(undefined);
  const [causeId, setCauseId] = useState<number | undefined>(undefined);
  const [developId, setDevelopId] = useState<number | undefined>(undefined);
  const [effectId, setEffectId] = useState<number | undefined>(undefined);
  const [timestampStart, setTimestampStart] = useState<number>(Math.floor(Date.now() / 1000));
  const [timestampEnd, setTimestampEnd] = useState<number | null>(null);
  const [vector, setVector] = useState<boolean>(false); // false = Outgoing, true = Incoming

  // Validation errors
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  // Template selection flow: Observe selectedTemplateContext (Android LaunchedEffect equivalent)
  useEffect(() => {
    if (selectedTemplateContext) {
      // Update form fields with template context (Android algorithm step 8-9)
      setCategoryId(selectedTemplateContext.categoryId);
      setFormaId(selectedTemplateContext.formaId);
      setCauseId(selectedTemplateContext.causeId);
      setDevelopId(selectedTemplateContext.developId);
      setEffectId(selectedTemplateContext.effectId);
      
      // Clear template selection after use (Android algorithm step 10)
      clearTemplateSelection();
    }
  }, [selectedTemplateContext, clearTemplateSelection]);

  const handleSelectTemplate = () => {
    // Android algorithm step 1-2: Set flag and navigate
    setSelectTemplateForEvent(true);
    onNavigate('context-editor');
  };

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    // Name: Required
    if (!name.trim()) {
      newErrors.name = 'Name is required';
    }

    // Description: Required
    if (!description.trim()) {
      newErrors.description = 'Description is required';
    }

    // All Context Fields: Required (Android validation rule 3)
    if (categoryId === undefined || categoryId === null) {
      newErrors.categoryId = 'Category is required';
    }
    if (formaId === undefined || formaId === null) {
      newErrors.formaId = 'Forma is required';
    }
    if (causeId === undefined || causeId === null) {
      newErrors.causeId = 'Cause is required';
    }
    if (developId === undefined || developId === null) {
      newErrors.developId = 'Develop is required';
    }
    if (effectId === undefined || effectId === null) {
      newErrors.effectId = 'Effect is required';
    }

    // Date validation (Android algorithm)
    const dateValidation = validateDateRange(timestampStart, timestampEnd);
    if (!dateValidation.valid && dateValidation.error) {
      newErrors.timestampEnd = dateValidation.error;
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validateForm()) {
      return;
    }

    setSubmitting(true);
    try {
      await createEvent({
        description: description || name, // Use description if provided, otherwise use name
        category_id: categoryId!,
        forma_id: formaId!,
        cause_id: causeId!,
        develop_id: developId!,
        effect_id: effectId!,
        vector: vector,
      });

      // Reset form
      setName('');
      setDescription('');
      setCategoryId(undefined);
      setFormaId(undefined);
      setCauseId(undefined);
      setDevelopId(undefined);
      setEffectId(undefined);
      setTimestampStart(Math.floor(Date.now() / 1000));
      setTimestampEnd(null);
      setVector(false);
      setErrors({});

      // Navigate back to dashboard
      onNavigate('home');
    } catch (error: any) {
      setErrors({ submit: error.message || 'Failed to create event' });
    } finally {
      setSubmitting(false);
    }
  };

  const isFormValid = name.trim() && description.trim() && 
    categoryId !== undefined && categoryId !== null &&
    formaId !== undefined && formaId !== null &&
    causeId !== undefined && causeId !== null &&
    developId !== undefined && developId !== null &&
    effectId !== undefined && effectId !== null &&
    Object.keys(errors).length === 0;

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-6">
          {getEmoji('screens', 'newEvent')} New Event
        </h1>

        <div className="space-y-6">
          {/* Name Field */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              {getEmoji('fields', 'name')} Name <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className={`w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:text-gray-100 dark:border-gray-600 ${errors.name ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'}`}
              required
            />
            {errors.name && <p className="mt-1 text-sm text-red-500">{errors.name}</p>}
          </div>

          {/* Description Field */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              {getEmoji('fields', 'description')} Description <span className="text-red-500">*</span>
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={4}
              className={`w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:text-gray-100 dark:border-gray-600 ${errors.description ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'}`}
              required
            />
            {errors.description && <p className="mt-1 text-sm text-red-500">{errors.description}</p>}
          </div>

          {/* Context Fields Section */}
          <div>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Context Fields</h2>
              <button
                onClick={handleSelectTemplate}
                className="px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded hover:bg-gray-200 dark:hover:bg-gray-600"
              >
                {getEmoji('actions', 'create')} Select Template
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <ContextPicker
                label={`${getEmoji('fields', 'category')} Category`}
                value={categoryId}
                onChange={setCategoryId}
                error={errors.categoryId}
                required
              />
              <ContextPicker
                label={`${getEmoji('fields', 'forma')} Forma`}
                value={formaId}
                onChange={setFormaId}
                error={errors.formaId}
                required
              />
              <ContextPicker
                label={`${getEmoji('fields', 'cause')} Cause`}
                value={causeId}
                onChange={setCauseId}
                error={errors.causeId}
                required
              />
              <ContextPicker
                label={`${getEmoji('fields', 'develop')} Develop`}
                value={developId}
                onChange={setDevelopId}
                error={errors.developId}
                required
              />
              <ContextPicker
                label={`${getEmoji('fields', 'effect')} Effect`}
                value={effectId}
                onChange={setEffectId}
                error={errors.effectId}
                required
              />
            </div>
          </div>

          {/* Timestamps */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <DatePickerField
              label={`${getEmoji('fields', 'startDate')} Start Timestamp`}
              value={timestampStart}
              onChange={(value) => setTimestampStart(value ?? Math.floor(Date.now() / 1000))}
              required
              minDate={timestampEnd ? timestampEnd : undefined}
            />
            <DatePickerField
              label={`${getEmoji('fields', 'endDate')} End Timestamp`}
              value={timestampEnd}
              onChange={(value) => setTimestampEnd(value)}
              allowClear
              minDate={timestampStart}
              error={errors.timestampEnd}
            />
          </div>

          {/* Vector Toggle */}
          <div>
            <label className="flex items-center space-x-2">
              <input
                type="checkbox"
                checked={vector}
                onChange={(e) => setVector(e.target.checked)}
                className="w-4 h-4 dark:bg-gray-700 dark:border-gray-600"
              />
              <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Incoming (checked) / Outgoing (unchecked)</span>
            </label>
          </div>

          {/* Submit Button */}
          <div className="flex justify-end space-x-4">
            <button
              onClick={() => onNavigate('home')}
              className="px-4 py-2 bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded hover:bg-gray-300 dark:hover:bg-gray-600"
            >
              {getEmoji('actions', 'cancel')} Cancel
            </button>
            <button
              onClick={handleSubmit}
              disabled={!isFormValid || submitting}
              className="px-4 py-2 bg-blue-600 dark:bg-blue-500 text-white rounded hover:bg-blue-700 dark:hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {getEmoji('actions', 'save')} {submitting ? 'Saving...' : 'Save'}
            </button>
          </div>

          {errors.submit && (
            <p className="text-sm text-red-500">{errors.submit}</p>
          )}
        </div>
      </div>
    </div>
  );
};

