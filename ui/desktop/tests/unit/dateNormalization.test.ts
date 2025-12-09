/**
 * Unit tests for date normalization algorithm.
 * Verifies against Android date normalization algorithm.
 */

import { describe, it, expect } from '@jest/globals';
import { normalizeToStartOfDay, normalizeTimestampToStartOfDay, validateDateRange } from '@/utils/dateNormalization';

describe('Date Normalization Algorithm Tests', () => {
  describe('normalizeToStartOfDay', () => {
    it('should normalize date to start of day (00:00:00.000)', () => {
      const date = new Date(2024, 0, 15, 14, 30, 45, 123);
      const normalized = normalizeToStartOfDay(date);
      
      expect(normalized.getFullYear()).toBe(2024);
      expect(normalized.getMonth()).toBe(0);
      expect(normalized.getDate()).toBe(15);
      expect(normalized.getHours()).toBe(0);
      expect(normalized.getMinutes()).toBe(0);
      expect(normalized.getSeconds()).toBe(0);
      expect(normalized.getMilliseconds()).toBe(0);
    });

    it('should not modify original date object', () => {
      const originalDate = new Date(2024, 0, 15, 14, 30, 45, 123);
      const originalHours = originalDate.getHours();
      normalizeToStartOfDay(originalDate);
      
      expect(originalDate.getHours()).toBe(originalHours);
    });

    it('should handle date already at start of day', () => {
      const date = new Date(2024, 0, 15, 0, 0, 0, 0);
      const normalized = normalizeToStartOfDay(date);
      
      expect(normalized.getHours()).toBe(0);
      expect(normalized.getMinutes()).toBe(0);
      expect(normalized.getSeconds()).toBe(0);
      expect(normalized.getMilliseconds()).toBe(0);
    });

    it('should handle date at end of day', () => {
      const date = new Date(2024, 0, 15, 23, 59, 59, 999);
      const normalized = normalizeToStartOfDay(date);
      
      expect(normalized.getHours()).toBe(0);
      expect(normalized.getMinutes()).toBe(0);
      expect(normalized.getSeconds()).toBe(0);
      expect(normalized.getMilliseconds()).toBe(0);
    });

    it('should handle different timezones correctly', () => {
      // Create date in UTC
      const utcDate = new Date('2024-01-15T14:30:45.123Z');
      const normalized = normalizeToStartOfDay(utcDate);
      
      // Should normalize to start of day in local timezone
      expect(normalized.getHours()).toBe(0);
      expect(normalized.getMinutes()).toBe(0);
      expect(normalized.getSeconds()).toBe(0);
      expect(normalized.getMilliseconds()).toBe(0);
    });
  });

  describe('normalizeTimestampToStartOfDay', () => {
    it('should normalize Unix timestamp (seconds) to start of day', () => {
      // Timestamp for 2024-01-15 14:30:45 UTC (in seconds)
      const timestamp = 1705332645;
      const normalized = normalizeTimestampToStartOfDay(timestamp);
      
      // Should return timestamp for start of day
      const normalizedDate = new Date(normalized * 1000);
      expect(normalizedDate.getHours()).toBe(0);
      expect(normalizedDate.getMinutes()).toBe(0);
      expect(normalizedDate.getSeconds()).toBe(0);
    });

    it('should return timestamp in seconds', () => {
      const timestamp = 1705332645;
      const normalized = normalizeTimestampToStartOfDay(timestamp);
      
      // Should be less than original (start of day is earlier)
      expect(normalized).toBeLessThanOrEqual(timestamp);
      // Should be a valid timestamp (reasonable range)
      expect(normalized).toBeGreaterThan(0);
    });

    it('should handle timestamp already at start of day', () => {
      // Create timestamp for start of day
      const date = new Date(2024, 0, 15, 0, 0, 0, 0);
      const timestamp = Math.floor(date.getTime() / 1000);
      const normalized = normalizeTimestampToStartOfDay(timestamp);
      
      expect(normalized).toBe(timestamp);
    });

    it('should handle edge case: timestamp at midnight', () => {
      const date = new Date(2024, 0, 15, 0, 0, 0, 0);
      const timestamp = Math.floor(date.getTime() / 1000);
      const normalized = normalizeTimestampToStartOfDay(timestamp);
      
      expect(normalized).toBe(timestamp);
    });
  });

  describe('validateDateRange', () => {
    it('should validate valid date range (End > Start)', () => {
      const start = Math.floor(new Date(2024, 0, 15, 10, 0, 0).getTime() / 1000);
      const end = Math.floor(new Date(2024, 0, 16, 10, 0, 0).getTime() / 1000);
      
      const result = validateDateRange(start, end);
      
      expect(result.valid).toBe(true);
      expect(result.error).toBeUndefined();
    });

    it('should validate valid date range (End == Start)', () => {
      const start = Math.floor(new Date(2024, 0, 15, 10, 0, 0).getTime() / 1000);
      const end = Math.floor(new Date(2024, 0, 15, 14, 0, 0).getTime() / 1000);
      
      const result = validateDateRange(start, end);
      
      // End can be equal to Start (normalized to same day)
      expect(result.valid).toBe(true);
      expect(result.error).toBeUndefined();
    });

    it('should validate invalid date range (End < Start)', () => {
      const start = Math.floor(new Date(2024, 0, 16, 10, 0, 0).getTime() / 1000);
      const end = Math.floor(new Date(2024, 0, 15, 10, 0, 0).getTime() / 1000);
      
      const result = validateDateRange(start, end);
      
      expect(result.valid).toBe(false);
      expect(result.error).toBe('End Timestamp cannot be less than Start Timestamp');
    });

    it('should validate when end is null (optional end date)', () => {
      const start = Math.floor(new Date(2024, 0, 15, 10, 0, 0).getTime() / 1000);
      
      const result = validateDateRange(start, null);
      
      expect(result.valid).toBe(true);
      expect(result.error).toBeUndefined();
    });

    it('should handle timestamps in milliseconds', () => {
      const start = new Date(2024, 0, 15, 10, 0, 0).getTime();
      const end = new Date(2024, 0, 16, 10, 0, 0).getTime();
      
      const result = validateDateRange(start, end);
      
      expect(result.valid).toBe(true);
    });

    it('should handle timestamps in seconds', () => {
      const start = Math.floor(new Date(2024, 0, 15, 10, 0, 0).getTime() / 1000);
      const end = Math.floor(new Date(2024, 0, 16, 10, 0, 0).getTime() / 1000);
      
      const result = validateDateRange(start, end);
      
      expect(result.valid).toBe(true);
    });
  });
});

