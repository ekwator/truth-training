/**
 * Contract test for emoji mapping system.
 * Verifies emoji mapping matches constitutional requirement Rule 8.
 */

import { describe, it, expect } from '@jest/globals';
import { defaultEmojiMapping, getEmoji, EmojiMapping } from '../../src/utils/emojiMapping';

describe('Emoji Mapping System Contract Tests', () => {
  describe('Constitutional Requirement Rule 8 Compliance', () => {
    it('should have emoji mapping for all screen types', () => {
      const screens = defaultEmojiMapping.screens;
      expect(screens.dashboard).toBeTruthy();
      expect(screens.newEvent).toBeTruthy();
      expect(screens.contextEditor).toBeTruthy();
      expect(screens.events).toBeTruthy();
      expect(screens.judgments).toBeTruthy();
      expect(screens.overallSummary).toBeTruthy();
      expect(screens.trainingResults).toBeTruthy();
      expect(screens.settings).toBeTruthy();
    });

    it('should have emoji mapping for all action types', () => {
      const actions = defaultEmojiMapping.actions;
      expect(actions.save).toBeTruthy();
      expect(actions.cancel).toBeTruthy();
      expect(actions.delete).toBeTruthy();
      expect(actions.edit).toBeTruthy();
      expect(actions.create).toBeTruthy();
      expect(actions.submit).toBeTruthy();
      expect(actions.refresh).toBeTruthy();
      expect(actions.sync).toBeTruthy();
      expect(actions.back).toBeTruthy();
      expect(actions.next).toBeTruthy();
    });

    it('should have emoji mapping for all field types', () => {
      const fields = defaultEmojiMapping.fields;
      expect(fields.name).toBeTruthy();
      expect(fields.description).toBeTruthy();
      expect(fields.category).toBeTruthy();
      expect(fields.forma).toBeTruthy();
      expect(fields.cause).toBeTruthy();
      expect(fields.develop).toBeTruthy();
      expect(fields.effect).toBeTruthy();
      expect(fields.startDate).toBeTruthy();
      expect(fields.endDate).toBeTruthy();
      expect(fields.assessment).toBeTruthy();
      expect(fields.confidence).toBeTruthy();
      expect(fields.reasoning).toBeTruthy();
    });

    it('should have emoji mapping for all status types', () => {
      const status = defaultEmojiMapping.status;
      expect(status.online).toBeTruthy();
      expect(status.offline).toBeTruthy();
      expect(status.syncing).toBeTruthy();
      expect(status.error).toBeTruthy();
      expect(status.success).toBeTruthy();
      expect(status.warning).toBeTruthy();
    });

    it('should have emoji mapping for all navigation types', () => {
      const navigation = defaultEmojiMapping.navigation;
      expect(navigation.home).toBeTruthy();
      expect(navigation.events).toBeTruthy();
      expect(navigation.judgments).toBeTruthy();
      expect(navigation.templates).toBeTruthy();
      expect(navigation.summary).toBeTruthy();
      expect(navigation.training).toBeTruthy();
      expect(navigation.settings).toBeTruthy();
    });

    it('should have semantically meaningful emojis (non-empty strings)', () => {
      const mapping: EmojiMapping = defaultEmojiMapping;
      
      // Check all screens
      Object.values(mapping.screens).forEach((emoji) => {
        expect(emoji).toBeTruthy();
        expect(typeof emoji).toBe('string');
        expect(emoji.length).toBeGreaterThan(0);
      });

      // Check all actions
      Object.values(mapping.actions).forEach((emoji) => {
        expect(emoji).toBeTruthy();
        expect(typeof emoji).toBe('string');
        expect(emoji.length).toBeGreaterThan(0);
      });

      // Check all fields
      Object.values(mapping.fields).forEach((emoji) => {
        expect(emoji).toBeTruthy();
        expect(typeof emoji).toBe('string');
        expect(emoji.length).toBeGreaterThan(0);
      });

      // Check all status
      Object.values(mapping.status).forEach((emoji) => {
        expect(emoji).toBeTruthy();
        expect(typeof emoji).toBe('string');
        expect(emoji.length).toBeGreaterThan(0);
      });

      // Check all navigation
      Object.values(mapping.navigation).forEach((emoji) => {
        expect(emoji).toBeTruthy();
        expect(typeof emoji).toBe('string');
        expect(emoji.length).toBeGreaterThan(0);
      });
    });
  });

  describe('getEmoji Function', () => {
    it('should return emoji for valid category and key', () => {
      expect(getEmoji('screens', 'dashboard')).toBe('🏠');
      expect(getEmoji('actions', 'save')).toBe('💾');
      expect(getEmoji('fields', 'name')).toBe('📝');
      expect(getEmoji('status', 'online')).toBe('🟢');
      expect(getEmoji('navigation', 'home')).toBe('🏠');
    });

    it('should return empty string for invalid category', () => {
      expect(getEmoji('invalid' as any, 'dashboard')).toBe('');
    });

    it('should return empty string for invalid key', () => {
      expect(getEmoji('screens', 'invalid')).toBe('');
    });

    it('should return consistent emojis for same function', () => {
      // Same function should have same emoji (consistency requirement)
      const dashboard1 = getEmoji('screens', 'dashboard');
      const dashboard2 = getEmoji('screens', 'dashboard');
      expect(dashboard1).toBe(dashboard2);
      expect(dashboard1).toBe('🏠');
    });
  });

  describe('Emoji Consistency', () => {
    it('should use same emoji for same function across categories', () => {
      // Dashboard screen and home navigation should use same emoji
      const dashboardEmoji = getEmoji('screens', 'dashboard');
      const homeEmoji = getEmoji('navigation', 'home');
      expect(dashboardEmoji).toBe(homeEmoji);
    });

    it('should have consistent emoji patterns for similar actions', () => {
      // Sync and refresh should be related
      const syncEmoji = getEmoji('actions', 'sync');
      const refreshEmoji = getEmoji('actions', 'refresh');
      expect(syncEmoji).toBe(refreshEmoji);
    });
  });
});

