/**
 * Contract test for navigation state store.
 * Verifies navigation state store matches Android savedStateHandle patterns.
 * Test logic verified against Android UI specification and navigation-state.md contract.
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { useNavigationStore } from '../../src/stores/navigation';
import type { NavigationState, SelectedTemplateContext, SelectedTemplateForEdit } from '../../src/types/navigation';

describe('Navigation State Store Contract Tests', () => {
  beforeEach(() => {
    // Reset store to initial state before each test
    const store = useNavigationStore.getState();
    store.clearTemplateSelection();
    store.clearJudgmentsSelection();
    store.clearTemplateEdit();
  });

  describe('Initial State', () => {
    it('should have correct initial state matching Android savedStateHandle pattern', () => {
      const state = useNavigationStore.getState();

      // Template Selection Flow - initial state
      expect(state.selectTemplateForEvent).toBe(false);
      expect(state.selectedTemplateContext).toBeNull();

      // View Judgments Flow - initial state
      expect(state.viewJudgments).toBe(false);
      expect(state.selectedEventIdForJudgments).toBeNull();

      // Template Creation Flow - initial state
      expect(state.selectedTemplateForEdit).toBeNull();
    });

    it('should have all required actions defined', () => {
      const state = useNavigationStore.getState();

      expect(typeof state.setSelectTemplateForEvent).toBe('function');
      expect(typeof state.setSelectedTemplateContext).toBe('function');
      expect(typeof state.setViewJudgments).toBe('function');
      expect(typeof state.setSelectedEventIdForJudgments).toBe('function');
      expect(typeof state.setSelectedTemplateForEdit).toBe('function');
      expect(typeof state.clearTemplateSelection).toBe('function');
      expect(typeof state.clearJudgmentsSelection).toBe('function');
      expect(typeof state.clearTemplateEdit).toBe('function');
    });
  });

  describe('Template Selection Flow', () => {
    it('should set selectTemplateForEvent flag', () => {
      const store = useNavigationStore.getState();
      store.setSelectTemplateForEvent(true);

      expect(useNavigationStore.getState().selectTemplateForEvent).toBe(true);
    });

    it('should set selectedTemplateContext with all context fields', () => {
      const store = useNavigationStore.getState();
      const context: SelectedTemplateContext = {
        categoryId: 1,
        formaId: 2,
        causeId: 3,
        developId: 4,
        effectId: 5,
      };

      store.setSelectedTemplateContext(context);

      const state = useNavigationStore.getState();
      expect(state.selectedTemplateContext).toEqual(context);
      expect(state.selectedTemplateContext?.categoryId).toBe(1);
      expect(state.selectedTemplateContext?.formaId).toBe(2);
      expect(state.selectedTemplateContext?.causeId).toBe(3);
      expect(state.selectedTemplateContext?.developId).toBe(4);
      expect(state.selectedTemplateContext?.effectId).toBe(5);
    });

    it('should set selectedTemplateContext with partial context fields', () => {
      const store = useNavigationStore.getState();
      const context: SelectedTemplateContext = {
        categoryId: 1,
        formaId: 2,
      };

      store.setSelectedTemplateContext(context);

      const state = useNavigationStore.getState();
      expect(state.selectedTemplateContext).toEqual(context);
      expect(state.selectedTemplateContext?.categoryId).toBe(1);
      expect(state.selectedTemplateContext?.formaId).toBe(2);
      expect(state.selectedTemplateContext?.causeId).toBeUndefined();
    });

    it('should clear template selection (flag and context)', () => {
      const store = useNavigationStore.getState();
      store.setSelectTemplateForEvent(true);
      store.setSelectedTemplateContext({ categoryId: 1 });
      store.clearTemplateSelection();

      const state = useNavigationStore.getState();
      expect(state.selectTemplateForEvent).toBe(false);
      expect(state.selectedTemplateContext).toBeNull();
    });

    it('should persist template selection state across multiple state updates', () => {
      const store = useNavigationStore.getState();
      store.setSelectTemplateForEvent(true);
      store.setSelectedTemplateContext({ categoryId: 1 });

      // Simulate navigation and state updates
      store.setSelectTemplateForEvent(false);
      store.setSelectTemplateForEvent(true);

      const state = useNavigationStore.getState();
      expect(state.selectTemplateForEvent).toBe(true);
      expect(state.selectedTemplateContext?.categoryId).toBe(1);
    });
  });

  describe('View Judgments Flow', () => {
    it('should set viewJudgments flag', () => {
      const store = useNavigationStore.getState();
      store.setViewJudgments(true);

      expect(useNavigationStore.getState().viewJudgments).toBe(true);
    });

    it('should set selectedEventIdForJudgments', () => {
      const store = useNavigationStore.getState();
      const eventId = 'event-123';

      store.setSelectedEventIdForJudgments(eventId);

      const state = useNavigationStore.getState();
      expect(state.selectedEventIdForJudgments).toBe(eventId);
    });

    it('should clear judgments selection (flag and event ID)', () => {
      const store = useNavigationStore.getState();
      store.setViewJudgments(true);
      store.setSelectedEventIdForJudgments('event-123');
      store.clearJudgmentsSelection();

      const state = useNavigationStore.getState();
      expect(state.viewJudgments).toBe(false);
      expect(state.selectedEventIdForJudgments).toBeNull();
    });

    it('should persist judgments selection state across navigation', () => {
      const store = useNavigationStore.getState();
      store.setViewJudgments(true);
      store.setSelectedEventIdForJudgments('event-123');

      // Simulate navigation
      store.setViewJudgments(false);
      store.setViewJudgments(true);

      const state = useNavigationStore.getState();
      expect(state.viewJudgments).toBe(true);
      expect(state.selectedEventIdForJudgments).toBe('event-123');
    });
  });

  describe('Template Creation Flow', () => {
    it('should set selectedTemplateForEdit with full template data', () => {
      const store = useNavigationStore.getState();
      const template: SelectedTemplateForEdit = {
        id: 1,
        name: 'Test Template',
        categoryId: 1,
        formaId: 2,
        causeId: 3,
        developId: 4,
        effectId: 5,
        description: 'Test description',
      };

      store.setSelectedTemplateForEdit(template);

      const state = useNavigationStore.getState();
      expect(state.selectedTemplateForEdit).toEqual(template);
      expect(state.selectedTemplateForEdit?.id).toBe(1);
      expect(state.selectedTemplateForEdit?.name).toBe('Test Template');
    });

    it('should set selectedTemplateForEdit with partial template data', () => {
      const store = useNavigationStore.getState();
      const template: SelectedTemplateForEdit = {
        id: 1,
        name: 'Test Template',
      };

      store.setSelectedTemplateForEdit(template);

      const state = useNavigationStore.getState();
      expect(state.selectedTemplateForEdit).toEqual(template);
      expect(state.selectedTemplateForEdit?.categoryId).toBeUndefined();
    });

    it('should clear template edit', () => {
      const store = useNavigationStore.getState();
      store.setSelectedTemplateForEdit({
        id: 1,
        name: 'Test Template',
      });
      store.clearTemplateEdit();

      const state = useNavigationStore.getState();
      expect(state.selectedTemplateForEdit).toBeNull();
    });
  });

  describe('Flag Persistence (Android savedStateHandle equivalent)', () => {
    it('should persist flags until explicitly cleared', () => {
      const store = useNavigationStore.getState();
      store.setSelectTemplateForEvent(true);
      store.setViewJudgments(true);
      store.setSelectedEventIdForJudgments('event-123');

      // Multiple state reads should return same values
      expect(useNavigationStore.getState().selectTemplateForEvent).toBe(true);
      expect(useNavigationStore.getState().viewJudgments).toBe(true);
      expect(useNavigationStore.getState().selectedEventIdForJudgments).toBe('event-123');
    });

    it('should allow independent flag management', () => {
      const store = useNavigationStore.getState();
      store.setSelectTemplateForEvent(true);
      store.setViewJudgments(true);

      // Clear only template selection
      store.clearTemplateSelection();

      const state = useNavigationStore.getState();
      expect(state.selectTemplateForEvent).toBe(false);
      expect(state.viewJudgments).toBe(true); // Still set
    });
  });

  describe('State Observation Pattern (React Hook equivalent)', () => {
    it('should support Zustand selector pattern for state observation', () => {
      const store = useNavigationStore.getState();
      store.setSelectTemplateForEvent(true);

      // Simulate React hook selector
      const selectTemplateForEvent = useNavigationStore.getState().selectTemplateForEvent;
      expect(selectTemplateForEvent).toBe(true);
    });

    it('should support multiple independent selectors', () => {
      const store = useNavigationStore.getState();
      store.setSelectTemplateForEvent(true);
      store.setViewJudgments(true);

      const selectTemplate = useNavigationStore.getState().selectTemplateForEvent;
      const viewJudgments = useNavigationStore.getState().viewJudgments;

      expect(selectTemplate).toBe(true);
      expect(viewJudgments).toBe(true);
    });
  });
});

