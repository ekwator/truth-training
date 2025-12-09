/**
 * Integration test for refactored impacts commands using core storage functions.
 * Verifies that impacts commands correctly use core::storage::add_impact().
 */

import { describe, it, expect, beforeAll } from '@jest/globals';
import { invoke } from '@tauri-apps/api/core';

describe('Storage Refactor - Impacts Commands Integration', () => {
  let testEventId: number | null = null;

  beforeAll(async () => {
    // Create a test event for impact testing
    const eventData = {
      description: 'Test Event for Impact',
      category_id: null,
      forma_id: null,
      cause_id: null,
      develop_id: null,
      effect_id: null,
      vector: false,
    };
    const created = await invoke('create_event_fast', { request: eventData });
    testEventId = (created as any).id;
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

    const result = await invoke('add_impact', { request: impactData });
    expect((result as any).impact_level).toBe(5);
  });
});

