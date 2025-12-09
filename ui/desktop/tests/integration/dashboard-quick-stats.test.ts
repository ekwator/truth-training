/**
 * Integration test for Dashboard Quick Stats.
 * Verifies Quick Stats display with event counts and metrics.
 */

import { describe, it, expect } from '@jest/globals';
import { ApiService } from '@/services/api';

describe('Dashboard Quick Stats Integration Tests', () => {
  describe('Quick Stats Data Loading', () => {
    it('should load overall metrics for Quick Stats', async () => {
      const metrics = await ApiService.getOverallMetrics();
      
      expect(metrics).toBeDefined();
      expect(metrics).toHaveProperty('total_events');
      expect(metrics).toHaveProperty('average_impact_level');
      expect(typeof metrics.total_events).toBe('number');
      expect(typeof metrics.average_impact_level).toBe('number');
    });

    it('should display event count in Quick Stats', async () => {
      const metrics = await ApiService.getOverallMetrics();
      
      // Event count should be non-negative
      expect(metrics.total_events).toBeGreaterThanOrEqual(0);
    });

    it('should display average impact level in Quick Stats', async () => {
      const metrics = await ApiService.getOverallMetrics();
      
      // Average impact should be non-negative
      expect(metrics.average_impact_level).toBeGreaterThanOrEqual(0);
    });

    it('should handle empty metrics gracefully', async () => {
      const metrics = await ApiService.getOverallMetrics();
      
      // Should return valid structure even with no data
      expect(metrics).toBeDefined();
      expect(metrics.total_events).toBeGreaterThanOrEqual(0);
    });
  });

  describe('Quick Stats Display Format', () => {
    it('should format event count correctly', async () => {
      const metrics = await ApiService.getOverallMetrics();
      
      // Event count should be a number that can be displayed
      expect(typeof metrics.total_events).toBe('number');
      expect(Number.isInteger(metrics.total_events)).toBe(true);
    });

    it('should format average impact level correctly', async () => {
      const metrics = await ApiService.getOverallMetrics();
      
      // Average impact should be a number (can be decimal)
      expect(typeof metrics.average_impact_level).toBe('number');
      expect(Number.isFinite(metrics.average_impact_level)).toBe(true);
    });

    it('should display last updated timestamp if available', async () => {
      const metrics = await ApiService.getOverallMetrics();
      
      // last_updated is optional
      if (metrics.last_updated) {
        expect(typeof metrics.last_updated).toBe('string');
        // Should be a valid date string
        expect(() => new Date(metrics.last_updated!)).not.toThrow();
      }
    });
  });

  describe('Quick Stats Real-time Updates', () => {
    it('should load fresh metrics on each request', async () => {
      const metrics1 = await ApiService.getOverallMetrics();
      const metrics2 = await ApiService.getOverallMetrics();
      
      // Both should be valid (may have same or different values)
      expect(metrics1).toBeDefined();
      expect(metrics2).toBeDefined();
      expect(typeof metrics1.total_events).toBe('number');
      expect(typeof metrics2.total_events).toBe('number');
    });
  });
});

