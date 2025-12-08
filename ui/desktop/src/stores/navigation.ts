import { create } from 'zustand';

/**
 * Navigation state store for flag-based conditional routing.
 * Matches Android navigation patterns using savedStateHandle flags.
 */
interface NavigationState {
  selectTemplateForEvent: boolean;
  viewJudgments: boolean;
  setSelectTemplateForEvent: (value: boolean) => void;
  setViewJudgments: (value: boolean) => void;
}

export const useNavigationStore = create<NavigationState>((set) => ({
  selectTemplateForEvent: false,
  viewJudgments: false,
  setSelectTemplateForEvent: (value) => set({ selectTemplateForEvent: value }),
  setViewJudgments: (value) => set({ viewJudgments: value }),
}));

