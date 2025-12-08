/**
 * Unit tests for dateNormalization utility.
 * Tests normalizeToStartOfDay and normalizeTimestampToStartOfDay functions.
 */

import {
  normalizeToStartOfDay,
  normalizeTimestampToStartOfDay,
} from '../dateNormalization';

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
    const originalDate = new Date(2024, 0, 15, 14, 30, 45);
    const normalized = normalizeToStartOfDay(originalDate);

    expect(originalDate.getHours()).toBe(14);
    expect(originalDate.getMinutes()).toBe(30);
    expect(normalized.getHours()).toBe(0);
    expect(normalized.getMinutes()).toBe(0);
  });

  it('should handle date already at start of day', () => {
    const date = new Date(2024, 0, 15, 0, 0, 0, 0);
    const normalized = normalizeToStartOfDay(date);

    expect(normalized.getTime()).toBe(date.getTime());
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
    const utcDate = new Date('2024-01-15T12:00:00Z');
    const normalized = normalizeToStartOfDay(utcDate);

    // Should normalize to local time start of day
    expect(normalized.getHours()).toBe(0);
    expect(normalized.getMinutes()).toBe(0);
    expect(normalized.getSeconds()).toBe(0);
  });
});

describe('normalizeTimestampToStartOfDay', () => {
  it('should normalize Unix timestamp (seconds) to start of day', () => {
    // 2024-01-15 14:30:45 UTC = 1705329045 seconds
    const timestamp = 1705329045;
    const normalized = normalizeTimestampToStartOfDay(timestamp);

    // Convert back to date to verify
    const normalizedDate = new Date(normalized * 1000);
    expect(normalizedDate.getHours()).toBe(0);
    expect(normalizedDate.getMinutes()).toBe(0);
    expect(normalizedDate.getSeconds()).toBe(0);
  });

  it('should return timestamp in seconds', () => {
    const timestamp = 1705329045; // 2024-01-15 14:30:45 UTC
    const normalized = normalizeTimestampToStartOfDay(timestamp);

    // Should be divisible by 60 (no seconds component)
    expect(normalized % 60).toBe(0);
  });

  it('should handle timestamp already at start of day', () => {
    const date = new Date(2024, 0, 15, 0, 0, 0, 0);
    const timestamp = Math.floor(date.getTime() / 1000);
    const normalized = normalizeTimestampToStartOfDay(timestamp);

    // Should be same or very close (within same day)
    const diff = Math.abs(normalized - timestamp);
    expect(diff).toBeLessThan(86400); // Less than 1 day
  });

  it('should handle edge case: timestamp at midnight', () => {
    const date = new Date(2024, 0, 15, 0, 0, 0, 0);
    const timestamp = Math.floor(date.getTime() / 1000);
    const normalized = normalizeTimestampToStartOfDay(timestamp);

    const normalizedDate = new Date(normalized * 1000);
    expect(normalizedDate.getHours()).toBe(0);
    expect(normalizedDate.getMinutes()).toBe(0);
  });
});

