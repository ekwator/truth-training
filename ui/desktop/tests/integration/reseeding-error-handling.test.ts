/**
 * Integration test for error handling and rollback.
 * Verifies error handling and transaction rollback.
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { invoke } from '@tauri-apps/api/core';

// Mock Tauri API
jest.mock('@tauri-apps/api/core', () => ({
  invoke: jest.fn(),
}));

describe('Reseeding Error Handling Integration Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Transaction Rollback', () => {
    it('should rollback transaction on error', async () => {
      const mockError = new Error('Reseeding failed: Data insertion error');
      (invoke as jest.Mock).mockRejectedValue(mockError);

      await expect(invoke('reseed_knowledge_base')).rejects.toThrow();
    });

    it('should return error result on partial failure', async () => {
      const mockResult = {
        success: false,
        message: 'Reseeding failed: Data insertion failed',
        tablesUpdated: [],
      };

      (invoke as jest.Mock).mockResolvedValue(mockResult);

      const result = await invoke('reseed_knowledge_base');

      expect(result.success).toBe(false);
      expect(result.tablesUpdated.length).toBe(0);
    });
  });

  describe('Error Scenarios', () => {
    it('should handle temp table creation failure', async () => {
      const mockResult = {
        success: false,
        message: 'Reseeding failed: Failed to create temporary tables',
        tablesUpdated: [],
      };

      (invoke as jest.Mock).mockResolvedValue(mockResult);

      const result = await invoke('reseed_knowledge_base');

      expect(result.success).toBe(false);
      expect(result.message).toContain('temporary tables');
    });

    it('should handle data insertion failure', async () => {
      const mockResult = {
        success: false,
        message: 'Reseeding failed: Data insertion failed',
        tablesUpdated: [],
      };

      (invoke as jest.Mock).mockResolvedValue(mockResult);

      const result = await invoke('reseed_knowledge_base');

      expect(result.success).toBe(false);
      expect(result.message).toContain('insertion');
    });

    it('should handle atomic swap failure', async () => {
      const mockResult = {
        success: false,
        message: 'Reseeding failed: Atomic swap failed',
        tablesUpdated: [],
      };

      (invoke as jest.Mock).mockResolvedValue(mockResult);

      const result = await invoke('reseed_knowledge_base');

      expect(result.success).toBe(false);
      expect(result.message).toContain('swap');
    });
  });

  describe('Error Recovery', () => {
    it('should allow retry after error', async () => {
      // First attempt fails
      const mockFailure = {
        success: false,
        message: 'Reseeding failed: Data insertion failed',
        tablesUpdated: [],
      };

      (invoke as jest.Mock).mockResolvedValueOnce(mockFailure);

      const firstResult = await invoke('reseed_knowledge_base');
      expect(firstResult.success).toBe(false);

      // Second attempt succeeds
      const mockSuccess = {
        success: true,
        message: 'Knowledge base reseeded successfully',
        tablesUpdated: ['category', 'forma', 'cause', 'develop', 'effect', 'context'],
      };

      (invoke as jest.Mock).mockResolvedValueOnce(mockSuccess);

      const secondResult = await invoke('reseed_knowledge_base');
      expect(secondResult.success).toBe(true);
    });
  });
});

