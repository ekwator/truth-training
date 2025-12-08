/**
 * Unit tests for validation utilities.
 * Tests validateEvent and validateTemplate functions matching Android validation rules.
 */

import {
  validateEvent,
  validateTemplate,
  EventValidationData,
  TemplateValidationData,
} from '../validation';
import { normalizeToStartOfDay } from '../dateNormalization';

// Mock translation function
const mockT = (key: string) => {
  const translations: Record<string, string> = {
    'validation.eventNameRequired': 'Event name is required',
    'validation.eventDescriptionRequired': 'Event description is required',
    'validation.categoryRequired': 'Category is required',
    'validation.formaRequired': 'Forma is required',
    'validation.causeRequired': 'Cause is required',
    'validation.developRequired': 'Develop is required',
    'validation.effectRequired': 'Effect is required',
    'validation.startTimestampRequired': 'Start timestamp is required',
    'validation.endTimestampBeforeStart': 'End timestamp cannot be less than start timestamp',
    'validation.templateNameRequired': 'Template name is required',
  };
  return translations[key] || key;
};

describe('validateEvent', () => {
  it('should return valid for complete event data', () => {
    const eventData: EventValidationData = {
      name: 'Test Event',
      description: 'Test Description',
      categoryId: 1,
      formaId: 2,
      causeId: 3,
      developId: 4,
      effectId: 5,
      timestampStart: new Date(2024, 0, 15),
    };

    const result = validateEvent(eventData, mockT);
    expect(result.isValid).toBe(true);
    expect(result.errors).toHaveLength(0);
  });

  it('should return error when name is missing', () => {
    const eventData: EventValidationData = {
      description: 'Test Description',
      categoryId: 1,
      formaId: 2,
      causeId: 3,
      developId: 4,
      effectId: 5,
      timestampStart: new Date(2024, 0, 15),
    };

    const result = validateEvent(eventData, mockT);
    expect(result.isValid).toBe(false);
    expect(result.errors).toContainEqual({
      field: 'name',
      message: 'Event name is required',
    });
  });

  it('should return error when name is empty string', () => {
    const eventData: EventValidationData = {
      name: '   ',
      description: 'Test Description',
      categoryId: 1,
      formaId: 2,
      causeId: 3,
      developId: 4,
      effectId: 5,
      timestampStart: new Date(2024, 0, 15),
    };

    const result = validateEvent(eventData, mockT);
    expect(result.isValid).toBe(false);
    expect(result.errors.some((e) => e.field === 'name')).toBe(true);
  });

  it('should return error when description is missing', () => {
    const eventData: EventValidationData = {
      name: 'Test Event',
      categoryId: 1,
      formaId: 2,
      causeId: 3,
      developId: 4,
      effectId: 5,
      timestampStart: new Date(2024, 0, 15),
    };

    const result = validateEvent(eventData, mockT);
    expect(result.isValid).toBe(false);
    expect(result.errors).toContainEqual({
      field: 'description',
      message: 'Event description is required',
    });
  });

  it('should return errors for all missing context fields', () => {
    const eventData: EventValidationData = {
      name: 'Test Event',
      description: 'Test Description',
      timestampStart: new Date(2024, 0, 15),
    };

    const result = validateEvent(eventData, mockT);
    expect(result.isValid).toBe(false);
    expect(result.errors.length).toBeGreaterThanOrEqual(5);
    expect(result.errors.some((e) => e.field === 'categoryId')).toBe(true);
    expect(result.errors.some((e) => e.field === 'formaId')).toBe(true);
    expect(result.errors.some((e) => e.field === 'causeId')).toBe(true);
    expect(result.errors.some((e) => e.field === 'developId')).toBe(true);
    expect(result.errors.some((e) => e.field === 'effectId')).toBe(true);
  });

  it('should return error when start timestamp is missing', () => {
    const eventData: EventValidationData = {
      name: 'Test Event',
      description: 'Test Description',
      categoryId: 1,
      formaId: 2,
      causeId: 3,
      developId: 4,
      effectId: 5,
    };

    const result = validateEvent(eventData, mockT);
    expect(result.isValid).toBe(false);
    expect(result.errors).toContainEqual({
      field: 'timestampStart',
      message: 'Start timestamp is required',
    });
  });

  it('should accept end timestamp equal to start timestamp', () => {
    const startDate = new Date(2024, 0, 15);
    const eventData: EventValidationData = {
      name: 'Test Event',
      description: 'Test Description',
      categoryId: 1,
      formaId: 2,
      causeId: 3,
      developId: 4,
      effectId: 5,
      timestampStart: startDate,
      timestampEnd: startDate,
    };

    const result = validateEvent(eventData, mockT);
    expect(result.isValid).toBe(true);
  });

  it('should return error when end timestamp is before start timestamp', () => {
    const startDate = new Date(2024, 0, 15);
    const endDate = new Date(2024, 0, 14);
    const eventData: EventValidationData = {
      name: 'Test Event',
      description: 'Test Description',
      categoryId: 1,
      formaId: 2,
      causeId: 3,
      developId: 4,
      effectId: 5,
      timestampStart: startDate,
      timestampEnd: endDate,
    };

    const result = validateEvent(eventData, mockT);
    expect(result.isValid).toBe(false);
    expect(result.errors).toContainEqual({
      field: 'timestampEnd',
      message: 'End timestamp cannot be less than start timestamp',
    });
  });

  it('should handle Unix timestamp (seconds) for timestampStart', () => {
    const timestamp = Math.floor(new Date(2024, 0, 15).getTime() / 1000);
    const eventData: EventValidationData = {
      name: 'Test Event',
      description: 'Test Description',
      categoryId: 1,
      formaId: 2,
      causeId: 3,
      developId: 4,
      effectId: 5,
      timestampStart: timestamp,
    };

    const result = validateEvent(eventData, mockT);
    expect(result.isValid).toBe(true);
  });

  it('should normalize dates before comparison', () => {
    const startDate = new Date(2024, 0, 15, 14, 30, 45);
    const endDate = new Date(2024, 0, 15, 10, 0, 0); // Same day, but earlier time
    const eventData: EventValidationData = {
      name: 'Test Event',
      description: 'Test Description',
      categoryId: 1,
      formaId: 2,
      causeId: 3,
      developId: 4,
      effectId: 5,
      timestampStart: startDate,
      timestampEnd: endDate,
    };

    const result = validateEvent(eventData, mockT);
    // Should be valid because normalized dates are equal
    const normalizedStart = normalizeToStartOfDay(startDate);
    const normalizedEnd = normalizeToStartOfDay(endDate);
    expect(normalizedStart.getTime()).toBe(normalizedEnd.getTime());
    expect(result.isValid).toBe(true);
  });

  it('should handle null context field values', () => {
    const eventData: EventValidationData = {
      name: 'Test Event',
      description: 'Test Description',
      categoryId: null,
      formaId: null,
      causeId: null,
      developId: null,
      effectId: null,
      timestampStart: new Date(2024, 0, 15),
    };

    const result = validateEvent(eventData, mockT);
    expect(result.isValid).toBe(false);
    expect(result.errors.length).toBeGreaterThanOrEqual(5);
  });
});

describe('validateTemplate', () => {
  it('should return valid for complete template data', () => {
    const templateData: TemplateValidationData = {
      name: 'Test Template',
      categoryId: 1,
      formaId: 2,
      causeId: 3,
      developId: 4,
      effectId: 5,
    };

    const result = validateTemplate(templateData, mockT);
    expect(result.isValid).toBe(true);
    expect(result.errors).toHaveLength(0);
  });

  it('should return error when name is missing', () => {
    const templateData: TemplateValidationData = {
      categoryId: 1,
      formaId: 2,
      causeId: 3,
      developId: 4,
      effectId: 5,
    };

    const result = validateTemplate(templateData, mockT);
    expect(result.isValid).toBe(false);
    expect(result.errors).toContainEqual({
      field: 'name',
      message: 'Template name is required',
    });
  });

  it('should return error when name is empty string', () => {
    const templateData: TemplateValidationData = {
      name: '   ',
      categoryId: 1,
      formaId: 2,
      causeId: 3,
      developId: 4,
      effectId: 5,
    };

    const result = validateTemplate(templateData, mockT);
    expect(result.isValid).toBe(false);
    expect(result.errors.some((e) => e.field === 'name')).toBe(true);
  });

  it('should return errors for all missing context fields', () => {
    const templateData: TemplateValidationData = {
      name: 'Test Template',
    };

    const result = validateTemplate(templateData, mockT);
    expect(result.isValid).toBe(false);
    expect(result.errors.length).toBeGreaterThanOrEqual(5);
    expect(result.errors.some((e) => e.field === 'categoryId')).toBe(true);
    expect(result.errors.some((e) => e.field === 'formaId')).toBe(true);
    expect(result.errors.some((e) => e.field === 'causeId')).toBe(true);
    expect(result.errors.some((e) => e.field === 'developId')).toBe(true);
    expect(result.errors.some((e) => e.field === 'effectId')).toBe(true);
  });

  it('should handle null context field values', () => {
    const templateData: TemplateValidationData = {
      name: 'Test Template',
      categoryId: null,
      formaId: null,
      causeId: null,
      developId: null,
      effectId: null,
    };

    const result = validateTemplate(templateData, mockT);
    expect(result.isValid).toBe(false);
    expect(result.errors.length).toBeGreaterThanOrEqual(5);
  });
});

