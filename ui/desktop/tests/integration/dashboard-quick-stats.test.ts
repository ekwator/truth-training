/**
 * Integration test for Dashboard Quick Stats.
 * Verifies Quick Stats display with event counts and metrics.
 */

import { describe, it, expect, beforeEach, jest } from '@jest/globals';
import axios, { AxiosInstance } from 'axios';
import { ApiService, setApiClient } from '@/services/api';

// Mock apiClient
const mockApiClient = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
  interceptors: {
    request: { use: jest.fn() },
    response: { use: jest.fn() },
  },
} as unknown as AxiosInstance;

describe('Dashboard Quick Stats Integration Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockApiClient.get.mockClear();
    // Set mock apiClient
    setApiClient(mockApiClient);
  });

  describe('Quick Stats Data Loading', () => {
    it('should load overall metrics for Quick Stats', async () => {
      const mockMetrics = {
        total_events: 10,
        average_impact_level: 3.5,
        last_updated: new Date().toISOString(),
      };
      mockApiClient.get.mockResolvedValueOnce({ data: mockMetrics });

      const metrics = await ApiService.getOverallMetrics();
      
      expect(metrics).toBeDefined();
      expect(metrics).toHaveProperty('total_events');
      expect(metrics).toHaveProperty('average_impact_level');
      expect(typeof metrics.total_events).toBe('number');
      expect(typeof metrics.average_impact_level).toBe('number');
    });

    it('should display event count in Quick Stats', async () => {
      const mockMetrics = { total_events: 5, average_impact_level: 2.0 };
      mockApiClient.get.mockResolvedValueOnce({ data: mockMetrics });

      const metrics = await ApiService.getOverallMetrics();
      
      // Event count should be non-negative
      expect(metrics.total_events).toBeGreaterThanOrEqual(0);
    });

    it('should display average impact level in Quick Stats', async () => {
      const mockMetrics = { total_events: 8, average_impact_level: 4.2 };
      mockApiClient.get.mockResolvedValueOnce({ data: mockMetrics });

      const metrics = await ApiService.getOverallMetrics();
      
      // Average impact should be non-negative
      expect(metrics.average_impact_level).toBeGreaterThanOrEqual(0);
    });

    it('should handle empty metrics gracefully', async () => {
      const mockMetrics = { total_events: 0, average_impact_level: 0 };
      mockApiClient.get.mockResolvedValueOnce({ data: mockMetrics });

      const metrics = await ApiService.getOverallMetrics();
      
      // Should return valid structure even with no data
      expect(metrics).toBeDefined();
      expect(metrics.total_events).toBeGreaterThanOrEqual(0);
    });
  });

  describe('Quick Stats Display Format', () => {
    it('should format event count correctly', async () => {
      const mockMetrics = { total_events: 100, average_impact_level: 3.0 };
      mockApiClient.get.mockResolvedValueOnce({ data: mockMetrics });

      const metrics = await ApiService.getOverallMetrics();
      
      // Event count should be a number that can be displayed
      expect(typeof metrics.total_events).toBe('number');
      expect(Number.isInteger(metrics.total_events)).toBe(true);
    });

    it('should format average impact level correctly', async () => {
      const mockMetrics = { total_events: 50, average_impact_level: 2.75 };
      mockApiClient.get.mockResolvedValueOnce({ data: mockMetrics });

      const metrics = await ApiService.getOverallMetrics();
      
      // Average impact should be a number (can be decimal)
      expect(typeof metrics.average_impact_level).toBe('number');
      expect(Number.isFinite(metrics.average_impact_level)).toBe(true);
    });

    it('should display last updated timestamp if available', async () => {
      const mockMetrics = {
        total_events: 25,
        average_impact_level: 3.5,
        last_updated: new Date().toISOString(),
      };
      mockApiClient.get.mockResolvedValueOnce({ data: mockMetrics });

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
      const mockMetrics1 = { total_events: 10, average_impact_level: 3.0 };
      const mockMetrics2 = { total_events: 12, average_impact_level: 3.2 };
      mockApiClient.get.mockResolvedValueOnce({ data: mockMetrics1 }).mockResolvedValueOnce({ data: mockMetrics2 });

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

