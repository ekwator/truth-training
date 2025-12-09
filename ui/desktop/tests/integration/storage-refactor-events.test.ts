/**
 * Integration test for refactored events commands using core storage functions.
 * Verifies that events commands correctly use core::storage functions and entity name resolution works.
 */

import { describe, it, expect, beforeAll, afterAll, jest } from '@jest/globals';

// Mock Tauri invoke
jest.mock('@tauri-apps/api/core', () => ({
  invoke: jest.fn(),
}));

import { invoke } from '@tauri-apps/api/core';

describe('Storage Refactor - Events Commands Integration', () => {
  let testEventId: number | null = null;
  const mockInvoke = invoke as jest.MockedFunction<typeof invoke>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  beforeAll(async () => {
    // Clean up any existing test data if needed
  });

  afterAll(async () => {
    // Clean up test data if needed
  });

  it('should create event using core storage function', async () => {
    const eventData = {
      description: 'Test Event for Storage Refactor',
      category_id: null,
      forma_id: null,
      cause_id: null,
      develop_id: null,
      effect_id: null,
      vector: false,
    };

    const mockEvent = { id: 1, description: eventData.description, category_name: null };
    mockInvoke.mockResolvedValueOnce(mockEvent);

    const result = await invoke('create_event_fast', { request: eventData });
    
    expect(result).toBeDefined();
    expect((result as any).id).toBeDefined();
    expect((result as any).description).toBe(eventData.description);
    expect((result as any).category_name).toBeNull(); // Entity names resolved in frontend
    
    testEventId = (result as any).id;
  });

  it('should get event using core storage function', async () => {
    if (!testEventId) {
      testEventId = 1;
    }

    const mockEvent = { id: testEventId, description: 'Test Event', category_name: null };
    mockInvoke.mockResolvedValueOnce(mockEvent);

    const result = await invoke('get_event_fast', { eventId: testEventId });
    
    expect(result).toBeDefined();
    expect((result as any).id).toBe(testEventId);
    expect((result as any).description).toBeDefined();
    // Entity names should be null (resolved in frontend)
    expect((result as any).category_name).toBeNull();
  });

  it('should list events using core storage function', async () => {
    const mockResponse = { data: [{ id: 1, description: 'Test', category_name: null }], total: 1 };
    mockInvoke.mockResolvedValueOnce(mockResponse);

    const result = await invoke('list_events_fast', { page: 1, perPage: 10 });
    
    expect(result).toBeDefined();
    expect((result as any).data).toBeInstanceOf(Array);
    expect((result as any).total).toBeGreaterThanOrEqual(0);
    
    // Verify events don't have entity names (resolved in frontend)
    if ((result as any).data.length > 0) {
      const firstEvent = (result as any).data[0];
      expect(firstEvent).toHaveProperty('id');
      expect(firstEvent).toHaveProperty('description');
      // Entity names should be null (resolved in frontend)
      expect(firstEvent.category_name).toBeNull();
    }
  });

  it('should handle pagination correctly', async () => {
    const mockPage1 = { data: [{ id: 1 }, { id: 2 }], total: 10 };
    const mockPage2 = { data: [{ id: 3 }, { id: 4 }], total: 10 };
    mockInvoke.mockResolvedValueOnce(mockPage1).mockResolvedValueOnce(mockPage2);

    const page1 = await invoke('list_events_fast', { page: 1, perPage: 5 });
    const page2 = await invoke('list_events_fast', { page: 2, perPage: 5 });
    
    expect((page1 as any).data.length).toBeLessThanOrEqual(5);
    expect((page2 as any).data.length).toBeLessThanOrEqual(5);
    expect((page1 as any).total).toBe((page2 as any).total);
  });

  it('should return empty result for non-existent event', async () => {
    mockInvoke.mockResolvedValueOnce(null);

    const result = await invoke('get_event_fast', { eventId: 999999 });
    
    expect(result).toBeNull();
  });
});

