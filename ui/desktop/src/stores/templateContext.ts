import { create } from 'zustand';

/**
 * Template context store for passing template context fields between screens.
 * Used when selecting a template for event creation.
 * Matches Android StateFlow pattern for template context passing.
 */
export interface TemplateContext {
  categoryId: number | null;
  formaId: number | null;
  causeId: number | null;
  developId: number | null;
  effectId: number | null;
}

interface TemplateContextState {
  selectedTemplateContext: TemplateContext | null;
  setSelectedTemplateContext: (context: TemplateContext | null) => void;
}

export const useTemplateContextStore = create<TemplateContextState>((set) => ({
  selectedTemplateContext: null,
  setSelectedTemplateContext: (context) => set({ selectedTemplateContext: context }),
}));

