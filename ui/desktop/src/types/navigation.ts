/**
 * Navigation state types.
 * Matches Android savedStateHandle patterns for flag-based routing.
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

