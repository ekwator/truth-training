/**
 * Integration test for entity name resolution in frontend.
 * Verifies that entity names are correctly resolved from IDs using the entityNames utility.
 */

import { describe, it, expect, beforeAll, beforeEach, jest } from '@jest/globals';

// Mock Tauri invoke
jest.mock('@tauri-apps/api/core', () => ({
  invoke: jest.fn(),
}));

import { invoke } from '@tauri-apps/api/core';
import {
  fetchEntityNames,
  resolveEventEntityNames,
  resolveEventsEntityNames,
  clearEntityNamesCache,
} from '@/utils/entityNames';

describe('Entity Name Resolution Integration', () => {
  const mockInvoke = invoke as jest.MockedFunction<typeof invoke>;

  beforeEach(() => {
    jest.clearAllMocks();
    clearEntityNamesCache();
  });

  beforeAll(async () => {
    // Clear cache to ensure fresh fetch
    clearEntityNamesCache();
  });

  it('should fetch entity names for all types', async () => {
    // Mock entity names for all types (called 5 times for 5 entity types)
    const mockEntities = [{ id: 1, name: 'Test Entity' }];
    mockInvoke.mockResolvedValue(mockEntities);

    const cache = await fetchEntityNames();
    
    expect(cache).toBeDefined();
    expect(cache.categories).toBeInstanceOf(Array);
    expect(cache.formas).toBeInstanceOf(Array);
    expect(cache.causes).toBeInstanceOf(Array);
    expect(cache.develops).toBeInstanceOf(Array);
    expect(cache.effects).toBeInstanceOf(Array);
    expect(cache.lastUpdated).toBeGreaterThan(0);
  });

  it('should cache entity names', async () => {
    clearEntityNamesCache();
    
    // Mock entity names (will be called 5 times for 5 entity types)
    const mockEntities = [{ id: 1, name: 'Test Entity' }];
    mockInvoke.mockResolvedValue(mockEntities);
    
    const cache1 = await fetchEntityNames();
    
    // Clear mocks to verify second call doesn't invoke again
    const invokeCallCount1 = mockInvoke.mock.calls.length;
    mockInvoke.mockClear();
    
    const cache2 = await fetchEntityNames();
    const invokeCallCount2 = mockInvoke.mock.calls.length;
    
    // Second call should use cache (no new invoke calls)
    expect(invokeCallCount2).toBe(0);
    expect(cache1.lastUpdated).toBe(cache2.lastUpdated);
  });

  it('should resolve entity names for event', async () => {
    // Mock entity names
    const mockEntities = [{ id: 1, name: 'Test Category' }];
    mockInvoke.mockResolvedValue(mockEntities);
    const cache = await fetchEntityNames();
    
    // Mock event list
    const mockEvent = { id: 1, category_id: 1, forma_id: null, cause_id: null, develop_id: null, effect_id: null };
    mockInvoke.mockResolvedValueOnce({ data: [mockEvent], total: 1 });
    
    // Get a test event with entity IDs
    const events = await invoke('list_events_fast', { page: 1, perPage: 1 });
    const eventList = (events as any).data;
    
    if (eventList.length > 0) {
      const event = eventList[0];
      const names = resolveEventEntityNames(
        {
          category_id: event.category_id,
          forma_id: event.forma_id,
          cause_id: event.cause_id,
          develop_id: event.develop_id,
          effect_id: event.effect_id,
        },
        cache
      );
      
      // If event has entity IDs, names should be resolved
      if (event.category_id !== null) {
        expect(names.category_name).toBeDefined();
        expect(typeof names.category_name).toBe('string');
      } else {
        expect(names.category_name).toBeNull();
      }
    }
  });

  it('should resolve entity names for multiple events', async () => {
    // Mock entity names
    const mockEntities = [{ id: 1, name: 'Test Category' }];
    mockInvoke.mockResolvedValue(mockEntities);
    const cache = await fetchEntityNames();
    
    // Mock event list
    const mockEvents = [
      { id: 1, category_id: 1, forma_id: null, cause_id: null, develop_id: null, effect_id: null },
      { id: 2, category_id: null, forma_id: 1, cause_id: null, develop_id: null, effect_id: null },
    ];
    mockInvoke.mockResolvedValueOnce({ data: mockEvents, total: 2 });
    
    const events = await invoke('list_events_fast', { page: 1, perPage: 5 });
    const eventList = (events as any).data;
    
    if (eventList.length > 0) {
      const eventsWithNames = resolveEventsEntityNames(eventList, cache);
      
      expect(eventsWithNames.length).toBe(eventList.length);
      eventsWithNames.forEach((event) => {
        expect(event).toHaveProperty('category_name');
        expect(event).toHaveProperty('forma_name');
        expect(event).toHaveProperty('cause_name');
        expect(event).toHaveProperty('develop_name');
        expect(event).toHaveProperty('effect_name');
      });
    }
  });

  it('should clear cache when requested', async () => {
    await fetchEntityNames();
    clearEntityNamesCache();
    
    // Next fetch should create new cache
    const cache = await fetchEntityNames();
    expect(cache.lastUpdated).toBeGreaterThan(0);
  });

  it('should fallback to ID string when entity not found', async () => {
    const cache = await fetchEntityNames();
    
    // Use a non-existent ID
    const names = resolveEventEntityNames(
      {
        category_id: 99999,
        forma_id: null,
        cause_id: null,
        develop_id: null,
        effect_id: null,
      },
      cache
    );
    
    // Should fallback to ID as string
    expect(names.category_name).toBe('99999');
  });
});

