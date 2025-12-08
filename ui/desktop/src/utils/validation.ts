/**
 * Validation utilities for events and templates.
 * Matches Android validation rules exactly.
 */

import { normalizeToStartOfDay } from './dateNormalization';

/**
 * Validation error interface
 */
export interface ValidationError {
  field: string;
  message: string;
}

/**
 * Event validation result
 */
export interface EventValidationResult {
  isValid: boolean;
  errors: ValidationError[];
}

/**
 * Template validation result
 */
export interface TemplateValidationResult {
  isValid: boolean;
  errors: ValidationError[];
}

/**
 * Event data for validation
 */
export interface EventValidationData {
  name?: string;
  description?: string;
  categoryId?: number | null;
  formaId?: number | null;
  causeId?: number | null;
  developId?: number | null;
  effectId?: number | null;
  timestampStart?: Date | number | null;
  timestampEnd?: Date | number | null;
}

/**
 * Template data for validation
 */
export interface TemplateValidationData {
  name?: string;
  categoryId?: number | null;
  formaId?: number | null;
  causeId?: number | null;
  developId?: number | null;
  effectId?: number | null;
}

/**
 * Validates event data according to Android validation rules.
 * 
 * Rules:
 * - Name: Required, cannot be empty
 * - Description: Required, cannot be empty
 * - All Context Fields: Required, cannot be NULL
 * - Start Timestamp: Required, defaults to current date
 * - End Timestamp: Optional, but if provided:
 *   - Cannot be less than Start Timestamp (normalized to start of day)
 *   - Can be equal to Start Timestamp
 * 
 * @param eventData - Event data to validate
 * @param getErrorMessage - Optional function to get localized error messages
 * @returns Validation result with errors
 */
export function validateEvent(
  eventData: EventValidationData,
  getErrorMessage?: (key: string) => string
): EventValidationResult {
  const errors: ValidationError[] = [];
  const t = getErrorMessage || ((key: string) => key);

  // Name validation
  if (!eventData.name || eventData.name.trim() === '') {
    errors.push({
      field: 'name',
      message: t('validation.eventNameRequired'),
    });
  }

  // Description validation
  if (!eventData.description || eventData.description.trim() === '') {
    errors.push({
      field: 'description',
      message: t('validation.eventDescriptionRequired'),
    });
  }

  // Context fields validation (all required, cannot be NULL)
  if (eventData.categoryId === null || eventData.categoryId === undefined) {
    errors.push({
      field: 'categoryId',
      message: t('validation.categoryRequired'),
    });
  }

  if (eventData.formaId === null || eventData.formaId === undefined) {
    errors.push({
      field: 'formaId',
      message: t('validation.formaRequired'),
    });
  }

  if (eventData.causeId === null || eventData.causeId === undefined) {
    errors.push({
      field: 'causeId',
      message: t('validation.causeRequired'),
    });
  }

  if (eventData.developId === null || eventData.developId === undefined) {
    errors.push({
      field: 'developId',
      message: t('validation.developRequired'),
    });
  }

  if (eventData.effectId === null || eventData.effectId === undefined) {
    errors.push({
      field: 'effectId',
      message: t('validation.effectRequired'),
    });
  }

  // Timestamp validation
  let startDate: Date | null = null;
  let endDate: Date | null = null;

  if (eventData.timestampStart) {
    startDate = eventData.timestampStart instanceof Date
      ? eventData.timestampStart
      : new Date(eventData.timestampStart * 1000); // Convert Unix timestamp to Date
  } else {
    errors.push({
      field: 'timestampStart',
      message: t('validation.startTimestampRequired'),
    });
  }

  if (eventData.timestampEnd) {
    endDate = eventData.timestampEnd instanceof Date
      ? eventData.timestampEnd
      : new Date(eventData.timestampEnd * 1000); // Convert Unix timestamp to Date
  }

  // End timestamp validation (if provided)
  // Use normalized dates for comparison to handle timezone and DST edge cases
  if (startDate && endDate) {
    const normalizedStart = normalizeToStartOfDay(startDate);
    const normalizedEnd = normalizeToStartOfDay(endDate);

    if (normalizedEnd < normalizedStart) {
      errors.push({
        field: 'timestampEnd',
        message: t('validation.endTimestampBeforeStart'),
      });
    }
    // Note: End can be equal to Start (no error in that case)
  }

  return {
    isValid: errors.length === 0,
    errors,
  };
}

/**
 * Validates template data according to Android validation rules.
 * 
 * Rules:
 * - Name: Required, cannot be empty
 * - All Context Fields: Required, cannot be NULL
 * - Duplicate Detection: Templates with identical non-NULL context fields cannot be created
 *   (This is handled server-side, not in this validation function)
 * 
 * @param templateData - Template data to validate
 * @param getErrorMessage - Optional function to get localized error messages
 * @returns Validation result with errors
 */
export function validateTemplate(
  templateData: TemplateValidationData,
  getErrorMessage?: (key: string) => string
): TemplateValidationResult {
  const errors: ValidationError[] = [];
  const t = getErrorMessage || ((key: string) => key);

  // Name validation
  if (!templateData.name || templateData.name.trim() === '') {
    errors.push({
      field: 'name',
      message: t('validation.templateNameRequired'),
    });
  }

  // Context fields validation (all required, cannot be NULL)
  if (templateData.categoryId === null || templateData.categoryId === undefined) {
    errors.push({
      field: 'categoryId',
      message: t('validation.categoryRequired'),
    });
  }

  if (templateData.formaId === null || templateData.formaId === undefined) {
    errors.push({
      field: 'formaId',
      message: t('validation.formaRequired'),
    });
  }

  if (templateData.causeId === null || templateData.causeId === undefined) {
    errors.push({
      field: 'causeId',
      message: t('validation.causeRequired'),
    });
  }

  if (templateData.developId === null || templateData.developId === undefined) {
    errors.push({
      field: 'developId',
      message: t('validation.developRequired'),
    });
  }

  if (templateData.effectId === null || templateData.effectId === undefined) {
    errors.push({
      field: 'effectId',
      message: t('validation.effectRequired'),
    });
  }

  return {
    isValid: errors.length === 0,
    errors,
  };
}

