/**
 * Unit tests for ImpactLevelMapper utility.
 * Tests all mapping functions for impact level conversion.
 */

import { describe, it, expect } from '@jest/globals';
import { mapToBoolean, mapToRange, getMinLevel, getMaxLevel, isValid } from '@/utils/impactLevelMapper';

describe('ImpactLevelMapper', () => {
  describe('mapToBoolean', () => {
    it('should map level 1-3 to false (negative)', () => {
      expect(mapToBoolean(1)).toBe(false);
      expect(mapToBoolean(2)).toBe(false);
      expect(mapToBoolean(3)).toBe(false);
    });

    it('should map level 4-5 to true (positive)', () => {
      expect(mapToBoolean(4)).toBe(true);
      expect(mapToBoolean(5)).toBe(true);
    });

    it('should throw error for invalid levels', () => {
      expect(() => mapToBoolean(0)).toThrow();
      expect(() => mapToBoolean(6)).toThrow();
      expect(() => mapToBoolean(-1)).toThrow();
    });
  });

  describe('mapToRange', () => {
    it('should return range 1-3 for false (negative)', () => {
      const range = mapToRange(false);
      expect(range.min).toBe(1);
      expect(range.max).toBe(3);
    });

    it('should return range 4-5 for true (positive)', () => {
      const range = mapToRange(true);
      expect(range.min).toBe(4);
      expect(range.max).toBe(5);
    });
  });

  describe('getMinLevel', () => {
    it('should return 1 for false (negative)', () => {
      expect(getMinLevel(false)).toBe(1);
    });

    it('should return 4 for true (positive)', () => {
      expect(getMinLevel(true)).toBe(4);
    });
  });

  describe('getMaxLevel', () => {
    it('should return 3 for false (negative)', () => {
      expect(getMaxLevel(false)).toBe(3);
    });

    it('should return 5 for true (positive)', () => {
      expect(getMaxLevel(true)).toBe(5);
    });
  });

  describe('isValid', () => {
    it('should return true for valid levels (1-5)', () => {
      expect(isValid(1)).toBe(true);
      expect(isValid(2)).toBe(true);
      expect(isValid(3)).toBe(true);
      expect(isValid(4)).toBe(true);
      expect(isValid(5)).toBe(true);
    });

    it('should return false for invalid levels', () => {
      expect(isValid(0)).toBe(false);
      expect(isValid(6)).toBe(false);
      expect(isValid(-1)).toBe(false);
      expect(isValid(10)).toBe(false);
    });
  });

  describe('Integration: mapToBoolean and mapToRange consistency', () => {
    it('should maintain consistency between mapToBoolean and mapToRange', () => {
      // Test all valid levels
      for (let level = 1; level <= 5; level++) {
        const booleanValue = mapToBoolean(level);
        const range = mapToRange(booleanValue);
        expect(level).toBeGreaterThanOrEqual(range.min);
        expect(level).toBeLessThanOrEqual(range.max);
      }
    });
  });
});

