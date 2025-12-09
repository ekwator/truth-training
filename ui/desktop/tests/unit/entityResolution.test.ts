/**
 * Unit tests for entity resolution algorithm.
 * Verifies against Android entity resolution algorithm.
 */

import { describe, it, expect } from '@jest/globals';
import { getEntityNameById, resolveContextFieldName } from '@/utils/entityResolution';

describe('Entity Resolution Algorithm Tests', () => {
  describe('getEntityNameById', () => {
    const mockEntities = [
      { id: 1, name: 'Category A' },
      { id: 2, name: 'Category B' },
      { id: 3, name: 'Category C' },
    ];

    it('should return entity name when found', () => {
      const result = getEntityNameById(
        1,
        mockEntities,
        (e) => e.id,
        (e) => e.name
      );
      
      expect(result).toBe('Category A');
    });

    it('should return ID as string when entity not found', () => {
      const result = getEntityNameById(
        999,
        mockEntities,
        (e) => e.id,
        (e) => e.name
      );
      
      expect(result).toBe('999');
    });

    it('should return null when id is null', () => {
      const result = getEntityNameById(
        null,
        mockEntities,
        (e) => e.id,
        (e) => e.name
      );
      
      expect(result).toBeNull();
    });

    it('should return ID as string when entities array is empty', () => {
      const result = getEntityNameById(
        1,
        [],
        (e) => e.id,
        (e) => e.name
      );
      
      expect(result).toBe('1');
    });

    it('should handle entities with different structure', () => {
      const customEntities = [
        { customId: 10, customName: 'Custom Entity' },
        { customId: 20, customName: 'Another Entity' },
      ];
      
      const result = getEntityNameById(
        10,
        customEntities,
        (e) => e.customId,
        (e) => e.customName
      );
      
      expect(result).toBe('Custom Entity');
    });

    it('should return first matching entity when multiple entities have same ID', () => {
      const duplicateEntities = [
        { id: 1, name: 'First' },
        { id: 1, name: 'Second' },
      ];
      
      const result = getEntityNameById(
        1,
        duplicateEntities,
        (e) => e.id,
        (e) => e.name
      );
      
      expect(result).toBe('First');
    });
  });

  describe('resolveContextFieldName', () => {
    const mockEntities = [
      { id: 1, name: 'Category A' },
      { id: 2, name: 'Category B' },
      { id: 3, name: 'Forma X' },
    ];

    it('should resolve category field name', () => {
      const result = resolveContextFieldName('category', 1, mockEntities);
      expect(result).toBe('Category A');
    });

    it('should resolve forma field name', () => {
      const result = resolveContextFieldName('forma', 3, mockEntities);
      expect(result).toBe('Forma X');
    });

    it('should resolve cause field name', () => {
      const result = resolveContextFieldName('cause', 2, mockEntities);
      expect(result).toBe('Category B');
    });

    it('should resolve develop field name', () => {
      const result = resolveContextFieldName('develop', 1, mockEntities);
      expect(result).toBe('Category A');
    });

    it('should resolve effect field name', () => {
      const result = resolveContextFieldName('effect', 2, mockEntities);
      expect(result).toBe('Category B');
    });

    it('should return ID as string when entity not found', () => {
      const result = resolveContextFieldName('category', 999, mockEntities);
      expect(result).toBe('999');
    });

    it('should handle empty entities array', () => {
      const result = resolveContextFieldName('category', 1, []);
      expect(result).toBe('1');
    });
  });
});

