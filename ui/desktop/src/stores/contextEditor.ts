import { create } from 'zustand';

interface ContextEditorPrefillData {
  category_id?: number;
  forma_id?: number;
  cause_id?: number;
  develop_id?: number;
  effect_id?: number;
  name?: string;
}

interface ContextEditorStore {
  prefilledData: ContextEditorPrefillData | null;
  setPrefilledData: (data: ContextEditorPrefillData | null) => void;
}

export const useContextEditorStore = create<ContextEditorStore>((set) => ({
  prefilledData: null,
  setPrefilledData: (data) => set({ prefilledData: data }),
}));

