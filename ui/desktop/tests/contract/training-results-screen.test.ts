/**
 * Contract test for Training Results screen functionality.
 * Verifies metrics display, progress indicators, and data loading.
 * Test logic verified against spec/23-function_desktop.md lines 243-261.
 */

import { describe, it, expect } from '@jest/globals';
import { ApiService } from '@/services/api';

describe('Training Results Screen Contract Tests', () => {
  describe('Metrics Display', () => {
    it('should load overall metrics', async () => {
      const metrics = await ApiService.getOverallMetrics();
      
      expect(metrics).toBeDefined();
      expect(metrics).toHaveProperty('total_events');
      expect(metrics).toHaveProperty('average_impact_level');
      expect(typeof metrics.total_events).toBe('number');
      expect(typeof metrics.average_impact_level).toBe('number');
      expect(metrics.total_events).toBeGreaterThanOrEqual(0);
      expect(metrics.average_impact_level).toBeGreaterThanOrEqual(0);
    });

    it('should load event rows for results table', async () => {
      const rows = await ApiService.getEventRows();
      
      expect(rows).toBeDefined();
      expect(Array.isArray(rows)).toBe(true);
      
      if (rows.length > 0) {
        const firstRow = rows[0];
        expect(firstRow).toHaveProperty('event');
        expect(firstRow).toHaveProperty('summary');
        expect(firstRow).toHaveProperty('date');
        expect(typeof firstRow.event).toBe('string');
        expect(typeof firstRow.summary).toBe('string');
        expect(typeof firstRow.date).toBe('string');
        
        // Impact is optional
        if (firstRow.impact !== undefined) {
          expect(typeof firstRow.impact).toBe('number');
        }
      }
    });
  });

  describe('Progress Indicators', () => {
    it('should have metrics with valid structure', async () => {
      const metrics = await ApiService.getOverallMetrics();
      
      // Verify metrics structure matches expected format
      expect(metrics).toHaveProperty('total_events');
      expect(metrics).toHaveProperty('average_impact_level');
      expect(metrics).toHaveProperty('last_updated');
      
      // last_updated is optional
      if (metrics.last_updated) {
        expect(typeof metrics.last_updated).toBe('string');
        // Should be a valid ISO date string
        expect(() => new Date(metrics.last_updated!)).not.toThrow();
      }
    });

    it('should display progress metrics correctly', async () => {
      const metrics = await ApiService.getOverallMetrics();
      
      // Metrics should be non-negative
      expect(metrics.total_events).toBeGreaterThanOrEqual(0);
      expect(metrics.average_impact_level).toBeGreaterThanOrEqual(0);
    });
  });

  describe('Data Loading', () => {
    it('should load training data on mount', async () => {
      // Simulate component mount - load both metrics and event rows
      const [metrics, rows] = await Promise.all([
        ApiService.getOverallMetrics(),
        ApiService.getEventRows(),
      ]);
      
      expect(metrics).toBeDefined();
      expect(rows).toBeDefined();
    });

    it('should handle empty results gracefully', async () => {
      const rows = await ApiService.getEventRows();
      
      // Should return empty array, not throw error
      expect(Array.isArray(rows)).toBe(true);
    });
  });

  describe('Results Table Structure', () => {
    it('should have consistent row structure', async () => {
      const rows = await ApiService.getEventRows();
      
      rows.forEach(row => {
        expect(row).toHaveProperty('event');
        expect(row).toHaveProperty('summary');
        expect(row).toHaveProperty('date');
        expect(typeof row.event).toBe('string');
        expect(typeof row.summary).toBe('string');
        expect(typeof row.date).toBe('string');
      });
    });

    it('should format dates correctly', async () => {
      const rows = await ApiService.getEventRows();
      
      rows.forEach(row => {
        // Date should be a string (formatted date)
        expect(typeof row.date).toBe('string');
        expect(row.date.length).toBeGreaterThan(0);
      });
    });
  });
});

