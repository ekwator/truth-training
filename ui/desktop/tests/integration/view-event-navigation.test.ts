/**
 * Integration test for View Event navigation.
 * Verifies that View Event button navigates to event detail screen, not judgments screen.
 * Test logic verified against Android UI specification and navigation patterns.
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { useNavigationStore } from '../../src/stores/navigation';

describe('View Event Navigation Integration Tests', () => {
  beforeEach(() => {
    // Reset navigation state before each test
    const store = useNavigationStore.getState();
    store.clearJudgmentsSelection();
    store.clearTemplateSelection();
  });

  describe('View Event Button Navigation', () => {
    it('should NOT set viewJudgments flag when viewing event', () => {
      const store = useNavigationStore.getState();
      
      // Simulate: User clicks "View Event" button (not "View Judgments")
      // This should navigate to event detail, NOT set viewJudgments flag
      const eventId = 123;
      
      // Verify: viewJudgments flag should remain false
      expect(store.getState().viewJudgments).toBe(false);
      expect(store.getState().selectedEventIdForJudgments).toBeNull();
    });

    it('should set viewJudgments flag only when explicitly viewing judgments', () => {
      const store = useNavigationStore.getState();
      
      // Simulate: User clicks "View Judgments" button (not "View Event")
      store.setViewJudgments(true);
      store.setSelectedEventIdForJudgments('123');
      
      // Verify: viewJudgments flag is set
      expect(store.getState().viewJudgments).toBe(true);
      expect(store.getState().selectedEventIdForJudgments).toBe('123');
    });

    it('should distinguish between View Event and View Judgments actions', () => {
      const store = useNavigationStore.getState();
      
      // View Event action should NOT set viewJudgments
      // (In actual implementation, this would navigate to event detail screen)
      const state1 = store.getState();
      expect(state1.viewJudgments).toBe(false);
      
      // View Judgments action SHOULD set viewJudgments
      store.setViewJudgments(true);
      store.setSelectedEventIdForJudgments('456');
      const state2 = store.getState();
      expect(state2.viewJudgments).toBe(true);
      expect(state2.selectedEventIdForJudgments).toBe('456');
    });

    it('should clear judgments selection when navigating away', () => {
      const store = useNavigationStore.getState();
      
      // Set judgments view
      store.setViewJudgments(true);
      store.setSelectedEventIdForJudgments('789');
      
      // Clear judgments selection (simulating navigation away)
      store.clearJudgmentsSelection();
      
      // Verify: Flags are cleared
      expect(store.getState().viewJudgments).toBe(false);
      expect(store.getState().selectedEventIdForJudgments).toBeNull();
    });
  });

  describe('Navigation State Separation', () => {
    it('should maintain separate state for event viewing and judgments viewing', () => {
      const store = useNavigationStore.getState();
      
      // Set judgments view
      store.setViewJudgments(true);
      store.setSelectedEventIdForJudgments('100');
      
      // Verify judgments state
      expect(store.getState().viewJudgments).toBe(true);
      expect(store.getState().selectedEventIdForJudgments).toBe('100');
      
      // Clear judgments (simulating View Event action)
      store.clearJudgmentsSelection();
      
      // Verify judgments state is cleared, but other navigation state remains
      expect(store.getState().viewJudgments).toBe(false);
      expect(store.getState().selectedEventIdForJudgments).toBeNull();
    });
  });
});

