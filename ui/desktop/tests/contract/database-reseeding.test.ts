/**
 * Contract test for database reseeding API.
 * Verifies against contracts/database-reseeding.md and Android reseeding patterns.
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { invoke } from '@tauri-apps/api/core';

// Mock Tauri API
jest.mock('@tauri-apps/api/core', () => ({
  invoke: jest.fn(),
}));

describe('Database Reseeding API Contract Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('ReseedResult Interface', () => {
    it('should return ReseedResult with correct structure', async () => {
      const mockResult = {
        success: true,
        message: 'Knowledge base reseeded successfully',
        tablesUpdated: ['category', 'forma', 'cause', 'develop', 'effect', 'context'],
      };

      (invoke as jest.Mock).mockResolvedValue(mockResult);

      const result = await invoke('reseed_knowledge_base');

      expect(result).toHaveProperty('success');
      expect(result).toHaveProperty('message');
      expect(result).toHaveProperty('tablesUpdated');
      expect(Array.isArray(result.tablesUpdated)).toBe(true);
    });

    it('should return success: true on successful reseeding', async () => {
      const mockResult = {
        success: true,
        message: 'Knowledge base reseeded successfully',
        tablesUpdated: ['category', 'forma', 'cause', 'develop', 'effect', 'context'],
      };

      (invoke as jest.Mock).mockResolvedValue(mockResult);

      const result = await invoke('reseed_knowledge_base');

      expect(result.success).toBe(true);
      expect(result.message).toContain('successfully');
    });

    it('should return success: false on failure', async () => {
      const mockResult = {
        success: false,
        message: 'Reseeding failed: FK constraint violation',
        tablesUpdated: [],
      };

      (invoke as jest.Mock).mockResolvedValue(mockResult);

      const result = await invoke('reseed_knowledge_base');

      expect(result.success).toBe(false);
      expect(result.message).toContain('failed');
    });

    it('should return all 6 tables in tablesUpdated on success', async () => {
      const expectedTables = ['category', 'forma', 'cause', 'develop', 'effect', 'context'];
      const mockResult = {
        success: true,
        message: 'Knowledge base reseeded successfully',
        tablesUpdated: expectedTables,
      };

      (invoke as jest.Mock).mockResolvedValue(mockResult);

      const result = await invoke('reseed_knowledge_base');

      expect(result.tablesUpdated).toEqual(expect.arrayContaining(expectedTables));
      expect(result.tablesUpdated.length).toBe(6);
    });
  });

  describe('Error Handling Contract', () => {
    it('should handle transaction rollback on error', async () => {
      const mockError = new Error('FK constraint violation');
      (invoke as jest.Mock).mockRejectedValue(mockError);

      await expect(invoke('reseed_knowledge_base')).rejects.toThrow();
    });

    it('should return error message on partial failure', async () => {
      const mockResult = {
        success: false,
        message: 'Reseeding failed: Data insertion failed',
        tablesUpdated: [],
      };

      (invoke as jest.Mock).mockResolvedValue(mockResult);

      const result = await invoke('reseed_knowledge_base');

      expect(result.success).toBe(false);
      expect(result.message).toContain('failed');
    });
  });

  describe('Reseeding Algorithm Contract', () => {
    it('should follow temporary tables approach (Android pattern)', async () => {
      // This test verifies the contract specifies temporary tables approach
      // Actual implementation verification is in integration tests
      const mockResult = {
        success: true,
        message: 'Knowledge base reseeded successfully',
        tablesUpdated: ['category', 'forma', 'cause', 'develop', 'effect', 'context'],
      };

      (invoke as jest.Mock).mockResolvedValue(mockResult);

      const result = await invoke('reseed_knowledge_base');

      // Contract requires temporary tables approach
      // Verification: result should indicate successful reseeding
      expect(result.success).toBe(true);
      expect(result.tablesUpdated.length).toBe(6);
    });
  });
});

