import { create } from 'zustand';

/**
 * Navigation state store for flag-based conditional routing.
 * Matches Android navigation patterns using savedStateHandle flags.
 * Equivalent to Android's savedStateHandle in Navigation Compose.
 */

export interface SelectedTemplateContext {
  categoryId?: number;
  formaId?: number;
  causeId?: number;
  developId?: number;
  effectId?: number;
}

export interface SelectedTemplateForEdit {
  id: number;
  name: string;
  categoryId?: number;
  formaId?: number;
  causeId?: number;
  developId?: number;
  effectId?: number;
  description?: string;
}

export interface NavigationState {
  // Template Selection Flow
  selectTemplateForEvent: boolean;
  selectedTemplateContext: SelectedTemplateContext | null;

  // View Judgments Flow
  viewJudgments: boolean;
  selectedEventIdForJudgments: string | null;

  // Template Creation Flow
  selectedTemplateForEdit: SelectedTemplateForEdit | null;

  // Actions
  setSelectTemplateForEvent: (value: boolean) => void;
  setSelectedTemplateContext: (context: SelectedTemplateContext | null) => void;
  setViewJudgments: (value: boolean) => void;
  setSelectedEventIdForJudgments: (eventId: string | null) => void;
  setSelectedTemplateForEdit: (template: SelectedTemplateForEdit | null) => void;
  clearTemplateSelection: () => void;
  clearJudgmentsSelection: () => void;
  clearTemplateEdit: () => void;
}

export const useNavigationStore = create<NavigationState>((set) => ({
  // Initial state
  selectTemplateForEvent: false,
  selectedTemplateContext: null,
  viewJudgments: false,
  selectedEventIdForJudgments: null,
  selectedTemplateForEdit: null,

  // Actions
  setSelectTemplateForEvent: (value) => set({ selectTemplateForEvent: value }),
  setSelectedTemplateContext: (context) => set({ selectedTemplateContext: context }),
  setViewJudgments: (value) => set({ viewJudgments: value }),
  setSelectedEventIdForJudgments: (eventId) => set({ selectedEventIdForJudgments: eventId }),
  setSelectedTemplateForEdit: (template) => set({ selectedTemplateForEdit: template }),
  clearTemplateSelection: () => set({ 
    selectTemplateForEvent: false, 
    selectedTemplateContext: null 
  }),
  clearJudgmentsSelection: () => set({ 
    viewJudgments: false, 
    selectedEventIdForJudgments: null 
  }),
  clearTemplateEdit: () => set({ selectedTemplateForEdit: null }),
}));

