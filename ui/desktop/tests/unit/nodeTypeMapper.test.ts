/**
 * Unit tests for NodeTypeMapper utility.
 * Tests all mapping functions for node type conversion.
 */

import { describe, it, expect } from '@jest/globals';
import { mapToUserFriendly, isHub, isLeaf, getBothTypes } from '@/utils/nodeTypeMapper';

describe('NodeTypeMapper', () => {
  describe('mapToUserFriendly', () => {
    it('should map RELAY to Hub', () => {
      expect(mapToUserFriendly('RELAY')).toBe('Hub');
      expect(mapToUserFriendly('relay')).toBe('Hub');
      expect(mapToUserFriendly('Relay')).toBe('Hub');
    });

    it('should map GLOBAL to Hub', () => {
      expect(mapToUserFriendly('GLOBAL')).toBe('Hub');
      expect(mapToUserFriendly('global')).toBe('Hub');
      expect(mapToUserFriendly('Global')).toBe('Hub');
    });

    it('should map LAN to Leaf', () => {
      expect(mapToUserFriendly('LAN')).toBe('Leaf');
      expect(mapToUserFriendly('lan')).toBe('Leaf');
      expect(mapToUserFriendly('Lan')).toBe('Leaf');
    });

    it('should map WIFI to Leaf', () => {
      expect(mapToUserFriendly('WIFI')).toBe('Leaf');
      expect(mapToUserFriendly('wifi')).toBe('Leaf');
      expect(mapToUserFriendly('WiFi')).toBe('Leaf');
    });

    it('should map CLIENT to Leaf', () => {
      expect(mapToUserFriendly('CLIENT')).toBe('Leaf');
      expect(mapToUserFriendly('client')).toBe('Leaf');
      expect(mapToUserFriendly('Client')).toBe('Leaf');
    });

    it('should return Unknown for unknown types', () => {
      expect(mapToUserFriendly('UNKNOWN')).toBe('Unknown');
      expect(mapToUserFriendly('')).toBe('Unknown');
      expect(mapToUserFriendly(null)).toBe('Unknown');
      expect(mapToUserFriendly(undefined)).toBe('Unknown');
    });
  });

  describe('isHub', () => {
    it('should return true for RELAY', () => {
      expect(isHub('RELAY')).toBe(true);
      expect(isHub('relay')).toBe(true);
    });

    it('should return true for GLOBAL', () => {
      expect(isHub('GLOBAL')).toBe(true);
      expect(isHub('global')).toBe(true);
    });

    it('should return false for Leaf types', () => {
      expect(isHub('LAN')).toBe(false);
      expect(isHub('WIFI')).toBe(false);
      expect(isHub('CLIENT')).toBe(false);
    });

    it('should return false for unknown types', () => {
      expect(isHub('UNKNOWN')).toBe(false);
      expect(isHub(null)).toBe(false);
      expect(isHub(undefined)).toBe(false);
    });
  });

  describe('isLeaf', () => {
    it('should return true for LAN', () => {
      expect(isLeaf('LAN')).toBe(true);
      expect(isLeaf('lan')).toBe(true);
    });

    it('should return true for WIFI', () => {
      expect(isLeaf('WIFI')).toBe(true);
      expect(isLeaf('wifi')).toBe(true);
    });

    it('should return true for CLIENT', () => {
      expect(isLeaf('CLIENT')).toBe(true);
      expect(isLeaf('client')).toBe(true);
    });

    it('should return false for Hub types', () => {
      expect(isLeaf('RELAY')).toBe(false);
      expect(isLeaf('GLOBAL')).toBe(false);
    });

    it('should return false for unknown types', () => {
      expect(isLeaf('UNKNOWN')).toBe(false);
      expect(isLeaf(null)).toBe(false);
      expect(isLeaf(undefined)).toBe(false);
    });
  });

  describe('getBothTypes', () => {
    it('should return both user-friendly and technical types for RELAY', () => {
      const result = getBothTypes('RELAY');
      expect(result.userFriendly).toBe('Hub');
      expect(result.technical).toBe('RELAY');
    });

    it('should return both user-friendly and technical types for LAN', () => {
      const result = getBothTypes('LAN');
      expect(result.userFriendly).toBe('Leaf');
      expect(result.technical).toBe('LAN');
    });

    it('should return Unknown for both when type is null', () => {
      const result = getBothTypes(null);
      expect(result.userFriendly).toBe('Unknown');
      expect(result.technical).toBe('Unknown');
    });

    it('should return Unknown for both when type is undefined', () => {
      const result = getBothTypes(undefined);
      expect(result.userFriendly).toBe('Unknown');
      expect(result.technical).toBe('Unknown');
    });

    it('should preserve original technical type even if unknown', () => {
      const result = getBothTypes('CUSTOM_TYPE');
      expect(result.userFriendly).toBe('Unknown');
      expect(result.technical).toBe('CUSTOM_TYPE');
    });
  });

  describe('Integration: Consistency checks', () => {
    it('should maintain consistency between isHub and isLeaf', () => {
      const testTypes = ['RELAY', 'GLOBAL', 'LAN', 'WIFI', 'CLIENT', 'UNKNOWN', null, undefined];
      
      testTypes.forEach(type => {
        const hubResult = isHub(type);
        const leafResult = isLeaf(type);
        const userFriendly = mapToUserFriendly(type);
        
        // Hub types should not be Leaf
        if (hubResult) {
          expect(leafResult).toBe(false);
          expect(userFriendly).toBe('Hub');
        }
        
        // Leaf types should not be Hub
        if (leafResult) {
          expect(hubResult).toBe(false);
          expect(userFriendly).toBe('Leaf');
        }
      });
    });
  });
});

