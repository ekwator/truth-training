/**
 * Unit tests for validation rules.
 * Verifies against Android validation rules.
 */

import { describe, it, expect } from '@jest/globals';
import { validateEvent, validateTemplate, ValidationError } from '@/utils/validation';

describe('Validation Rules Tests', () => {
  describe('validateEvent', () => {
    it('should return valid for complete event data', () => {
      const eventData = {
        name: 'Test Event',
        description: 'Test Description',
        categoryId: 1,
        formaId: 2,
        causeId: 3,
        developId: 4,
        effectId: 5,
        timestampStart: Math.floor(Date.now() / 1000),
        timestampEnd: null,
      };
      
      const result = validateEvent(eventData);
      
      expect(result.isValid).toBe(true);
      expect(result.errors).toHaveLength(0);
    });

    it('should return error when name is missing', () => {
      const eventData = {
        description: 'Test Description',
        categoryId: 1,
        formaId: 2,
        causeId: 3,
        developId: 4,
        effectId: 5,
        timestampStart: Math.floor(Date.now() / 1000),
      };
      
      const result = validateEvent(eventData);
      
      expect(result.isValid).toBe(false);
      expect(result.errors).toContainEqual({
        field: 'name',
        message: 'Event name is required',
      });
    });

    it('should return error when name is empty string', () => {
      const eventData = {
        name: '   ',
        description: 'Test Description',
        categoryId: 1,
        formaId: 2,
        causeId: 3,
        developId: 4,
        effectId: 5,
        timestampStart: Math.floor(Date.now() / 1000),
      };
      
      const result = validateEvent(eventData);
      
      expect(result.isValid).toBe(false);
      expect(result.errors.some(e => e.field === 'name')).toBe(true);
    });

    it('should return error when description is missing', () => {
      const eventData = {
        name: 'Test Event',
        categoryId: 1,
        formaId: 2,
        causeId: 3,
        developId: 4,
        effectId: 5,
        timestampStart: Math.floor(Date.now() / 1000),
      };
      
      const result = validateEvent(eventData);
      
      expect(result.isValid).toBe(false);
      expect(result.errors).toContainEqual({
        field: 'description',
        message: 'Event description is required',
      });
    });

    it('should return errors for all missing context fields', () => {
      const eventData = {
        name: 'Test Event',
        description: 'Test Description',
        timestampStart: Math.floor(Date.now() / 1000),
      };
      
      const result = validateEvent(eventData);
      
      expect(result.isValid).toBe(false);
      expect(result.errors).toContainEqual({
        field: 'categoryId',
        message: 'Category is required',
      });
      expect(result.errors).toContainEqual({
        field: 'formaId',
        message: 'Forma is required',
      });
      expect(result.errors).toContainEqual({
        field: 'causeId',
        message: 'Cause is required',
      });
      expect(result.errors).toContainEqual({
        field: 'developId',
        message: 'Develop is required',
      });
      expect(result.errors).toContainEqual({
        field: 'effectId',
        message: 'Effect is required',
      });
    });

    it('should return error when start timestamp is missing', () => {
      const eventData = {
        name: 'Test Event',
        description: 'Test Description',
        categoryId: 1,
        formaId: 2,
        causeId: 3,
        developId: 4,
        effectId: 5,
      };
      
      const result = validateEvent(eventData);
      
      expect(result.isValid).toBe(false);
      expect(result.errors).toContainEqual({
        field: 'timestampStart',
        message: 'Start timestamp is required',
      });
    });

    it('should accept end timestamp equal to start timestamp', () => {
      const startTimestamp = Math.floor(Date.now() / 1000);
      const eventData = {
        name: 'Test Event',
        description: 'Test Description',
        categoryId: 1,
        formaId: 2,
        causeId: 3,
        developId: 4,
        effectId: 5,
        timestampStart: startTimestamp,
        timestampEnd: startTimestamp,
      };
      
      const result = validateEvent(eventData);
      
      expect(result.isValid).toBe(true);
      expect(result.errors).toHaveLength(0);
    });

    it('should return error when end timestamp is before start timestamp', () => {
      const startTimestamp = Math.floor(Date.now() / 1000);
      const endTimestamp = startTimestamp - 86400; // 1 day before
      
      const eventData = {
        name: 'Test Event',
        description: 'Test Description',
        categoryId: 1,
        formaId: 2,
        causeId: 3,
        developId: 4,
        effectId: 5,
        timestampStart: startTimestamp,
        timestampEnd: endTimestamp,
      };
      
      const result = validateEvent(eventData);
      
      expect(result.isValid).toBe(false);
      expect(result.errors).toContainEqual({
        field: 'timestampEnd',
        message: 'End timestamp cannot be less than start timestamp',
      });
    });

    it('should handle Unix timestamp (seconds) for timestampStart', () => {
      const timestamp = Math.floor(Date.now() / 1000);
      const eventData = {
        name: 'Test Event',
        description: 'Test Description',
        categoryId: 1,
        formaId: 2,
        causeId: 3,
        developId: 4,
        effectId: 5,
        timestampStart: timestamp,
      };
      
      const result = validateEvent(eventData);
      
      expect(result.isValid).toBe(true);
    });

    it('should normalize dates before comparison', () => {
      // Start at 10:00, End at 08:00 same day (should be valid after normalization)
      const startDate = new Date(2024, 0, 15, 10, 0, 0);
      const endDate = new Date(2024, 0, 15, 8, 0, 0);
      
      const eventData = {
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
      
      const result = validateEvent(eventData);
      
      // After normalization, both are same day start, so should be valid
      expect(result.isValid).toBe(true);
    });

    it('should handle null context field values', () => {
      const eventData = {
        name: 'Test Event',
        description: 'Test Description',
        categoryId: null,
        formaId: undefined,
        causeId: null,
        developId: undefined,
        effectId: null,
        timestampStart: Math.floor(Date.now() / 1000),
      };
      
      const result = validateEvent(eventData);
      
      expect(result.isValid).toBe(false);
      expect(result.errors.length).toBeGreaterThan(0);
    });
  });

  describe('validateTemplate', () => {
    it('should return valid for complete template data', () => {
      const templateData = {
        name: 'Test Template',
        categoryId: 1,
        formaId: 2,
        causeId: 3,
        developId: 4,
        effectId: 5,
      };
      
      const result = validateTemplate(templateData);
      
      expect(result.isValid).toBe(true);
      expect(result.errors).toHaveLength(0);
    });

    it('should return error when name is missing', () => {
      const templateData = {
        categoryId: 1,
        formaId: 2,
        causeId: 3,
        developId: 4,
        effectId: 5,
      };
      
      const result = validateTemplate(templateData);
      
      expect(result.isValid).toBe(false);
      expect(result.errors).toContainEqual({
        field: 'name',
        message: 'Template name is required',
      });
    });

    it('should return error when name is empty string', () => {
      const templateData = {
        name: '   ',
        categoryId: 1,
        formaId: 2,
        causeId: 3,
        developId: 4,
        effectId: 5,
      };
      
      const result = validateTemplate(templateData);
      
      expect(result.isValid).toBe(false);
      expect(result.errors.some(e => e.field === 'name')).toBe(true);
    });

    it('should return errors for all missing context fields', () => {
      const templateData = {
        name: 'Test Template',
      };
      
      const result = validateTemplate(templateData);
      
      expect(result.isValid).toBe(false);
      expect(result.errors).toContainEqual({
        field: 'categoryId',
        message: 'Category is required',
      });
      expect(result.errors).toContainEqual({
        field: 'formaId',
        message: 'Forma is required',
      });
      expect(result.errors).toContainEqual({
        field: 'causeId',
        message: 'Cause is required',
      });
      expect(result.errors).toContainEqual({
        field: 'developId',
        message: 'Develop is required',
      });
      expect(result.errors).toContainEqual({
        field: 'effectId',
        message: 'Effect is required',
      });
    });

    it('should handle null context field values', () => {
      const templateData = {
        name: 'Test Template',
        categoryId: null,
        formaId: undefined,
        causeId: null,
        developId: undefined,
        effectId: null,
      };
      
      const result = validateTemplate(templateData);
      
      expect(result.isValid).toBe(false);
      expect(result.errors.length).toBeGreaterThan(0);
    });
  });
});

