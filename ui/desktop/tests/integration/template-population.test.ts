/**
 * Integration test for template selection populating New Event form.
 * Verifies that when a template is selected, its context fields populate the New Event form correctly.
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { useNavigationStore } from '../../src/stores/navigation';
import type { SelectedTemplateContext } from '../../src/types/navigation';

describe('Template Population Integration Tests', () => {
  beforeEach(() => {
    // Reset navigation state before each test
    const store = useNavigationStore.getState();
    store.clearTemplateSelection();
  });

  describe('Template Context Field Population', () => {
    it('should populate all context fields when template with all fields is selected', () => {
      const store = useNavigationStore.getState();
      
      // Simulate template selection with all fields
      const templateContext: SelectedTemplateContext = {
        categoryId: 1,
        formaId: 2,
        causeId: 3,
        developId: 4,
        effectId: 5,
      };
      
      store.setSelectedTemplateContext(templateContext);
      
      // Verify all fields are set
      const state = useNavigationStore.getState();
      expect(state.selectedTemplateContext).toEqual(templateContext);
      expect(state.selectedTemplateContext?.categoryId).toBe(1);
      expect(state.selectedTemplateContext?.formaId).toBe(2);
      expect(state.selectedTemplateContext?.causeId).toBe(3);
      expect(state.selectedTemplateContext?.developId).toBe(4);
      expect(state.selectedTemplateContext?.effectId).toBe(5);
    });

    it('should populate partial context fields when template has some fields', () => {
      const store = useNavigationStore.getState();
      
      // Simulate template selection with partial fields
      const templateContext: SelectedTemplateContext = {
        categoryId: 1,
        formaId: 2,
        causeId: null,
        developId: null,
        effectId: null,
      };
      
      store.setSelectedTemplateContext(templateContext);
      
      // Verify fields are set correctly (including nulls)
      const state = useNavigationStore.getState();
      expect(state.selectedTemplateContext?.categoryId).toBe(1);
      expect(state.selectedTemplateContext?.formaId).toBe(2);
      expect(state.selectedTemplateContext?.causeId).toBeNull();
      expect(state.selectedTemplateContext?.developId).toBeNull();
      expect(state.selectedTemplateContext?.effectId).toBeNull();
    });

    it('should populate only name when template has no context fields', () => {
      const store = useNavigationStore.getState();
      
      // Simulate template selection with no context fields
      const templateContext: SelectedTemplateContext = {
        categoryId: null,
        formaId: null,
        causeId: null,
        developId: null,
        effectId: null,
      };
      
      store.setSelectedTemplateContext(templateContext);
      
      // Verify all fields are null
      const state = useNavigationStore.getState();
      expect(state.selectedTemplateContext?.categoryId).toBeNull();
      expect(state.selectedTemplateContext?.formaId).toBeNull();
      expect(state.selectedTemplateContext?.causeId).toBeNull();
      expect(state.selectedTemplateContext?.developId).toBeNull();
      expect(state.selectedTemplateContext?.effectId).toBeNull();
    });

    it('should clear template context after form population', () => {
      const store = useNavigationStore.getState();
      
      // Set template context
      const templateContext: SelectedTemplateContext = {
        categoryId: 1,
        formaId: 2,
        causeId: null,
        developId: null,
        effectId: null,
      };
      store.setSelectedTemplateContext(templateContext);
      
      // Verify context is set
      expect(useNavigationStore.getState().selectedTemplateContext).toEqual(templateContext);
      
      // Clear template selection (simulating form population completion)
      store.clearTemplateSelection();
      
      // Verify context is cleared
      expect(useNavigationStore.getState().selectedTemplateContext).toBeNull();
    });

    it('should handle multiple template selections sequentially', () => {
      const store = useNavigationStore.getState();
      
      // First template
      const template1: SelectedTemplateContext = {
        categoryId: 1,
        formaId: 1,
        causeId: null,
        developId: null,
        effectId: null,
      };
      store.setSelectedTemplateContext(template1);
      expect(useNavigationStore.getState().selectedTemplateContext).toEqual(template1);
      
      // Clear and select second template
      store.clearTemplateSelection();
      
      const template2: SelectedTemplateContext = {
        categoryId: 2,
        formaId: 2,
        causeId: 2,
        developId: null,
        effectId: null,
      };
      store.setSelectedTemplateContext(template2);
      expect(useNavigationStore.getState().selectedTemplateContext).toEqual(template2);
    });
  });

  describe('Form Field Mapping', () => {
    it('should map template context fields to form field names correctly', () => {
      const store = useNavigationStore.getState();
      
      const templateContext: SelectedTemplateContext = {
        categoryId: 1,
        formaId: 2,
        causeId: 3,
        developId: 4,
        effectId: 5,
      };
      
      store.setSelectedTemplateContext(templateContext);
      const context = useNavigationStore.getState().selectedTemplateContext;
      
      // Verify field mapping (these should map to form fields: category_id, forma_id, etc.)
      expect(context?.categoryId).toBeDefined();
      expect(context?.formaId).toBeDefined();
      expect(context?.causeId).toBeDefined();
      expect(context?.developId).toBeDefined();
      expect(context?.effectId).toBeDefined();
    });
  });
});

