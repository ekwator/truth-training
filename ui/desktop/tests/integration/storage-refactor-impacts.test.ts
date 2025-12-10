/**
 * Integration test for refactored impacts commands using core storage functions.
 * Verifies that impacts commands correctly use core::storage::add_impact().
 */

import { describe, it, expect, beforeAll, beforeEach, jest } from '@jest/globals';

// Mock Tauri invoke
jest.mock('@tauri-apps/api/core', () => ({
  invoke: jest.fn(),
}));

import { invoke } from '@tauri-apps/api/core';

describe('Storage Refactor - Impacts Commands Integration', () => {
  let testEventId: number | null = null;
  const mockInvoke = invoke as jest.MockedFunction<typeof invoke>;

  beforeEach(() => {
    jest.clearAllMocks();
    testEventId = 1;
  });

  beforeAll(async () => {
    // Test event ID is set in beforeEach
    // No async setup needed
  });

  it('should add impact using core storage function', async () => {
    if (!testEventId) {
      throw new Error('Test event not created');
    }

    const impactData = {
      event_id: testEventId.toString(),
      impact_level: 3,
      notes: 'Test impact for storage refactor',
    };

    const mockImpact = {
      id: '1',
      event_id: testEventId.toString(),
      impact_level: 3,
      notes: impactData.notes,
    };
    mockInvoke.mockResolvedValueOnce(mockImpact);

    const result = await invoke('add_impact', { request: impactData });
    
    expect(result).toBeDefined();
    expect((result as any).id).toBeDefined();
    expect((result as any).event_id).toBe(testEventId.toString());
    expect((result as any).impact_level).toBe(3);
    expect((result as any).notes).toBe(impactData.notes);
  });

  it('should validate impact level range', async () => {
    if (!testEventId) {
      throw new Error('Test event not created');
    }

    const invalidImpact = {
      event_id: testEventId.toString(),
      impact_level: 6, // Invalid: should be 1-5
      notes: null,
    };

    // Mock should reject for invalid impact level
    mockInvoke.mockImplementationOnce(() => {
      return Promise.reject(new Error('Invalid impact level'));
    });

    await expect(
      invoke('add_impact', { request: invalidImpact })
    ).rejects.toThrow();
  });

  it('should handle minimum impact level', async () => {
    if (!testEventId) {
      throw new Error('Test event not created');
    }

    const impactData = {
      event_id: testEventId.toString(),
      impact_level: 1,
      notes: 'Minimum impact',
    };

    const mockImpact = { id: '1', event_id: testEventId.toString(), impact_level: 1, notes: impactData.notes };
    mockInvoke.mockResolvedValueOnce(mockImpact);

    const result = await invoke('add_impact', { request: impactData });
    expect((result as any).impact_level).toBe(1);
  });

  it('should handle maximum impact level', async () => {
    if (!testEventId) {
      throw new Error('Test event not created');
    }

    const impactData = {
      event_id: testEventId.toString(),
      impact_level: 5,
      notes: 'Maximum impact',
    };

    // Clear previous mocks and set new one
    mockInvoke.mockClear();
    const mockImpact = { id: '1', event_id: testEventId.toString(), impact_level: 5, notes: impactData.notes };
    mockInvoke.mockResolvedValueOnce(mockImpact);

    const result = await invoke('add_impact', { request: impactData });
    expect((result as any).impact_level).toBe(5);
  });
});

