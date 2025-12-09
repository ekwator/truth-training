/**
 * Integration test for entity name resolution in frontend.
 * Verifies that entity names are correctly resolved from IDs using the entityNames utility.
 */

import { describe, it, expect, beforeAll } from '@jest/globals';
import { invoke } from '@tauri-apps/api/core';
import {
  fetchEntityNames,
  resolveEventEntityNames,
  resolveEventsEntityNames,
  clearEntityNamesCache,
} from '@/utils/entityNames';

describe('Entity Name Resolution Integration', () => {
  beforeAll(async () => {
    // Clear cache to ensure fresh fetch
    clearEntityNamesCache();
  });

  it('should fetch entity names for all types', async () => {
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
    
    const start1 = Date.now();
    const cache1 = await fetchEntityNames();
    const time1 = Date.now() - start1;
    
    const start2 = Date.now();
    const cache2 = await fetchEntityNames();
    const time2 = Date.now() - start2;
    
    // Second call should be faster (cached)
    expect(time2).toBeLessThan(time1);
    expect(cache1.lastUpdated).toBe(cache2.lastUpdated);
  });

  it('should resolve entity names for event', async () => {
    const cache = await fetchEntityNames();
    
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
    const cache = await fetchEntityNames();
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

