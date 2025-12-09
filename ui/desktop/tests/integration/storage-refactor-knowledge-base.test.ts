/**
 * Integration test for refactored knowledge base commands using core storage functions.
 * Verifies that knowledge base commands correctly use core::storage functions.
 */

import { describe, it, expect } from '@jest/globals';
import { invoke } from '@tauri-apps/api/core';

describe('Storage Refactor - Knowledge Base Commands Integration', () => {
  it('should get entity names for all entity types', async () => {
    const entityTypes = ['category', 'forma', 'cause', 'develop', 'effect'];
    
    for (const entityType of entityTypes) {
      const result = await invoke('get_entity_names', { entityType });
      
      expect(result).toBeInstanceOf(Array);
      const entities = result as Array<{ id: number; name: string }>;
      
      if (entities.length > 0) {
        expect(entities[0]).toHaveProperty('id');
        expect(entities[0]).toHaveProperty('name');
        expect(typeof entities[0].id).toBe('number');
        expect(typeof entities[0].name).toBe('string');
      }
    }
  });

  it('should return error for invalid entity type', async () => {
    await expect(
      invoke('get_entity_names', { entityType: 'invalid_type' })
    ).rejects.toThrow();
  });

  it('should reseed knowledge base using core function', async () => {
    const result = await invoke('reseed_knowledge_base');
    
    expect(result).toBeDefined();
    expect((result as any).success).toBe(true);
    expect((result as any).message).toContain('successfully');
    expect((result as any).tables_updated).toBeInstanceOf(Array);
    expect((result as any).tables_updated.length).toBeGreaterThan(0);
  });

  it('should list contexts using core function', async () => {
    const result = await invoke('list_contexts');
    
    expect(result).toBeDefined();
    expect((result as any).data).toBeInstanceOf(Array);
    expect((result as any).fetched_at).toBeDefined();
    
    if ((result as any).data.length > 0) {
      const context = (result as any).data[0];
      expect(context).toHaveProperty('id');
      expect(context).toHaveProperty('name');
    }
  });
});

